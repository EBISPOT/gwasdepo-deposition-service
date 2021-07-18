package uk.ac.ebi.spot.gwas.deposition.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.*;
import uk.ac.ebi.spot.gwas.deposition.service.BackendEmailService;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;

@Service
@ConditionalOnProperty(name = "gwas-sumstats-service.enabled", havingValue = "true")
public class SumStatsServiceImpl extends GatewayService implements SumStatsService {

    private static final Logger log = LoggerFactory.getLogger(SumStatsService.class);

    @Autowired
    private BackendEmailService backendEmailService;

    @Override
    public SummaryStatsResponseDto retrieveSummaryStatsStatus(String callbackId) {
        log.info("Retrieving summary stats status for callback ID: {}", callbackId);
        String endpoint = restInteractionConfig.getSumStatsEndpoint() + "/" + callbackId;

        try {
            HttpEntity httpEntity = restRequestUtil.httpEntity()
                    .build();
            ResponseEntity<SummaryStatsResponseDto> response =
                    restTemplate.exchange(endpoint,
                            HttpMethod.GET, httpEntity,
                            new ParameterizedTypeReference<SummaryStatsResponseDto>() {
                            });

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.info(" - Received: {}", new ObjectMapper().writeValueAsString(response.getBody()));

                log.info("Summary stats status successfully retrieved: {}", response.getBody().getCompleted());
                return response.getBody();
            }
        } catch (Exception e) {
            log.error("Unable to call gwas-sumstats-service: {}", e.getMessage(), e);
        }

        return null;
    }

    @Override
    @Async
    public void wrapUpGlobusSubmission(String callbackId, SSWrapUpRequestDto ssWrapUpRequestDto) {
        log.info("Calling SS service to wrap-up Globus submission: {}", callbackId);
        String endpoint = restInteractionConfig.getSumStatsEndpoint() + "/" + callbackId;

        try {
            HttpEntity httpEntity = restRequestUtil.httpEntity()
                    .withJsonBody(ssWrapUpRequestDto)
                    .build();
            ResponseEntity<Void> response =
                    restTemplate.exchange(endpoint,
                            HttpMethod.PUT, httpEntity,
                            new ParameterizedTypeReference<Void>() {
                            });

            if (response.getStatusCode().equals(HttpStatus.OK)) {
                log.info("SS call successfully completed.");
                return;
            }
        } catch (Exception e) {
            log.error("Unable to call gwas-sumstats-service: {}", e.getMessage(), e);
        }
    }

    @Override
    public String registerStatsForProcessing(SummaryStatsRequestDto summaryStatsRequestDto) {
        log.info("Registering summary stats for validation: {}", summaryStatsRequestDto.getRequestEntries().size());
        String endpoint = restInteractionConfig.getSumStatsEndpoint();

        try {
            log.info(" - Sending: {}", new ObjectMapper().writeValueAsString(summaryStatsRequestDto));
            HttpEntity httpEntity = restRequestUtil.httpEntity()
                    .withJsonBody(summaryStatsRequestDto)
                    .build();
            ResponseEntity<SummaryStatsAckDto> response =
                    restTemplate.exchange(endpoint,
                            HttpMethod.POST, httpEntity,
                            new ParameterizedTypeReference<SummaryStatsAckDto>() {
                            });

            if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                log.info("Summary stats status successfully registered: {}", response.getBody().getCallbackID());
                return response.getBody().getCallbackID();
            }
        } catch (Exception e) {
            log.error("Unable to call gwas-sumstats-service: {}", e.getMessage(), e);
        }

        return null;
    }

    @Override
    @Async
    public void cleanUp(String callbackId) {
        log.info("Requesting clean-up for callback ID: {}", callbackId);
        String endpoint = restInteractionConfig.getSumStatsEndpoint() + "/" + callbackId;

        try {
            HttpEntity httpEntity = restRequestUtil.httpEntity()
                    .build();
            restTemplate.exchange(endpoint,
                    HttpMethod.DELETE, httpEntity,
                    new ParameterizedTypeReference<Void>() {
                    });
        } catch (Exception e) {
            log.error("Unable to call gwas-sumstats-service: {}", e.getMessage(), e);
        }
    }

    @Override
    public void deleteGlobusFolder(Submission submission) {
        log.info("Deleting Globus folder ID: {} for Submission ID: {}",
                submission.getGlobusFolderId(), submission.getId());
        String endpoint = restInteractionConfig.getGlobusEndpoint() + "/" + submission.getGlobusFolderId();
        try {
            HttpEntity httpEntity = restRequestUtil.httpEntity()
                    .build();
            restTemplate.exchange(endpoint,
                    HttpMethod.DELETE, httpEntity,
                    new ParameterizedTypeReference<Void>() {
                    });
        }
        catch (Exception e) {
            log.error("Unable to call gwas-sumstats-service: {}", e.getMessage(), e);
        }
    }

    public SSGlobusResponse createGlobusFolder(SSGlobusFolderDto ssGlobusFolderDto) {
        log.info("Requesting Globus folder creation: {} - {}", ssGlobusFolderDto.getUniqueId(), ssGlobusFolderDto.getEmail());

        try {
            HttpEntity httpEntity = restRequestUtil.httpEntity()
                    .withJsonBody(ssGlobusFolderDto)
                    .build();
            ResponseEntity<SSGlobusFolderRequestResponseDto> response = restTemplate.exchange(restInteractionConfig.getGlobusMkdirEndpoint(),
                    HttpMethod.POST, httpEntity,
                    new ParameterizedTypeReference<SSGlobusFolderRequestResponseDto>() {
                    });
            SSGlobusFolderRequestResponseDto responseDto = response.getBody();
            if (response.getStatusCode().equals(HttpStatus.CREATED)) {
                log.info("Globus folder [{}] successfully created for: {}",
                        ssGlobusFolderDto.getUniqueId(), ssGlobusFolderDto.getEmail());
                return new SSGlobusResponse(true, responseDto.getGlobusOriginID());
            }
            if (response.getStatusCode().equals(HttpStatus.OK)) {
                if (responseDto.getError() != null) {
                    backendEmailService.sendErrorsEmail("SummaryStats Service",
                            "[" + ssGlobusFolderDto.getUniqueId() + " | " + ssGlobusFolderDto.getEmail() + "]: " + responseDto.getError());
                    return new SSGlobusResponse(false, responseDto.getError());
                }
            }
            log.info("SS Service returned an unexpected code: {}", response.getStatusCode());
            backendEmailService.sendErrorsEmail("SummaryStats Service", response.getStatusCode().toString());
            return null;
        } catch (Exception e) {
            backendEmailService.sendErrorsEmail("SummaryStats Service", e.getMessage());
            log.error("Unable to call gwas-sumstats-service: {}", e.getMessage(), e);
            return null;
        }
    }
}
