package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;

@Component
public class SystemConfigProperties {

    @Value("${spring.profiles.active}")
    private String activeSpringProfile;

    @Value("${server.name}")
    private String serverName;

    @Value("${server.port}")
    private String serverPort;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${gwas-deposition.db:#{NULL}}")
    private String dbName;

    public String getActiveSpringProfile() {
        return activeSpringProfile;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerPort() {
        return serverPort;
    }

    public String getMongoUri() {
        return mongoUri;
    }

    public String getDbUser() {
        return System.getenv(GWASDepositionBackendConstants.DB_USER);
    }

    public String getDbPassword() {
        return System.getenv(GWASDepositionBackendConstants.DB_PASSWORD);
    }

    public String getDbName() {
        return dbName;
    }
}
