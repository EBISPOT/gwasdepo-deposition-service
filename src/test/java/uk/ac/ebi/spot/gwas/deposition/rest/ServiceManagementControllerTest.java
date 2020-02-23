package uk.ac.ebi.spot.gwas.deposition.rest;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class})
public class ServiceManagementControllerTest extends IntegrationTest {

    /**
     * DELETE /v1/publications/{pmid}
     */
    @Ignore
    @Test
    public void shouldGetPublicationById() throws Exception {
        mockMvc.perform(delete(GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_PUBLICATIONS + "/" + eligiblePublication.getPmid()))
                .andExpect(status().isOk());

        mockMvc.perform(get(GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_PUBLICATIONS + "/" + eligiblePublication.getPmid() +
                "?" + GWASDepositionBackendConstants.PARAM_PMID + "=true")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

}
