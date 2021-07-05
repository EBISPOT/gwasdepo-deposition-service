package uk.ac.ebi.spot.gwas.deposition.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.IDPConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.AuthToken;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.exception.AuthorizationException;
import uk.ac.ebi.spot.gwas.deposition.repository.AuthTokenRepository;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

@Service
public class JWTServiceImpl implements JWTService {

    private static final Logger log = LoggerFactory.getLogger(JWTService.class);

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    private PublicKey verifyingKey;

    @PostConstruct
    public void initialize() {
        log.info("Initializing auth cert. Auth enabled: {}", gwasDepositionBackendConfig.isAuthEnabled());
        if (gwasDepositionBackendConfig.isAuthEnabled()) {
            String certPath = gwasDepositionBackendConfig.getCertPath();
            log.info("Using cert: {}", certPath);
            if (certPath == null) {
                log.error("Unable to initialize cert. Path is NULL.");
            } else {
                try {
                    InputStream inputStream = new DefaultResourceLoader().getResource(certPath).getInputStream();
                    final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
                    final X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(inputStream);
                    verifyingKey = certificate.getPublicKey();
                } catch (Exception e) {
                    log.error("Unable to initialize cert: {}", e.getMessage(), e);
                }
            }
        }
    }

    @Override
    public User extractUser(String jwt) {
        log.info("Auth enabled: {}", gwasDepositionBackendConfig.isAuthEnabled());
        if (gwasDepositionBackendConfig.isAuthEnabled()) {
            if (jwt == null) {
                log.error("Unauthorised access. JWT missing.");
                throw new AuthorizationException("Unauthorised access. JWT missing.");
            }

            Optional<AuthToken> authTokenOptional = authTokenRepository.findByToken(jwt);
            if (authTokenOptional.isPresent()) {
                User user = new User("Auto Curator", gwasDepositionBackendConfig.getAutoCuratorServiceAccount());
                user.setDomains(gwasDepositionBackendConfig.getCuratorDomains());
                return user;
            }

            Claims jwtClaims;
            try {
                jwtClaims = Jwts.parser().setSigningKey(verifyingKey).parseClaimsJws(jwt).getBody();
            } catch (Exception e) {
                log.error("Unable to parse JWT: {}", e.getMessage(), e);
                throw new AuthorizationException("Unauthorised access: " + e.getMessage());
            }
            String userReference = jwtClaims.getSubject();
            String name = null;
            String email = null;
            if (jwtClaims.get(IDPConstants.JWT_EMAIL) != null) {
                email = (String) jwtClaims.get(IDPConstants.JWT_EMAIL);
            }
            if (jwtClaims.get(IDPConstants.JWT_NAME) != null) {
                name = (String) jwtClaims.get(IDPConstants.JWT_NAME);
            }
            if (name == null || email == null || userReference == null) {
                log.error("Unable to parse JWT: Name, email or userReference missing.");
                throw new AuthorizationException("Unauthorised access: Name, email or userReference missing.");
            }
            User user = new User(name, email);
            user.setUserReference(userReference);

            if (jwtClaims.get(IDPConstants.JWT_NICKNAME) != null) {
                String nickname = (String) jwtClaims.get(IDPConstants.JWT_NICKNAME);
                user.setNickname(nickname);
            }
            if (jwtClaims.get(IDPConstants.JWT_DOMAINS) != null) {
                List<String> domains = (List<String>) jwtClaims.get(IDPConstants.JWT_DOMAINS);
                user.setDomains(domains);
            }

            log.info("Found user: {}", user);
            return user;
        }

        User user = new User("Auto Curator", gwasDepositionBackendConfig.getAutoCuratorServiceAccount());
        user.setDomains(gwasDepositionBackendConfig.getCuratorDomains());
        return user;
    }
}
