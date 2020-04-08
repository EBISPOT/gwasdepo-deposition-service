package uk.ac.ebi.spot.gwas.deposition.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.GCPCounterItem;
import uk.ac.ebi.spot.gwas.deposition.repository.GCPCounterItemRepository;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class GCPCounter {

    private static final int PADDING_LENGTH = 6;

    private AtomicInteger counter;

    @Autowired
    private GCPCounterItemRepository gcpCounterItemRepository;

    @PostConstruct
    public void initialize() {
        List<GCPCounterItem> gcpCounterItemList = gcpCounterItemRepository.findAll();
        if (gcpCounterItemList.isEmpty()) {
            counter = new AtomicInteger(1);
        } else {
            counter = new AtomicInteger(gcpCounterItemList.get(0).getCurrentValue());
        }
    }

    public String getNext() {
        int currentValue = counter.get();
        this.increment();
        return GWASDepositionBackendConstants.PREFIX_GCP + padCounter(currentValue);
    }

    public void increment() {
        while (true) {
            int existingValue = counter.get();
            int newValue = existingValue + 1;
            if (counter.compareAndSet(existingValue, newValue)) {
                gcpCounterItemRepository.deleteAll();
                gcpCounterItemRepository.insert(new GCPCounterItem(newValue));
                return;
            }
        }
    }

    private String padCounter(int value) {
        String sValue = Integer.toString(value);
        int padding = PADDING_LENGTH - sValue.length();
        String sPadding = "";
        for (int i = 0; i < padding; i++) {
            sPadding += "0";
        }
        return sPadding + sValue;
    }
}
