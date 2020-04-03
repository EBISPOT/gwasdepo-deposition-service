package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.dto.BodyOfWorkDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.BodyOfWorkDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockGWASCatalogRESTServiceConfig.class,
        IntegrationTest.MockTemplateServiceConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class})
public class BodyOfWorkControllerTest extends IntegrationTest {

    @Autowired
    private SumStatsService sumStatsService;

    @Before
    public void setup() {
        super.setup();
        reset(sumStatsService);
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));
    }

    private BodyOfWorkDto create() throws Exception {
        String endpoint = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_BODY_OF_WORK;

        String response = mockMvc.perform(post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(BodyOfWorkDtoAssembler.assemble(bodyOfWork))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<BodyOfWorkDto> actualResource = mapper.readValue(response, new TypeReference<Resource<BodyOfWorkDto>>() {
        });
        BodyOfWorkDto actual = actualResource.getContent();

        assertEquals(bodyOfWork.getTitle(), actual.getTitle());
        assertEquals(bodyOfWork.getJournal(), actual.getJournal());
        assertEquals(bodyOfWork.getFirstAuthor().getFirstName(), actual.getFirstAuthor().getFirstName());
        assertEquals(bodyOfWork.getLastAuthor().getFirstName(), actual.getLastAuthor().getFirstName());
        return actual;
    }

    /**
     * POST /v1/bodyofwork
     */
    @Test
    public void shouldCreateBodyOfWork() throws Exception {
        create();
    }

    /**
     * GET /v1/bodyofwork/{bodyofworkId}
     */
    @Test
    public void shouldGetBodyOfWork() throws Exception {
        BodyOfWorkDto bodyOfWorkDto = create();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_BODY_OF_WORK +
                "/" + bodyOfWorkDto.getBodyOfWorkId();

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<BodyOfWorkDto> actualResource = mapper.readValue(response, new TypeReference<Resource<BodyOfWorkDto>>() {
        });
        BodyOfWorkDto actual = actualResource.getContent();

        assertEquals(bodyOfWork.getTitle(), actual.getTitle());
        assertEquals(bodyOfWork.getJournal(), actual.getJournal());
        assertEquals(bodyOfWork.getFirstAuthor().getFirstName(), actual.getFirstAuthor().getFirstName());
        assertEquals(bodyOfWork.getLastAuthor().getFirstName(), actual.getLastAuthor().getFirstName());
    }

    /**
     * DELETE /v1/bodyofwork/{bodyofworkId}
     */
    @Test
    public void shouldDeleteBodyOfWork() throws Exception {
        BodyOfWorkDto bodyOfWorkDto = create();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_BODY_OF_WORK +
                "/" + bodyOfWorkDto.getBodyOfWorkId();

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<BodyOfWorkDto> actualResource = mapper.readValue(response, new TypeReference<Resource<BodyOfWorkDto>>() {
        });
        BodyOfWorkDto actual = actualResource.getContent();

        assertEquals(bodyOfWork.getTitle(), actual.getTitle());
        assertEquals(bodyOfWork.getJournal(), actual.getJournal());
        assertEquals(bodyOfWork.getFirstAuthor().getFirstName(), actual.getFirstAuthor().getFirstName());
        assertEquals(bodyOfWork.getLastAuthor().getFirstName(), actual.getLastAuthor().getFirstName());

        mockMvc.perform(delete(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    /**
     * GET /v1/bodyofwork
     */
    @Test
    public void shouldGetBodyOfWorks() throws Exception {
        BodyOfWorkDto bodyOfWorkDto = create();
        String endpoint = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_BODY_OF_WORK;

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();


        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(1, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.bodyOfWorks[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> submissionData = JsonPath.read(response, "$._embedded.bodyOfWorks[0]");
        assertEquals(bodyOfWorkDto.getBodyOfWorkId(), submissionData.get("bodyOfWorkId"));
        assertEquals(bodyOfWorkDto.getTitle(), submissionData.get("title"));
        assertEquals(bodyOfWorkDto.getJournal(), submissionData.get("journal"));

    }

}
