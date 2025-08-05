package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.ac.ebi.spot.gwas.deposition.domain.PgsValidationInfo;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;

@Getter
@AllArgsConstructor
public class ExtendedFileUploadDto {

    private final FileUploadDto     fileUpload;
    private final PgsValidationInfo pgsValidationInfo;
}