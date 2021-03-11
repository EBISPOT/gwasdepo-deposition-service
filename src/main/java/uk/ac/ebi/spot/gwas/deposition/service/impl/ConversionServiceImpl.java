package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.audit.AuditProxy;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionProvenanceType;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.dto.*;
import uk.ac.ebi.spot.gwas.deposition.dto.templateschema.TemplateSchemaDto;
import uk.ac.ebi.spot.gwas.deposition.repository.*;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.AssociationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.NoteDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SampleDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.StudyDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.ConversionService;
import uk.ac.ebi.spot.gwas.deposition.service.FileUploadsService;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.service.SummaryStatsProcessingService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import uk.ac.ebi.spot.gwas.deposition.util.GCSTCounter;
import uk.ac.ebi.spot.gwas.template.validator.service.TemplateConverterService;
import uk.ac.ebi.spot.gwas.template.validator.util.StreamSubmissionTemplateReader;
import uk.ac.ebi.spot.gwas.template.validator.util.SubmissionConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ConversionServiceImpl implements ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionService.class);

    @Autowired
    private TemplateConverterService templateConverterService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private SubmissionService submissionService;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private AssociationRepository associationRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private SummaryStatsProcessingService summaryStatsProcessingService;

    @Autowired
    private GCSTCounter gcstCounter;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    private BodyOfWorkRepository bodyOfWorkRepository;

    @Autowired
    private AuditProxy auditProxy;

    @Async
    @Override
    public void convertData(Submission submission, FileUpload fileUpload,
                            StreamSubmissionTemplateReader streamSubmissionTemplateReader,
                            TemplateSchemaDto schema, String userId) {
        log.info("Converting data ...");
        SubmissionDataDto submissionDataDto = SubmissionConverter.fromSubmissionDocument(
                templateConverterService.convert(streamSubmissionTemplateReader, schema)
        );
        streamSubmissionTemplateReader.close();

        List<SummaryStatsEntry> summaryStatsEntries = new ArrayList<>();
        log.info("Found {} studies.", submissionDataDto.getStudies().size());
        for (StudyDto studyDto : submissionDataDto.getStudies()) {
            Study study = StudyDtoAssembler.disassemble(studyDto);
            if (study.getAccession() == null) {
                study.setAccession(gcstCounter.getNext());
            }
            study.setSubmissionId(submission.getId());
            study.setAgreedToCc0(submission.isAgreedToCc0());
            if (submission.getProvenanceType().equalsIgnoreCase(SubmissionProvenanceType.BODY_OF_WORK.name())) {
                if (!submission.getBodyOfWorks().isEmpty()) {
                    study.setBodyOfWorkList(submission.getBodyOfWorks());

                    Optional<BodyOfWork> bodyOfWorkOptional = bodyOfWorkRepository.findByBowIdAndArchived(submission.getBodyOfWorks().get(0), false);
                    if (bodyOfWorkOptional.isPresent()) {
                        if (bodyOfWorkOptional.get().getPmids() != null) {
                            study.setPmids(bodyOfWorkOptional.get().getPmids());
                        }
                    }
                }
            } else {
                Optional<Publication> publicationOptional = publicationRepository.findById(submission.getPublicationId());
                if (publicationOptional.isPresent()) {
                    List<String> pmids = new ArrayList<>();
                    pmids.add(publicationOptional.get().getPmid());
                    study.setPmids(pmids);
                }
            }
            study = studyRepository.insert(study);
            submission.addStudy(study.getId());

            if (!BackendUtil.ssIsNR(study)) {
                summaryStatsEntries.add(new SummaryStatsEntry(fileUpload.getId(),
                        study.getStudyTag(), study.getSummaryStatisticsFile(),
                        study.getRawFilePath(), study.getChecksum(), study.getSummaryStatisticsAssembly(),
                        study.getReadmeFile(), submission.getGlobusFolderId()));
            }
        }
        if (!summaryStatsEntries.isEmpty()) {
            submission.setSummaryStatsStatus(Status.VALIDATING.name());
            submissionService.saveSubmission(submission, userId);
        }

        log.info("Found {} associations.", submissionDataDto.getAssociations().size());
        for (AssociationDto associationDto : submissionDataDto.getAssociations()) {
            Association association = AssociationDtoAssembler.disassemble(associationDto);
            association.setSubmissionId(submission.getId());
            association = associationRepository.insert(association);
            submission.addAssociation(association.getId());
        }

        log.info("Found {} samples.", submissionDataDto.getSamples().size());
        for (SampleDto sampleDto : submissionDataDto.getSamples()) {
            Sample sample = SampleDtoAssembler.disassemble(sampleDto);
            sample.setSubmissionId(submission.getId());
            sample = sampleRepository.insert(sample);
            submission.addSample(sample.getId());
        }

        log.info("Found {} notes.", submissionDataDto.getNotes().size());
        for (NoteDto noteDto : submissionDataDto.getNotes()) {
            Note note = NoteDtoAssembler.disassemble(noteDto);
            note.setSubmissionId(submission.getId());
            note = noteRepository.insert(note);
            submission.addNote(note.getId());
        }

        log.info("Data conversion finalised.");
        submission.setOverallStatus(Status.VALIDATING.name());
        submission.setMetadataStatus(Status.VALID.name());
        submissionService.saveSubmission(submission, userId);

        fileUpload.setStatus(FileUploadStatus.VALID.name());
        fileUploadsService.save(fileUpload);

        Publication publication = null;
        BodyOfWork bodyOfWork = null;
        if (submission.getProvenanceType().equalsIgnoreCase(SubmissionProvenanceType.PUBLICATION.name())) {
            Optional<Publication> publicationOptional = publicationRepository.findById(submission.getPublicationId());
            if (publicationOptional.isPresent()) {
                publication = publicationOptional.get();
            }
        } else {
            Optional<BodyOfWork> bodyOfWorkOptional = bodyOfWorkRepository.findByBowIdAndArchived(submission.getBodyOfWorks().get(0), false);
            if (bodyOfWorkOptional.isPresent()) {
                bodyOfWork = bodyOfWorkOptional.get();
            }
        }
        summaryStatsProcessingService.processSummaryStats(submission, fileUpload.getId(), summaryStatsEntries, publication, bodyOfWork, userId);
    }

}
