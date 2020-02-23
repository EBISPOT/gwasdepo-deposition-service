package uk.ac.ebi.spot.gwas.deposition.service;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;
import uk.ac.ebi.spot.gwas.deposition.rest.IntegrationTest;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GWASCatalogRESTServiceTest extends IntegrationTest {

    @Autowired
    private GWASCatalogRESTService gwasCatalogRESTService;

    private String pmid;

    @Before
    public void setup() {
        pmid = "27863252";
    }

    @Test
    public void shouldRetrievePMIDData() {
        List<SSTemplateEntryDto> list = gwasCatalogRESTService.getSSTemplateEntries(pmid);
        assertEquals(36, list.size());
    }
}
