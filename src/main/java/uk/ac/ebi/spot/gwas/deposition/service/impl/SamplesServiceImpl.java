package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Sample;
import uk.ac.ebi.spot.gwas.deposition.domain.Submission;
import uk.ac.ebi.spot.gwas.deposition.repository.SampleRepository;
import uk.ac.ebi.spot.gwas.deposition.service.SamplesService;
import uk.ac.ebi.spot.gwas.deposition.util.IdCollector;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class SamplesServiceImpl implements SamplesService {

    private static final Logger log = LoggerFactory.getLogger(SamplesService.class);

    @Autowired
    private SampleRepository sampleRepository;

    @Override
    public Page<Sample> getSamples(Submission submission, Pageable page) {
        log.info("Retrieving samples: {} - {} - {}", page.getPageNumber(), page.getPageSize(), page.getSort());
        return sampleRepository.findBySubmissionId(submission.getId(), page);
    }

    @Override
    @Async
    public void deleteSamples(List<String> samples) {
        log.info("Removing {} samples.", samples.size());
        List<Sample> sampleList = sampleRepository.findByIdIn(samples);
        for (Sample sample : sampleList) {
            sampleRepository.delete(sample);
        }
        log.info("Successfully removed {} samples.", samples.size());
    }

    @Override
    public List<Sample> retrieveSamples(List<String> samples) {
        log.info("Retrieving samples: {}", samples);
        List<Sample> sampleList = sampleRepository.findByIdIn(samples);
        log.info("Found {} samples.", sampleList.size());
        return sampleList;
    }

    @Override
    public Sample getSample(String sampleId) {
        log.info("Retrieving sample: {}", sampleId);
        Optional<Sample> sampleOptional = sampleRepository.findById(sampleId);
        if (sampleOptional.isPresent()) {
            log.info("Found sample: {}", sampleOptional.get().getStudyTag());
            return sampleOptional.get();
        }
        log.error("Unable to find sample: {}", sampleId);
        return null;
    }

    @Override
    public void deleteSamples(String submissionId) {
        log.info("Removing samples for submission: {}", submissionId);
        Stream<Sample> sampleStream = sampleRepository.readBySubmissionId(submissionId);
        IdCollector idCollector = new IdCollector();
        sampleStream.forEach(sample -> idCollector.addId(sample.getId()));
        sampleStream.close();
        log.info(" - Found {} samples.", idCollector.getIds().size());
        for (String id : idCollector.getIds()) {
            sampleRepository.deleteById(id);
        }
    }

}
