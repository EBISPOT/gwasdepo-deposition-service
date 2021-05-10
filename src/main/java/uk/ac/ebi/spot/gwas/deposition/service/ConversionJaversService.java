package uk.ac.ebi.spot.gwas.deposition.service;

import uk.ac.ebi.spot.gwas.deposition.javers.JaversChangeWrapper;

import java.util.List;
import java.util.Optional;

public interface ConversionJaversService {

    public Optional<List<JaversChangeWrapper>> filterJaversResponse(List<JaversChangeWrapper> javersChangeWrapperList);
}
