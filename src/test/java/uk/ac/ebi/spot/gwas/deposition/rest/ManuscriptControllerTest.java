package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.Manuscript;
import uk.ac.ebi.spot.gwas.deposition.domain.Provenance;
import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.dto.ManuscriptDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.ManuscriptDtoDisassembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.ManuscriptService;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockGWASCatalogRESTServiceConfig.class,
        IntegrationTest.MockTemplateServiceConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class})
public class ManuscriptControllerTest extends IntegrationTest {

    @Autowired
    private SumStatsService sumStatsService;

    private Manuscript manuscript;

    @Autowired
    private ManuscriptService manuscriptService;

    @Before
    public void setup() {
        super.setup();
        reset(sumStatsService);
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));

        manuscript = ManuscriptDtoDisassembler.diassemble(PublicationDtoAssembler.assemble(manuscriptPublication),
                new Provenance(DateTime.now(), user.getId()));
        manuscript = manuscriptService.createManuscript(manuscript);
    }

    /**
     * GET /v1/manuscripts/{manuscriptId}
     */
    @Test
    public void shouldGetManuscript() throws Exception {
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_MANUSCRIPTS +
                "/" + manuscript.getId();

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<ManuscriptDto> actualResource = mapper.readValue(response, new TypeReference<Resource<ManuscriptDto>>() {
        });
        ManuscriptDto actual = actualResource.getContent();

        assertEquals(manuscript.getTitle(), actual.getTitle());
        assertEquals(manuscript.getJournal(), actual.getJournal());
        assertEquals(manuscript.getFirstAuthor(), actual.getFirstAuthor());
        assertEquals(manuscript.getCorrespondingAuthor().getAuthorName(), actual.getCorrespondingAuthor().getAuthorName());
    }

    /**
     * GET /v1/manuscripts
     */
    @Test
    public void shouldGetManuscripts() throws Exception {
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_MANUSCRIPTS;

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();


        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(1, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.manuscripts[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> submissionData = JsonPath.read(response, "$._embedded.manuscripts[0]");
        assertEquals(manuscript.getId(), submissionData.get("manuscriptId"));
        assertEquals(manuscript.getTitle(), submissionData.get("title"));
        assertEquals(manuscript.getJournal(), submissionData.get("journal"));
        assertEquals(manuscript.getFirstAuthor(), submissionData.get("firstAuthor"));

    }

}
