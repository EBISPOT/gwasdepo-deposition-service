package uk.ac.ebi.spot.gwas.deposition.service.impl;

import com.mongodb.bulk.BulkWriteResult;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.components.BodyOfWorkListener;
import uk.ac.ebi.spot.gwas.deposition.constants.Status;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionProvenanceType;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.domain.ensembl.Variation;
import uk.ac.ebi.spot.gwas.deposition.domain.ensembl.VariationSynonym;
import uk.ac.ebi.spot.gwas.deposition.dto.summarystats.SSGlobusFolderDto;
import uk.ac.ebi.spot.gwas.deposition.exception.AuthorizationException;
import uk.ac.ebi.spot.gwas.deposition.exception.EmailAccountNotLinkedToGlobusException;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.exception.SSGlobusFolderCreatioException;
import uk.ac.ebi.spot.gwas.deposition.repository.*;
import uk.ac.ebi.spot.gwas.deposition.repository.ensembl.VariationRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.ensembl.VariationSynonymRepository;
import uk.ac.ebi.spot.gwas.deposition.service.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SubmissionServiceImpl implements SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    @Autowired
    private SubmissionRepository submissionRepository;

    @Autowired
    private CuratorAuthService curatorAuthService;

    @Autowired
    private ArchivedSubmissionRepository archivedSubmissionRepository;

    @Autowired
    private CallbackIdRepository callbackIdRepository;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private SummaryStatsEntryRepository summaryStatsEntryRepository;

    @Autowired
    private BodyOfWorkListener bodyOfWorkListener;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private AssociationRepository associationRepository;

    @Autowired
    private SampleRepository sampleRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private PublicationRepository publicationRepository;

    @Autowired
    PublicationService publicationService;

    @Autowired
    BodyOfWorkService bodyOfWorkService;

    @Autowired
    SumStatsService sumStatsService;

    @Autowired
    private VariationRepository variationRepository;

    @Autowired
    private VariationSynonymRepository variationSynonymRepository;

    @Value("${ensembl-snp-validation.enabled}")
    private boolean ensemblSnpValidationEnabled;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Submission createSubmission(Submission submission) {
        log.info("Creating submission for publication: {}", submission.getPublicationId());
        submission = submissionRepository.insert(submission);
        log.info("Submission created: {}", submission.getId());

        return submission;
    }

    @Override
    public Submission getSubmission(String submissionId, User user) {
        log.info("Retrieving submission: {}", submissionId);
        Optional<Submission> optionalSubmission = curatorAuthService.isCurator(user) ?
                submissionRepository.findByIdAndArchived(submissionId, false) :
                submissionRepository.findByIdAndArchivedAndCreated_UserId(submissionId, false, user.getId());
        if (!optionalSubmission.isPresent()) {
            log.error("Unable to find submission: {}", submissionId);
            throw new EntityNotFoundException("Unable to find submission: " + submissionId);
        }
        log.info("Submission successfully retrieved: {}", optionalSubmission.get().getId());
        return optionalSubmission.get();
    }

    @Override
    public Submission getSubmission(String publicationId) {
        log.info("Retrieving submission for publication: {}", publicationId);

        Optional<Submission> optionalSubmission = submissionRepository.findByPublicationIdAndArchived(publicationId, false);
        if (!optionalSubmission.isPresent()) {
            log.error("Unable to find submission for publication: {}", publicationId);
            return null;
        }

        return optionalSubmission.get();
    }

    @Override
    public Submission saveSubmission(Submission submission, String userId) {
        log.info("Saving submission: {}", submission.getId());
        submission.setLastUpdated(new Provenance(DateTime.now(), userId));
        submission = submissionRepository.save(submission);
        return submission;
    }

    @Override
    public Page<Submission> getSubmissions(String publicationId, String bowId, Pageable page, User user) {
        log.info("Retrieving submissions: {} - {} - {}", page.getPageNumber(), page.getPageSize(), page.getSort().toString());
        if (curatorAuthService.isCurator(user)) {
            if (publicationId != null) {
                return submissionRepository.findByPublicationIdAndArchived(publicationId, false, page);
            }
            if (bowId != null) {
                return submissionRepository.findByBodyOfWorksContainsAndArchived(bowId, false, page);
            }
            return submissionRepository.findByArchived(false, page);
        }

        if (publicationId != null) {
            return submissionRepository.findByPublicationIdAndArchivedAndCreated_UserId(publicationId, false, user.getId(), page);
        }
        if (bowId != null) {
            return submissionRepository.findByBodyOfWorksContainsAndCreated_UserIdAndArchived(bowId, user.getId(), false, page);
        }
        return submissionRepository.findByArchivedAndCreated_UserId(false, user.getId(), page);
    }

    @Override
    public Submission updateSubmissionStatus(String submissionId, String status, User user) {
        log.info("Updating status [{}] for submission : {}", status, submissionId);
        Submission submission = this.getSubmission(submissionId, user);
        submission.setOverallStatus(status);
        if (status.equals(Status.DEPOSITION_COMPLETE)) {
            submission.setDateSubmitted(LocalDate.now());
            if (submission.getProvenanceType().equalsIgnoreCase(SubmissionProvenanceType.BODY_OF_WORK.name())) {
                bodyOfWorkListener.update(submission);
            }
        }
        submission.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
        submission = submissionRepository.save(submission);
        return submission;
    }

    @Override
    public Submission findByBodyOfWork(String bodyOfWorkId, String userId) {
        log.info("Retrieving submission for: {} | {}", bodyOfWorkId, userId);
        Optional<Submission> submissionOptional = submissionRepository.findByBodyOfWorksContainsAndCreated_UserIdAndArchived(bodyOfWorkId, userId, false);
        if (submissionOptional.isPresent()) {
            log.info("Found submission {} for: {} | {}", submissionOptional.get().getId(), bodyOfWorkId, userId);
            return submissionOptional.get();
        }

        log.info("No submission found for: {} | {}", bodyOfWorkId, userId);
        return null;
    }


    @Override
    public void deleteSubmission(String submissionId, User user) {
        log.info("Deleting submission: {}", submissionId);
        Submission submission = this.getSubmission(submissionId, user);
        for (String fileId : submission.getFileUploads()) {
            FileUpload fileUpload = fileUploadsService.getFileUpload(fileId);
            if (fileUpload.getCallbackId() != null) {
                deleteCallbackId(fileUpload.getCallbackId());
            }
            List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByFileUploadId(fileId);
            /* Modifying fo Javers Bug with DeleteALl operation */
            //summaryStatsEntryRepository.deleteAll(summaryStatsEntries);
            Optional.ofNullable(summaryStatsEntries).ifPresent((sumstats) ->
                    sumstats.forEach((sumstat) -> summaryStatsEntryRepository.delete(sumstat)));
        }

        ArchivedSubmission archivedSubmission = ArchivedSubmission.fromSubmission(submission);
        archivedSubmissionRepository.insert(archivedSubmission);
        submission.setArchived(true);
        submission.setDeletedOn(DateTime.now());
        submission.setStudies(new ArrayList<>());
        submission.setAssociations(new ArrayList<>());
        submission.setSamples(new ArrayList<>());
        submission.setNotes(new ArrayList<>());
        submission.setFileUploads(new ArrayList<>());
        submission.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
        submissionRepository.save(submission);
    }

    /**
     * @param submissionId
     * @param user         Reset the Submission related Child objects for Uploading new template
     */
    public Submission editFileUploadSubmissionDetails(String submissionId, User user) {
        log.info("Updating submission with new File Content: {}", submissionId);
        Submission submission = this.getSubmission(submissionId, user);
        log.info("Reached Callback & Sumstats Stage");
        Optional.ofNullable(submission.getFileUploads()).ifPresent((fileUploadIds) -> {
            fileUploadIds.forEach((fileUploadId) -> {
                Optional.ofNullable(fileUploadsService.getFileUpload(fileUploadId)).ifPresent((fileUpload) -> {
                    Optional.ofNullable(fileUpload.getCallbackId()).ifPresent((callbackId) ->
                            deleteCallbackId(callbackId));
                    Optional.ofNullable(summaryStatsEntryRepository.findByFileUploadId(fileUploadId)).ifPresent((sumstats) ->
                            sumstats.forEach((sumstat) -> summaryStatsEntryRepository.delete(sumstat)));

                });
            });
        });
        submission.setAssociations(new ArrayList<>());
        submission.setSamples(new ArrayList<>());
        submission.setNotes(new ArrayList<>());
        submission.setStudies(new ArrayList<>());
        submission.setFileUploads(new ArrayList<>());
        submission.setLastUpdated(new Provenance(DateTime.now(), user.getId()));
        submission.setEditTemplate(new Provenance(DateTime.now(), user.getId()));
        return saveSubmission(submission, user.getId());

    }

    /**
     * @param submissionId Delete Old submission related child objects
     */
    @Override
    public void deleteSubmissionChildren(String submissionId) {
        log.info("Deleting Old Submission related object for new File Content: {}", submissionId);

        log.info("Reached Deleting Study Stage");
        Optional.ofNullable(studyRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((studies) -> studies.forEach((study) -> {
                    study.setSubmissionId("");
                    studyRepository.save(study);

                }));
        log.info("Reached Deleting Association Stage");
        Optional.ofNullable(associationRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((associations) ->
                        associations.forEach((association) -> {
                            association.setSubmissionId("");
                            associationRepository.save(association);

                        }));

        log.info("Reached Deleting Sample Stage");
        Optional.ofNullable(sampleRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((samples) -> samples.forEach((sample) -> {
                            sample.setSubmissionId("");
                            sampleRepository.save(sample);
                        }
                ));

        log.info("Reached Deleting Note Stage");
        Optional.ofNullable(noteRepository.findBySubmissionId(submissionId, Pageable.unpaged())).
                ifPresent((notes) -> notes.forEach((note) -> {
                            note.setSubmissionId("");
                            noteRepository.save(note);
                        }
                ));

    }


    @Override
    public void deleteSubmissionFile(Submission submission, String fileUploadId, String userId) {
        log.info("Removing file [{}] from submission: {}", fileUploadId, submission.getId());
        FileUpload fileUpload = fileUploadsService.getFileUpload(fileUploadId);
        /**
         * TODO: Add this validation later at some point in time
         */
        /*
        if (submission.getType().equals(SubmissionType.SUMMARY_STATS.name())
                && fileUpload.getType().equals(FileUploadType.SUMMARY_STATS_TEMPLATE)) {
            log.error("[{}] Cannot delete the file template associated with a Summary Stats submission: {}",
                    submission.getId(), fileUploadId);
            throw new CannotDeleteSSTemplateFileException("[" + submission.getId() + "] Cannot delete the file template associated with a Summary Stats submission: " + fileUploadId);
        }
        */

        if (fileUpload.getCallbackId() != null) {
            deleteCallbackId(fileUpload.getCallbackId());
        }
        List<SummaryStatsEntry> summaryStatsEntries = summaryStatsEntryRepository.findByFileUploadId(fileUploadId);
        /* Modifying due to Javers Bug for DeleteAll operation */
        //summaryStatsEntryRepository.deleteAll(summaryStatsEntries);
        Optional.ofNullable(summaryStatsEntries).ifPresent((sumstats) ->
                sumstats.forEach((sumstat) -> summaryStatsEntryRepository.delete(sumstat)));
        ArchivedSubmission archivedSubmission = ArchivedSubmission.fromSubmission(submission);
        archivedSubmissionRepository.insert(archivedSubmission);
        submission.removeFileUpload(fileUploadId);
        submission.setStudies(new ArrayList<>());
        submission.setAssociations(new ArrayList<>());
        submission.setSamples(new ArrayList<>());
        submission.setNotes(new ArrayList<>());
        submission.setOverallStatus(Status.STARTED.name());
        submission.setMetadataStatus(Status.NA.name());
        submission.setSummaryStatsStatus(Status.NA.name());
        submission.setLastUpdated(new Provenance(DateTime.now(), userId));
        submissionRepository.save(submission);
    }

    private void deleteCallbackId(String callbackId) {
        Optional<CallbackId> callbackIdOptional = callbackIdRepository.findByCallbackId(callbackId);
        if (callbackIdOptional.isPresent()) {
            callbackIdRepository.delete(callbackIdOptional.get());
        }
    }

    public Submission lockSubmission(Submission submission, User user, String status) {
        Optional.ofNullable(status).ifPresent((lockstatus) -> {
            if (lockstatus.equals("lock"))
                submission.setLockDetails(new LockDetails(new Provenance(DateTime.now(),
                        user.getId()), Status.LOCKED_FOR_EDITING.name()));
            else
                submission.setLockDetails(null);
        });
        return saveSubmission(submission, user.getId());
    }

    @Override
    public Submission createGlobusFolderForReopenedSubmission(String submissionId, User apiCaller, String globusEmail) {
        if (!curatorAuthService.isCurator(apiCaller)) {
            log.error("Unauthorized access: {}", apiCaller.getId());
            throw new AuthorizationException("User [" + apiCaller.getId() + "] does not have access to perform Globus folder creation.");
        }
        String globusFolder = UUID.randomUUID().toString();
        SSGlobusResponse outcome = sumStatsService.createGlobusFolder(
                new SSGlobusFolderDto(globusFolder, globusEmail)
        );
        if (outcome != null) {
            if (!outcome.isValid()) {
                log.error("Unable to create Globus folder: {}", outcome.getOutcome());
                throw new EmailAccountNotLinkedToGlobusException(outcome.getOutcome());
            }
            Submission submission = getSubmission(submissionId);
            submission.setGlobusFolderId(globusFolder);
            submission.setGlobusOriginId(outcome.getOutcome());
            saveSubmission(submission, apiCaller.getId());
            return submission;
        } else {
            throw new SSGlobusFolderCreatioException("An error occurred when communicating with SS/Globus.");
        }
    }

    /**
     * Get List of Studies for previous submission & Publication
     *
     * @param submissionId
     * @return
     */
    public List<Study> getStudies(String submissionId) {
        List<Study> studies = studyRepository.readBySubmissionId(submissionId)
                .collect(Collectors.toList());
        List<String> studyTags = studies.stream().map(study -> study.getStudyTag())
                .collect(Collectors.toList());
        Submission submission = submissionRepository.findById(submissionId).get();
        if (submission.getPublicationId() != null && !submission.getPublicationId().isEmpty()) {
            Publication publication = publicationRepository.findById(submission.getPublicationId()).get();
            List<Study> pmIdStudies = studyRepository.findByPmidsContains(publication.getPmid());
            List<Study> uniqueStudies = pmIdStudies.stream().filter(study -> !studyTags.contains(study.getStudyTag()))
                    .collect(Collectors.groupingBy(Study::getStudyTag))
                    .values().stream()
                    .map((studyArr) -> studyArr.stream().findFirst().get())
                    .collect(Collectors.toList());
            studies.addAll(uniqueStudies);
        } else {
            if (submission.getBodyOfWorks() != null && !submission.getBodyOfWorks().isEmpty()) {
                List<Study> bowStudies = studyRepository.findByBodyOfWorkListContains(submission.getBodyOfWorks().get(0));
                List<Study> uniqueStudies = bowStudies.stream().filter(study -> !studyTags.contains(study.getStudyTag()))
                        .collect(Collectors.groupingBy(Study::getStudyTag))
                        .values().stream()
                        .map((studyArr) -> studyArr.stream().findFirst().get())
                        .collect(Collectors.toList());
                studies.addAll(uniqueStudies);
            }
        }
        return studies;
    }

    @Override
    public boolean validateSnps(String submissionId) {
        try {
            if (!ensemblSnpValidationEnabled) {
                return false;
            }
            log.info("Started validating SNPs for submission: {}", submissionId);
            BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Association.class);
            // removes any duplicated rsid in a study map(tag - rsid, association)
            Map<String, Association> snps = associationRepository.readBySubmissionId(submissionId).collect(Collectors.toMap(a -> a.getStudyTag() + "-" + a.getVariantId(), association -> association, (a1, a2) -> null));
            // removes any duplicated rsid in the map for SQL query
            Set<String> snpNames = snps.values().stream().filter(Objects::nonNull).map(Association::getVariantId).collect(Collectors.toSet());
            // map(rsid, association[])
            Map<String, List<Association>> submissionSnpsByRsid = new HashMap<>();
            snps.forEach((s, association) -> {
                if (!submissionSnpsByRsid.containsKey(association.getVariantId())) {
                    submissionSnpsByRsid.put(association.getVariantId(), new ArrayList<>());
                }
                submissionSnpsByRsid.get(association.getVariantId()).add(association);
            });
            List<Variation> foundVariations;
            List<VariationSynonym> foundVariationSynonyms;
            foundVariations = variationRepository.findByNameIn(snpNames);
            foundVariationSynonyms = variationSynonymRepository.findByNameIn(snpNames);
            log.info("Found {} valid SNPs", foundVariations.size() + foundVariationSynonyms.size());
            log.info("Marking SNPs as valid in bulk");
            for (Variation variation : foundVariations) {
                for (Association a : submissionSnpsByRsid.get(variation.getName())) {
                    Query query = new Query().addCriteria(new Criteria("id").is(a.getId()));
                    Update update = new Update().set("isValid", true);
                    bulkOps.updateOne(query, update);
                }
            }
            for (VariationSynonym variation : foundVariationSynonyms) {
                for (Association a : submissionSnpsByRsid.get(variation.getName())) {
                    Query query = new Query().addCriteria(new Criteria("id").is(a.getId()));
                    Update update = new Update().set("isValid", true);
                    bulkOps.updateOne(query, update);
                }
            }
            BulkWriteResult bulkWriteResult = null;
            if (!foundVariations.isEmpty() || !foundVariationSynonyms.isEmpty()) {
                bulkWriteResult = bulkOps.execute();
            }
            if (bulkWriteResult != null && bulkWriteResult.wasAcknowledged()) {
                log.info("Finished validating SNPs for submission: {}", submissionId);
            } else {
                return false;
            }
            return true;
        }
        catch (Exception e) {
            log.error("SNP Validation Error: {}", e.getMessage(), e);
            return false;
        }
    }
}
