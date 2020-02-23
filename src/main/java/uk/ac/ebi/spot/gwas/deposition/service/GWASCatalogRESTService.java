package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;

import java.util.List;

public interface GWASCatalogRESTService {

    List<SSTemplateEntryDto> getSSTemplateEntries(String pmid);
}
