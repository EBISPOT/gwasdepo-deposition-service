package uk.ac.ebi.spot.gwas.deposition.service;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.spot.gwas.deposition.domain.SSGlobusResponse;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.service.impl.SubmissionServiceImpl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class SubmissionServiceTest {

    @Mock
    private CuratorAuthService curatorAuthService;

    @Mock
    private SumStatsService sumStatsService;

    @Spy
    @InjectMocks
    private SubmissionServiceImpl submissionService;

    @Before
    public void setup() {}

    @Test
    public void shouldReturnSubmission() {

        // given
        when(curatorAuthService.isCurator(any())).thenReturn(true);
        SSGlobusResponse ssGlobusResponse = new SSGlobusResponse(true, RandomStringUtils.randomAlphanumeric(10));
        when(sumStatsService.createGlobusFolder(any())).thenReturn(ssGlobusResponse);
        Submission s = new Submission();
        doReturn(s).when(submissionService).getSubmission(anyString());
        doReturn(s).when(submissionService).saveSubmission(any(), anyString());
        User user = new User();
        user.setId(RandomStringUtils.randomAlphanumeric(10));

        // when
        Submission submission = submissionService.createGlobusFolderForReopenedSubmission(RandomStringUtils.randomAlphanumeric(10),
                user, RandomStringUtils.randomAlphanumeric(10));

        // then
        assertEquals(submission.getGlobusOriginId(), ssGlobusResponse.getOutcome());
    }
}
