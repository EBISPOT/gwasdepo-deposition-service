package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.ArchivedSubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.CallbackIdRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SummaryStatsEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.service.CuratorAuthService;
import uk.ac.ebi.spot.gwas.deposition.service.FileUploadsService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CuratorAuthService curatorAuthService;

    @Autowired
    private ArchivedSubmissionRepository archivedSubmissionRepository;

    @Autowired
    private CallbackIdRepository callbackIdRepository;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private SummaryStatsEntryRepository summaryStatsEntryRepository;

    @Autowired
    private AuditProxy auditProxy;

    @Override
    public Submission createSubmission(Submission submission) {
        log.info("Creating submission for publication: {}", submission.getPublicationId());
        submission = submissionRepository.insert(submission);
        log.info("Submission created: {}", submission.getId());

        return submission;
    }

    @Override
    public Submission getSubmission(String submissionId, User user) {
        log.info("Retrieving submission: {}", submissionId);
        Optional<Submission> optionalSubmission = curatorAuthService.isCurator(user) ?
                submissionRepository.findByIdAndArchived(submissionId, false) :
                submissionRepository.findByIdAndArchivedAndCreated_UserId(submissionId, false, user.getId());
        if (!optionalSubmission.isPresent()) {
            log.error("Unable to find submission: {}", submissionId);
            throw new EntityNotFoundException("Unable to find submission: " + submissionId);
        }
        log.info("Submission successfully retrieved: {}", optionalSubmission.get().getId());
        return optionalSubmission.get();
    }

    @Override
    public Submission getSubmission(String publicationId) {
        log.info("Retrieving submission for publication: {}", publicationId);

        Optional<Submission> optionalSubmission = submissionRepository.findByPublicationIdAndArchived(publicationId, false);
        if (!optionalSubmission.isPresent()) {
            log.error("Unable to find submission for publication: {}", publicationId);
            return null;
        }

        return optionalSubmission.get();
    }

    @Override
    public Submission saveSubmission(Submission submission) {
        log.info("Saving submission: {}", submission.getId());
        submission = submissionRepository.save(submission);
        return submission;
    }

    @Override
    public Page<Submission> getSubmissions(String publicationId, Pageable page, User user) {
        log.info("Retrieving submissions: {} - {} - {}", page.getPageNumber(), page.getPageSize(), page.getSort().toString());
        if (curatorAuthService.isCurator(user)) {
            return publicationId != null ?
                    submissionRepository.findByPublicationIdAndArchived(publicationId, false, page) :
                    submissionRepository.findByArchived(false, page);
        }

        return publicationId != null ?
                submissionRepository.findByPublicationIdAndArchivedAndCreated_UserId(publicationId, false, user.getId(), page) :
                submissionRepository.findByArchivedAndCreated_UserId(false, user.getId(), page);
    }

    @Override
    public Submission updateSubmissionStatus(String submissionId, String status, User user) {
        log.info("Updating status [{}] for submission : {}", status, submissionId);
        Submission submission = this.getSubmission(submissionId, user);
        submission.setOverallStatus(status);
        if (status.equals(Status.SUBMITTED)) {
            submission.setDateSubmitted(LocalDate.now());
        }
        submission.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
        submission = submissionRepository.save(submission);
        return submission;
    }

    @Override
    public void deleteSubmission(String submissionId, User user) {
        log.info("Deleting submission: {}", submissionId);
        Submission submission = this.getSubmission(submissionId, user);
        for (String fileId : submission.getFileUploads()) {
            FileUpload fileUpload = fileUploadsService.getFileUpload(fileId);
            if (fileUpload.getCallbackId() != null) {
                deleteCallbackId(fileUpload.getCallbackId());
            }
            List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByFileUploadId(fileId);
            summaryStatsEntryRepository.deleteAll(summaryStatsEntries);
        }

        ArchivedSubmission archivedSubmission = ArchivedSubmission.fromSubmission(submission);
        archivedSubmissionRepository.insert(archivedSubmission);
        submission.setArchived(true);
        submission.setDeletedOn(DateTime.now());
        submission.setStudies(new ArrayList<>());
        submission.setAssociations(new ArrayList<>());
        submission.setSamples(new ArrayList<>());
        submission.setNotes(new ArrayList<>());
        submission.setFileUploads(new ArrayList<>());
        submissionRepository.save(submission);
    }

    @Override
    public void deleteSubmissionFile(Submission submission, String fileUploadId, String userId) {
        log.info("Removing file [{}] from submission: {}", fileUploadId, submission.getId());
        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);
        /**
         * TODO: Add this validation later at some point in time
         */
        /*
        if (submission.getType().equals(SubmissionType.SUMMARY_STATS.name())
                && fileUpload.getType().equals(FileUploadType.SUMMARY_STATS_TEMPLATE)) {
            log.error("[{}] Cannot delete the file template associated with a Summary Stats submission: {}",
                    submission.getId(), fileUploadId);
            throw new CannotDeleteSSTemplateFileException("[" + submission.getId() + "] Cannot delete the file template associated with a Summary Stats submission: " + fileUploadId);
        }
        */

        if (fileUpload.getCallbackId() != null) {
            deleteCallbackId(fileUpload.getCallbackId());
        }
        List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByFileUploadId(fileUploadId);
        summaryStatsEntryRepository.deleteAll(summaryStatsEntries);

        ArchivedSubmission archivedSubmission = ArchivedSubmission.fromSubmission(submission);
        archivedSubmissionRepository.insert(archivedSubmission);
        submission.removeFileUpload(fileUploadId);
        submission.setStudies(new ArrayList<>());
        submission.setAssociations(new ArrayList<>());
        submission.setSamples(new ArrayList<>());
        submission.setNotes(new ArrayList<>());
        submission.setOverallStatus(Status.STARTED.name());
        submission.setMetadataStatus(Status.NA.name());
        submission.setSummaryStatsStatus(Status.NA.name());
        submission.setLastUpdated(new Provenance(DateTime.now(), userId));
        submissionRepository.save(submission);
        auditProxy.addAuditEntry(AuditHelper.fileDeleted(userId, fileUpload));
    }

    private void deleteCallbackId(String callbackId) {
        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findByCallbackId(callbackId);
        if (callbackIdOptional.isPresent()) {
            callbackIdRepository.delete(callbackIdOptional.get());
        }
    }
}
