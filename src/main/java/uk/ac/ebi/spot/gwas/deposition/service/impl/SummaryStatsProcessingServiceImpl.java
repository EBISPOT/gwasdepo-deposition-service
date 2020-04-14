package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditHelper;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSWrapUpRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSWrapUpRequestEntryDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SummaryStatsRequestEntryDto;
import uk.ac.ebi.spot.gwas.deposition.repository.CallbackIdRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SSTemplateEntryPlaceholderRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SummaryStatsEntryRepository;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SummaryStatsRequestEntryDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.FileUploadsService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.SumStatsService;
import uk.ac.ebi.spot.gwas.deposition.service.SummaryStatsProcessingService;

import java.util.*;

@Service
public class SummaryStatsProcessingServiceImpl implements SummaryStatsProcessingService {

    private static final Logger log = LoggerFactory.getLogger(SummaryStatsProcessingService.class);

    @Autowired(required = false)
    private SumStatsService sumStatsService;

    @Autowired
    private SummaryStatsEntryRepository summaryStatsEntryRepository;

    @Autowired
    private CallbackIdRepository callbackIdRepository;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private SSTemplateEntryPlaceholderRepository ssTemplateEntryPlaceholderRepository;

    @Autowired
    private AuditProxy auditProxy;

    @Override
    @Async
    public void processSummaryStats(Submission submission, String fileUploadId, List<SummaryStatsEntry> summaryStatsEntries) {
        log.info("Processing {} summary stats.", summaryStatsEntries.size());
        log.info("Summary stats service enabled: {}", (sumStatsService != null));

        if (summaryStatsEntries.isEmpty() || sumStatsService == null) {
            if (sumStatsService == null) {
                log.info("Summary Stats Service is not active.");
            }
            submission.setOverallStatus(Status.VALID.name());
            submission.setSummaryStatsStatus(Status.NA.name());
            submissionService.saveSubmission(submission);
            return;
        }
        submission.setSummaryStatsStatus(Status.VALIDATING.name());
        submissionService.saveSubmission(submission);

        List<SummaryStatsRequestEntryDto> list = new ArrayList<>();
        for (SummaryStatsEntry summaryStatsEntry : summaryStatsEntries) {
            summaryStatsEntry = summaryStatsEntryRepository.insert(summaryStatsEntry);
            list.add(SummaryStatsRequestEntryDtoAssembler.assemble(summaryStatsEntry));
        }
        String callbackId = sumStatsService.registerStatsForProcessing(new SummaryStatsRequestDto(list));
        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);

        if (callbackId == null) {
            log.error("Unable to register summary stats for processing. Received NULL callback ID.");

            submission.setOverallStatus(Status.INVALID.name());
            submission.setSummaryStatsStatus(Status.INVALID.name());
            submissionService.saveSubmission(submission);

            List<String> errors = Arrays.asList(new String[]{"Sorry! There is a fault on our end. Please contact gwas-info@ebi.ac.uk for help."});
            fileUpload.setStatus(FileUploadStatus.INVALID.name());
            fileUpload.setErrors(errors);
            fileUploadsService.save(fileUpload);

            auditProxy.addAuditEntry(AuditHelper.fileValidate(submission.getCreated().getUserId(), fileUpload, submission, true, false, errors));
            auditProxy.addAuditEntry(AuditHelper.submissionValidate(submission.getCreated().getUserId(), submission, false));
        } else {
            log.info("Successfully registered {} summary stats with callback ID: {}",
                    summaryStatsEntries.size(), callbackId);
            callbackIdRepository.insert(new CallbackId(callbackId, submission.getId()));

            for (SummaryStatsEntry summaryStatsEntry : summaryStatsEntries) {
                summaryStatsEntry.setCallbackId(callbackId);
                summaryStatsEntryRepository.save(summaryStatsEntry);
            }
            fileUpload.setStatus(FileUploadStatus.VALIDATING.name());
            fileUpload.setCallbackId(callbackId);
            fileUploadsService.save(fileUpload);

            submission.setSummaryStatsStatus(Status.VALIDATING.name());
            submissionService.saveSubmission(submission);
        }
    }

    public void callGlobusWrapUp(Publication publication) {
        log.info("Performing Globus wrap up operation for: {} | {}", publication.getId(), publication.getPmid());
        if (sumStatsService == null) {
            log.info("Summary Stats Service is not active.");
            return;
        }
        Submission submission = submissionService.getSubmission(publication.getId());
        if (submission == null) {
            log.error("Unable to perform Globus wrap up operation. Submission not found for publication: {}", publication.getId());
            return;
        }
        log.info("Found submission: {}", submission.getId());
        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findBySubmissionIdAndCompleted(submission.getId(), true);
        if (!callbackIdOptional.isPresent()) {
            log.error("Unable to perform Globus wrap up operation. Cannot find callback ID for submission: {}", submission.getId());
            return;
        }

        /**
         * Might not work if there are multiple summary stats entries with the same study tag
         */

        Optional<SSTemplateEntryPlaceholder> ssTemplateEntryPlaceholderOptional = ssTemplateEntryPlaceholderRepository.findByPmid(publication.getPmid());
        if (!ssTemplateEntryPlaceholderOptional.isPresent()) {
            log.error("Unable to perform Globus wrap up operation. Study accession data is missing.");
            return;
        }
        if (ssTemplateEntryPlaceholderOptional.get().getSsTemplateEntries() == null) {
            log.error("Unable to perform Globus wrap up operation. Study accession data is missing.");
            return;
        }

        Map<String, String> accessionMap = new LinkedHashMap<>();
        for (SSTemplateEntry ssTemplateEntry : ssTemplateEntryPlaceholderOptional.get().getSsTemplateEntries()) {
            accessionMap.put(ssTemplateEntry.getStudyTag(), ssTemplateEntry.getStudyAccession());
        }

        log.info("Accession map [{}]: {}", callbackIdOptional.get().getCallbackId(), accessionMap);

        List<SSWrapUpRequestEntryDto> ssWrapUpRequestEntryDtos = new ArrayList<>();
        List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByCallbackId(callbackIdOptional.get().getCallbackId());
        for (SummaryStatsEntry summaryStatsEntry : summaryStatsEntries) {
            log.info("Verifying: {}", summaryStatsEntry.getStudyTag());
            String gcst = accessionMap.get(summaryStatsEntry.getStudyTag());
            if (gcst == null) {
                log.error("Unable to add ss data for [{}]. GCST is missing.", summaryStatsEntry.getStudyTag());
                continue;
            }
            ssWrapUpRequestEntryDtos.add(new SSWrapUpRequestEntryDto(summaryStatsEntry.getId(), gcst));
        }

        if (ssWrapUpRequestEntryDtos.isEmpty()) {
            log.error("Unable to perform Globus wrap up operation. No data entry items were populated.");
            return;
        }

        SSWrapUpRequestDto ssWrapUpRequestDto = new SSWrapUpRequestDto(publication.getPmid(),
                publication.getFirstAuthor(), ssWrapUpRequestEntryDtos);
        sumStatsService.wrapUpGlobusSubmission(callbackIdOptional.get().getCallbackId(), ssWrapUpRequestDto);
    }
}
