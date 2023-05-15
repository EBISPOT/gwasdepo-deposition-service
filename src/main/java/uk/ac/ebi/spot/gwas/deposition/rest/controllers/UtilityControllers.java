package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import uk.ac.ebi.spot.gwas.deposition.service.SummaryStatsProcessingService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = GeneralCommon.API_V1)
public class UtilityControllers {

    private static final Logger log = LoggerFactory.getLogger(UtilityControllers.class);

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    private BodyOfWorkService bodyOfWorkService;

    @Autowired
    private SummaryStatsProcessingService  summaryStatsProcessingService;

    /**
     * POST /v1/remove-embargo
     */
    @PostMapping(value = GWASDepositionBackendConstants.API_REMOVE_EMBARGO,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void removeEmbargo(@RequestBody String bowId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to remove embargo on body of work: {}", user.getId(), bowId);
        bodyOfWorkService.removeEmbargo(bowId, user);
    }

    /**
     * POST /v1/add-embargo
     */
    @PostMapping(value = GWASDepositionBackendConstants.API_ADD_EMBARGO,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void addEmbargo(@RequestBody String bowId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        log.info("[{}] Request to remove embargo on body of work: {}", user.getId(), bowId);
        bodyOfWorkService.addEmbargo(bowId, user);
    }

    /**
     * POST /v1/bypass-ss-validation
     */
    @PostMapping(value = GWASDepositionBackendConstants.API_BYPASS_SS_VALIDATION,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public void bypassSSValidation(@RequestBody String submissionId, HttpServletRequest request) {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        summaryStatsProcessingService.bypassSSValidation(submissionId, user);

    }
}
