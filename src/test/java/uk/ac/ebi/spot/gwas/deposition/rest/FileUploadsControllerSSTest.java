package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ebi.spot.gwas.deposition.constants.*;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionCreationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.repository.CallbackIdRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SummaryStatsEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockTemplateServiceConfig.class,
        IntegrationTest.MockGWASCatalogRESTServiceConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class})
public class FileUploadsControllerSSTest extends IntegrationTest {

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
    private GWASCatalogRESTService gwasCatalogRESTService;

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
    public void shouldFailWithInvalidFile() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromPublished();
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
        assertEquals(Status.INVALID.name(), submission.getSummaryStatsStatus());

        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadDtoActual.getFileUploadId());
        assertEquals(Status.INVALID.name(), fileUpload.getStatus());
        assertTrue(fileUpload.getErrors().get(0).startsWith("Unable to read and process file"));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithNoSchema() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromPublished();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("ss_no_schema.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "ss_no_schema.xlsx",
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
        assertEquals(Status.INVALID.name(), submission.getSummaryStatsStatus());

        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadDtoActual.getFileUploadId());
        assertEquals(Status.INVALID.name(), fileUpload.getStatus());
        assertTrue(fileUpload.getErrors().get(0).startsWith("Invalid file uploaded. Please download a new template from the submission page and try again."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithSchemaMistmatch() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromPublished();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("ss_wrong_schema.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "ss_wrong_schema.xlsx",
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
        assertEquals(Status.INVALID.name(), submission.getSummaryStatsStatus());

        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadDtoActual.getFileUploadId());
        assertEquals(Status.INVALID.name(), fileUpload.getStatus());
        assertTrue(fileUpload.getErrors().get(0).startsWith("Invalid file uploaded. Please download a new template from the submission page and try again."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithNoSchemaVersion1() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromPublished();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("ss_data.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "ss_data.xlsx",
                "xlsx", fileAsStream);

        when(templateService.retrieveTemplateSchemaInfo(eq("1.2"))).thenReturn(null);
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
        assertEquals(Status.INVALID.name(), submission.getSummaryStatsStatus());

        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadDtoActual.getFileUploadId());
        assertEquals(Status.INVALID.name(), fileUpload.getStatus());
        assertTrue(fileUpload.getErrors().get(0).startsWith("Invalid file uploaded. Please download a new template from the submission page and try again."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithNoSchemaVersion2() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromPublished();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("ss_data.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "ss_data.xlsx",
                "xlsx", fileAsStream);

        Map<String, Map<String, String>> submissionTypes = new HashMap<>();
        Map<String, String> metadataSubmission = new HashMap<>();

        submissionTypes.put(SubmissionType.SUMMARY_STATS.name(), metadataSubmission);
        TemplateSchemaResponseDto templateSchemaResponseDto = new TemplateSchemaResponseDto("1.2",
                submissionTypes);
        when(templateService.retrieveTemplateSchemaInfo(eq("1.2"))).thenReturn(templateSchemaResponseDto);
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
        assertEquals(Status.INVALID.name(), submission.getSummaryStatsStatus());

        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadDtoActual.getFileUploadId());
        assertEquals(Status.INVALID.name(), fileUpload.getStatus());
        assertTrue(fileUpload.getErrors().get(0).startsWith("Sorry! There is a fault on our end. Please contact gwas-subs@ebi.ac.uk for help."));
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldFailWithInvalidData() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromPublished();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("ss_invalid.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "ss_invalid.xlsx",
                "xlsx", fileAsStream);

        Map<String, Map<String, String>> submissionTypes = new HashMap<>();
        Map<String, String> metadataSubmission = new HashMap<>();

        submissionTypes.put(SubmissionType.SUMMARY_STATS.name(), metadataSubmission);
        TemplateSchemaResponseDto templateSchemaResponseDto = new TemplateSchemaResponseDto("1.2",
                submissionTypes);
        when(templateService.retrieveTemplateSchemaInfo(eq("1.2"))).thenReturn(templateSchemaResponseDto);

        InputStream is = getClass().getClassLoader().getResourceAsStream("schema_v1.2_SS.json");
        TemplateSchemaDto schema = new ObjectMapper().readValue(is, TemplateSchemaDto.class);
        when(templateService.retrieveTemplateSchema(eq("1.2"), eq(SubmissionType.SUMMARY_STATS.name()))).thenReturn(schema);

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

        assertEquals(Status.INVALID.name(), submission.getOverallStatus());
        assertEquals(Status.INVALID.name(), submission.getSummaryStatsStatus());

        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadDtoActual.getFileUploadId());
        assertEquals(Status.INVALID.name(), fileUpload.getStatus());
    }

    /**
     * POST /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldHandleSummaryStats() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromPublished();
        String endpoint = GeneralCommon.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        InputStream fileAsStream = new ClassPathResource("ss_data.xlsx").getInputStream();
        MockMultipartFile testFile = new MockMultipartFile("file", "ss_data.xlsx",
                "xlsx", fileAsStream);

        Map<String, Map<String, String>> submissionTypes = new HashMap<>();
        Map<String, String> metadataSubmission = new HashMap<>();

        submissionTypes.put(SubmissionType.SUMMARY_STATS.name(), metadataSubmission);
        TemplateSchemaResponseDto templateSchemaResponseDto = new TemplateSchemaResponseDto("1.2",
                submissionTypes);
        when(templateService.retrieveTemplateSchemaInfo(eq("1.2"))).thenReturn(templateSchemaResponseDto);

        InputStream is = getClass().getClassLoader().getResourceAsStream("schema_v1.2_SS.json");
        TemplateSchemaDto schema = new ObjectMapper().readValue(is, TemplateSchemaDto.class);
        when(templateService.retrieveTemplateSchema(eq("1.2"), eq(SubmissionType.SUMMARY_STATS.name()))).thenReturn(schema);

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
        assertEquals(Status.VALIDATING.name(), submission.getSummaryStatsStatus());
        assertEquals(FileUploadStatus.PROCESSING.name(), fileUploadDtoActual.getStatus());

        assertEquals(2, submission.getStudies().size());
        assertEquals(0, submission.getAssociations().size());
        assertEquals(0, submission.getSamples().size());

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
    }


    private SubmissionDto createSubmissionFromPublished() throws Exception {
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

        InputStream is = getClass().getClassLoader().getResourceAsStream("ss_template.xlsx");
        FileObject fileObject = new FileObject("ss_template.xlsx", IOUtils.toByteArray(is));
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
        return actual.getContent();
    }

}
