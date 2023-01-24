package uk.ac.ebi.spot.gwas.deposition.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.domain.*;
import uk.ac.ebi.spot.gwas.deposition.repository.CurationStatusRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.CuratorRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.SubmissionRepository;
import uk.ac.ebi.spot.gwas.deposition.repository.UserRepository;
import uk.ac.ebi.spot.gwas.deposition.service.SubmissionService;
import uk.ac.ebi.spot.gwas.deposition.solr.SOLRPublication;

import java.io.IOException;
import java.util.Optional;

@Component
public class SOLRPublicationAssembler {

    @Autowired
    UserRepository userRepository;

    @Autowired
    SubmissionService submissionService;

    @Autowired
    CuratorRepository curatorRepository;

    @Autowired
    CurationStatusRepository curationStatusRepository;

    @Autowired
    SubmissionRepository submissionRepository;


    public  SOLRPublication assemble(Publication publication) {
        try {
            String correspondingAuthor = new ObjectMapper().writeValueAsString(publication.getCorrespondingAuthor());
            return new SOLRPublication(
                    publication.getId(),
                    publication.getPmid(),
                    publication.getTitle(),
                    publication.getFirstAuthor(),
                    publication.getStatus(),
                    publication.getJournal(),
                    publication.getPublicationDate(),
                    correspondingAuthor,
                    Optional.ofNullable(publication.getCurator())
                    .map(curatorId -> curatorRepository.findById(curatorId))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(curator -> curator.getFirstName() +" " + curator.getLastName())
                    .orElse(null),
                    Optional.ofNullable(publication.getCurationStatus())
                            .map(statusId -> curationStatusRepository.findById(statusId))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(status -> status.getStatus())
                            .orElse(null),
                    Optional.ofNullable(submissionRepository.findByPublicationIdAndArchived(publication.getId(),
                            false, Pageable.unpaged() ))
                            .map(page -> page.stream().findFirst())
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(Submission::getCreated)
                            .map(Provenance::getUserId)
                             .map(userId -> userRepository.findById(userId))
                            .filter(Optional::isPresent)
                            .map(Optional::get)
                            .map(user -> user.getName())
                            .orElse(null)

                    );
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public  Publication disassemble(SOLRPublication solrPublication) {
        CorrespondingAuthor correspondingAuthor = null;
        try {
            if (solrPublication.getCorrespondingAuthor() != null) {
                if (!solrPublication.getCorrespondingAuthor().equalsIgnoreCase("") &&
                        !solrPublication.getCorrespondingAuthor().equalsIgnoreCase("null")) {
                    correspondingAuthor = new ObjectMapper().readValue(
                            solrPublication.getCorrespondingAuthor(), CorrespondingAuthor.class);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Publication publication = new Publication(
                solrPublication.getPmid(),
                solrPublication.getJournal(),
                solrPublication.getTitle(),
                solrPublication.getFirstAuthor(),
                solrPublication.getPublicationDate(),
                correspondingAuthor,
                solrPublication.getStatus(),
                Optional.ofNullable(solrPublication.getCurator())
                        .map((curator) -> getFirstAndLastName(curator))
                        .map(pair -> curatorRepository.findByFirstNameAndLastName(pair.getLeft(),pair.getRight()))
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .map(Curator::getId)
                        .orElse(null),
                Optional.ofNullable(solrPublication.getCurationStatus())
                .map(status -> curationStatusRepository.findByStatus(status))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(CurationStatus::getId)
                .orElse(null),
                Optional.ofNullable(solrPublication.getSubmitter())
                .map(submitter -> userRepository.findByName(submitter))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(User::getId)
                .orElse(null),
                null,
                null,
                null
        );

        publication.setId(solrPublication.getPublicationid());
        return publication;
    }

    private  Pair<String, String> getFirstAndLastName(String fullName) {
        if(fullName.contains(" ")) {
           return Pair.of(fullName.split(" ")[0],
                    fullName.split(" ")[1]);
        } else {
            return Pair.of(fullName,"");
        }
    }

}
