package uk.ac.ebi.spot.gwas.deposition.util;

import uk.ac.ebi.spot.gwas.deposition.domain.Study;

import java.util.ArrayList;
import java.util.List;

public class StudyCollector {

    public List<Study> studyList;

    public StudyCollector() {
        studyList = new ArrayList<>();
    }

    public void add(Study study) {
        this.studyList.add(study);
    }

    public List<Study> getStudyList() {
        return studyList;
    }
}
