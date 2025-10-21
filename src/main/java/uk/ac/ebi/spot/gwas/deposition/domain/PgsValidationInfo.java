package uk.ac.ebi.spot.gwas.deposition.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
@Document(collection = "pgsValidationInfos")
public class PgsValidationInfo {

    @Id
    private String id;                // same as fileUploadId
    private String validationStatus;  // e.g. VALID / INVALID
    private List<String> pcstIds;     // generated PCST‑IDs
}