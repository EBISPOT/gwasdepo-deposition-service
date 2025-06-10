package uk.ac.ebi.spot.gwas.deposition.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.GCPCounterItem;
import uk.ac.ebi.spot.gwas.deposition.repository.GCPCounterItemRepository;

import java.util.List;

@Service
public class GCPCounter {

    private static final Logger log = LoggerFactory.getLogger(GCPCounter.class);

    private static final int PADDING_LENGTH = 6;

    @Autowired
    private GCPCounterItemRepository gcpCounterItemRepository;

    public String getNext() {
        int counter = 1;
        List<GCPCounterItem> gcpCounterItemList = gcpCounterItemRepository.findAll();
        if (!gcpCounterItemList.isEmpty()) {
            counter = gcpCounterItemList.get(0).getCurrentValue();
        }
        log.info("Current counter: {}", counter);

        counter++;
        gcpCounterItemRepository.deleteAll();
        GCPCounterItem gcpCounterItem = gcpCounterItemRepository.insert(new GCPCounterItem(counter));
        log.info("Inserted new counter: {} | {}", gcpCounterItem.getId(), gcpCounterItem.getCurrentValue());
        return GWASDepositionBackendConstants.PREFIX_GCP + padCounter(gcpCounterItem.getCurrentValue());
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
