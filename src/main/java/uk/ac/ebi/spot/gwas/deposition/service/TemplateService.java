package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.FileObject;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.SSTemplateCuratorRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.gcst.SSTemplateGCSTRequestDto;

public interface TemplateService {

    TemplateSchemaResponseDto retrieveTemplateSchemaInfo(String templateVersion);

    TemplateSchemaDto retrieveTemplateSchema(String templateVersion, String submissionType);

    FileObject retrievePrefilledTemplate(SSTemplateRequestDto ssTemplateRequestDto);

    FileObject retrieveCuratorPrefilledTemplate(SSTemplateCuratorRequestDto ssTemplateCuratorRequestDto);

    FileObject retrieveGCSTPrefilledTemplate(SSTemplateGCSTRequestDto ssTemplateGCSTRequestDto);
}
