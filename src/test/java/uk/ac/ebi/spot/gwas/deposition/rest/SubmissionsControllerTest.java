package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.constants.*;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionCreationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;
import uk.ac.ebi.spot.gwas.deposition.repository.BodyOfWorkRepository;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.BodyOfWorkDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.GCPCounter;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockGWASCatalogRESTServiceConfig.class,
        IntegrationTest.MockTemplateServiceConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class})
public class SubmissionsControllerTest extends IntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private UserService userService;

    @Autowired
    private GWASCatalogRESTService gwasCatalogRESTService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private SumStatsService sumStatsService;

    @Autowired
    private BodyOfWorkRepository bodyOfWorkRepository;

    @Autowired
    private GCPCounter gcpCounter;

    @Before
    public void setup() {
        super.setup();
        reset(sumStatsService);
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));
    }

    /**
     * POST /v1/submissions
     */
    @Test
    public void shouldCreateEligibleSubmission() throws Exception {
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));
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

        assertEquals(submissionCreationDto.getPublication().getPmid(), actual.getContent().getPublication().getPmid());
        assertEquals(PublicationStatus.UNDER_SUBMISSION.name(), actual.getContent().getPublication().getStatus());
        assertEquals(user.getName(), actual.getContent().getCreated().getUser().getName());
        assertEquals(user.getEmail(), actual.getContent().getCreated().getUser().getEmail());
        assertEquals(Status.STARTED.name(), actual.getContent().getSubmissionStatus());
        assertEquals(Status.NA.name(), actual.getContent().getMetadataStatus());
        assertEquals(Status.NA.name(), actual.getContent().getSummaryStatisticsStatus());

        assertTrue(actual.getContent().getFiles().isEmpty());
        verify(sumStatsService, times(1)).createGlobusFolder(any());
    }

    /**
     * POST /v1/submissions
     */
    @Test
    public void shouldCreatePublishedSubmission() throws Exception {
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));
        SubmissionCreationDto submissionCreationDto = new SubmissionCreationDto(PublicationDtoAssembler.assemble(publishedPublication),
                null,
                RandomStringUtils.randomAlphanumeric(10));
        SSTemplateEntryDto ssTemplateEntryDto = new SSTemplateEntryDto(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                false);
        List<SSTemplateEntryDto> ssTemplateEntryDtoList = Arrays.asList(new SSTemplateEntryDto[]{ssTemplateEntryDto});
        when(gwasCatalogRESTService.getSSTemplateEntries(eq(publishedPublication.getPmid()))).thenReturn(ssTemplateEntryDtoList);

        InputStream is = getClass().getClassLoader().getResourceAsStream(GWASDepositionBackendConstants.FILE_TEMPLATE_EXAMPLE);
        FileObject fileObject = new FileObject(RandomStringUtils.randomAlphanumeric(10),
                IOUtils.toByteArray(is));
        when(templateService.retrievePrefilledTemplate(any())).thenReturn(fileObject);

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

        assertEquals(submissionCreationDto.getPublication().getPmid(), actual.getContent().getPublication().getPmid());
        assertEquals(PublicationStatus.UNDER_SUMMARY_STATS_SUBMISSION.name(), actual.getContent().getPublication().getStatus());
        assertEquals(user.getName(), actual.getContent().getCreated().getUser().getName());
        assertEquals(user.getEmail(), actual.getContent().getCreated().getUser().getEmail());
        assertEquals(Status.STARTED.name(), actual.getContent().getSubmissionStatus());
        assertEquals(Status.NA.name(), actual.getContent().getMetadataStatus());
        assertEquals(Status.NA.name(), actual.getContent().getSummaryStatisticsStatus());

        assertEquals(1, actual.getContent().getFiles().size());
        FileUploadDto fileUploadDto = actual.getContent().getFiles().get(0);
        assertEquals(FileUploadType.SUMMARY_STATS_TEMPLATE.name(), fileUploadDto.getType());
        assertEquals(fileObject.getFileName(), fileUploadDto.getFileName());
        assertEquals(FileUploadStatus.VALID.name(), fileUploadDto.getStatus());
        assertEquals(fileObject.getContent().length, fileUploadDto.getFileSize().intValue());
        verify(sumStatsService, times(1)).createGlobusFolder(any());
    }

    /**
     * POST /v1/submissions
     */
    @Test
    public void shouldCreateBodyOfWorkSubmission() throws Exception {
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));
        bodyOfWork.setBowId(gcpCounter.getNext());
        bodyOfWorkRepository.insert(bodyOfWork);

        SubmissionCreationDto submissionCreationDto = new SubmissionCreationDto(null,
                BodyOfWorkDtoAssembler.assemble(bodyOfWork),
                RandomStringUtils.randomAlphanumeric(10));
        String response = mockMvc.perform(post(GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(submissionCreationDto)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<SubmissionDto> actualResource = mapper.readValue(response, new TypeReference<Resource<SubmissionDto>>() {
        });
        SubmissionDto actual = actualResource.getContent();

        assertNull(actual.getPublication());
        assertEquals(SubmissionProvenanceType.BODY_OF_WORK.name(), actual.getProvenanceType());
        assertEquals(user.getName(), actual.getCreated().getUser().getName());
        assertEquals(user.getEmail(), actual.getCreated().getUser().getEmail());
        assertEquals(bodyOfWork.getJournal(), actual.getBodyOfWork().getJournal());
        assertEquals(bodyOfWork.getTitle(), actual.getBodyOfWork().getTitle());
        assertEquals(bodyOfWork.getFirstAuthor().getFirstName(), actual.getBodyOfWork().getFirstAuthor().getFirstName());

        assertTrue(actual.getFiles().isEmpty());
        assertEquals(1, bodyOfWorkRepository.findAll().size());
        verify(sumStatsService, times(1)).createGlobusFolder(any());

        mockMvc.perform(delete(GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_BODY_OF_WORK +
                "/" + bodyOfWork.getBowId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    /**
     * GET /v1/submissions/{submissionId}
     */
    @Test
    public void shouldGetSubmission() throws Exception {
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
        submission.setAssociations(Arrays.asList(new String[]{association.getId()}));
        submission.setSamples(Arrays.asList(new String[]{sample.getId()}));
        submission.setStudies(Arrays.asList(new String[]{study.getId()}));
        submission.setNotes(Arrays.asList(new String[]{note.getId()}));
        submissionService.saveSubmission(submission);

        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId();

        response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        actual = mapper.readValue(response, new TypeReference<Resource<SubmissionDto>>() {
        });

        assertEquals(submissionCreationDto.getPublication().getPmid(), actual.getContent().getPublication().getPmid());
        assertEquals(PublicationStatus.UNDER_SUBMISSION.name(), actual.getContent().getPublication().getStatus());
        assertEquals(user.getName(), actual.getContent().getCreated().getUser().getName());
        assertEquals(user.getEmail(), actual.getContent().getCreated().getUser().getEmail());
        assertEquals(Status.STARTED.name(), actual.getContent().getSubmissionStatus());
        assertEquals(Status.NA.name(), actual.getContent().getMetadataStatus());
        assertEquals(Status.NA.name(), actual.getContent().getSummaryStatisticsStatus());

        assertTrue(actual.getContent().getFiles().isEmpty());
        assertEquals(1, actual.getContent().getSampleCount().intValue());
        assertEquals(1, actual.getContent().getStudyCount().intValue());
        assertEquals(1, actual.getContent().getAssociationCount().intValue());
    }

    /**
     * DELETE /v1/submissions/{submissionId}
     */
    @Test
    public void shouldDeleteSubmission() throws Exception {
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

        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + actual.getContent().getSubmissionId();

        mockMvc.perform(delete(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        Publication publication = publicationService.retrievePublication(actual.getContent().getPublication().getPmid(), false);
        assertEquals(PublicationStatus.ELIGIBLE.name(), publication.getStatus());
    }

    /**
     * DELETE /v1/submissions/{submissionId}
     */
    public void shouldDeleteSubmissionOfPublishedPublication() throws Exception {
        SubmissionCreationDto submissionCreationDto = new SubmissionCreationDto(PublicationDtoAssembler.assemble(publishedPublication),
                null,
                RandomStringUtils.randomAlphanumeric(10));

        SSTemplateEntryDto ssTemplateEntryDto = new SSTemplateEntryDto(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                false);
        List<SSTemplateEntryDto> ssTemplateEntryDtoList = Arrays.asList(new SSTemplateEntryDto[]{ssTemplateEntryDto});
        when(gwasCatalogRESTService.getSSTemplateEntries(eq(publishedPublication.getPmid()))).thenReturn(ssTemplateEntryDtoList);

        InputStream is = getClass().getClassLoader().getResourceAsStream(GWASDepositionBackendConstants.FILE_TEMPLATE_EXAMPLE);
        FileObject fileObject = new FileObject(RandomStringUtils.randomAlphanumeric(10),
                IOUtils.toByteArray(is));
        when(templateService.retrievePrefilledTemplate(any())).thenReturn(fileObject);

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

        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + actual.getContent().getSubmissionId();

        mockMvc.perform(delete(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        Publication publication = publicationService.retrievePublication(actual.getContent().getPublication().getPmid(), false);
        assertEquals(PublicationStatus.PUBLISHED.name(), publication.getStatus());
    }

    /**
     * PUT /v1/submissions/{submissionId}/submit
     */
    @Test
    public void shouldSubmitSubmission() throws Exception {
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

        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + actual.getContent().getSubmissionId() +
                GWASDepositionBackendConstants.API_SUBMIT;

        response = mockMvc.perform(put(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<SubmissionDto> updated = mapper.readValue(response, new TypeReference<Resource<SubmissionDto>>() {
        });

        assertEquals(Status.SUBMITTED.name(), updated.getContent().getSubmissionStatus());
    }

    /**
     * GET /v1/submissions
     */
    @Test
    public void shouldGetSubmissions() throws Exception {
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
        submission.setAssociations(Arrays.asList(new String[]{association.getId()}));
        submission.setSamples(Arrays.asList(new String[]{sample.getId()}));
        submission.setStudies(Arrays.asList(new String[]{study.getId()}));
        submission.setNotes(Arrays.asList(new String[]{note.getId()}));
        submissionService.saveSubmission(submission);

        String endpoint = GeneralCommon.API_V1 + GWASDepositionBackendConstants.API_SUBMISSIONS;

        response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Map<String, String> objectMap = JsonPath.read(response, "$.page");
        assertEquals(1, objectMap.get("totalElements"));
        JSONArray jsonArray = JsonPath.read(response, "$._embedded.submissions[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> submissionData = JsonPath.read(response, "$._embedded.submissions[0]");
        assertEquals(submission.getId(), submissionData.get("submissionId"));
        assertEquals(submission.getSamples().size(), submissionData.get("sample_count"));
        assertEquals(submission.getStudies().size(), submissionData.get("study_count"));
        assertEquals(submission.getAssociations().size(), submissionData.get("association_count"));
        assertEquals(submission.getOverallStatus(), submissionData.get("submission_status"));
        assertEquals(submission.getMetadataStatus(), submissionData.get("metadata_status"));
        assertEquals(submission.getSummaryStatsStatus(), submissionData.get("summary_statistics_status"));
    }
}
