package uk.ac.ebi.spot.gwas.deposition.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import uk.ac.ebi.spot.gwas.deposition.constants.PublicationStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.SummaryStatsResponseConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestEntryDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsResponseDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsStatusDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TestUtil {

    public static User user() {
        return new User(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10));
    }

    public static BodyOfWork bodyOfWork(String userId) {
        Author author = new Author(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10));

        return new BodyOfWork(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                author,
                author,
                new ArrayList<>(),
                new ArrayList<>(),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                LocalDate.now(),
                true,
                new Provenance(DateTime.now(), userId));
    }

    public static Publication eligiblePublication() {
        return publication(PublicationStatus.ELIGIBLE.name());
    }

    public static Publication publishedPublication() {
        return publication(PublicationStatus.PUBLISHED.name());
    }

    public static Publication publication(String status) {
        return new Publication(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10) + " " + RandomStringUtils.randomAlphanumeric(10),
                LocalDate.now(),
                new CorrespondingAuthor(RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10)),
                status);
    }

    public static TemplateSchemaDto templateSchemaDto() {
        return new TemplateSchemaDto(RandomStringUtils.randomAlphanumeric(10),
                templateSheetDto(false),
                templateSheetDto(true),
                templateSheetDto(false),
                templateSheetDto(false));
    }

    public static TemplateSchemaResponseDto templateSchemaResponseDto() {
        Map<String, Map<String, String>> map = new HashMap<>();
        Map<String, String> linkMap = new HashMap<>();
        linkMap.put("href", "http://10.43.226.14:8080/v1/template-schema/1.0/METADATA");
        map.put("METADATA", linkMap);

        return new TemplateSchemaResponseDto(RandomStringUtils.randomAlphanumeric(10), map);
    }

    public static TemplateSheetDto templateSheetDto(boolean withSS) {
        return new TemplateSheetDto(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                Arrays.asList(new TemplateColumnDto[]{new TemplateColumnDto(
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        true,
                        true,
                        true,
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        10.0,
                        10.0,
                        10,
                        Arrays.asList(new String[]{
                                RandomStringUtils.randomAlphanumeric(10),
                                RandomStringUtils.randomAlphanumeric(10),
                                RandomStringUtils.randomAlphanumeric(10)
                        }),
                        withSS ? summaryStatsSchemaDto() : null
                )}));
    }

    public static SummaryStatsRequestDto summaryStatsRequestDto() {
        return new SummaryStatsRequestDto(Arrays.asList(new SummaryStatsRequestEntryDto[]{
                new SummaryStatsRequestEntryDto(RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10)),
                new SummaryStatsRequestEntryDto(RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10))
        }));
    }

    public static SummaryStatsSchemaDto summaryStatsSchemaDto() {
        return new SummaryStatsSchemaDto(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10));
    }

    public static SummaryStatsResponseDto summaryStatsResponseDto(String callbackId) {
        return new SummaryStatsResponseDto(callbackId,
                false,
                SummaryStatsResponseConstants.PROCESSING,
                new ArrayList<>(),
                Arrays.asList(new SummaryStatsStatusDto[]{
                        new SummaryStatsStatusDto(
                                RandomStringUtils.randomAlphanumeric(10),
                                RandomStringUtils.randomAlphanumeric(10),
                                RandomStringUtils.randomAlphanumeric(10)),
                        new SummaryStatsStatusDto(
                                RandomStringUtils.randomAlphanumeric(10),
                                RandomStringUtils.randomAlphanumeric(10),
                                RandomStringUtils.randomAlphanumeric(10))
                }));
    }
}
