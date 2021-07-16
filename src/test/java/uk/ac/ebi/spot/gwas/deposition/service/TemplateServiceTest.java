package uk.ac.ebi.spot.gwas.deposition.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.exceptions.base.MockitoAssertionError;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.client.RestTemplate;
import uk.ac.ebi.spot.gwas.deposition.config.RestInteractionConfig;
import uk.ac.ebi.spot.gwas.deposition.domain.FileObject;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateEntryDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSTemplateRequestStudyDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaResponseDto;
import uk.ac.ebi.spot.gwas.deposition.rest.HttpEntityBuilder;
import uk.ac.ebi.spot.gwas.deposition.rest.RestRequestUtil;
import uk.ac.ebi.spot.gwas.deposition.service.impl.TemplateServiceImpl;
import uk.ac.ebi.spot.gwas.deposition.util.TestUtil;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TemplateServiceTest {

    @InjectMocks
    private TemplateService templateService;

    @Mock
    private RestInteractionConfig restInteractionConfig;

    @Mock
    private RestRequestUtil restRequestUtil;

    @Mock
    private RestTemplate restTemplate;

    private String templateServiceUrl;

    private String templateSchemaEndpoint;

    private String prefilledTemplateEndpoint;

    @Before
    public void setup() {
        templateServiceUrl = "http://gwas-template-service:8080";
        templateSchemaEndpoint = "/v1/template-schema";
        prefilledTemplateEndpoint = "/v1/prefilled-template";

        templateService = new TemplateServiceImpl();
        MockitoAnnotations.initMocks(this);

        MockHttpServletRequest mockRequest = new MockHttpServletRequest();

        when(restInteractionConfig.getTemplateSchemaEndpoint()).thenReturn(templateServiceUrl + templateSchemaEndpoint);
        when(restInteractionConfig.getPrefilledTemplateEndpoint()).thenReturn(templateServiceUrl + prefilledTemplateEndpoint);
        when(restRequestUtil.httpEntity()).thenReturn(new HttpEntityBuilder(mockRequest, ""));
    }

    @Test
    public void shouldRetrieveTemplateSchema() {
        String version = RandomStringUtils.randomAlphanumeric(5);
        String endpoint = templateServiceUrl + templateSchemaEndpoint + "/" + version + "/" + "METADATA";

        TemplateSchemaDto expected = TestUtil.templateSchemaDto();
        ResponseEntity<TemplateSchemaDto> response = new ResponseEntity<>(expected, HttpStatus.OK);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.GET), any(), eq(new ParameterizedTypeReference<TemplateSchemaDto>() {
        }))).thenReturn(response);

        TemplateSchemaDto actual = templateService.retrieveTemplateSchema(version, "METADATA");
        try {
            verify(restTemplate, times(1)).exchange(eq(endpoint),
                    eq(HttpMethod.GET),
                    any(), eq(new ParameterizedTypeReference<TemplateSchemaDto>() {
                    }));
        } catch (MockitoAssertionError e) {
            throw new MockitoAssertionError("A GET request should have been sent to the specified service endpoint !" +
                    " ERROR: " + e.getMessage());
        }
        assertEquals(actual, expected);
    }

    @Test
    public void shouldRetrieveTemplateSchemaInfo() {
        String version = RandomStringUtils.randomAlphanumeric(5);
        String endpoint = templateServiceUrl + templateSchemaEndpoint + "/" + version;

        TemplateSchemaResponseDto expected = TestUtil.templateSchemaResponseDto();
        ResponseEntity<TemplateSchemaResponseDto> response = new ResponseEntity<>(expected, HttpStatus.OK);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.GET), any(), eq(new ParameterizedTypeReference<TemplateSchemaResponseDto>() {
        }))).thenReturn(response);

        TemplateSchemaResponseDto actual = templateService.retrieveTemplateSchemaInfo(version);
        try {
            verify(restTemplate, times(1)).exchange(eq(endpoint),
                    eq(HttpMethod.GET),
                    any(), eq(new ParameterizedTypeReference<TemplateSchemaResponseDto>() {
                    }));
        } catch (MockitoAssertionError e) {
            throw new MockitoAssertionError("A GET request should have been sent to the specified service endpoint !" +
                    " ERROR: " + e.getMessage());
        }
        assertEquals(actual, expected);
    }

    @Test
    public void shouldRetrievePrefilledTemplate() {
        String endpoint = templateServiceUrl + prefilledTemplateEndpoint;

        SSTemplateRequestDto ssTemplateRequestDto = new SSTemplateRequestDto(true,
                new SSTemplateRequestStudyDto(
                        Arrays.asList(new SSTemplateEntryDto[]{
                                new SSTemplateEntryDto(RandomStringUtils.randomAlphanumeric(10),
                                        RandomStringUtils.randomAlphanumeric(10),
                                        RandomStringUtils.randomAlphanumeric(10),
                                        RandomStringUtils.randomAlphanumeric(10),
                                        false)
                        })));

        FileObject expected = new FileObject(RandomStringUtils.randomAlphanumeric(10), new byte[10]);
        ResponseEntity<byte[]> response = new ResponseEntity<>(expected.getContent(), HttpStatus.OK);
        when(restTemplate.exchange(eq(endpoint), eq(HttpMethod.POST), any(), eq(new ParameterizedTypeReference<byte[]>() {
        }))).thenReturn(response);

        FileObject actual = templateService.retrievePrefilledTemplate(ssTemplateRequestDto);
        try {
            verify(restTemplate, times(1)).exchange(eq(endpoint),
                    eq(HttpMethod.POST),
                    any(), eq(new ParameterizedTypeReference<byte[]>() {
                    }));
        } catch (MockitoAssertionError e) {
            throw new MockitoAssertionError("A GET request should have been sent to the specified service endpoint !" +
                    " ERROR: " + e.getMessage());
        }
        assertEquals(actual.getContent(), expected.getContent());
    }
}
