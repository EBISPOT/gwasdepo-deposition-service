package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.gwas.deposition.domain.*;

import java.util.List;

public interface FileHandlerService {

    void handleSummaryStatsTemplate(Submission submission, Publication publication, User user);

    FileUpload handleMetadataFile(Submission submission, MultipartFile file, User user, List<Study> oldStudies,String appType);

    FileUpload handleSummaryStatsFile(Submission submission, MultipartFile file, User user);
}
