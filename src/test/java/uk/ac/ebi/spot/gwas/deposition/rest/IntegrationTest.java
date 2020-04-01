package uk.ac.ebi.spot.gwas.deposition.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.ac.ebi.spot.gwas.deposition.Application;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.GeneralCommon;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionCreationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.repository.*;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.scheduler.tasks.SSCallbackTask;
import uk.ac.ebi.spot.gwas.deposition.service.*;
import uk.ac.ebi.spot.gwas.deposition.util.TestUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ContextConfiguration(classes = {Application.class})
public abstract class IntegrationTest {

    @Configuration
    public static class MockJWTServiceConfig {

        @Bean
        public JWTService jwtService() {
            return mock(JWTService.class);
        }
    }

    @Configuration
    public static class MockTaskExecutorConfig {

        @Bean
        public TaskExecutor taskExecutor() {
            return new SyncTaskExecutor();
        }
    }

    @Configuration
    public static class MockGWASCatalogRESTServiceConfig {

        @Bean
        public GWASCatalogRESTService gwasCatalogRESTService() {
            return mock(GWASCatalogRESTService.class);
        }
    }

    @Configuration
    public static class MockTemplateServiceConfig {

        @Bean
        public TemplateService templateService() {
            return mock(TemplateService.class);
        }
    }

    @Configuration
    public static class MockSumStatsServiceConfig {

        @Bean
        public SumStatsService sumStatsService() {
            return mock(SumStatsService.class);
        }
    }

    @Configuration
    public static class MockBackendEmailServiceConfig {

        @Bean
        public BackendEmailService backendEmailService() {
            return mock(BackendEmailService.class);
        }
    }

    @Configuration
    public static class MockSSCallbackTaskConfig {

        @Bean
        public SSCallbackTask ssCallbackTask() {
            return mock(SSCallbackTask.class);
        }
    }

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private JWTService jwtService;

    @Autowired
    protected StudyRepository studyRepository;

    @Autowired
    protected SampleRepository sampleRepository;

    @Autowired
    protected AssociationRepository associationRepository;

    @Autowired
    protected NoteRepository noteRepository;

    @Autowired
    protected BodyOfWorkRepository bodyOfWorkRepository;

    protected MockMvc mockMvc;

    protected ObjectMapper mapper;

    protected User user;

    protected Publication eligiblePublication;

    protected Publication publishedPublication;

    protected BodyOfWork bodyOfWork;

    protected Study study;

    protected Note note;

    protected Sample sample;

    protected Association association;

    @Before
    public void setup() {
        mongoTemplate.getDb().drop();
        TextIndexDefinition textIndex = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("firstAuthor")
                .build();
        mongoTemplate.indexOps(Publication.class).ensureIndex(textIndex);
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

        user = userRepository.insert(TestUtil.user());
        eligiblePublication = publicationRepository.insert(TestUtil.eligiblePublication());
        publishedPublication = publicationRepository.insert(TestUtil.publishedPublication());
        bodyOfWork = bodyOfWorkRepository.insert(TestUtil.bodyOfWork(user.getId()));

        when(jwtService.extractUser(any())).thenReturn(user);

        createPrerequisites();
    }

    private void createPrerequisites() {
        study = new Study();
        study.setStudyTag(RandomStringUtils.randomAlphanumeric(10));
        study.setGenotypingTechnology(RandomStringUtils.randomAlphanumeric(10));
        study.setArrayManufacturer(RandomStringUtils.randomAlphanumeric(10));
        study.setArrayInformation(RandomStringUtils.randomAlphanumeric(10));
        study.setImputation(false);
        study.setVariantCount(10);
        study.setStatisticalModel(RandomStringUtils.randomAlphanumeric(10));
        study.setStudyDescription(RandomStringUtils.randomAlphanumeric(10));
        study.setTrait(RandomStringUtils.randomAlphanumeric(10));
        study.setEfoTrait(RandomStringUtils.randomAlphanumeric(10));
        study.setBackgroundTrait(RandomStringUtils.randomAlphanumeric(10));
        study.setBackgroundEfoTrait(RandomStringUtils.randomAlphanumeric(10));
        study.setSummaryStatisticsFile(RandomStringUtils.randomAlphanumeric(10));
        study.setSummaryStatisticsAssembly(RandomStringUtils.randomAlphanumeric(10));
        study = studyRepository.insert(study);

        note = new Note();
        note.setNote(RandomStringUtils.randomAlphanumeric(10));
        note.setNoteSubject(RandomStringUtils.randomAlphanumeric(10));
        note.setStatus(RandomStringUtils.randomAlphanumeric(10));
        note.setStudyTag(RandomStringUtils.randomAlphanumeric(10));
        note = noteRepository.insert(note);

        sample = new Sample();
        sample.setStudyTag(RandomStringUtils.randomAlphanumeric(10));
        sample.setStage(RandomStringUtils.randomAlphanumeric(10));
        sample.setSize(10);
        sample.setCases(10);
        sample.setControls(10);
        sample.setSampleDescription(RandomStringUtils.randomAlphanumeric(10));
        sample.setAncestryCategory(RandomStringUtils.randomAlphanumeric(10));
        sample.setAncestry(RandomStringUtils.randomAlphanumeric(10));
        sample.setAncestryDescription(RandomStringUtils.randomAlphanumeric(10));
        sample.setCountryRecruitement(RandomStringUtils.randomAlphanumeric(10));
        sample = sampleRepository.insert(sample);

        association = new Association();
        association.setStudyTag(RandomStringUtils.randomAlphanumeric(10));
        association.setHaplotypeId(RandomStringUtils.randomAlphanumeric(10));
        association.setVariantId(RandomStringUtils.randomAlphanumeric(10));
        association.setPvalue("10.0");
        association.setPvalueText(RandomStringUtils.randomAlphanumeric(10));
        association.setProxyVariant(RandomStringUtils.randomAlphanumeric(10));
        association.setEffectAllele(RandomStringUtils.randomAlphanumeric(10));
        association.setOtherAllele(RandomStringUtils.randomAlphanumeric(10));
        association.setEffectAlleleFrequency(10.0);
        association.setOddsRatio(10.0);
        association.setBeta(10.0);
        association.setBetaUnit(RandomStringUtils.randomAlphanumeric(10));
        association.setCiLower(10.0);
        association.setCiUpper(10.0);
        association.setStandardError(10.0);
        association = associationRepository.insert(association);
    }

    protected SubmissionDto createSubmissionFromEligible() throws Exception {
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
        return actual.getContent();
    }

}
