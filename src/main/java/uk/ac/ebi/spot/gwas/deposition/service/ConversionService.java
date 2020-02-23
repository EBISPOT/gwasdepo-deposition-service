package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.template.validator.util.StreamSubmissionTemplateReader;

public interface ConversionService {
    void convertData(Submission submission, FileUpload fileUpload, StreamSubmissionTemplateReader streamSubmissionTemplateReader, TemplateSchemaDto schema);
}
