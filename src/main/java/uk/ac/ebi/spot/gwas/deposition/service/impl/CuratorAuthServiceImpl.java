package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.IDPConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.CuratorWhitelist;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.repository.CuratorWhitelistRepository;
import uk.ac.ebi.spot.gwas.deposition.service.CuratorAuthService;
import java.util.Collections;
import java.util.Optional;

@Service
public class CuratorAuthServiceImpl implements CuratorAuthService {

    private static final Logger log = LoggerFactory.getLogger(CuratorAuthService.class);

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private CuratorWhitelistRepository curatorWhitelistRepository;

    @Override
    public boolean isCurator(User user) {
        log.info("Checking user for curator permissions: {}", user.getEmail());
        if (gwasDepositionBackendConfig.getAutoCuratorServiceAccount() != null && user.getEmail().equalsIgnoreCase(gwasDepositionBackendConfig.getAutoCuratorServiceAccount())) {
            return true;
        }
        if (gwasDepositionBackendConfig.getCuratorAuthMechanism().equalsIgnoreCase(IDPConstants.CURATOR_AUTH_JWT_DOMAIN)) {
            return !Collections.disjoint(gwasDepositionBackendConfig.getCuratorDomains(), user.getDomains());
        }
        if (gwasDepositionBackendConfig.getCuratorAuthMechanism().equalsIgnoreCase(IDPConstants.CURATOR_AUTH_EMAIL_WHITELISTING)) {
            Optional<CuratorWhitelist> curatorWhitelist = curatorWhitelistRepository.findByEmailIgnoreCase(user.getEmail());
            return curatorWhitelist.isPresent();
        }
        return false;
    }
}
