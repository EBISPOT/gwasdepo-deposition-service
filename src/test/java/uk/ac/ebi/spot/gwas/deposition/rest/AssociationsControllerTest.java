package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionCreationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class})
public class AssociationsControllerTest extends IntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private SumStatsService sumStatsService;

    @Before
    public void setup() {
        super.setup();
        reset(sumStatsService);
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));
    }

    /**
     * GET /v1/submissions/{submissionId}/associations
     */
    @Test
    public void shouldGetSamples() throws Exception {
        SubmissionCreationDto submissionCreationDto = new SubmissionCreationDto(PublicationDtoAssembler.assemble(eligiblePublication),
                null,
                RandomStringUtils.randomAlphanumeric(10));
        String response = mockMvc.perform(post(GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(submissionCreationDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<SubmissionDto> actual = mapper.readValue(response, new TypeReference<Resource<SubmissionDto>>() {
        });
        SubmissionDto submissionDto = actual.getContent();

        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        association.setSubmissionId(submission.getId());
        associationRepository.save(association);
        sample.setSubmissionId(submission.getId());
        sampleRepository.save(sample);
        study.setSubmissionId(submission.getId());
        studyRepository.save(study);
        note.setSubmissionId(submission.getId());
        noteRepository.save(note);

        submission.setAssociations(Arrays.asList(new String[]{association.getId()}));
        submission.setSamples(Arrays.asList(new String[]{sample.getId()}));
        submission.setStudies(Arrays.asList(new String[]{study.getId()}));
        submission.setNotes(Arrays.asList(new String[]{note.getId()}));
        submissionService.saveSubmission(submission, user.getId());

        String endpoint = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() + GWASDepositionBackendConstants.API_ASSOCIATIONS;

        response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(1, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.associations[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> associationData = JsonPath.read(response, "$._embedded.associations[0]");
        assertEquals(association.getStudyTag(), associationData.get("study_tag"));
        assertEquals(association.getVariantId(), associationData.get("variant_id"));
        assertEquals(association.getPvalue(), associationData.get("pvalue"));
        assertEquals(association.getEffectAllele(), associationData.get("effect_allele"));
        assertEquals(association.getBeta(), associationData.get("beta"));
        assertEquals(association.getBetaUnit(), associationData.get("beta_unit"));
        assertEquals(association.getEffectAlleleFrequency(), associationData.get("effect_allele_frequency"));
        assertEquals(association.getStandardError(), associationData.get("standard_error"));
    }
}
