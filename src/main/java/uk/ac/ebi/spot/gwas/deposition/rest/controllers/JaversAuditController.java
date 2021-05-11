package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javers.core.Changes;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.repository.jql.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;


import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;
import uk.ac.ebi.spot.gwas.deposition.javers.JaversChangeWrapper;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.StudyDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.HeadersUtil;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(value = GeneralCommon.API_V1+ GWASDepositionBackendConstants.API_Javers)
public class JaversAuditController {

    private static final Logger log = LoggerFactory.getLogger(JaversAuditController.class);

    private final Javers javers;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private AssociationsService associationsService;

    @Autowired
    private StudiesService studiesService;

    @Autowired
    private SamplesService samplesService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTService jwtService;

    @Autowired
    public JaversAuditController(Javers javers){
        this.javers = javers;
    }

/*
     * GET /v1/submissions/{submissionId}/changes

 */


    @GetMapping(value = "/submissions/{submissionId}/changes",
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public List<JaversChangeWrapper> getSubmissionChanges(@PathVariable  String submissionId, HttpServletRequest request) throws Exception {
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        Submission submission = submissionService.getSubmission(submissionId, user);
        QueryBuilder queryBuilder = QueryBuilder.byInstance(submission);
        Changes changes = javers.findChanges(queryBuilder.build());
        try {
            JaversChangeWrapper[] javersChangeWrapperArray = new ObjectMapper().readValue(
                    javers.getJsonConverter().toJson(changes), JaversChangeWrapper[].class);
            return Arrays.asList(javersChangeWrapperArray);

        }catch(Exception ex){
            log.error("Error in mapping Javers Changes "+ex.getMessage(),ex);
            return null;
        }

    }

    @GetMapping(value = "/association/{associationId}/changes",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getAssociationChanges(@PathVariable  String associationId, HttpServletRequest request){
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        Association association = associationsService.getAssociation(associationId);
        QueryBuilder queryBuilder = QueryBuilder.byInstance(association);
        Changes changes = javers.findChanges(queryBuilder.build());
        return javers.getJsonConverter().toJson(changes);

    }


    @GetMapping(value = "/study/{studyId}/changes",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getStudyChanges(@PathVariable  String studyId, HttpServletRequest request, @RequestParam String studyId1){
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        Study study = studiesService.getStudy(studyId);
        Study study1 = studiesService.getStudy(studyId1);

        StudyDto studyDto1 = StudyDtoAssembler.assemble(study);
        StudyDto studyDto2 = StudyDtoAssembler.assemble(study1);
        Javers javers1 = JaversBuilder.javers().build();
        Diff diff = javers1.compare(studyDto1, studyDto2);
        log.info("************");
        log.info("Diff"+ diff);
        List<ValueChange> valChanges = diff.getChangesByType(ValueChange.class);
        log.info("Changes ->"+valChanges);
        QueryBuilder queryBuilder = QueryBuilder.byInstance(study);
        Changes changes = javers.findChanges(queryBuilder.build());
        return javers.getJsonConverter().toJson(valChanges);
    }


    @GetMapping(value = "/sample/{sampleId}/changes",
            produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public String getSampleChanges(@PathVariable  String sampleId, HttpServletRequest request){
        User user = userService.findUser(jwtService.extractUser(HeadersUtil.extractJWT(request)), false);
        Sample sample = samplesService.getSample(sampleId);
        QueryBuilder queryBuilder = QueryBuilder.byInstance(sample);
        Changes changes = javers.findChanges(queryBuilder.build());
        return javers.getJsonConverter().toJson(changes);
    }


}



