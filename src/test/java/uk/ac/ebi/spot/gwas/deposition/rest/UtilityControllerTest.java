package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.BodyOfWorkDto;
import uk.ac.ebi.spot.gwas.deposition.repository.GCPCounterItemRepository;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.BodyOfWorkDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockGWASCatalogRESTServiceConfig.class,
        IntegrationTest.MockTemplateServiceConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class})
public class UtilityControllerTest extends IntegrationTest {

    @Autowired
    private SumStatsService sumStatsService;

    @Autowired
    private GCPCounterItemRepository gcpCounterItemRepository;

    @Autowired
    private BodyOfWorkService bodyOfWorkService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

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

        assertEquals("GCP000002", actual.getBodyOfWorkId());
        assertEquals(1, gcpCounterItemRepository.findAll().size());
        assertEquals(2, gcpCounterItemRepository.findAll().get(0).getCurrentValue());

        assertEquals(bodyOfWork.getTitle(), actual.getTitle());
        assertEquals(bodyOfWork.getJournal(), actual.getJournal());
        assertEquals(bodyOfWork.getFirstAuthor().getFirstName(), actual.getFirstAuthor().getFirstName());
        assertEquals(bodyOfWork.getLastAuthor().getFirstName(), actual.getLastAuthor().getFirstName());
        assertTrue(bodyOfWork.getEmbargoUntilPublished());
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
     * POST /v1/remove-embargo
     */
    @Test
    public void shouldRemoveEmbargo() throws Exception {
        BodyOfWorkDto bodyOfWorkDto = create();
        bodyOfWorkService.removeEmbargo(bodyOfWorkDto.getBodyOfWorkId(), new User("auto-curator-service@ebi.ac.uk", "auto-curator-service@ebi.ac.uk"));
        String endpoint = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_BODY_OF_WORK + "/" + bodyOfWorkDto.getBodyOfWorkId();

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<BodyOfWorkDto> actualResource = mapper.readValue(response, new TypeReference<Resource<BodyOfWorkDto>>() {
        });
        BodyOfWorkDto actual = actualResource.getContent();
        assertFalse(actual.getEmbargoUntilPublished());
    }

}
