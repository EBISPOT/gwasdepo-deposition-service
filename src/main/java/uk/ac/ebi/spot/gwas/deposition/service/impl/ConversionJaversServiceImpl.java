package uk.ac.ebi.spot.gwas.deposition.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.javers.ElementChange;
import uk.ac.ebi.spot.gwas.deposition.javers.JaversChangeWrapper;
import uk.ac.ebi.spot.gwas.deposition.service.ConversionJaversService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversionJaversServiceImpl implements ConversionJaversService {
    private static final Logger log = LoggerFactory.getLogger(ConversionServiceImpl.class);

    public Optional<List<JaversChangeWrapper>> filterJaversResponse(List<JaversChangeWrapper> javersChangeWrapperList) {

        Optional<List<JaversChangeWrapper>> listChanges = Optional.of(javersChangeWrapperList).
                map((changeList) -> {

                    List<JaversChangeWrapper> fileUploadProps = changeList.stream().
                            filter((javersChangeWrapper) ->
                                    javersChangeWrapper.getProperty().equals("fileUploads"))
                            .collect(Collectors.toList());

                    log.info("fileUploadProps -:"+fileUploadProps);
                    List<JaversChangeWrapper> commitIDChanges = fileUploadProps.stream().
                            filter((fileUploadProp) -> {
                                List<ElementChange> elChanges = fileUploadProp.getElementChanges().
                                        stream().filter((elementChange) -> elementChange.getElementChangeType()
                                        .equals("ElementValueChange")
                                ).collect(Collectors.toList());
                                if (!elChanges.isEmpty()) return true;
                                else return false;
                            }).collect(Collectors.toList());

                    log.info("commitIDChanges -:"+commitIDChanges);

                    if(!commitIDChanges.isEmpty()) {
                        Double commitId = Optional.of(commitIDChanges).map(changes ->
                                changes.get(0).getCommitMetadata().getId()).get();

                        log.info("commitId -:"+commitId);
                        return Optional.of(changeList).map((changes) ->
                                changes.stream().filter((change) ->
                                        change.getCommitMetadata().getId().equals(commitId)).
                                        collect(Collectors.toList())).get();
                    } else {
                        return null;
                    }



                });

        return listChanges;
    }

    /*public void filterStudiesFromJavers(List<JaversChangeWrapper> javersChangeWrapperList) {
        javersChangeWrapperList.stream().filter( (javersChangeWrapper) ->
            javersChangeWrapper.getProperty().equals("studies")
        ).map((javersChange) -> javersChange.getElementChanges())
                .map((changes) -> getStudyListfromJavers(changes));



    }

    List<Pair<Study, Study>> getStudyListfromJavers(List<ElementChange> elementChanges){

        elementChanges.stream().map((elementChange) -> processStudyTag(elementChange) );

    }

    public Pair<Study, Study> processStudyTag(ElementChange elementChange){

    }*/
}
