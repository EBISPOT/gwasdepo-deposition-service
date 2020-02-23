package uk.ac.ebi.spot.gwas.deposition.rest.dto;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceAssembler;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.config.GWASDepositionBackendConfig;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.Association;
import uk.ac.ebi.spot.gwas.deposition.dto.AssociationDto;
import uk.ac.ebi.spot.gwas.deposition.rest.controllers.SubmissionsController;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;

@Component
public class AssociationDtoAssembler implements ResourceAssembler<Association, Resource<AssociationDto>> {

    @Autowired
    private GWASDepositionBackendConfig gwasDepositionBackendConfig;

    public Resource<AssociationDto> toResource(Association association) {
        AssociationDto associationDto = new AssociationDto(association.getStudyTag(),
                association.getHaplotypeId(),
                association.getVariantId(),
                association.getPvalue(),
                association.getPvalueText(),
                association.getProxyVariant(),
                association.getEffectAllele(),
                association.getOtherAllele(),
                association.getEffectAlleleFrequency(),
                association.getOddsRatio(),
                association.getBeta(),
                association.getBetaUnit(),
                association.getCiLower(),
                association.getCiUpper(),
                association.getStandardError());

        final ControllerLinkBuilder lb = ControllerLinkBuilder.linkTo(
                ControllerLinkBuilder.methodOn(SubmissionsController.class).getSubmission(association.getSubmissionId(), null));

        Resource<AssociationDto> resource = new Resource<>(associationDto);
        resource.add(BackendUtil.underBasePath(lb, gwasDepositionBackendConfig.getProxyPrefix()).withRel(GWASDepositionBackendConstants.LINKS_PARENT));
        return resource;
    }

    public static Association disassemble(AssociationDto associationDto) {
        Association association = new Association();
        association.setStudyTag(associationDto.getStudyTag());
        association.setHaplotypeId(associationDto.getHaplotypeId());
        association.setVariantId(associationDto.getVariantId());
        association.setPvalue(associationDto.getPvalue());
        association.setProxyVariant(associationDto.getProxyVariant());
        association.setPvalueText(associationDto.getPvalueText());
        association.setEffectAllele(associationDto.getEffectAllele());
        association.setOtherAllele(associationDto.getOtherAllele());
        association.setEffectAlleleFrequency(associationDto.getEffectAlleleFrequency());
        association.setOddsRatio(associationDto.getOddsRatio());
        association.setBeta(associationDto.getBeta());
        association.setBetaUnit(associationDto.getBetaUnit());
        association.setCiLower(associationDto.getCiLower());
        association.setCiUpper(associationDto.getCiUpper());
        association.setStandardError(associationDto.getStandardError());

        return association;
    }

}
