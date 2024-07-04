package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.spot.gwas.deposition.audit.PublicationAuditEntryDto;
import uk.ac.ebi.spot.gwas.deposition.audit.PublicationAuditHelper;
import uk.ac.ebi.spot.gwas.deposition.config.RestInteractionConfig;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.UserDto;
import uk.ac.ebi.spot.gwas.deposition.rest.RestRequestUtil;
import uk.ac.ebi.spot.gwas.deposition.service.PublicationAuditService;

@Service
public class PublicationAuditServiceImpl implements PublicationAuditService {


    private static final Logger log = LoggerFactory.getLogger(PublicationAuditServiceImpl.class);
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private RestRequestUtil restRequestUtil;

    @Autowired
    RestInteractionConfig restInteractionConfig;

    public void createAuditEvent(String eventType, String subOrPubId, String event,
                          Boolean isPublication, User user) {
       PublicationAuditEntryDto publicationAuditEntryDto = PublicationAuditHelper.createAuditEvent(eventType,
                subOrPubId, event,
               isPublication, new UserDto(user.getName(), user.getEmail(),
                        user.getNickname(), user.getUserReference(), user.getDomains()),
              DateTime.now());
       log.info("The date in publicationAuditEntryDto is {}",publicationAuditEntryDto.getProvenanceDto().getTimestamp());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        //String uri = restInteractionConfig.getAuditServiceUri()+restInteractionConfig.getAuditServicePublicationEndpoint()+subOrPubId+restInteractionConfig.getAuditServicePubAuditEntriesEndpoint();
        String uri = String.format("%s%s/%s%s",restInteractionConfig.getAuditServiceUrl(),
                restInteractionConfig.getAuditServicePublicationEndpoint(),subOrPubId,restInteractionConfig.getAuditServicePubAuditEntriesEndpoint());
        log.info("Audit Service API url is {}",uri);
        HttpEntity httpEntity = this.restRequestUtil.httpEntity().withJsonBody(publicationAuditEntryDto).build();
        restTemplate.exchange(uri, HttpMethod.POST, httpEntity,
                new ParameterizedTypeReference<Resource<PublicationAuditEntryDto>>() {
        });


    }
}
