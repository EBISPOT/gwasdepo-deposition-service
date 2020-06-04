package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionType;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.repository.CallbackIdRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SummaryStatsEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.service.*;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockTemplateServiceConfig.class,
        IntegrationTest.MockGWASCatalogRESTServiceConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class})
public class FileUploadsControllerMetadataTest extends IntegrationTest {

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private UserService userService;

    @Autowired
    private TemplateService templateService;

    @Autowired
    private SumStatsService sumStatsService;

    @Autowired
    private SummaryStatsEntryRepository summaryStatsEntryRepository;

    @Autowired
    private CallbackIdRepository callbackIdRepository;

    @Before
    public void setup() {
        super.setup();
        Mockito.reset(sumStatsService);
        Mockito.reset(templateService);
        when(sumStatsService.createGlobusFolder(any())).thenReturn(new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10)));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldUploadFile() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        createMetadataFileUpload(submissionDto);
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithInvalidFile() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("test.pdf").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "test.pdf",
                "pdf", fileAsStream);

        String response =
                mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                        .file(testFile))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();
        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        assertEquals(Status.INVALID.name(), submission.getOverallStatus());
        assertEquals(Status.INVALID.name(), submission.getMetadataStatus());
        assertEquals(Status.INVALID.name(), fileUploadDtoActual.getStatus());
        assertTrue(fileUploadDtoActual.getErrors().get(0).startsWith("Unable to read and process file"));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithNoSchema() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("no_schema.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "no_schema.xlsx",
                "xlsx", fileAsStream);

        String response =
                mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                        .file(testFile))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();
        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        assertEquals(Status.INVALID.name(), submission.getOverallStatus());
        assertEquals(Status.INVALID.name(), submission.getMetadataStatus());
        assertEquals(Status.INVALID.name(), fileUploadDtoActual.getStatus());
        assertTrue(fileUploadDtoActual.getErrors().get(0).startsWith("Invalid file uploaded. Please download a new template from the submission page and try again."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithUnusableSchema() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("valid.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "valid.xlsx",
                "xlsx", fileAsStream);

        Map<String, Map<String, String>> submissionTypes = new HashMap<>();
        Map<String, String> metadataSubmission = new HashMap<>();

        submissionTypes.put(SubmissionType.METADATA.name(), metadataSubmission);
        TemplateSchemaResponseDto templateSchemaResponseDto = new TemplateSchemaResponseDto("1.0",
                submissionTypes);
        when(templateService.retrieveTemplateSchemaInfo(eq("1.0"))).thenReturn(templateSchemaResponseDto);

        when(templateService.retrieveTemplateSchema(eq("1.0"), eq(SubmissionType.METADATA.name()))).thenReturn(null);

        String response =
                mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                        .file(testFile))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();
        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        assertEquals(Status.INVALID.name(), submission.getOverallStatus());
        assertEquals(Status.INVALID.name(), submission.getMetadataStatus());
        assertEquals(Status.INVALID.name(), fileUploadDtoActual.getStatus());
        assertTrue(fileUploadDtoActual.getErrors().get(0).startsWith("Sorry! There is a fault on our end. Please contact gwas-subs@ebi.ac.uk for help."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithNoSchemaVersion1() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("valid.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "valid.xlsx",
                "xlsx", fileAsStream);

        when(templateService.retrieveTemplateSchemaInfo(eq("1.0"))).thenReturn(null);
        String response =
                mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                        .file(testFile))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();
        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        assertEquals(Status.INVALID.name(), submission.getOverallStatus());
        assertEquals(Status.INVALID.name(), submission.getMetadataStatus());
        assertEquals(Status.INVALID.name(), fileUploadDtoActual.getStatus());
        assertTrue(fileUploadDtoActual.getErrors().get(0).startsWith("Invalid file uploaded. Please download a new template from the submission page and try again."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithNoSchemaVersion2() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("valid.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "valid.xlsx",
                "xlsx", fileAsStream);

        Map<String, Map<String, String>> submissionTypes = new HashMap<>();
        TemplateSchemaResponseDto templateSchemaResponseDto = new TemplateSchemaResponseDto("1.0",
                submissionTypes);
        when(templateService.retrieveTemplateSchemaInfo(eq("1.0"))).thenReturn(templateSchemaResponseDto);

        String response =
                mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                        .file(testFile))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();
        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        assertEquals(Status.INVALID.name(), submission.getOverallStatus());
        assertEquals(Status.INVALID.name(), submission.getMetadataStatus());
        assertEquals(Status.INVALID.name(), fileUploadDtoActual.getStatus());
        assertTrue(fileUploadDtoActual.getErrors().get(0).startsWith("Invalid file uploaded. Please download a new template from the submission page and try again."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithInvalidData() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("invalid.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "invalid.xlsx",
                "xlsx", fileAsStream);

        Map<String, Map<String, String>> submissionTypes = new HashMap<>();
        Map<String, String> metadataSubmission = new HashMap<>();

        submissionTypes.put(SubmissionType.METADATA.name(), metadataSubmission);
        TemplateSchemaResponseDto templateSchemaResponseDto = new TemplateSchemaResponseDto("1.0",
                submissionTypes);
        when(templateService.retrieveTemplateSchemaInfo(eq("1.0"))).thenReturn(templateSchemaResponseDto);

        InputStream is = getClass().getClassLoader().getResourceAsStream("schema_v1.json");
        TemplateSchemaDto schema = new ObjectMapper().readValue(is, TemplateSchemaDto.class);
        when(templateService.retrieveTemplateSchema(eq("1.0"), eq(SubmissionType.METADATA.name()))).thenReturn(schema);

        String response =
                mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                        .file(testFile))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();
        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        assertEquals(Status.INVALID.name(), submission.getOverallStatus());
        assertEquals(Status.INVALID.name(), submission.getMetadataStatus());
        assertEquals(Status.INVALID.name(), fileUploadDtoActual.getStatus());
        assertEquals(2, fileUploadDtoActual.getErrors().size());
    }

    private FileUploadDto createMetadataFileUpload(SubmissionDto submissionDto) throws Exception {
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("valid.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "valid.xlsx",
                "xlsx", fileAsStream);

        Map<String, Map<String, String>> submissionTypes = new HashMap<>();
        Map<String, String> metadataSubmission = new HashMap<>();

        submissionTypes.put(SubmissionType.METADATA.name(), metadataSubmission);
        TemplateSchemaResponseDto templateSchemaResponseDto = new TemplateSchemaResponseDto("1.0",
                submissionTypes);
        when(templateService.retrieveTemplateSchemaInfo(eq("1.0"))).thenReturn(templateSchemaResponseDto);

        InputStream is = getClass().getClassLoader().getResourceAsStream("schema_v1.json");
        TemplateSchemaDto schema = new ObjectMapper().readValue(is, TemplateSchemaDto.class);
        when(templateService.retrieveTemplateSchema(eq("1.0"), eq(SubmissionType.METADATA.name()))).thenReturn(schema);

        String callbackId = RandomStringUtils.randomAlphanumeric(10);
        when(sumStatsService.registerStatsForProcessing(any())).thenReturn(callbackId);

        String response =
                mockMvc.perform(MockMvcRequestBuilders.multipart(endpoint)
                        .file(testFile))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString();
        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();

        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);
        assertEquals(Status.VALIDATING.name(), submission.getOverallStatus());
        assertEquals(Status.VALID.name(), submission.getMetadataStatus());
        assertEquals(Status.VALID.name(), fileUploadDtoActual.getStatus());

        assertEquals(2, submission.getStudies().size());
        assertEquals(4, submission.getAssociations().size());
        assertEquals(7, submission.getSamples().size());

        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadDtoActual.getFileUploadId());
        assertEquals(callbackId, fileUpload.getCallbackId());

        List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByFileUploadId(fileUpload.getId());
        assertEquals(2, summaryStatsEntries.size());
        for (SummaryStatsEntry summaryStatsEntry : summaryStatsEntries) {
            assertEquals(callbackId, summaryStatsEntry.getCallbackId());
        }

        List<CallbackId> callbackIds = callbackIdRepository.findAll();
        assertEquals(1, callbackIds.size());
        assertEquals(callbackId, callbackIds.get(0).getCallbackId());

        verify(sumStatsService, times(1)).registerStatsForProcessing(any());

        return fileUploadDtoActual;
    }

}
