package uk.ac.ebi.spot.gwas.deposition.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.domain.AuthToken;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.exception.AuthorizationException;
import uk.ac.ebi.spot.gwas.deposition.repository.AuthTokenRepository;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest,
                             HttpServletResponse httpServletResponse,
                             Object o) {
        if (!"/error".equals(httpServletRequest.getRequestURI())) {
            log.info("Authentication enabled: {}", gwasDepositionBackendConfig.isAuthEnabled());
            if (!gwasDepositionBackendConfig.isAuthEnabled()) {
                return true;
            }

            String jwt = HeadersUtil.extractJWT(httpServletRequest);
            String requestedURI = httpServletRequest.getRequestURI();
            log.info("JWT found: {}", jwt);
            log.info("Requested URI: {}", requestedURI);

            List<String> unauthenticatedEndpoints = gwasDepositionBackendConfig.getUnauthenticatedEndpoints();
            if (!unauthenticatedEndpoints.isEmpty()) {
                for (String endpoint : unauthenticatedEndpoints) {
                    if (requestedURI.startsWith(endpoint)) {
                        return true;
                    }
                }
            }

            if (jwt == null) {
                log.error("Authorization failure. JWT token is null.");
                throw new AuthorizationException("Authorization failure. JWT token is null.");
            }
            if ("".equals(jwt)) {
                log.error("Authorization failure. JWT token is null.");
                throw new AuthorizationException("Authorization failure. JWT token is null.");
            }

            Optional<AuthToken> authTokenOptional = authTokenRepository.findByToken(jwt);
            log.info("Token is privileged: {}", authTokenOptional.isPresent());
            if (authTokenOptional.isPresent()) {
                User user = jwtService.extractUser(jwt);
                log.info("User found: {} - {}", user.getName(), user.getEmail());
                userService.findUser(user, true);
                return true;
            }

            try {
                User user = jwtService.extractUser(jwt);
                log.info("User found: {} - {}", user.getName(), user.getEmail());
                userService.findUser(user, true);
                return true;
            } catch (Exception e) {
                log.error("Authorization failure: {}", e.getMessage(), e);
                throw new AuthorizationException(e.getMessage());
            }
        }

        return false;
    }
}
