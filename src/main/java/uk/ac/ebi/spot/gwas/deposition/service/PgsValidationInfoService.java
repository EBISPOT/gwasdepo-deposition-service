package uk.ac.ebi.spot.gwas.deposition.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.ac.ebi.spot.gwas.deposition.domain.PgsValidationInfo;
import uk.ac.ebi.spot.gwas.deposition.repository.PgsValidationInfoRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PgsValidationInfoService {

    private final PgsValidationInfoRepository repo;

    public void save(PgsValidationInfo info) { repo.save(info); }

    public Optional<PgsValidationInfo> get(String fileUploadId) {
        return repo.findById(fileUploadId);
    }
}