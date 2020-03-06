package uk.ac.ebi.spot.gwas.deposition.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

import java.util.List;

@Component
public class GWASDepositionBackendConfig {

    @Value("${gwas-deposition.auth.enabled}")
    private boolean authEnabled;

    @Value("${gwas-deposition.auth.cert:#{NULL}}")
    private String certPath;

    @Value("${gwas-deposition.auth.auto-curator-service-account:#{NULL}}")
    private String autoCuratorServiceAccount;

    @Value("${gwas-deposition.auth.unauthenticated-endpoints:#{NULL}}")
    private String unauthenticatedEndpoints;

    @Value("${gwas-deposition.auth.curators.auth-mechanism:JWT_DOMAIN}")
    private String curatorAuthMechanism;

    @Value("${gwas-deposition.auth.curators.jwt-domains:#{NULL}}")
    private String curatorDomains;

    @Value("${gwas-deposition.proxy-prefix:#{NULL}}")
    private String proxyPrefix;

    @Value("${gwas-deposition.email-config.base-url}")
    private String submissionsBaseURL;

    @Value("${gwas-deposition.db:#{NULL}}")
    private String dbName;

    public String getDbName() {
        return dbName;
    }

    public String getSubmissionsBaseURL() {
        return submissionsBaseURL;
    }

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public String getProxyPrefix() {
        return proxyPrefix;
    }

    public String getCertPath() {
        return certPath;
    }

    public String getAutoCuratorServiceAccount() {
        return autoCuratorServiceAccount;
    }

    public String getCuratorAuthMechanism() {
        return curatorAuthMechanism;
    }

    public List<String> getUnauthenticatedEndpoints() {
        return BackendUtil.sToList(unauthenticatedEndpoints);
    }

    public List<String> getCuratorDomains() {
        return BackendUtil.sToList(curatorDomains);
    }
}
