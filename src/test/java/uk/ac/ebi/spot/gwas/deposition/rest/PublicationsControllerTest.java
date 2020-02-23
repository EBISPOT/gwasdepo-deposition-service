package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.dto.PublicationDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class})
public class PublicationsControllerTest extends IntegrationTest {

    /**
     * GET /v1/publications/{publicationId}
     */
    @Test
    public void shouldGetPublicationById() throws Exception {
        String response = mockMvc.perform(get(GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_PUBLICATIONS + "/" + eligiblePublication.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<PublicationDto> actual = mapper.readValue(response, new TypeReference<Resource<PublicationDto>>() {
        });

        assertEquals(PublicationDtoAssembler.assemble(eligiblePublication), actual.getContent());
    }

    /**
     * GET /v1/publications/{publicationId}?pmid=true
     */
    @Test
    public void shouldGetPublicationByPMID() throws Exception {
        String response = mockMvc.perform(get(GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_PUBLICATIONS + "/" + eligiblePublication.getPmid() +
                "?" + GWASDepositionBackendConstants.PARAM_PMID + "=true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<PublicationDto> actual = mapper.readValue(response, new TypeReference<Resource<PublicationDto>>() {
        });

        assertEquals(PublicationDtoAssembler.assemble(eligiblePublication), actual.getContent());
    }

    /**
     * GET /v1/publications/{publicationId}
     */
    @Test
    public void shouldNotFindGetPublicationById() throws Exception {
        mockMvc.perform(get(GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_PUBLICATIONS + "/" + RandomStringUtils.randomAlphanumeric(10))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /v1/publications
     */
    @Test
    public void shouldAllPublications() throws Exception {
        String response = mockMvc.perform(get(GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_PUBLICATIONS)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(2, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.publications[*]");
        assertEquals(2, jsonArray.size());
        Map<String, String> publicationData = JsonPath.read(response, "$._embedded.publications[0]");
        assertEquals(eligiblePublication.getPmid(), publicationData.get("pmid"));
        assertEquals(eligiblePublication.getFirstAuthor(), publicationData.get("firstAuthor"));
        assertEquals(eligiblePublication.getJournal(), publicationData.get("journal"));
        assertEquals(eligiblePublication.getStatus(), publicationData.get("status"));
        assertEquals(eligiblePublication.getTitle(), publicationData.get("title"));
        assertEquals(eligiblePublication.getPublicationDate().toString(), publicationData.get("publicationDate"));
    }

    /**
     * GET /v1/publications?author=<author>
     */
    @Test
    public void shouldGetPublicationsByAuthor() throws Exception {
        String authorName = eligiblePublication.getFirstAuthor();
        String[] parts = authorName.split(" ");
        String value = parts[1].toUpperCase();

        String response = mockMvc.perform(get(GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_PUBLICATIONS + "?author=" + value)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(1, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.publications[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> publicationData = JsonPath.read(response, "$._embedded.publications[0]");
        assertEquals(eligiblePublication.getPmid(), publicationData.get("pmid"));
        assertEquals(eligiblePublication.getFirstAuthor(), publicationData.get("firstAuthor"));
        assertEquals(eligiblePublication.getJournal(), publicationData.get("journal"));
        assertEquals(eligiblePublication.getStatus(), publicationData.get("status"));
        assertEquals(eligiblePublication.getTitle(), publicationData.get("title"));
        assertEquals(eligiblePublication.getPublicationDate().toString(), publicationData.get("publicationDate"));
    }
}
