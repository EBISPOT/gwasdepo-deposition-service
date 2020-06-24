package uk.ac.ebi.spot.gwas.deposition.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.spot.gwas.deposition.config.RestInteractionConfig;
import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.*;
import uk.ac.ebi.spot.gwas.deposition.rest.HttpEntityBuilder;
import uk.ac.ebi.spot.gwas.deposition.rest.RestRequestUtil;
import uk.ac.ebi.spot.gwas.deposition.service.impl.SumStatsServiceImpl;
import uk.ac.ebi.spot.gwas.deposition.util.TestUtil;

import java.util.Arrays;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SumStatsServiceTest {

    @InjectMocks
    private SumStatsService sumStatsService;

    @Mock
    private RestInteractionConfig restInteractionConfig;

    @Mock
    private RestRequestUtil restRequestUtil;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private BackendEmailService backendEmailService;

    private String sumStatsServiceUrl;

    private String sumStatsEndpoint;

    private String globusMkdirEndpoint;

    @Before
    public void setup() {
        sumStatsServiceUrl = "https://gwas-sumstats-service";
        sumStatsEndpoint = "/v1/sum-stats";
        globusMkdirEndpoint = "/v1/sum-stats/globus/mkdir";

        sumStatsService = new SumStatsServiceImpl();
        MockitoAnnotations.initMocks(this);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();

        when(restInteractionConfig.getSumStatsEndpoint()).thenReturn(sumStatsServiceUrl + sumStatsEndpoint);
        when(restInteractionConfig.getGlobusMkdirEndpoint()).thenReturn(sumStatsServiceUrl + globusMkdirEndpoint);
        when(restRequestUtil.httpEntity()).thenReturn(new HttpEntityBuilder(mockRequest, ""));
        doNothing().when(backendEmailService).sendErrorsEmail(any(), any());
    }

    @Test
    public void shouldRetrieveStatsByCallbackId() {
        String callbackId = RandomStringUtils.randomAlphanumeric(5);
        String endpoint = sumStatsServiceUrl + sumStatsEndpoint + "/" + callbackId;

        SummaryStatsResponseDto expected = TestUtil.summaryStatsResponseDto(callbackId);
        ResponseEntity<SummaryStatsResponseDto> response = new ResponseEntity<>(expected, HttpStatus.OK);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.GET), any(), eq(new ParameterizedTypeReference<SummaryStatsResponseDto>() {
        }))).thenReturn(response);

        SummaryStatsResponseDto actual = sumStatsService.retrieveSummaryStatsStatus(callbackId);
        verify(restTemplate, times(1)).exchange(eq(endpoint),
                eq(HttpMethod.GET),
                any(), eq(new ParameterizedTypeReference<SummaryStatsResponseDto>() {
                }));
        assertEquals(actual, expected);
    }

    @Test
    public void shouldWrapUpGlobusSubmission() {
        String callbackId = RandomStringUtils.randomAlphanumeric(5);
        String endpoint = sumStatsServiceUrl + sumStatsEndpoint + "/" + callbackId;
        SSWrapUpRequestDto ssWrapUpRequestDto = new SSWrapUpRequestDto(
                Arrays.asList(new SSWrapUpRequestEntryDto[]{
                        new SSWrapUpRequestEntryDto(RandomStringUtils.randomAlphanumeric(10),
                                RandomStringUtils.randomAlphanumeric(10))
                })
        );

        ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.PUT), any(), eq(new ParameterizedTypeReference<Void>() {
        }))).thenReturn(response);

        sumStatsService.wrapUpGlobusSubmission(callbackId, ssWrapUpRequestDto);
        verify(restTemplate, times(1)).exchange(eq(endpoint),
                eq(HttpMethod.PUT),
                any(), eq(new ParameterizedTypeReference<Void>() {
                }));
    }

    @Test
    public void shouldRegisterForValidation() {
        String callbackId = RandomStringUtils.randomAlphanumeric(5);
        String endpoint = sumStatsServiceUrl + sumStatsEndpoint;

        ResponseEntity<SummaryStatsAckDto> response = new ResponseEntity<>(new SummaryStatsAckDto(callbackId), HttpStatus.CREATED);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(), eq(new ParameterizedTypeReference<SummaryStatsAckDto>() {
        }))).thenReturn(response);

        String actual = sumStatsService.registerStatsForProcessing(TestUtil.summaryStatsRequestDto());
        verify(restTemplate, times(1)).exchange(eq(endpoint),
                eq(HttpMethod.POST),
                any(), eq(new ParameterizedTypeReference<SummaryStatsAckDto>() {
                }));
        assertEquals(callbackId, actual);
    }

    @Test
    public void shouldCreateGlobusFolder() {
        String endpoint = sumStatsServiceUrl + globusMkdirEndpoint;
        SSGlobusFolderDto ssGlobusFolderDto = new SSGlobusFolderDto(
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10)
        );

        SSGlobusFolderRequestResponseDto returnPayload = new SSGlobusFolderRequestResponseDto(RandomStringUtils.randomAlphabetic(10), null);
        ResponseEntity<SSGlobusFolderRequestResponseDto> response = new ResponseEntity<>(returnPayload, HttpStatus.CREATED);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(), eq(new ParameterizedTypeReference<SSGlobusFolderRequestResponseDto>() {
        }))).thenReturn(response);

        SSGlobusResponse ssGlobusFolderResponse = sumStatsService.createGlobusFolder(ssGlobusFolderDto);
        assertEquals(returnPayload.getGlobusOriginID(), ssGlobusFolderResponse.getOutcome());
        assertTrue(ssGlobusFolderResponse.isValid());
        verify(restTemplate, times(1)).exchange(eq(endpoint),
                eq(HttpMethod.POST),
                any(), eq(new ParameterizedTypeReference<SSGlobusFolderRequestResponseDto>() {
                }));
    }

    @Test
    public void shouldFailToCreateGlobusFolder() {
        String endpoint = sumStatsServiceUrl + globusMkdirEndpoint;
        SSGlobusFolderDto ssGlobusFolderDto = new SSGlobusFolderDto(
                RandomStringUtils.randomAlphabetic(10),
                RandomStringUtils.randomAlphabetic(10)
        );

        SSGlobusFolderRequestResponseDto returnPayload = new SSGlobusFolderRequestResponseDto(null, RandomStringUtils.randomAlphabetic(10));
        ResponseEntity<SSGlobusFolderRequestResponseDto> response = new ResponseEntity<>(returnPayload, HttpStatus.OK);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(), eq(new ParameterizedTypeReference<SSGlobusFolderRequestResponseDto>() {
        }))).thenReturn(response);

        SSGlobusResponse ssGlobusFolderResponse = sumStatsService.createGlobusFolder(ssGlobusFolderDto);
        assertEquals(returnPayload.getError(), ssGlobusFolderResponse.getOutcome());
        assertFalse(ssGlobusFolderResponse.isValid());
        verify(restTemplate, times(1)).exchange(eq(endpoint),
                eq(HttpMethod.POST),
                any(), eq(new ParameterizedTypeReference<SSGlobusFolderRequestResponseDto>() {
                }));
    }
}
