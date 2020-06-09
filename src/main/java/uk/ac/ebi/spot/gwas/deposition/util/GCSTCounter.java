package uk.ac.ebi.spot.gwas.deposition.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.deposition.domain.GCSTCounterItem;
import uk.ac.ebi.spot.gwas.deposition.repository.GCSTCounterItemRepository;

import java.util.List;

@Component
public class GCSTCounter {

    @Autowired
    private GCSTCounterItemRepository gcstCounterItemRepository;

    public String getNext() {
        int counter = 90000000;
        List<GCSTCounterItem> gcstCounterItemList = gcstCounterItemRepository.findAll();
        if (!gcstCounterItemList.isEmpty()) {
            counter = gcstCounterItemList.get(0).getCurrentValue();
        }
        counter++;
        gcstCounterItemRepository.deleteAll();
        gcstCounterItemRepository.insert(new GCSTCounterItem(counter));
        return GWASDepositionBackendConstants.PREFIX_GCST + Integer.toString(counter);
    }
}
