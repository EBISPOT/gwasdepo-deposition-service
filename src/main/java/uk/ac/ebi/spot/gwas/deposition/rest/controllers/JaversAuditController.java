/*
package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.repository.jql.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = GeneralCommon.API_V1)
public class JaversAuditController {

    private final Javers javers;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    public JaversAuditController(Javers javers){
        this.javers = javers;
    }

    */
/**
     * GET /v1/submissions/{submissionId}/changes
     *//*

    @GetMapping(value = "submissions/{submissionId}/changes",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getSubmissionChanges(@PathVariable  String submissionId, HttpServletRequest request){
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        Submission submission = submissionService.getSubmission(submissionId, user);
        QueryBuilder queryBuilder = QueryBuilder.byInstance(submission);
        Changes changes = javers.findChanges(queryBuilder.build());
        return javers.getJsonConverter().toJson(changes);

    }
}



*/
