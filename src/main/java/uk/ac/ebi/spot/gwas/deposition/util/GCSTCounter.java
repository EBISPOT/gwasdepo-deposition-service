package uk.ac.ebi.spot.gwas.deposition.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.GCSTCounterItem;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.repository.GCSTCounterItemRepository;

import java.util.List;

@Service
public class GCSTCounter {

    private static final Logger log = LoggerFactory.getLogger(GCSTCounter.class);

    @Autowired
    private GCSTCounterItemRepository gcstCounterItemRepository;

    @Autowired
    Environment environment;

    public synchronized String getNext() {

        List<GCSTCounterItem> gcstCounterItemList = gcstCounterItemRepository.findAll();
        if (gcstCounterItemList.isEmpty()) {
            if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                log.info("Current counter: not found in db");
                int counter = 90000001;
                GCSTCounterItem gcstCounterItem = gcstCounterItemRepository.insert(new GCSTCounterItem(counter));
                log.info("Inserted new counter: {} | {}", gcstCounterItem.getId(), gcstCounterItem.getCurrentValue());
                return GWASDepositionBackendConstants.PREFIX_GCST + gcstCounterItem.getCurrentValue();
            }
            log.error("Collection gcstCounter is empty, no entries found.");
            throw new EntityNotFoundException("Collection gcstCounter is empty, no entries found.");
        }
        GCSTCounterItem gcstCounterItem = gcstCounterItemList.get(0);
        int counter = gcstCounterItem.getCurrentValue();
        log.info("Current counter: {}", counter);
        counter++;
        gcstCounterItem.setCurrentValue(counter);
        gcstCounterItem = gcstCounterItemRepository.save(gcstCounterItem);
        log.info("Updated counter: {} with new value: {}", gcstCounterItem.getId(), gcstCounterItem.getCurrentValue());
        return GWASDepositionBackendConstants.PREFIX_GCST + gcstCounterItem.getCurrentValue();
    }
}
