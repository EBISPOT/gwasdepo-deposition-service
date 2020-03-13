package uk.ac.ebi.spot.gwas.deposition.scheduler;


import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadType;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SummaryStatsEntryStatus;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsResponseDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsStatusDto;
import uk.ac.ebi.spot.gwas.deposition.repository.*;
import uk.ac.ebi.spot.gwas.deposition.rest.IntegrationTest;
import uk.ac.ebi.spot.gwas.deposition.scheduler.tasks.SSCallbackTask;
import uk.ac.ebi.spot.gwas.deposition.service.BackendEmailService;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {IntegrationTest.MockJWTServiceConfig.class,
        IntegrationTest.MockTaskExecutorConfig.class,
        IntegrationTest.MockSumStatsServiceConfig.class,
        IntegrationTest.MockBackendEmailServiceConfig.class})
public class SSCallbackTaskTest extends IntegrationTest {

    @Autowired
    private SSCallbackTask ssCallbackTask;

    @Autowired
    private SumStatsService sumStatsService;

    @Autowired
    private CallbackIdRepository callbackIdRepository;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Autowired
    private CuratorWhitelistRepository curatorWhitelistRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private SummaryStatsEntryRepository summaryStatsEntryRepository;

    @Autowired
    private BackendEmailService backendEmailService;

    @Autowired
    private PublicationRepository publicationRepository;

    private CallbackId callbackId;

    private SummaryStatsEntry summaryStatsEntry;

    private FileUpload fileUpload;

    @Before
    public void setup() {
        super.setup();

        User user = new User("auto-curator-service@ebi.ac.uk", "auto-curator-service@ebi.ac.uk");
        CuratorWhitelist curatorWhitelist = new CuratorWhitelist();
        curatorWhitelist.setEmail(user.getEmail());
        curatorWhitelistRepository.insert(curatorWhitelist);
        user = userRepository.insert(user);

        Publication publication = new Publication(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                LocalDate.now(),
                new CorrespondingAuthor(RandomStringUtils.randomAlphanumeric(10),
                        RandomStringUtils.randomAlphanumeric(10)),
                RandomStringUtils.randomAlphanumeric(10));
        publication = publicationRepository.insert(publication);

        Submission submission = new Submission();
        submission.setPublicationId(publication.getId());
        submission.setCreated(new Provenance(DateTime.now(), user.getId()));
        submission.setStudies(new ArrayList<>());
        submission.setSamples(new ArrayList<>());
        submission.setAssociations(new ArrayList<>());
        submission.setNotes(new ArrayList<>());
        submission = submissionRepository.insert(submission);

        callbackId = new CallbackId(RandomStringUtils.randomAlphanumeric(10), submission.getId());
        callbackId = callbackIdRepository.insert(callbackId);

        fileUpload = new FileUpload(RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                new Long(1000),
                FileUploadStatus.PROCESSING.name(),
                FileUploadType.SUMMARY_STATS.name());
        fileUpload = fileUploadRepository.insert(fileUpload);
        summaryStatsEntry = new SummaryStatsEntry(fileUpload.getId(),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10),
                RandomStringUtils.randomAlphanumeric(10));
        summaryStatsEntry = summaryStatsEntryRepository.insert(summaryStatsEntry);
        doNothing().when(backendEmailService).sendSuccessEmail(any(), any(), any());
        doNothing().when(backendEmailService).sendFailEmail(any(), any(), any(), any());
    }

    @Test
    public void shouldCheckCallbackIds() {
        SummaryStatsResponseDto summaryStatsResponseDto = new SummaryStatsResponseDto(callbackId.getCallbackId(),
                true,
                Arrays.asList(new SummaryStatsStatusDto[]{
                        new SummaryStatsStatusDto(summaryStatsEntry.getId(),
                                SummaryStatsEntryStatus.VALID.name(),
                                null)
                }));

        when(sumStatsService.retrieveSummaryStatsStatus(callbackId.getCallbackId())).thenReturn(summaryStatsResponseDto);
        ssCallbackTask.checkCallbackIds();
        Optional<Submission> submissionOptional = submissionRepository.findByIdAndArchived(callbackId.getSubmissionId(), false);

        assertTrue(submissionOptional.isPresent());
        assertEquals(Status.VALID.name(), submissionOptional.get().getOverallStatus());
        assertEquals(Status.VALID.name(), submissionOptional.get().getSummaryStatsStatus());

        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findById(callbackId.getId());
        assertTrue(callbackIdOptional.isPresent());
        assertTrue(callbackIdOptional.get().isCompleted());
    }

    @Test
    public void shouldFailToCheckCallbackIdsWithInvalidSS() {
        SummaryStatsResponseDto summaryStatsResponseDto = new SummaryStatsResponseDto(callbackId.getCallbackId(),
                true,
                Arrays.asList(new SummaryStatsStatusDto[]{
                        new SummaryStatsStatusDto(summaryStatsEntry.getId(),
                                SummaryStatsEntryStatus.INVALID.name(),
                                "ERROR")
                }));

        when(sumStatsService.retrieveSummaryStatsStatus(callbackId.getCallbackId())).thenReturn(summaryStatsResponseDto);
        ssCallbackTask.checkCallbackIds();
        Optional<Submission> submissionOptional = submissionRepository.findByIdAndArchived(callbackId.getSubmissionId(), false);

        assertTrue(submissionOptional.isPresent());
        assertEquals(Status.INVALID.name(), submissionOptional.get().getOverallStatus());
        assertEquals(Status.INVALID.name(), submissionOptional.get().getSummaryStatsStatus());

        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findById(callbackId.getId());
        assertTrue(callbackIdOptional.isPresent());
        assertTrue(callbackIdOptional.get().isCompleted());

        Optional<FileUpload> fileUploadOptional = fileUploadRepository.findById(fileUpload.getId());
        assertEquals(FileUploadStatus.INVALID.name(), fileUploadOptional.get().getStatus());
        assertEquals(1, fileUploadOptional.get().getErrors().size());
    }

    @Test
    public void shouldNotCompleteCheck() {
        SummaryStatsResponseDto summaryStatsResponseDto = new SummaryStatsResponseDto(callbackId.getCallbackId(),
                false,
                Arrays.asList(new SummaryStatsStatusDto[]{
                        new SummaryStatsStatusDto(summaryStatsEntry.getId(),
                                SummaryStatsEntryStatus.VALID.name(),
                                null)
                }));

        when(sumStatsService.retrieveSummaryStatsStatus(callbackId.getCallbackId())).thenReturn(summaryStatsResponseDto);
        ssCallbackTask.checkCallbackIds();
        Optional<Submission> submissionOptional = submissionRepository.findByIdAndArchived(callbackId.getSubmissionId(), false);

        assertTrue(submissionOptional.isPresent());
        assertNotEquals(Status.VALID.name(), submissionOptional.get().getOverallStatus());
        assertNotEquals(Status.VALID.name(), submissionOptional.get().getSummaryStatsStatus());

        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findById(callbackId.getId());
        assertTrue(callbackIdOptional.isPresent());
    }

}
