package uk.ac.ebi.spot.gwas.deposition.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.Association;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.domain.Sample;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;
import uk.ac.ebi.spot.gwas.deposition.dto.AssociationDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SampleDto;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;
import uk.ac.ebi.spot.gwas.deposition.javers.*;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.AssociationDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.SampleDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.rest.dto.StudyDtoAssembler;
import uk.ac.ebi.spot.gwas.deposition.service.*;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConversionJaversServiceImpl implements ConversionJaversService {
    private static final Logger log = LoggerFactory.getLogger(ConversionServiceImpl.class);


    @Autowired
    private StudiesService studiesService;

    @Autowired
    private AssociationsService associationsService;

    @Autowired
    private SamplesService samplesService;

    @Autowired
    private FileUploadsService fileUploadsService;

    @Override
    public Optional<Map<Double, List<JaversChangeWrapper>>> filterJaversResponse(List<JaversChangeWrapper> javersChangeWrapperList) {
        return Optional.of(javersChangeWrapperList)
                .map((changeList) -> {
                    List<Double> commitIdChanges = changeList.stream()
                            .filter((javersChangeWrapper) ->
                                    ( javersChangeWrapper.getProperty().equals("metadataStatus")) &&
                                    javersChangeWrapper.getRight().toString().equals("VALID"))
                            .map((change) -> change.getCommitMetadata().getId())
                            .collect(Collectors.toList());
                    Map<Double, List<JaversChangeWrapper>> versionMap = new LinkedHashMap<>();
                    commitIdChanges.forEach((commitId) -> {
                      List<JaversChangeWrapper> versionChanges =  changeList.stream()
                              .filter((change) -> change.getCommitMetadata().getId().equals(commitId))
                              .collect(Collectors.toList());
                      versionMap.put(commitId, versionChanges );
                    });
                   return versionMap;
                });

    }
    @Override
    public Optional<List<FileUpload>> filterJaversResponseForFiles(List<JaversChangeWrapper> javersChangeWrapperList) {

        return Optional.of(javersChangeWrapperList)
                .map((changes) ->
                    changes.stream()
                            .filter((change) -> change.getProperty().equals("fileUploads"))
                            .flatMap((change) -> change.getElementChanges().stream())
                            .map(this::processFileUploadTag)
                            .collect(Collectors.toList()).stream()
                            .distinct()
                            .map(this::getFileUploadDetails)
                            .filter(fileUpload -> !fileUpload.getStatus().equals("INVALID"))
                            .collect(Collectors.toList()));




    }

    @Override
    public List<VersionSummary> filterStudiesFromJavers(Optional<Map<Double, List<JaversChangeWrapper>>> javersChangeWrapperList) {
        Map<Double, List<JaversChangeWrapper>> versionMap = javersChangeWrapperList.get();
        log.info("versionMap ****"+versionMap);
        List<VersionSummary> summaries = new ArrayList<>();
        Set<Double> keys = versionMap.keySet();
        log.info("keys ****"+keys);
        Double[] keysArray =  keys.toArray(new Double[keys.size()]);
        log.info("keysArray ****"+keysArray);
        for(int i = 0 ; i < keys.size() -1 ; i++) {
            log.info("Inside Keys");
            VersionSummary versionSummary = compareVersions(versionMap.get(keysArray[i]),
                    versionMap.get(keysArray[i+1]));
            summaries.add(versionSummary);
        }
        return summaries;
    }

    public List<VersionSummary> mapFilesToVersionSummary(List<VersionSummary> summaries, List<FileUpload> fileUploads) {
        VersionSummary[] summaryArr = summaries.toArray(new VersionSummary[summaries.size()]);
        FileUpload[] fileUploadArr = fileUploads.toArray(new FileUpload[fileUploads.size()]);

        for(int i = 0 ; i < summaryArr.length; i++) {
            summaryArr[i].setOldFileDetails(new FileSummaryStats( fileUploadArr[i+1].getFileName(),
                    fileUploadArr[i+1].getId()));
            summaryArr[i].setNewFileDetails(new FileSummaryStats( fileUploadArr[i].getFileName(),
                    fileUploadArr[i].getId()));
        }

        return Arrays.asList(summaryArr);
    }



    private VersionSummary compareVersions(List<JaversChangeWrapper> newChange, List<JaversChangeWrapper> oldChange) {
        List<Study> newStudies = newChange.stream()
                .filter( (javersChangeWrapper) ->
                javersChangeWrapper.getProperty().equals("studies"))
                .flatMap((javersChange) -> javersChange.getElementChanges().stream())
                .map(this::processStudyTag)
                .collect(Collectors.toList());

        List<Study> prevStudies = oldChange.stream()
                .filter( (javersChangeWrapper) ->
                javersChangeWrapper.getProperty().equals("studies"))
                .flatMap((javersChange) -> javersChange.getElementChanges().stream())
                .map(this::processStudyTag)
                .collect(Collectors.toList());

        List<Association> newAssociations = newChange.stream()
                .filter( (javersChangeWrapper) ->
                javersChangeWrapper.getProperty().equals("associations"))
                .flatMap((javersChange) -> javersChange.getElementChanges().stream())
                .map(this::processAssociationTag)
                .collect(Collectors.toList());


        List<Association> prevAssociations = oldChange.stream()
                .filter( (javersChangeWrapper) ->
                javersChangeWrapper.getProperty().equals("associations"))
                .flatMap((javersChange) -> javersChange.getElementChanges().stream())
                .map(this::processAssociationTag)
                .collect(Collectors.toList());

        List<Sample> newSamples = newChange.stream()
                .filter( (javersChangeWrapper) ->
                        javersChangeWrapper.getProperty().equals("samples"))
                .flatMap((javersChange) -> javersChange.getElementChanges().stream())
                .map(this::processSampleTag)
                .collect(Collectors.toList());


        List<Sample> prevSamples = oldChange.stream()
                .filter( (javersChangeWrapper) ->
                        javersChangeWrapper.getProperty().equals("samples"))
                .flatMap((javersChange) -> javersChange.getElementChanges().stream())
                .map(this::processSampleTag)
                .collect(Collectors.toList());

        log.info("newStudies****"+newStudies);
        log.info("prevStudies****"+prevStudies);
        log.info("newAssociations****"+newAssociations);
        log.info("prevAssociations****"+prevAssociations);

        VersionSummary versionSummary = new VersionSummary();
        versionSummary.setCurrentVersionSummary(populateCurrentVersionSummary(
                newStudies.size(), newAssociations.size(), newSamples.size()));
        VersionSummaryStats versionSummaryStats = new VersionSummaryStats();
        VersionDiffStats  versionDiffStats = new VersionDiffStats();
        AddedRemoved addedRemoved = getStudyVersionStats(prevStudies , newStudies, versionDiffStats);

        VersionSummaryStats studyStats = populateVersionSummaryStudyStats(addedRemoved.getAdded(),
                addedRemoved.getRemoved(), versionSummaryStats);


        AddedRemoved addedRemovedasscn = getAssociationVersionStats(prevAssociations, newAssociations);
        VersionSummaryStats asscnStats = populateVersionSummaryAssociationStats(addedRemovedasscn.getAdded(),
                addedRemovedasscn.getRemoved(), studyStats);

        AddedRemoved addedRemovedSmpl = getSampleVersionStats(prevSamples, newSamples);
        VersionSummaryStats sampleStats = populateVersionSummarySampleStats(addedRemovedSmpl.getAdded() ,
                addedRemovedSmpl.getRemoved(), asscnStats);

        AddedRemoved traitsAddedRemoved = getReportedTraitVersionStats(prevStudies, newStudies);
        VersionSummaryStats traitsStats = populateVersionSummaryTraitsStats(traitsAddedRemoved.getAdded(), traitsAddedRemoved.getRemoved(),
                sampleStats);
        AddedRemoved efosAddedRemoved = getReportedEfoVersionStats(prevStudies, newStudies);
        VersionSummaryStats efoStats = populateVersionSummaryEfoStats(efosAddedRemoved.getAdded(), efosAddedRemoved.getRemoved(),
                traitsStats);
        versionSummary.setVersionSummaryStats(efoStats);


        versionDiffStats.setStudies(new ArrayList<VersionDiffStats>());
        Map<String,List<Association>> prevstudyAscnsMap = prevAssociations.stream()
                .collect(Collectors.groupingBy(Association::getStudyTag));
        Map<String,List<Association>> newstudyAscnsMap = newAssociations.stream()
                .collect(Collectors.groupingBy(Association::getStudyTag));
        Map<String,List<Sample>> prevStudySamplesMap = prevSamples.stream()
                .collect(Collectors.groupingBy(Sample::getStudyTag));
        Map<String,List<Sample>> newStudySamplesMap = newSamples.stream()
                .collect(Collectors.groupingBy(Sample::getStudyTag));
        Map<String, List<Study>> prevStudyMap = prevStudies.stream()
                .collect(Collectors.groupingBy(Study::getStudyTag));
        Map<String, List<Study>> newStudyMap = newStudies.stream()
                .collect(Collectors.groupingBy(Study::getStudyTag));


        prevStudyMap.forEach((tag, studyList) -> {
            log.info("Study Tag ****"+tag);
            VersionDiffStats  versionStudyDiffStats = findStudyChanges(tag, studyList, newStudies);
            if(prevstudyAscnsMap.get(tag) != null  ) {
                log.info("Inside Association loop ");
                AddedRemoved addedRemovedAsscns = getAssociationVersionStats(prevstudyAscnsMap.get(tag),
                        newstudyAscnsMap.get(tag) !=null ? newstudyAscnsMap.get(tag) : Collections.emptyList());

                versionStudyDiffStats.setAscnsAdded(addedRemovedAsscns.getAdded());
                versionStudyDiffStats.setAscnsRemoved(addedRemovedAsscns.getRemoved());
                VersionDiffStats aggregateDiffStats = findAssociationChanges(tag, prevstudyAscnsMap.get(tag),
                        newstudyAscnsMap.get(tag) !=null ? newstudyAscnsMap.get(tag) : Collections.emptyList(), versionStudyDiffStats);

            }else{
                if(newstudyAscnsMap.get(tag) != null) {
                    log.info("Inside Association loop where old study has no asscn ");
                    AddedRemoved addedRemovedAsscns = getAssociationVersionStats(Collections.emptyList(),
                            newstudyAscnsMap.get(tag) );
                    versionStudyDiffStats.setAscnsAdded(addedRemovedAsscns.getAdded());
                    versionStudyDiffStats.setAscnsRemoved(addedRemovedAsscns.getRemoved());
                    versionDiffStats.getStudies().add(versionStudyDiffStats);
                }else {
                    AddedRemoved addedRemovedAsscns = getAssociationVersionStats(Collections.emptyList(),
                            Collections.emptyList() );
                    versionStudyDiffStats.setAscnsAdded(addedRemovedAsscns.getAdded());
                    versionStudyDiffStats.setAscnsRemoved(addedRemovedAsscns.getRemoved());
                    versionDiffStats.getStudies().add(versionStudyDiffStats);
                }
            }

            if(prevStudySamplesMap.get(tag) != null  ) {
                log.info("Inside Sample loop ");
                AddedRemoved addedRemovedSamples = getSampleVersionStats(prevStudySamplesMap.get(tag), newStudySamplesMap.get(tag) !=null ?
                        newStudySamplesMap.get(tag) : Collections.emptyList());
                versionStudyDiffStats.setSamplesAdded(addedRemovedSamples.getAdded());
                versionStudyDiffStats.setSamplesRemoved(addedRemovedSamples.getRemoved());
                VersionDiffStats simpleDiffStats = findSampleChanges(tag, prevStudySamplesMap.get(tag), newStudySamplesMap.get(tag) !=null ?
                        newStudySamplesMap.get(tag) : Collections.emptyList(), versionStudyDiffStats);
                versionDiffStats.getStudies().add(simpleDiffStats);
            }else {
                if (newStudySamplesMap.get(tag) != null) {
                    log.info("Inside Study loop where old study has no Sample ");
                    AddedRemoved addedRemovedSamples = getSampleVersionStats(Collections.emptyList(), newStudySamplesMap.get(tag) !=null ?
                            newStudySamplesMap.get(tag) : Collections.emptyList());
                    versionStudyDiffStats.setSamplesAdded(addedRemovedSamples.getAdded());
                    versionStudyDiffStats.setSamplesRemoved(addedRemovedSamples.getRemoved());
                    versionDiffStats.getStudies().add(versionStudyDiffStats);
                }else {
                    AddedRemoved addedRemovedSamples = getSampleVersionStats(Collections.emptyList(),
                            Collections.emptyList() );
                    versionStudyDiffStats.setSamplesAdded(addedRemovedSamples.getAdded());
                    versionStudyDiffStats.setSamplesRemoved(addedRemovedSamples.getRemoved());
                    versionDiffStats.getStudies().add(versionStudyDiffStats);
                }
            }
        });

        String[] studyTagsAdded = versionDiffStats.getStudyTagsAdded().split(",");
        List<String> studyTagsList = Arrays.asList(studyTagsAdded);

        newStudyMap.forEach((tag, studyList) -> {
            if(studyTagsList.contains(tag)) {
                log.info("Studies added newly");
                log.info("Studies added ->"+tag);
                VersionDiffStats newversionDiffStats = new VersionDiffStats();
                newversionDiffStats.setEntity(tag);
                AddedRemoved addedRemovedAsscns = getAssociationVersionStats(Collections.emptyList(),
                        newstudyAscnsMap.get(tag) !=null ? newstudyAscnsMap.get(tag) : Collections.emptyList());
                AddedRemoved addedRemovedSamples = getSampleVersionStats(Collections.emptyList(), newStudySamplesMap.get(tag) !=null ?
                        newStudySamplesMap.get(tag) : Collections.emptyList());
                newversionDiffStats.setAscnsAdded(addedRemovedAsscns.getAdded());
                newversionDiffStats.setAscnsRemoved(addedRemovedAsscns.getRemoved());
                newversionDiffStats.setSamplesAdded(addedRemovedSamples.getAdded());
                newversionDiffStats.setSamplesRemoved(addedRemovedSamples.getRemoved());
                versionDiffStats.getStudies().add(newversionDiffStats);
            }
        });



        versionSummary.setVersionDiffStats(versionDiffStats);
        return versionSummary;
    }

    private CurrentVersionSummary populateCurrentVersionSummary(int countStudies, int countAscns, int  countSamples) {
        CurrentVersionSummary currentVersionSummary = new CurrentVersionSummary();
        currentVersionSummary.setTotalStudies(countStudies);
        currentVersionSummary.setTotalAssociations(countAscns);
        currentVersionSummary.setTotalSamples(countSamples);
        return currentVersionSummary;
    }

    private VersionSummaryStats populateVersionSummaryStudyStats(int added, int removed, VersionSummaryStats stats) {
        stats.setStudiesAdded(added);
        stats.setStudiesRemoved(removed);
        return stats;
    }

    private VersionSummaryStats populateVersionSummaryTraitsStats(int added, int removed, VersionSummaryStats stats) {
        stats.setReportedTraitsAdded(added);
        stats.setReportedTraitsRemoved(removed);
        return stats;
    }

    private VersionSummaryStats populateVersionSummaryEfoStats(int added, int removed, VersionSummaryStats stats) {
        stats.setReportedTraitsAdded(added);
        stats.setReportedTraitsRemoved(removed);
        return stats;
    }

    private VersionSummaryStats populateVersionSummaryAssociationStats(int added, int removed, VersionSummaryStats stats) {
        stats.setAscnsAdded(added);
        stats.setAscnsRemoved(removed);
        return stats;
    }

    private VersionSummaryStats populateVersionSummarySampleStats(int added, int removed, VersionSummaryStats stats) {
        stats.setSamplesAdded(added);
        stats.setSamplesRemoved(removed);
        return stats;
    }

    private VersionDiffStats findStudyChanges(String tag, List<Study> studyList, List<Study> newStudies) {

        List<StudyDto> newStudiesDTO = newStudies.stream()
                .filter((study) -> study.getStudyTag().equals(tag))
                .map(StudyDtoAssembler::assemble)
                .collect(Collectors.toList());

        log.info("Inside findStudyChanges newStudie**** ");

        List<StudyDto> prevStudiesDTO = studyList.stream()
                .map(StudyDtoAssembler::assemble)
                .collect(Collectors.toList());


        VersionDiffStats versionDiffStats = new VersionDiffStats();
        versionDiffStats.setEntity(tag);
        if(newStudiesDTO != null && !newStudiesDTO.isEmpty()) {
            List<ValueChangeWrapper> studyChanges = diffStudies(prevStudiesDTO.get(0),
                    newStudiesDTO.get(0));
            versionDiffStats.setEdited(studyChanges.stream()
                    .map(this::mapChangetoVersionStats)
                    .collect(Collectors.toList()));
        }
        return versionDiffStats;

    }

    private VersionDiffStats findAssociationChanges(String tag, List<Association> prevAscns, List<Association> newAscns, VersionDiffStats diffStats) {

        if(!newAscns.isEmpty())
        diffStats.setAssociations(new ArrayList<VersionDiffStats>());

        prevAscns.forEach((asscn) -> {
            log.info("VariantId*****"+asscn.getVariantId());
            List<AssociationDto> newAsscnsDto = newAscns.stream()
                    .filter((ascn) -> ascn.getVariantId().equals(asscn.getVariantId()))
                    .map(AssociationDtoAssembler::assemble)
                    .collect(Collectors.toList());
            AssociationDto prevAsscnsDto = AssociationDtoAssembler.assemble(asscn);
            if (!newAsscnsDto.isEmpty()) {
               List<ValueChangeWrapper> valChanges = diffAssociations(newAsscnsDto.get(0), prevAsscnsDto);
               if(!valChanges.isEmpty()) {
                   VersionDiffStats versionDiffStats = new VersionDiffStats();
                   versionDiffStats.setEntity(asscn.getVariantId());
                   versionDiffStats.setEdited(valChanges.stream().
                           map(this::mapChangetoVersionStats)
                           .collect(Collectors.toList()));
                   diffStats.getAssociations().add(versionDiffStats);
               }
            }

        });

    return diffStats;


    }

    private VersionDiffStats findSampleChanges(String tag, List<Sample> prevSamples, List<Sample> newSamples, VersionDiffStats diffStats) {

        if(!newSamples.isEmpty())
            diffStats.setSampleGroups(new ArrayList<VersionDiffStats>());

        prevSamples.forEach((sample) -> {
            log.info("Sample*****"+sample.getStage()+"|"+sample.getAncestryCategory());
            List<SampleDto> newSamplesDto = newSamples.stream()
                    .filter((sampleGroup) -> (sampleGroup.getStage() + sample.getAncestryCategory())
                            .equals(sampleGroup.getStage() + sample.getAncestryCategory()))
                    .map(SampleDtoAssembler::assemble)
                    .collect(Collectors.toList());
            SampleDto prevSampleDto = SampleDtoAssembler.assemble(sample);
            if (!newSamplesDto.isEmpty()) {
                List<ValueChangeWrapper> valChanges = diffSamples(newSamplesDto.get(0), prevSampleDto);
                if(!valChanges.isEmpty()) {
                    VersionDiffStats versionDiffStats = new VersionDiffStats();
                    versionDiffStats.setEntity(sample.getStage()+"|"+sample.getAncestryCategory());
                    versionDiffStats.setEdited(valChanges.stream().
                            map(this::mapChangetoVersionStats)
                            .collect(Collectors.toList()));
                    diffStats.getSampleGroups().add(versionDiffStats);
                }
            }

        });

        return diffStats;


    }

    private AddedRemoved    getStudyVersionStats(List<Study> prevStudies, List<Study> newStudies, VersionDiffStats versionDiffStats) {
        List<String> newStudyTags = newStudies.stream()
                .map(Study::getStudyTag)
                .collect(Collectors.toList());

        List<String> prevStudyTags = prevStudies.stream()
                .map(Study::getStudyTag)
                .collect(Collectors.toList());

        List<Study> studiesRemoved = prevStudies.stream()
                .filter((study) -> !newStudyTags.contains(study.getStudyTag()))
                .collect(Collectors.toList());

        String studyTagsRemoved = studiesRemoved.stream()
                .map(study -> study.getStudyTag())
                .collect(Collectors.joining(","));

        List<Study> studiesAdded = newStudies.stream()
                .filter((study) -> !prevStudyTags.contains(study.getStudyTag()))
                .collect(Collectors.toList());

        String studyTagsAdded = studiesAdded.stream()
                .map(study -> study.getStudyTag())
                .collect(Collectors.joining(","));


        log.info("newStudyTags****"+newStudyTags);
        log.info("prevStudyTags****"+prevStudyTags);
        log.info("studiesRemoved****"+studiesRemoved);
        log.info("studiesAdded****"+studiesAdded);

        versionDiffStats.setStudyTagsAdded(studyTagsAdded);
        versionDiffStats.setStudyTagsRemoved(studyTagsRemoved);

        AddedRemoved addedRemoved = new AddedRemoved();
        addedRemoved.setAdded(studiesAdded.size());
        addedRemoved.setRemoved(studiesRemoved.size());
        return addedRemoved;

    }

    private AddedRemoved  getReportedTraitVersionStats(List<Study> prevStudies, List<Study> newStudies) {
        List<String> newReportedTraits = newStudies.stream()
                .map(Study::getTrait)
                .collect(Collectors.toList());

        List<String> prevReportedTraits = prevStudies.stream()
                .map(Study::getTrait)
                .collect(Collectors.toList());

        List<Study> traitsRemoved = prevStudies.stream()
                .filter((study) -> !newReportedTraits.contains(study.getTrait()))
                .collect(Collectors.toList());

        List<Study> traitsAdded = newStudies.stream()
                .filter((study) -> !prevReportedTraits.contains(study.getTrait()))
                .collect(Collectors.toList());

        AddedRemoved addedRemoved = new AddedRemoved();
        addedRemoved.setAdded(traitsAdded.size());
        addedRemoved.setRemoved(traitsRemoved.size());
        return addedRemoved;

    }


    private AddedRemoved  getReportedEfoVersionStats(List<Study> prevStudies, List<Study> newStudies) {
        List<String> newEfoTraits = newStudies.stream()
                .map(Study::getEfoTrait)
                .flatMap((efos) -> Arrays.asList(efos.split("|")).stream())
                .map(efo -> efo.trim())
                .collect(Collectors.toList());

        List<String> prevEfoTraits = prevStudies.stream()
                .map(Study::getEfoTrait)
                .flatMap((efos) -> Arrays.asList(efos.split("|")).stream())
                .map(efo -> efo.trim())
                .collect(Collectors.toList());

       /* List<Study> efoRemoved = prevStudies.stream()
                .filter((study) -> !newEfoTraits.contains(study.getTrait()))
                .collect(Collectors.toList());*/

        List<String> efoRemoved = prevStudies.stream()
                .flatMap(study -> Arrays.asList(study.getEfoTrait().split("|")).stream())
                .map(efo -> efo.trim())
                .filter(efo ->  !newEfoTraits.contains(efo))
                .collect(Collectors.toList());


        /*List<Study> efoAdded = newStudies.stream()
                .filter((study) -> !prevEfoTraits.contains(study.getTrait()))
                .collect(Collectors.toList());*/
        List<String> efoAdded = newStudies.stream()
                .flatMap(study -> Arrays.asList(study.getEfoTrait().split("|")).stream())
                .map(efo -> efo.trim())
                .filter(efo -> !prevEfoTraits.contains(efo))
                .collect(Collectors.toList());

        log.info("newEfoTraits****"+newEfoTraits);
        log.info("prevEfoTraits****"+prevEfoTraits);
        log.info("efoRemoved****"+efoRemoved);
        log.info("efoAdded****"+efoAdded);
        AddedRemoved addedRemoved = new AddedRemoved();
        addedRemoved.setAdded(efoAdded.size());
        addedRemoved.setRemoved(efoRemoved.size());
        return addedRemoved;

    }

    private AddedRemoved getAssociationVersionStats(List<Association> prevAscns, List<Association> newAscns) {
        log.info("Inside getAssociationVersionStats() ");

        List<String> newAscnsTags = newAscns.stream()
                .map(asscn -> asscn.getStudyTag() + asscn.getVariantId())
                .collect(Collectors.toList());

        List<String> prevAscnsTags = prevAscns.stream()
                .map(asscn -> asscn.getStudyTag() + asscn.getVariantId())
                .collect(Collectors.toList());

        List<Association> asscnsRemoved = prevAscns.stream()
                .filter(asscn -> !newAscnsTags.contains(asscn.getStudyTag() + asscn.getVariantId()))
                .collect(Collectors.toList());

        List<Association> asscnsAdded = newAscns.stream()
                .filter(asscn -> !prevAscnsTags.contains(asscn.getStudyTag() + asscn.getVariantId()))
                .collect(Collectors.toList());

        log.info("newAscnsTags****"+newAscnsTags);
        log.info("prevAscnsTags****"+prevAscnsTags);
        log.info("asscnsRemoved****"+asscnsRemoved);
        log.info("asscnsAdded****"+asscnsAdded);

        AddedRemoved addedRemoved = new AddedRemoved();
        addedRemoved.setAdded(asscnsAdded.size());
        addedRemoved.setRemoved(asscnsRemoved.size());

        return addedRemoved;
    }

    private AddedRemoved getSampleVersionStats(List<Sample> prevSamples, List<Sample> newSamples) {
        log.info("Inside getAssociationVersionStats() ");

        List<String> newSamplesTags = newSamples.stream()
                .map(sample -> sample.getStudyTag() + sample.getStage() +sample.getAncestryCategory())
                .collect(Collectors.toList());

        List<String> prevSamplesTags = prevSamples.stream()
                .map(sample -> sample.getStudyTag() + sample.getStage() +sample.getAncestryCategory())
                .collect(Collectors.toList());

        List<Sample> samplesRemoved = prevSamples.stream()
                .filter(sample -> !newSamplesTags.contains(sample.getStudyTag() + sample.getStage() +
                        sample.getAncestryCategory()))
                .collect(Collectors.toList());

        List<Sample> samplesAdded = newSamples.stream()
                .filter(sample -> !prevSamplesTags.contains(sample.getStudyTag() + sample.getStage() +
                        sample.getAncestryCategory()))
                .collect(Collectors.toList());

        log.info("newAscnsTags****"+newSamplesTags);
        log.info("prevAscnsTags****"+prevSamplesTags);
        log.info("asscnsRemoved****"+samplesRemoved);
        log.info("asscnsAdded****"+samplesAdded);

        AddedRemoved addedRemoved = new AddedRemoved();
        addedRemoved.setAdded(samplesAdded.size());
        addedRemoved.setRemoved(samplesRemoved.size());

        return addedRemoved;
    }

    private DiffPropertyObject mapChangetoVersionStats(ValueChangeWrapper valueChangeWrapper) {
        DiffPropertyObject diffStats = new DiffPropertyObject();
        diffStats.setProperty(valueChangeWrapper.getProperty());
        diffStats.setOldValue(valueChangeWrapper.getLeft());
        diffStats.setNewValue(valueChangeWrapper.getRight());
        return diffStats;

    }

    private List<ValueChangeWrapper> diffStudies(StudyDto dto1, StudyDto dto2) {
        Javers javers = JaversBuilder.javers().build();
        Diff diff = javers.compare(dto1, dto2);
        log.info("************");
        log.info("Diff"+ diff);
        List<ValueChange> valChanges = diff.getChangesByType(ValueChange.class);
        try {
            ValueChangeWrapper[]  changes = new ObjectMapper().readValue(
                    javers.getJsonConverter().toJson(valChanges), ValueChangeWrapper[].class);
            return Arrays.asList(changes);
        } catch(Exception ex){
            log.error("Error in mapping Javers Changes"+ex.getMessage(),ex );
            return null;
        }
    }

    private List<ValueChangeWrapper> diffAssociations(AssociationDto dto1, AssociationDto dto2) {
        Javers javers = JaversBuilder.javers().build();
        Diff diff = javers.compare(dto1, dto2);
        log.info("************");
        log.info("Diff Asscn"+ diff);
        List<ValueChange> valChanges = diff.getChangesByType(ValueChange.class);
        try {
            ValueChangeWrapper[]  changes = new ObjectMapper().readValue(
                    javers.getJsonConverter().toJson(valChanges), ValueChangeWrapper[].class);
            return Arrays.asList(changes);
        } catch(Exception ex){
            log.error("Error in mapping Javers Changes"+ex.getMessage(),ex );
            return null;
        }
    }

    private List<ValueChangeWrapper> diffSamples(SampleDto dto1, SampleDto dto2) {
        Javers javers = JaversBuilder.javers().build();
        Diff diff = javers.compare(dto1, dto2);
        log.info("************");
        log.info("Diff Sample"+ diff);
        List<ValueChange> valChanges = diff.getChangesByType(ValueChange.class);
        try {
            ValueChangeWrapper[]  changes = new ObjectMapper().readValue(
                    javers.getJsonConverter().toJson(valChanges), ValueChangeWrapper[].class);
            return Arrays.asList(changes);
        } catch(Exception ex){
            log.error("Error in mapping Javers Changes"+ex.getMessage(),ex );
            return null;
        }
    }



    private String processFileUploadTag(ElementChange elementChange){
        if (elementChange.getElementChangeType().equals("ValueAdded")){
            return elementChange.getValue().toString();
        }
        else if(elementChange.getElementChangeType().equals("ValueRemoved")){
            return elementChange.getValue().toString();
        }
        return null;
    }


    private Study processStudyTag(ElementChange elementChange){
        if (elementChange.getElementChangeType().equals("ValueAdded")){
            return studiesService.getStudy(elementChange.getValue().toString() );
        }
        return null;
    }

    private Association processAssociationTag(ElementChange elementChange){
      if (elementChange.getElementChangeType().equals("ValueAdded")){
          return associationsService.getAssociation(elementChange.getValue().toString() );
        }
        return null;
    }

    private Sample processSampleTag(ElementChange elementChange){
        if (elementChange.getElementChangeType().equals("ValueAdded")){
            return samplesService.getSample(elementChange.getValue().toString() );
        }
        return null;
    }

    private FileUpload getFileUploadDetails(String fileId){
        return fileUploadsService.getFileUpload(fileId);
    }


}
