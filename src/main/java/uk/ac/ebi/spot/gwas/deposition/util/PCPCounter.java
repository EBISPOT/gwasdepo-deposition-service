package uk.ac.ebi.spot.gwas.deposition.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.spot.gwas.deposition.domain.PCPCounterItem;
import uk.ac.ebi.spot.gwas.deposition.repository.PCPCounterItemRepository;

import java.util.Optional;

@Slf4j
@Service
public class PCPCounter {

    private static final int PADDING_LENGTH = 6;
    private static final int INITIAL_COUNTER_VALUE = 1;
    private static final String PREFIX_PCP = "PCP";


    private final PCPCounterItemRepository pcpCounterItemRepository;

    public PCPCounter(PCPCounterItemRepository pcpCounterItemRepository) {
        this.pcpCounterItemRepository = pcpCounterItemRepository;
    }

    @Transactional
    public synchronized String getNext() {
        Optional<PCPCounterItem> counterItemOptional = pcpCounterItemRepository.findFirstBy();
        PCPCounterItem counterItem;
        if (counterItemOptional.isPresent()) {
            counterItem = counterItemOptional.get();
            log.info("Found existing counter: {}", counterItem.getCurrentValue());
            counterItem.setCurrentValue(counterItem.getCurrentValue() + 1);
        } else {
            log.info("No counter found. Initializing a new one.");
            counterItem = new PCPCounterItem(INITIAL_COUNTER_VALUE);
        }
        PCPCounterItem savedCounterItem = pcpCounterItemRepository.save(counterItem);
        log.info("Saved new counter value: {}", savedCounterItem.getCurrentValue());
        return PREFIX_PCP + String.format("%0" + PADDING_LENGTH + "d", savedCounterItem.getCurrentValue());
    }
}
