package uk.ac.ebi.spot.gwas.deposition.rest.dto;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.domain.BodyOfWork;
import uk.ac.ebi.spot.gwas.deposition.dto.AuthorDto;
import uk.ac.ebi.spot.gwas.deposition.dto.BodyOfWorkDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.BodyOfWorkController;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class BodyOfWorkDtoAssembler implements ResourceAssembler<BodyOfWork, Resource<BodyOfWorkDto>> {

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    public static BodyOfWorkDto assemble(BodyOfWork bodyOfWork) {
        List<AuthorDto> correspondingAuthorDtoList = new ArrayList<>();
        if (bodyOfWork.getCorrespondingAuthors() != null) {
            correspondingAuthorDtoList = bodyOfWork.getCorrespondingAuthors().stream().map(AuthorDtoAssembler::assemble).collect(Collectors.toList());
        }

        return new BodyOfWorkDto(bodyOfWork.getId(),
                bodyOfWork.getTitle(),
                bodyOfWork.getDescription(),
                AuthorDtoAssembler.assemble(bodyOfWork.getFirstAuthor()),
                AuthorDtoAssembler.assemble(bodyOfWork.getLastAuthor()),
                bodyOfWork.getJournal(),
                bodyOfWork.getDoi(),
                bodyOfWork.getUrl(),
                correspondingAuthorDtoList,
                bodyOfWork.getPrePrintServer(),
                bodyOfWork.getPreprintServerDOI(),
                bodyOfWork.getEmbargoDate(),
                bodyOfWork.getEmbargoUntilPublished(),
                bodyOfWork.getPmids(),
                bodyOfWork.getStatus());
    }

    @Override
    public Resource<BodyOfWorkDto> toResource(BodyOfWork bodyOfWork) {
        List<AuthorDto> correspondingAuthorDtoList = new ArrayList<>();
        if (bodyOfWork.getCorrespondingAuthors() != null) {
            correspondingAuthorDtoList = bodyOfWork.getCorrespondingAuthors().stream().map(AuthorDtoAssembler::assemble).collect(Collectors.toList());
        }

        BodyOfWorkDto bodyOfWorkDto = new BodyOfWorkDto(bodyOfWork.getId(),
                bodyOfWork.getTitle(),
                bodyOfWork.getDescription(),
                AuthorDtoAssembler.assemble(bodyOfWork.getFirstAuthor()),
                AuthorDtoAssembler.assemble(bodyOfWork.getLastAuthor()),
                bodyOfWork.getJournal(),
                bodyOfWork.getDoi(),
                bodyOfWork.getUrl(),
                correspondingAuthorDtoList,
                bodyOfWork.getPrePrintServer(),
                bodyOfWork.getPreprintServerDOI(),
                bodyOfWork.getEmbargoDate(),
                bodyOfWork.getEmbargoUntilPublished(),
                bodyOfWork.getPmids(),
                bodyOfWork.getStatus());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(BodyOfWorkController.class).getBodyOfWork(bodyOfWork.getId(), null));

        Resource<BodyOfWorkDto> resource = new Resource<>(bodyOfWorkDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withSelfRel());
        return resource;
    }
}
