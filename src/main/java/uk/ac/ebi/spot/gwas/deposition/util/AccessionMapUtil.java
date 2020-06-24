package uk.ac.ebi.spot.gwas.deposition.util;

import java.util.LinkedHashMap;
import java.util.Map;

public class AccessionMapUtil {

    private Map<String, String> accessionMap;

    public AccessionMapUtil() {
        accessionMap = new LinkedHashMap<>();
    }

    public Map<String, String> getAccessionMap() {
        return accessionMap;
    }

    public void addStudy(String studyTag, String accession) {
        this.accessionMap.put(studyTag, accession);
    }
}
