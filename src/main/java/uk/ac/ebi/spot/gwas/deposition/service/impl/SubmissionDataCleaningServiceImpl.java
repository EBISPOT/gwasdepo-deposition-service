package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.ArchivedSubmission;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Note;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.ArchivedSubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.NoteRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.service.*;

import java.util.List;

@Service
public class SubmissionDataCleaningServiceImpl implements SubmissionDataCleaningService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionDataCleaningService.class);

    @Autowired
    private AssociationsService associationsService;

    @Autowired
    private StudiesService studiesService;

    @Autowired
    private SamplesService samplesService;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired(required = false)
    private SumStatsService sumStatsService;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private ArchivedSubmissionRepository archivedSubmissionRepository;

    @Override
    @Async
    public void deleteSubmission(Submission submission) {
        log.info("Deleting submission: {}", submission.getId());
        List<ArchivedSubmission> archivedSubmissions = archivedSubmissionRepository.findBySubmissionId(submission.getId());

        log.info("Found {} archived submissions.", archivedSubmissions.size());
        for (ArchivedSubmission archivedSubmission : archivedSubmissions) {
            log.info("Removing archived submission: {}", archivedSubmission.getId());

            log.info("Removing files: {}", archivedSubmission.getFileUploads());
            for (String fileUploadId : archivedSubmission.getFileUploads()) {
                FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);
                if (fileUpload.getCallbackId() != null && sumStatsService != null) {
                    sumStatsService.cleanUp(fileUpload.getCallbackId());
                }
                fileUploadsService.deleteFileUpload(fileUploadId);
            }

            log.info("Removing {} associations.", archivedSubmission.getAssociations().size());
            associationsService.deleteAssociations(archivedSubmission.getAssociations());

            log.info("Removing {} studies.", archivedSubmission.getStudies().size());
            studiesService.deleteStudies(archivedSubmission.getStudies());

            log.info("Removing {} samples.", archivedSubmission.getSamples().size());
            samplesService.deleteSamples(archivedSubmission.getSamples());

            log.info("Removing {} notes.", archivedSubmission.getNotes().size());
            List<Note> notes = noteRepository.findByIdIn(archivedSubmission.getNotes());
            for (Note note : notes) {
                noteRepository.delete(note);
            }
        }

        submissionRepository.delete(submission);
    }

    @Override
    @Async
    public void cleanSubmission(Submission submission) {
        log.info("Cleaning submission data: {}", submission.getId());

        log.info("Removing associations ...");
        associationsService.deleteAssociations(submission.getId());

        log.info("Removing studies ...");
        studiesService.deleteStudies(submission.getId());

        log.info("Removing samples ...");
        samplesService.deleteSamples(submission.getId());
    }
}
