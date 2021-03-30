package uk.ac.ebi.spot.gwas.deposition.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@JaversSpringDataAuditable
public interface SubmissionRepository extends MongoRepository<Submission, String> {

    Optional<Submission> findByIdAndArchived(String id, boolean archived);

    Optional<Submission> findByIdAndArchivedAndCreated_UserId(String id, boolean archived, String userId);

    Page<Submission> findByArchived(boolean archived, Pageable page);

    List<Submission> findByArchived(boolean archived);

    Stream<Submission> readByOverallStatusAndArchivedOrderByLastUpdatedDesc(String overallStatus, boolean archived);

    Optional<Submission> findByPublicationIdAndArchived(String publicationId, boolean archived);

    Optional<Submission> findByBodyOfWorksContainsAndCreated_UserIdAndArchived(String bodyOfWorkId, String userId, boolean archived);

    Page<Submission> findByBodyOfWorksContainsAndCreated_UserIdAndArchived(String bodyOfWorkId, String userId, boolean archived, Pageable page);

    Page<Submission> findByBodyOfWorksContainsAndArchived(String bodyOfWorkId, boolean archived, Pageable page);

    List<Submission> findByBodyOfWorksContainsAndArchived(String bodyOfWorkId, boolean archived);

    Page<Submission> findByPublicationIdAndArchived(String publicationId, boolean archived, Pageable page);

    Page<Submission> findByPublicationIdAndArchivedAndCreated_UserId(String publicationId, boolean b, String userId, Pageable page);

    Page<Submission> findByArchivedAndCreated_UserId(boolean b, String userId, Pageable page);

    @Query(value = "{'archived': ?0}", count = true)
    Long countByArchived(boolean archived);

    @Query(value = "{'overallStatus': ?0}", count = true)
    Long countByOverallStatus(String status);

    Stream<Submission> readByArchived(boolean archived);
}
