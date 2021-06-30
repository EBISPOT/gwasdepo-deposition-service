package uk.ac.ebi.spot.gwas.deposition.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.GCSTCounterItem;
import uk.ac.ebi.spot.gwas.deposition.repository.GCSTCounterItemRepository;

import java.util.List;

@Service
public class GCSTCounter {

    private static final Logger log = LoggerFactory.getLogger(GCSTCounter.class);

    @Autowired
    private GCSTCounterItemRepository gcstCounterItemRepository;

    public synchronized String getNext() {
        int counter = 90000000;
        List<GCSTCounterItem> gcstCounterItemList = gcstCounterItemRepository.findAll();
        if (!gcstCounterItemList.isEmpty()) {
            counter = gcstCounterItemList.get(0).getCurrentValue();
        }
        log.info("Current counter: {}", counter);
        counter++;
        gcstCounterItemRepository.deleteAll();
        GCSTCounterItem gcstCounterItem = gcstCounterItemRepository.insert(new GCSTCounterItem(counter));
        log.info("Inserted new counter: {} | {}", gcstCounterItem.getId(), gcstCounterItem.getCurrentValue());
        return GWASDepositionBackendConstants.PREFIX_GCST + Integer.toString(gcstCounterItem.getCurrentValue());
    }
}
