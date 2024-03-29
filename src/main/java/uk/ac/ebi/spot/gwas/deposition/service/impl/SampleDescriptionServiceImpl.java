package uk.ac.ebi.spot.gwas.deposition.service.impl;


import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.constants.AncestryConstants;
import uk.ac.ebi.spot.gwas.deposition.dto.SampleDto;
import uk.ac.ebi.spot.gwas.deposition.dto.StudyDto;
import uk.ac.ebi.spot.gwas.deposition.dto.SubmissionDataDto;
import uk.ac.ebi.spot.gwas.deposition.service.SampleDescriptionService;

import java.util.Arrays;
import java.util.List;

@Service
public class SampleDescriptionServiceImpl implements SampleDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(SampleDescriptionServiceImpl.class);

    @Override
    public Pair<String, String> buildSampleDescription(SubmissionDataDto submissionDataDto, StudyDto studyDto) {


        List<SampleDto> sampleDtos = submissionDataDto.getSamples();
        String initialSampleSize;
        String replicateSampleSize;

        final StringBuilder sbInitialSampleDesc = new StringBuilder();
        final StringBuilder sbReplicateSampleDesc = new StringBuilder();
        if(sampleDtos != null ) {
            sampleDtos.stream().filter((sampleDto -> sampleDto.getStudyTag().equalsIgnoreCase(studyDto.getStudyTag())))
                    .forEach(sampleDto -> {

                        if (sampleDto.getStage().equalsIgnoreCase(AncestryConstants.DISCOVERY)) {
                            sbInitialSampleDesc.append(buildDescription(sampleDto));
                            sbInitialSampleDesc.append(", ");
                        } else if (sampleDto.getStage().equalsIgnoreCase(AncestryConstants.REPLICATION)) {
                            sbReplicateSampleDesc.append(buildDescription(sampleDto));
                            sbReplicateSampleDesc.append(", ");
                        }
                    });
        }

        initialSampleSize = sbInitialSampleDesc.toString().trim();
        replicateSampleSize = sbReplicateSampleDesc.toString().trim();

        if (initialSampleSize.endsWith(",")) {
            initialSampleSize = initialSampleSize.substring(0, initialSampleSize.length() - 1);
        }
        if (replicateSampleSize.endsWith(",")) {
            replicateSampleSize = replicateSampleSize.substring(0, replicateSampleSize.length() - 1);
        }
        if (replicateSampleSize.equalsIgnoreCase("")) {
            replicateSampleSize = "NA";
        }

        log.info("Initial SampleDescription for Study Tag {} is {}",studyDto.getStudyTag(),initialSampleSize);
        log.info("Replicated SampleDescription for Study Tag {} is {}",studyDto.getStudyTag(),replicateSampleSize);
        //studyDto.builder().initialSampleDescription(sbInitialSampleDesc.toString()).build();
        // studyDto.builder().replicateSampleDescription(sbReplicateSampleDesc.toString()).build();
        return Pair.of(initialSampleSize, replicateSampleSize);

    }

    public String buildDescription(SampleDto sampleDto ) {


        String ancestry;
        if (sampleDto.getAncestry() != null) {
            ancestry = sampleDto.getAncestry().trim();
        } else {
            ancestry = sampleDto.getAncestryCategory().trim();
        }

        if (ancestry.trim().equalsIgnoreCase("NR")) {
            if (sampleDto.getCases() != null && sampleDto.getControls() != null && sampleDto.getCases() != 0 && sampleDto.getControls() != 0) {
                ancestry = String.format("%,d", sampleDto.getCases()) + " " + AncestryConstants.CASES + ", " + String.format("%,d", sampleDto.getControls()) + " " + AncestryConstants.CONTROLS;
            } else {
                if (sampleDto.getSize() != -1) {
                    ancestry = String.format("%,d", sampleDto.getSize()) + " " + AncestryConstants.INDIVIDUALS;
                } else {
                    ancestry += " " + AncestryConstants.INDIVIDUALS;
                }
            }
            return ancestry;
        }

        List<String> ancestryList = Arrays.asList(ancestry.split("\\|"));
        ancestry = "";
        for (String entry : ancestryList) {
            String adaptEntry = adaptEntry(entry);

            if (sampleDto.getCases() != null && sampleDto.getControls() != null && sampleDto.getCases() != 0 && sampleDto.getControls() != 0) {
                ancestry += String.format("%,d", sampleDto.getCases()) + " " + adaptEntry + " " + AncestryConstants.CASES + ", " +
                        String.format("%,d", sampleDto.getControls()) + " " + adaptEntry + " " + AncestryConstants.CONTROLS;
            } else {
                ancestry += adaptEntry;
            }

            ancestry += ", ";
        }
        ancestry = ancestry.trim();
        if (ancestry.endsWith(",")) {
            ancestry = ancestry.substring(0, ancestry.length() - 1).trim();
        }
        if (!ancestry.endsWith(AncestryConstants.CONTROLS)) {
            ancestry += " " + AncestryConstants.INDIVIDUALS;
            if (sampleDto.getSize() != -1) {
                ancestry = String.format("%,d", sampleDto.getSize()) + " " + ancestry;
            }
        }

        return ancestry;

    }


    private String adaptEntry(String entry) {
        entry = entry.trim();
        if (entry.contains(AncestryConstants.UNSPECIFIED)) {
            return entry.replace(AncestryConstants.UNSPECIFIED, AncestryConstants.ANCESTRY);
        }
        if (entry.contains(" or ")) {
            return entry;
        }

        if (!Arrays.asList(AncestryConstants.ANCESTRY_CATS).contains(entry.toLowerCase())) {
            entry += " " + AncestryConstants.ANCESTRY;
        }
        return entry;
    }
}
