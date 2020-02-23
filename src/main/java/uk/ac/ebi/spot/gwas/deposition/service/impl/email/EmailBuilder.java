package uk.ac.ebi.spot.gwas.deposition.service.impl.email;

import java.util.Map;

public interface EmailBuilder {

    String getEmailContent(Map<String, String> metadata);

}
