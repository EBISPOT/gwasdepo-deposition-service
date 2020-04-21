package uk.ac.ebi.spot.gwas.deposition.service;

import java.util.List;
import java.util.Map;

public interface BackendEmailService {

    void sendSuccessEmail(String userId, String pubmedId, Map<String, Object> metadata);

    void sendFailEmail(String userId, String pubmedId, Map<String, Object> metadata, List<String> errors);

}
