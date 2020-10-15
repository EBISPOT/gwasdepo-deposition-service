package uk.ac.ebi.spot.gwas.deposition.util;

import java.util.ArrayList;
import java.util.List;

public class IdCollector {

    private List<String> ids;

    public IdCollector() {
        ids = new ArrayList<>();
    }

    public void addId(String id) {
        ids.add(id);
    }

    public List<String> getIds() {
        return ids;
    }
}
