package uk.ac.ebi.spot.gwas.deposition.rest.unit;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.hateoas.Resource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.domain.User;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.SubmissionsController;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SubmissionDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SubmissionPatchDto;
import uk.ac.ebi.spot.gwas.deposition.service.JWTService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.service.impl.SubmissionAssemblyService;

import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class SubmissionControllerUnitTest {

    @Mock
    private SubmissionService submissionService;

    @Mock
    private UserService userService;

    @Mock
    private SubmissionAssemblyService submissionAssemblyService;

    @Mock
    private JWTService jwtService;

    @InjectMocks
    private SubmissionsController submissionsController;

    @Before
    public void setup() {

        // as we add more tests, extract repeating code for @Test methods and put here
    }

    @Test
    public void shouldPatchAndCreateGlobusFolder() {

        // given
        HttpServletRequest httpServletRequest = new MockHttpServletRequest();
        User user = new User();
        SubmissionPatchDto submissionPatchDto = new SubmissionPatchDto();
        submissionPatchDto.setGlobusEmail(RandomStringUtils.randomAlphanumeric(10));
        Submission submission = new Submission();
        submission.setId(RandomStringUtils.randomAlphanumeric(10));
        submission.setGlobusFolderId(RandomStringUtils.randomAlphanumeric(10));
        SubmissionDto submissionDto = SubmissionDtoAssembler.assemble(submission, null, null,
                null, null, null, null, null);
        when(jwtService.extractUser(anyString())).thenReturn(user);
        when(userService.findUser(any(), anyBoolean())).thenReturn(user);
        when(submissionService.createGlobusFolderForReopenedSubmission(anyString(), any(), anyString())).thenReturn(submission);
        when(submissionAssemblyService.toResource(submission)).thenReturn(new Resource<>(submissionDto));

        // when
        Resource<SubmissionDto> submissionDtoResource = submissionsController.patchSubmission(RandomStringUtils.randomAlphanumeric(10), httpServletRequest, submissionPatchDto);

        // then
        assertEquals(submissionDtoResource.getContent().getGlobusFolder(), submission.getGlobusFolderId());
        assertEquals(submissionDtoResource.getContent().getSubmissionId(), submission.getId());
        verify(submissionService, times(1))
                .createGlobusFolderForReopenedSubmission(anyString(), any(), anyString());
        verifyNoMoreInteractions(submissionService);
    }
}
