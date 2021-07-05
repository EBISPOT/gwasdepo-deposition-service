package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.dto.gwas.GWASCatalogLinksDto;
import uk.ac.ebi.spot.gwas.deposition.dto.gwas.GWASCatalogSSResponseDto;
import uk.ac.ebi.spot.gwas.deposition.dto.gwas.GWASCatalogStudyDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SSTemplateEntryRequestDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.GWASCatalogRESTService;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "gwas-catalog-service.enabled", havingValue = "true")
public class GWASCatalogRESTServiceImpl extends GatewayService implements GWASCatalogRESTService {

    private static final Logger log = LoggerFactory.getLogger(GWASCatalogRESTService.class);

    @Override
    public List<SSTemplateEntryDto> getSSTemplateEntries(String pmid) {
        log.info("Retrieving GWAS Catalog data for Pubmed ID: {}", pmid);
        String endpoint = restInteractionConfig.getGwasCatalogServiceEndpoint() + pmid;

        GWASCatalogSSResponseDto initialResponse = retrieveSSData(endpoint);
        List<SSTemplateEntryDto> templateList = new ArrayList<>();
        if (initialResponse != null) {
            if (initialResponse.getEmbedded() != null) {
                templateList.addAll(processEntries(initialResponse.getEmbedded().getStudies()));
            }
            GWASCatalogLinksDto links = initialResponse.getLinks();
            while (links != null && links.getNext() != null) {
                String nextEndpoint = links.getNext().getHref();
                GWASCatalogSSResponseDto nextResponse = retrieveSSData(nextEndpoint);
                if (nextResponse == null) {
                    links = null;
                } else {
                    if (nextResponse.getEmbedded() != null) {
                        templateList.addAll(processEntries(nextResponse.getEmbedded().getStudies()));
                    }
                    links = nextResponse.getLinks();
                }
            }
        }
        return templateList;
    }

    private List<SSTemplateEntryDto> processEntries(List<GWASCatalogStudyDto> studies) {
        List<SSTemplateEntryDto> list = new ArrayList<>();
        if (studies == null) {
            return list;
        }
        for (GWASCatalogStudyDto gwasCatalogStudyDto : studies) {
            SSTemplateEntryDto ssTemplateEntryDto = SSTemplateEntryRequestDtoAssembler.assemble(gwasCatalogStudyDto);
            if (ssTemplateEntryDto != null) {
                list.add(ssTemplateEntryDto);
            }
        }
        return list;
    }

    private GWASCatalogSSResponseDto retrieveSSData(String endpoint) {
        log.info("Retrieving GWAS Catalog data using endpoint: {}", endpoint);
        HttpEntity httpEntity = restRequestUtil.httpEntity()
                .build();
        ResponseEntity<GWASCatalogSSResponseDto> response =
                restTemplate.exchange(endpoint,
                        HttpMethod.GET, httpEntity,
                        new ParameterizedTypeReference<GWASCatalogSSResponseDto>() {
                        });

        if (response.getStatusCode().equals(HttpStatus.OK)) {
            log.info("GWAS catalog data successfully retrieved.");
            return response.getBody();
        }

        log.error("Unable to call GWAS catalog service: {}", response.getStatusCode());
        return null;
    }

}
