package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadType;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.SSTemplateCuratorRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.SSTemplateCuratorRequestStudyDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.gcst.SSTemplateGCSTRequestDto;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.gcst.SSTemplateGCSTRequestStudyDto;
import uk.ac.ebi.spot.gwas.deposition.repository.StudyRepository;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SSTemplateGCSTStudyDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.StudyDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.FileUploadsService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.TemplatePrefillService;
import uk.ac.ebi.spot.gwas.deposition.service.TemplateService;
import uk.ac.ebi.spot.gwas.deposition.util.StudyCollector;

import java.io.ByteArrayInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TemplatePrefillServiceImpl implements TemplatePrefillService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    @Autowired
    private SubmissionService submissionService;

    @Autowired(required = false)
    private TemplateService templateService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private StudyRepository studyRepository;

    @Override
    public void prefill(String submissionId, User user) {
        log.info("Pre-filling: {}", submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);

        StudyCollector studyCollector = new StudyCollector();
        Stream<Study> studyStream = studyRepository.readBySubmissionId(submissionId);
        studyStream.forEach(study -> studyCollector.add(study));
        studyStream.close();

        try {
            FileObject fileObject = templateService.retrieveCuratorPrefilledTemplate(new SSTemplateCuratorRequestDto(true,
                    new SSTemplateCuratorRequestStudyDto(studyCollector.getStudyList().stream()
                            .map(StudyDtoAssembler::assemble).collect(Collectors.toList()))));
            if (fileObject == null) {
                log.error("No file object received from the template service!");
                return;
            }

            FileUpload fileUpload = fileUploadsService.storeFile(new ByteArrayInputStream(fileObject.getContent()),
                    fileObject.getFileName(), "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fileObject.getContent().length, FileUploadType.METADATA_FILLED.name());
            fileUpload.setStatus(FileUploadStatus.VALID.name());
            fileUploadsService.save(fileUpload);

            submission.addFileUpload(fileUpload.getId());
            submissionService.saveSubmission(submission, user.getId());
        } catch (Exception e) {
            log.error("ERROR: {}", e.getMessage(), e);
        }
    }

    @Override
    public FileObject prefillGCST(String submissionId, User user) {
        log.info("Pre-filling GCST list: {}", submissionId);
        Submission submission = submissionService.getSubmission(submissionId, user);
        StudyCollector studyCollector = new StudyCollector();
        Stream<Study> studyStream = studyRepository.readByIdIn(submission.getStudies());
//        Stream<Study> studyStream = studyRepository.readBySubmissionId(submission.getId());
        studyStream.forEach(study -> studyCollector.add(study));
        studyStream.close();

        try {
            FileObject fileObject = templateService.retrieveGCSTPrefilledTemplate(new SSTemplateGCSTRequestDto(true,
                    new SSTemplateGCSTRequestStudyDto(studyCollector.getStudyList().stream()
                            .map(SSTemplateGCSTStudyDtoAssembler::assemble).collect(Collectors.toList()))));
            if (fileObject == null) {
                log.error("No file object received from the template service!");
                return null;
            }
            return fileObject;
        } catch (Exception e) {
            log.error("ERROR: {}", e.getMessage(), e);
        }
        return null;
    }
}
