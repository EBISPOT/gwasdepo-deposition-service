package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.components.BodyOfWorkListener;
import uk.ac.ebi.spot.gwas.deposition.constants.BodyOfWorkStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.PublicationStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionProvenanceType;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.*;
import uk.ac.ebi.spot.gwas.deposition.service.*;

import javax.swing.text.html.Option;
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
    private BodyOfWorkListener bodyOfWorkListener;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private AssociationRepository associationRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    PublicationService publicationService;

    @Autowired
    BodyOfWorkService bodyOfWorkService;

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
    public Submission saveSubmission(Submission submission, String userId) {
        log.info("Saving submission: {}", submission.getId());
        submission.setLastUpdated(new Provenance(DateTime.now(), userId));
        submission = submissionRepository.save(submission);
        return submission;
    }

    @Override
    public Page<Submission> getSubmissions(String publicationId, String bowId, Pageable page, User user) {
        log.info("Retrieving submissions: {} - {} - {}", page.getPageNumber(), page.getPageSize(), page.getSort().toString());
        if (curatorAuthService.isCurator(user)) {
            if (publicationId != null) {
                return submissionRepository.findByPublicationIdAndArchived(publicationId, false, page);
            }
            if (bowId != null) {
                return submissionRepository.findByBodyOfWorksContainsAndArchived(bowId, false, page);
            }
            return submissionRepository.findByArchived(false, page);
        }

        if (publicationId != null) {
            return submissionRepository.findByPublicationIdAndArchivedAndCreated_UserId(publicationId, false, user.getId(), page);
        }
        if (bowId != null) {
            return submissionRepository.findByBodyOfWorksContainsAndCreated_UserIdAndArchived(bowId, user.getId(), false, page);
        }
        return submissionRepository.findByArchivedAndCreated_UserId(false, user.getId(), page);
    }

    @Override
    public Submission updateSubmissionStatus(String submissionId, String status, User user) {
        log.info("Updating status [{}] for submission : {}", status, submissionId);
        Submission submission = this.getSubmission(submissionId, user);
        submission.setOverallStatus(status);
        if (status.equals(Status.SUBMITTED)) {
            submission.setDateSubmitted(LocalDate.now());
            if (submission.getProvenanceType().equalsIgnoreCase(SubmissionProvenanceType.BODY_OF_WORK.name())) {
                bodyOfWorkListener.update(submission);
            }
        }
        submission.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
        submission = submissionRepository.save(submission);
        return submission;
    }

    @Override
    public Submission findByBodyOfWork(String bodyOfWorkId, String userId) {
        log.info("Retrieving submission for: {} | {}", bodyOfWorkId, userId);
        Optional<Submission> submissionOptional = submissionRepository.findByBodyOfWorksContainsAndCreated_UserIdAndArchived(bodyOfWorkId, userId, false);
        if (submissionOptional.isPresent()) {
            log.info("Found submission {} for: {} | {}", submissionOptional.get().getId(), bodyOfWorkId, userId);
            return submissionOptional.get();
        }

        log.info("No submission found for: {} | {}", bodyOfWorkId, userId);
        return null;
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
        submission.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
        submissionRepository.save(submission);
    }

    /**
     *
     * @param submissionId
     * @param user
     * Reset the Submission related Child objects for Uploading new template
     */
    public Submission editFileUploadSubmissionDetails(String submissionId, User user) {
        log.info("Updating submission with new File Content: {}", submissionId);
        Submission submission = this.getSubmission(submissionId, user);
        Optional.ofNullable(submission.getFileUploads()).ifPresent((fileUploadIds) -> {
            fileUploadIds.forEach((fileUploadId) -> {
                Optional.ofNullable(fileUploadsService.getFileUpload(fileUploadId)).ifPresent((fileUpload) -> {
                    Optional.ofNullable(fileUpload.getCallbackId()).ifPresent((callbackId) ->
                            deleteCallbackId(callbackId));
                    Optional.ofNullable(summaryStatsEntryRepository.findByFileUploadId(fileUploadId)).ifPresent((sumstats) ->
                            summaryStatsEntryRepository.deleteAll(sumstats));

                });
            });
        });
            submission.setAssociations(new ArrayList<>());
            submission.setSamples(new ArrayList<>());
            submission.setNotes(new ArrayList<>());
            submission.setStudies(new ArrayList<>());
            submission.setFileUploads(new ArrayList<>());
            submission.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
            submission.setEditTemplate(new Provenance(DateTime.now(), user.getId()));
            return saveSubmission(submission, user.getId());

    }

    /**
     *
     * @param submissionId
     * Delete Old submission related child objects
     */
    @Override
    public void deleteSubmissionChildren(String submissionId) {
        log.info("Deleting Old Submission related object for new File Content: {}", submissionId);
        Optional.ofNullable(studyRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((studies) ->  studyRepository.deleteAll(studies));
        log.info("Reached Deleting Assocation Stage");
        Optional.ofNullable(associationRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((associations) -> {
                    try {
                        log.info("Debugging Association Javers data");
                        associations.forEach((association) -> associationRepository.delete(association));
                    //associationRepository.deleteAll(associations);
                }catch(Exception ex){
                    log.error("Exception in Javers Audit associaation"+ ex.getMessage(),ex);
                }

                });

        Optional.ofNullable(sampleRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((samples) ->  sampleRepository.deleteAll(samples));

        Optional.ofNullable(noteRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((notes) ->  noteRepository.deleteAll(notes));

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
    }

    private void deleteCallbackId(String callbackId) {
        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findByCallbackId(callbackId);
        if (callbackIdOptional.isPresent()) {
            callbackIdRepository.delete(callbackIdOptional.get());
        }
    }
}
