package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.PgsValidationInfo;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsStatusDto;

import java.util.List;

public class ExtendedFileUploadDtoAssembler {

    public static ExtendedFileUploadDto assemble(FileUpload fileUpload,
                                                 List<SummaryStatsStatusDto> ssStatuses,
                                                 PgsValidationInfo pvi) {
        FileUploadDto base = new FileUploadDto(
                fileUpload.getId(),
                fileUpload.getStatus(),
                fileUpload.getType(),
                fileUpload.getFileName(),
                fileUpload.getFileSize(),
                fileUpload.getErrors(),
                ssStatuses);

        return new ExtendedFileUploadDto(base, pvi);
    }
}