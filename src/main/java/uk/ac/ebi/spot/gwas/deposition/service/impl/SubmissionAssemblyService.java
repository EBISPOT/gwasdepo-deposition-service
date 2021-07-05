package uk.ac.ebi.spot.gwas.deposition.service.impl;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;
import static org.springframework.hateoas.mvc.ControllerLinkBuilder.methodOn;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.constants.SubmissionProvenanceType;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Publication;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.dto.FileUploadDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.AssociationsController;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.FileUploadsController;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.SamplesController;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.StudiesController;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.SubmissionsController;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.BodyOfWorkDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.FileUploadDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.ProvenanceDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.PublicationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SubmissionDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.BodyOfWorkService;
import uk.ac.ebi.spot.gwas.deposition.service.FileUploadsService;
import uk.ac.ebi.spot.gwas.deposition.service.PublicationService;
import uk.ac.ebi.spot.gwas.deposition.service.UserService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import java.util.ArrayList;
import java.util.List;

@Service
public class SubmissionAssemblyService implements ResourceAssembler<Submission, Resource<SubmissionDto>> {

    @Autowired
    private PublicationService publicationService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Autowired
    private UserService userService;

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    @Autowired
    private BodyOfWorkService bodyOfWorkService;

    @Override
    public Resource<SubmissionDto> toResource(Submission submission) {
        Publication publication = null;
        BodyOfWork bodyOfWork = null;
        if (submission.getProvenanceType().equalsIgnoreCase(SubmissionProvenanceType.PUBLICATION.name())) {
            publication = publicationService.retrievePublication(submission.getPublicationId(), true);
        } else {
            if (!submission.getBodyOfWorks().isEmpty()) {
                bodyOfWork = bodyOfWorkService.retrieveBodyOfWork(submission.getBodyOfWorks().get(0), submission.getCreated().getUserId());
            }
            if (submission.getPublicationId() != null) {
                publication = publicationService.retrievePublication(submission.getPublicationId(), true);
            }
        }
        List<FileUpload> fileUploads = fileUploadsService.getFileUploads(submission.getFileUploads());

        List<FileUploadDto> fileUploadDtos = new ArrayList<>();
        for (FileUpload fileUpload : fileUploads) {
            fileUploadDtos.add(FileUploadDtoAssembler.assemble(fileUpload, null));
        }

        SubmissionDto submissionDto = SubmissionDtoAssembler.assemble(submission,
                publication != null ? PublicationDtoAssembler.assemble(publication) : null,
                bodyOfWork != null ? BodyOfWorkDtoAssembler.assemble(bodyOfWork) : null,
                fileUploadDtos,
                ProvenanceDtoAssembler.assemble(submission.getCreated(), userService.getUser(submission.getCreated().getUserId())),
                submission.getLastUpdated() != null ?
                        ProvenanceDtoAssembler.assemble(submission.getLastUpdated(), userService.getUser(submission.getLastUpdated().getUserId())) :
                        ProvenanceDtoAssembler.assemble(submission.getCreated(), userService.getUser(submission.getCreated().getUserId()))
        );

        final ControllerLinkBuilder lb = linkTo(
                methodOn(SubmissionsController.class).getSubmission(submission.getId(), null));

        Resource<SubmissionDto> resource = new Resource<>(submissionDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        if (!fileUploads.isEmpty()) {
            Link fileUploadsLink = linkTo(methodOn(FileUploadsController.class)
                    .getFileUploads(submission.getId(), null))
                    .withRel(GWASDepositionBackendConstants.LINKS_FILES);
            resource.add(fileUploadsLink);
        }

        if (!submission.getStudies().isEmpty()) {
            Link studiesLink = linkTo(methodOn(StudiesController.class)
                    .getStudies(submission.getId(), null, null, null))
                    .withRel(GWASDepositionBackendConstants.LINKS_STUDIES);
            resource.add(studiesLink);
        }

        if (!submission.getSamples().isEmpty()) {
            Link samplesLink = linkTo(methodOn(SamplesController.class)
                    .getSamples(submission.getId(), null, null, null))
                    .withRel(GWASDepositionBackendConstants.LINKS_SAMPLES);
            resource.add(samplesLink);
        }

        if (!submission.getAssociations().isEmpty()) {
            Link associationsLink = linkTo(methodOn(AssociationsController.class)
                    .getAssociations(submission.getId(), null, null, null))
                    .withRel(GWASDepositionBackendConstants.LINKS_ASSOCIATIONS);
            resource.add(associationsLink);
        }
        return resource;
    }
}
