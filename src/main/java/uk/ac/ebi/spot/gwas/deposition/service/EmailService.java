package uk.ac.ebi.spot.gwas.deposition.service;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendMessage(String emailAddress, String content, String pubmedId);

    void sendSuccessEmail(String userId, String pubmedId, Map<String, String> metadata);

    void sendFailEmail(String userId, String pubmedId, Map<String, String> metadata, List<String> errors);

}
