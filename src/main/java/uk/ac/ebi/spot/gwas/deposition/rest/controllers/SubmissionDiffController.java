package uk.ac.ebi.spot.gwas.deposition.rest.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ebi.spot.gwas.deposition.constants.DepositionCurationConstants;

import uk.ac.ebi.spot.gwas.deposition.service.SubmissionDiffService;
import uk.ac.ebi.spot.gwas.deposition.service.ConversionJaversService;
import uk.ac.ebi.spot.gwas.deposition.util.CurationUtil;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.javers.JaversChangeWrapper;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping(value = GeneralCommon.API_V1 + DepositionCurationConstants.API_SUBMISSIONS)
public class SubmissionDiffController {

    private static final Logger log = LoggerFactory.getLogger(SubmissionDiffController.class);
    @Autowired
    SubmissionDiffService submissionDiffService;

    @Autowired
    ConversionJaversService conversionService;

    @GetMapping(
            value = "/{submissionId}" + DepositionCurationConstants.API_SUBMISSION_VERSION,
            produces = MediaType.APPLICATION_JSON_VALUE
    )

    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public List<JaversChangeWrapper> diffVersionSubmissions(@PathVariable String submissionId, HttpServletRequest request) {
        String jwtToken = CurationUtil.parseJwt(request);
        ResponseEntity<List<JaversChangeWrapper>> responseEntity = submissionDiffService.diffVersionsSubmission(submissionId, jwtToken );
        Optional<List<JaversChangeWrapper>> convertedEntityOptional = conversionService.filterJaversResponse(responseEntity.getBody());

        if(convertedEntityOptional.isPresent())
            return convertedEntityOptional.get();
        else
            return null;


    }
}
