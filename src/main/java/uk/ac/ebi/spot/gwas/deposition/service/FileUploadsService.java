package uk.ac.ebi.spot.gwas.deposition.service;

import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;

import java.io.InputStream;
import java.util.List;

public interface FileUploadsService {

    FileUpload storeFile(InputStream is, String fileName, String contentType, long fileSize, String fileType);

    FileUpload storeFile(MultipartFile file, String fileType);

    FileUpload getFileUpload(String fileUploadId);

    byte[] retrieveFileContent(String fileUploadId);

    List<FileUpload> getFileUploads(List<String> fileUploads);

    void deleteFileUpload(String fileUploadId);

    FileUpload save(FileUpload fileUpload);

    void setNotLatest(List<String> fileUploads);

    FileUpload getFileUploadByCallbackId(String callbackId);
}
