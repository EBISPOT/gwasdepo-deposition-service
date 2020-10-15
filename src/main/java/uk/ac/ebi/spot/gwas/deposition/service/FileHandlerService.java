package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;

public interface FileHandlerService {

    void handleSummaryStatsTemplate(Submission submission, Publication publication, User user);

    FileUpload handleMetadataFile(Submission submission, MultipartFile file, User user);

    FileUpload handleSummaryStatsFile(Submission submission, MultipartFile file, User user);
}
