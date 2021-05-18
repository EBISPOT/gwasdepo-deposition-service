package uk.ac.ebi.spot.gwas.deposition.repository;

import org.javers.spring.annotation.JaversSpringDataAuditable;
import org.springframework.data.mongodb.repository.MongoRepository;
import uk.ac.ebi.spot.gwas.deposition.domain.CallbackId;

import java.util.List;
import java.util.Optional;


public interface CallbackIdRepository extends MongoRepository<CallbackId, String> {

    Optional<CallbackId> findByCallbackId(String callbackId);

    List<CallbackId> findByCompleted(boolean completed);

    Optional<CallbackId> findBySubmissionIdAndCompleted(String submissionId, boolean completed);
}
