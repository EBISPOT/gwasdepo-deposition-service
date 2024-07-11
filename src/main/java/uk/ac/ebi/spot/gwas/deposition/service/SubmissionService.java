package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;

import java.util.List;

public interface SubmissionService {

    Submission createSubmission(Submission submission);

    Submission getSubmission(String submissionId, User user);

    Submission getSubmission(String publicationId);

    Submission saveSubmission(Submission submission, String userId);

    Page<Submission> getSubmissions(String pmid, String bowId, Pageable page, User user);

    Submission findByBodyOfWork(String bodyOfWorkId, String userId);

    Submission findByBodyOfWork(String bodyOfWorkId);

    void deleteSubmission(String submissionId, User user);

    void deleteSubmissionChildren(String submissionId);

    Submission editFileUploadSubmissionDetails(String submissionId, User user);

    Submission updateSubmissionStatus(String submissionId, String status, User user);

    void deleteSubmissionFile(Submission submission, String fileUploadId, String userId);

    Submission lockSubmission(Submission submission,User user, String status);

    Submission createGlobusFolderForReopenedSubmission(String submissionId, User apiCaller, String globusEmail);

    public List<Study> getStudies(String submissionId);

    boolean validateSnps(String submissionId);
}
