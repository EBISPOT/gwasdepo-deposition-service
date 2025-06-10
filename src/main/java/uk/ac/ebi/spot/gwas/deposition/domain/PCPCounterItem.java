package uk.ac.ebi.spot.gwas.deposition.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@NoArgsConstructor
@Getter
@Setter
@Document(collection = "pcpCounter")
public class PCPCounterItem {
    @Id
    private String id;
    private int currentValue;
    public PCPCounterItem(int newValue) {
        this.currentValue = newValue;
    }
}
