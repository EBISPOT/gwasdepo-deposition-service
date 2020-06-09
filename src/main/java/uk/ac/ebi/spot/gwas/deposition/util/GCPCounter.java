package uk.ac.ebi.spot.gwas.deposition.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.GCPCounterItem;
import uk.ac.ebi.spot.gwas.deposition.repository.GCPCounterItemRepository;

import java.util.List;

@Component
public class GCPCounter {

    private static final int PADDING_LENGTH = 6;

    @Autowired
    private GCPCounterItemRepository gcpCounterItemRepository;

    public String getNext() {
        int counter = 1;
        List<GCPCounterItem> gcpCounterItemList = gcpCounterItemRepository.findAll();
        if (!gcpCounterItemList.isEmpty()) {
            counter = gcpCounterItemList.get(0).getCurrentValue();
        }
        counter++;
        gcpCounterItemRepository.deleteAll();
        gcpCounterItemRepository.insert(new GCPCounterItem(counter));
        return GWASDepositionBackendConstants.PREFIX_GCP + padCounter(counter);
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
