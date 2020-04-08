package uk.ac.ebi.spot.gwas.deposition.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "gcpCounter")
public class GCPCounterItem {

    @Id
    private String id;

    private int currentValue;

    public GCPCounterItem() {

    }

    public GCPCounterItem(int newValue) {
        this.currentValue = newValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(int currentValue) {
        this.currentValue = currentValue;
    }
}
