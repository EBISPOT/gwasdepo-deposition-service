package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.FileObject;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;

public interface TemplateService {

    TemplateSchemaResponseDto retrieveTemplateSchemaInfo(String templateVersion);

    TemplateSchemaDto retrieveTemplateSchema(String templateVersion, String submissionType);

    FileObject retrievePrefilledTemplate(SSTemplateRequestDto ssTemplateRequestDto);
}
