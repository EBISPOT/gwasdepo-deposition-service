package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.spot.gwas.deposition.constants.DepositionCurationConstants;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionDiffService;
import uk.ac.ebi.spot.gwas.deposition.javers.JaversChangeWrapper;

import java.util.List;

@Service
public class SubmissionDiffServiceImpl implements SubmissionDiffService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionDiffServiceImpl.class);

    @Autowired
    private RestTemplate restTemplate;



    public ResponseEntity<List<JaversChangeWrapper>> diffVersionsSubmission(String submissionId, String jwtToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        headers.setBearerAuth(jwtToken);
        HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);
        String endpoint = "http://localhost:8080/v1/javers"+DepositionCurationConstants.API_SUBMISSIONS+
                "/"+submissionId + DepositionCurationConstants.API_JAVERS_CHANGES;
        log.info("Endpoint Javers->"+endpoint);
        ResponseEntity<List<JaversChangeWrapper>> javersChangeWrapperList = restTemplate.exchange(endpoint, HttpMethod.GET,
                httpEntity, new ParameterizedTypeReference<List<JaversChangeWrapper>> () {
        });
        return javersChangeWrapperList;

    }
}
