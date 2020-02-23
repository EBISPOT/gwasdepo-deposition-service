package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.ac.ebi.spot.gwas.deposition.constants.*;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.repository.CallbackIdRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SummaryStatsEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.scheduler.tasks.SSCallbackTask;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockTemplateServiceConfig.class,
        IntegrationTest.MockGWASCatalogRESTServiceConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class,
        IntegrationTest.MockSSCallbackTaskConfig.class})
public class FileUploadsControllerTest extends IntegrationTest {

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

    @Autowired
    private SSCallbackTask ssCallbackTask;

    @Before
    public void setup() {
        super.setup();
        Mockito.reset(sumStatsService);
        Mockito.reset(templateService);
        doNothing().when(ssCallbackTask).checkCallbackIds();
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
     * GET /v1/submissions/{submissionId}/uploads/{fileUploadId}
     */
    @Test
    public void shouldGetFileUpload() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        FileUploadDto fileUploadDto = createMetadataFileUpload(submissionDto);

        String endpoint = GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS +
                "/" + fileUploadDto.getFileUploadId();

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Resource<FileUploadDto> actual = mapper.readValue(response, new TypeReference<Resource<FileUploadDto>>() {
        });

        FileUploadDto fileUploadDtoActual = actual.getContent();
        assertEquals(fileUploadDto.getErrors(), fileUploadDtoActual.getErrors());
        assertEquals(fileUploadDto.getFileUploadId(), fileUploadDtoActual.getFileUploadId());
        assertEquals(FileUploadStatus.VALIDATING.name(), fileUploadDtoActual.getStatus());
    }

    /**
     * GET /v1/submissions/{submissionId}/uploads
     */
    @Test
    public void shouldGetFileUploads() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        FileUploadDto fileUploadDto = createMetadataFileUpload(submissionDto);

        String endpoint = GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS;

        String response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JSONArray jsonArray = JsonPath.read(response, "$._embedded.fileUploads[*]");
        assertEquals(1, jsonArray.size());
        Map<String, String> fileUploadData = JsonPath.read(response, "$._embedded.fileUploads[0]");
        assertEquals(fileUploadDto.getFileUploadId(), fileUploadData.get("fileUploadId"));
        assertEquals(FileUploadType.METADATA.name(), fileUploadData.get("type"));
        assertEquals(FileUploadStatus.VALIDATING.name(), fileUploadData.get("status"));
        assertEquals("valid.xlsx", fileUploadData.get("fileName"));
    }

    /**
     * GET /v1/submissions/{submissionId}/uploads/{fileUploadId}/download
     */
    @Test
    public void shouldDownloadFile() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        FileUploadDto fileUploadDto = createMetadataFileUpload(submissionDto);

        String endpoint = GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS +
                "/" + fileUploadDto.getFileUploadId() +
                GWASDepositionBackendConstants.API_DOWNLOAD;

        MockHttpServletResponse response = mockMvc.perform(get(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        byte[] body = response.getContentAsByteArray();
        assertEquals(fileUploadDto.getFileSize().intValue(), body.length);
        assertEquals("attachment; filename=" + fileUploadDto.getFileName(),
                response.getHeader(HttpHeaders.CONTENT_DISPOSITION));

        InputStream fileAsStream = new ClassPathResource("valid.xlsx").getInputStream();
        assertTrue(Arrays.equals(IOUtils.toByteArray(fileAsStream), body));
    }

    /**
     * DELETE /v1/submissions/{submissionId}/uploads/{fileUploadId}
     */
    @Test
    public void shouldDeleteFileUpload() throws Exception {
        SubmissionDto submissionDto = createSubmissionFromEligible();
        FileUploadDto fileUploadDto = createMetadataFileUpload(submissionDto);

        String endpoint = GWASDepositionBackendConstants.API_V1 +
                GWASDepositionBackendConstants.API_SUBMISSIONS +
                "/" + submissionDto.getSubmissionId() +
                GWASDepositionBackendConstants.API_UPLOADS +
                "/" + fileUploadDto.getFileUploadId();

        doNothing().when(sumStatsService).cleanUp(any());

        mockMvc.perform(delete(endpoint)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        User user = userService.findUser(new User(submissionDto.getCreated().getUser().getName(),
                submissionDto.getCreated().getUser().getEmail()), false);
        Submission submission = submissionService.getSubmission(submissionDto.getSubmissionId(), user);

        assertEquals(Status.STARTED.name(), submission.getOverallStatus());
        assertEquals(Status.NA.name(), submission.getMetadataStatus());
        assertEquals(Status.NA.name(), submission.getSummaryStatsStatus());
        assertTrue(submission.getFileUploads().isEmpty());
        assertEquals(0, submission.getStudies().size());
        assertEquals(0, submission.getAssociations().size());
        assertEquals(0, submission.getSamples().size());

        List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByFileUploadId(fileUploadDto.getFileUploadId());
        assertTrue(summaryStatsEntries.isEmpty());
    }

    private FileUploadDto createMetadataFileUpload(SubmissionDto submissionDto) throws Exception {
        String endpoint = GWASDepositionBackendConstants.API_V1 +
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
