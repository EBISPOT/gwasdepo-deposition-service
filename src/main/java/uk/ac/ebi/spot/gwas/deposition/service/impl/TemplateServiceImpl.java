package uk.ac.ebi.spot.gwas.deposition.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.FileObject;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.SSTemplateCuratorRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.gcst.SSTemplateGCSTRequestDto;
import uk.ac.ebi.spot.gwas.deposition.service.TemplateService;

@Service
@ConditionalOnProperty(name = "gwas-template-service.enabled", havingValue = "true")
public class TemplateServiceImpl extends GatewayService implements TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);

    @Override
    public TemplateSchemaResponseDto retrieveTemplateSchemaInfo(String templateVersion) {
        log.info("Retrieving template schema version: {}", templateVersion);
        String endpoint = restInteractionConfig.getTemplateSchemaEndpoint() + "/" + templateVersion;

        HttpEntity httpEntity = restRequestUtil.httpEntity()
                .build();
        ResponseEntity<TemplateSchemaResponseDto> response =
                restTemplate.exchange(endpoint,
                        HttpMethod.GET, httpEntity,
                        new ParameterizedTypeReference<TemplateSchemaResponseDto>() {
                        });

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            log.info("Schema successfully retrieved: {} | {}", response.getBody().getSchemaVersion(),
                    response.getBody().getSubmissionTypes());
            return response.getBody();
        }

        log.error("Unable to call gwas-template-service: {}", response.getStatusCode());
        return null;
    }

    @Override
    public TemplateSchemaDto retrieveTemplateSchema(String templateVersion, String submissionType) {
        log.info("Retrieving template schema version: {}", templateVersion);
        String endpoint = restInteractionConfig.getTemplateSchemaEndpoint() + "/" + templateVersion + "/" + submissionType.toUpperCase();

        HttpEntity httpEntity = restRequestUtil.httpEntity()
                .build();
        ResponseEntity<TemplateSchemaDto> response =
                restTemplate.exchange(endpoint,
                        HttpMethod.GET, httpEntity,
                        new ParameterizedTypeReference<TemplateSchemaDto>() {
                        });

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            log.info("Schema successfully retrieved: {}", response.getBody().getVersion());
            return response.getBody();
        }

        log.error("Unable to call gwas-template-service: {}", response.getStatusCode());
        return null;
    }

    @Override
    public FileObject retrievePrefilledTemplate(SSTemplateRequestDto ssTemplateRequestDto) {
        try {
            log.info("[{}] Retrieving pre-filled template for {} studies: {}",
                    restInteractionConfig.getPrefilledTemplateEndpoint(),
                    ssTemplateRequestDto.getPrefillData().getStudy().size(),
                    new ObjectMapper().writeValueAsString(ssTemplateRequestDto));
        } catch (Exception e) {
        }
        String endpoint = restInteractionConfig.getPrefilledTemplateEndpoint();

        HttpEntity httpEntity = restRequestUtil.httpEntity()
                .withJsonBodyNoContentType(ssTemplateRequestDto)
                .build();
        try {
            ResponseEntity<byte[]> response =
                    restTemplate.exchange(endpoint,
                            HttpMethod.POST, httpEntity,
                            new ParameterizedTypeReference<byte[]>() {
                            });
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                String fileName = response.getHeaders().getContentDisposition().getFilename();
                byte[] payload = response.getBody();
                log.info("Pre-filled template [{}] successfully retrieved: {}", fileName, payload.length);
                return new FileObject(fileName, payload);
            }
            log.error("Unable to call gwas-template-service: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Unable to call gwas-template-service: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public FileObject retrieveCuratorPrefilledTemplate(SSTemplateCuratorRequestDto ssTemplateCuratorRequestDto) {
        try {
            log.info("[{}] Retrieving pre-filled template for {} studies: {}",
                    restInteractionConfig.getPrefilledTemplateEndpoint(),
                    ssTemplateCuratorRequestDto.getPrefillData().getStudy().size(),
                    new ObjectMapper().writeValueAsString(ssTemplateCuratorRequestDto));
        } catch (Exception e) {
        }
        String endpoint = restInteractionConfig.getPrefilledTemplateEndpoint();

        HttpEntity httpEntity = restRequestUtil.httpEntity()
                .withJsonBodyNoContentType(ssTemplateCuratorRequestDto)
                .build();
        try {
            ResponseEntity<byte[]> response =
                    restTemplate.exchange(endpoint,
                            HttpMethod.POST, httpEntity,
                            new ParameterizedTypeReference<byte[]>() {
                            });
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                String fileName = response.getHeaders().getContentDisposition().getFilename();
                byte[] payload = response.getBody();
                log.info("Pre-filled template [{}] successfully retrieved: {}", fileName, payload.length);
                return new FileObject(fileName, payload);
            }
            log.error("Unable to call gwas-template-service: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Unable to call gwas-template-service: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public FileObject retrieveGCSTPrefilledTemplate(SSTemplateGCSTRequestDto ssTemplateGCSTRequestDto) {
        try {
            log.info("[{}] Retrieving GCST template for {} studies: {}",
                    restInteractionConfig.getPrefilledTemplateEndpoint(),
                    ssTemplateGCSTRequestDto.getPrefillData().getStudy().size(),
                    new ObjectMapper().writeValueAsString(ssTemplateGCSTRequestDto));
        } catch (Exception e) {
        }
        String endpoint = restInteractionConfig.getPrefilledTemplateEndpoint();

        HttpEntity httpEntity = restRequestUtil.httpEntity()
                .withJsonBodyNoContentType(ssTemplateGCSTRequestDto)
                .build();
        try {
            ResponseEntity<byte[]> response =
                    restTemplate.exchange(endpoint,
                            HttpMethod.POST, httpEntity,
                            new ParameterizedTypeReference<byte[]>() {
                            });
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                String fileName = response.getHeaders().getContentDisposition().getFilename();
                byte[] payload = response.getBody();
                log.info("Pre-filled template [{}] successfully retrieved: {}", fileName, payload.length);
                return new FileObject(fileName, payload);
            }
            log.error("Unable to call gwas-template-service: {}", response.getStatusCode());
            return null;
        } catch (Exception e) {
            log.error("Unable to call gwas-template-service: {}", e.getMessage(), e);
            return null;
        }
    }
}
