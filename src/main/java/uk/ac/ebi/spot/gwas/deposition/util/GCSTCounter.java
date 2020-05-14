package uk.ac.ebi.spot.gwas.deposition.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.GCSTCounterItem;
import uk.ac.ebi.spot.gwas.deposition.repository.GCSTCounterItemRepository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GCSTCounter {

    private AtomicInteger counter;

    @Autowired
    private GCSTCounterItemRepository gcstCounterItemRepository;

    @PostConstruct
    public void initialize() {
        List<GCSTCounterItem> gcstCounterItemList = gcstCounterItemRepository.findAll();
        if (gcstCounterItemList.isEmpty()) {
            counter = new AtomicInteger(90000000);
        } else {
            counter = new AtomicInteger(gcstCounterItemList.get(0).getCurrentValue());
        }
    }

    public String getNext() {
        int currentValue = counter.get();
        this.increment();
        return GWASDepositionBackendConstants.PREFIX_GCST + Integer.toString(currentValue);
    }

    public void increment() {
        while (true) {
            int existingValue = counter.get();
            int newValue = existingValue + 1;
            if (counter.compareAndSet(existingValue, newValue)) {
                gcstCounterItemRepository.deleteAll();
                gcstCounterItemRepository.insert(new GCSTCounterItem(newValue));
                return;
            }
        }
    }
}
