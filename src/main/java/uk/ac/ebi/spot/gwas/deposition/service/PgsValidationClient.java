package uk.ac.ebi.spot.gwas.deposition.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.ParameterizedTypeReference;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PgsValidationClient {

    @Value("${pgs.validator.url:https://deposition-validator-dot-pgs-catalog.appspot.com/validate_metadata}")
    private String validatorUrl;
    private final RestTemplateBuilder builder;
    private static final String FIELD_VALIDATION_STATUS            = "validationStatus";
    private static final String FIELD_ERROR_MESSAGES   = "errorMessages";
    private static final String FIELD_WARNING_MESSAGES = "warningMessages";
    private static final String FIELD_PCST_IDS         = "pcstIds";

    /**
     * Send the uploaded metadata file to the external PGS validator.
     * Returns a map with keys: validationStatus, pcstIds (List<String>)
     * or, on failure, validationStatus=ERROR and message=... .
     */
    public Map<String, Object> validate(MultipartFile file,
                                        HttpServletRequest req) throws IOException {

        RestTemplate rest = builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new MultipartInputStreamFileResource(
                file.getInputStream(), file.getOriginalFilename(), file.getSize()));

        HttpHeaders hdr = new HttpHeaders();
        hdr.setContentType(MediaType.MULTIPART_FORM_DATA);
        Optional.ofNullable(req.getHeader(HttpHeaders.AUTHORIZATION))
                .ifPresent(token -> hdr.set(HttpHeaders.AUTHORIZATION, token));

        HttpEntity<?> entity = new HttpEntity<>(body, hdr);

        try {
            ResponseEntity<Map<String, Object>> rsp =
                    rest.exchange(
                            validatorUrl,
                            HttpMethod.POST,
                            entity,
                            new ParameterizedTypeReference<Map<String, Object>>() {});

            Map<String, Object> remote = rsp.getBody() != null
                    ? rsp.getBody()
                    : Collections.<String, Object>emptyMap();

            Map<String, Object> out = new HashMap<String, Object>();
            boolean valid = Boolean.TRUE.equals(remote.get("valid"));

            out.put(FIELD_VALIDATION_STATUS,  valid ? "VALID" : "INVALID");
            out.put(FIELD_PCST_IDS,           Collections.emptyList());    // reserved
            out.put(FIELD_ERROR_MESSAGES,     remote.get(FIELD_ERROR_MESSAGES));
            out.put(FIELD_WARNING_MESSAGES,   remote.get(FIELD_WARNING_MESSAGES));

            log.info("PGS validator [{}] {}", out.get(FIELD_VALIDATION_STATUS),
                    valid ? "" : remote.get(FIELD_ERROR_MESSAGES));
            return out;

        } catch (RestClientResponseException ex) {
            log.warn("PGS validator returned {} — body: {}",
                    ex.getRawStatusCode(), ex.getResponseBodyAsString());

            Map<String,Object> err = new HashMap<>();
            err.put(FIELD_VALIDATION_STATUS,  "ERROR");
            err.put("message",           ex.getResponseBodyAsString());
            err.put(FIELD_PCST_IDS,           Collections.emptyList());
            err.put(FIELD_ERROR_MESSAGES,     Collections.emptyMap());
            err.put(FIELD_WARNING_MESSAGES,   Collections.emptyMap());
            return err;
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Helper class to wrap MultipartFile as a Spring Resource
    // ─────────────────────────────────────────────────────────────────────────
    private static final class MultipartInputStreamFileResource
            extends org.springframework.core.io.InputStreamResource {

        private final String filename;
        private final long contentLength;

        MultipartInputStreamFileResource(InputStream inputStream,
                                         String filename,
                                         long contentLength) {
            super(inputStream);
            this.filename = filename;
            this.contentLength = contentLength;
        }
        @Override public String getFilename()     { return filename; }
        @Override public long   contentLength()   { return contentLength; }
    }
}
