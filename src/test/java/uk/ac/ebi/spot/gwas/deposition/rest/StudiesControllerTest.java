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
public class StudiesControllerTest extends IntegrationTest {

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
     * GET /v1/submissions/{submissionId}/studies
     */
    @Test
    public void shouldGetStudies() throws Exception {
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
        submissionService.saveSubmission(submission);

        String endpoint = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() + GWASDepositionBackendConstants.API_STUDIES;

        response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(1, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.studies[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> studyData = JsonPath.read(response, "$._embedded.studies[0]");
        assertEquals(study.getStudyTag(), studyData.get("study_tag"));
        assertEquals(study.getGenotypingTechnology(), studyData.get("genotyping_technology"));
        assertEquals(study.getArrayManufacturer(), studyData.get("array_manufacturer"));
        assertEquals(study.getArrayInformation(), studyData.get("array_information"));
        assertEquals(study.getImputation(), studyData.get("imputation"));
        assertEquals(study.getVariantCount(), studyData.get("variant_count"));
        assertEquals(study.getTrait(), studyData.get("trait"));
        assertEquals(study.getSummaryStatisticsFile(), studyData.get("summary_statistics_file"));
    }


    /**
     * GET /v1/submissions/{submissionId}/study-envelopes
     */
    @Test
    public void shouldGetStudyEnvelopes() throws Exception {
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
        submissionService.saveSubmission(submission);

        String endpoint = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() + GWASDepositionBackendConstants.API_STUDY_ENVELOPES;

        response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(1, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.studies[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> studyData = JsonPath.read(response, "$._embedded.studies[0]");
        assertEquals(study.getAccession(), studyData.get("study_accession"));
    }
}
