package uk.ac.ebi.spot.gwas.deposition.service.impl;

import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.ac.ebi.spot.gwas.deposition.constants.FileUploadStatus;
import uk.ac.ebi.spot.gwas.deposition.domain.FileUpload;
import uk.ac.ebi.spot.gwas.deposition.exception.EntityNotFoundException;
import uk.ac.ebi.spot.gwas.deposition.exception.FileProcessingException;
import uk.ac.ebi.spot.gwas.deposition.repository.FileUploadRepository;
import uk.ac.ebi.spot.gwas.deposition.service.FileUploadsService;
import uk.ac.ebi.spot.gwas.deposition.util.BackendUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class FileUploadsServiceImpl implements FileUploadsService {

    private static final Logger log = LoggerFactory.getLogger(FileUploadsService.class);

    @Autowired
    private GridFsOperations gridFsOperations;

    @Autowired
    private FileUploadRepository fileUploadRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public FileUpload storeFile(InputStream is, String fileName, String contentType,
                                long fileSize, String fileType) {
        log.info("Storing new file: {}", fileName);
        ObjectId objectId = is == null ? null : gridFsOperations.store(is, fileName, contentType);
        log.info("File successfully stored: {}", objectId);
        FileUpload fileUpload = new FileUpload(
                objectId != null ? objectId.toString() : null, fileName,
                fileSize, FileUploadStatus.PROCESSING.name(),
                fileType);
        fileUpload = fileUploadRepository.insert(fileUpload);
        log.info("FileUpload object successfully created: {}", fileUpload.getId());
        return fileUpload;
    }

    @Override
    public FileUpload storeFile(MultipartFile file, String fileType) {
        try {
            String fileName = BackendUtil.normalize(file.getOriginalFilename());
            return storeFile(file.getInputStream(), fileName,
                    file.getContentType(), file.getSize(), fileType);
        } catch (IOException e) {
            log.error("Unable to store file [{}]: {}", file.getOriginalFilename(), e.getMessage(), e);
        }
        throw new FileProcessingException("Unable to store file: " + file.getOriginalFilename());
    }

    @Override
    public FileUpload getFileUpload(String fileUploadId) {
        log.info("Retrieving file upload: {}", fileUploadId);
        Optional<FileUpload> optionalFileUpload = fileUploadRepository.findById(fileUploadId);
        if (!optionalFileUpload.isPresent()) {
            log.error("Unable to find file upload: {}", fileUploadId);
            throw new EntityNotFoundException("Unable to find file upload: " + fileUploadId);
        }
        log.info("File upload successfully retrieved: {}", optionalFileUpload.get().getId());
        return optionalFileUpload.get();
    }

    @Override
    public byte[] retrieveFileContent(String fileUploadId) {
        log.info("Retrieving file content: {}", fileUploadId);
        FileUpload fileUpload = getFileUpload(fileUploadId);

        GridFSFile file = getGridFsdbFileForFileId(fileUpload.getFileId());
        byte[] attachmentByteArray;

        try (InputStream inputStream = getFileDownloadStream(file.getObjectId())) {
            attachmentByteArray = IOUtils.toByteArray(inputStream);
        } catch (IOException e) {
            log.error("Unable to get file {}: {}", fileUpload.getFileId(), e.getMessage(), e);
            throw new FileProcessingException("Unable to get file " + fileUpload.getFileId()
                    + ": " + e.getMessage());
        }
        log.info("Content for file {} successfully retrieved: {}", fileUpload.getFileId(),
                attachmentByteArray.length);

        return attachmentByteArray;
    }

    @Override
    public List<FileUpload> getFileUploads(List<String> ids) {
        log.info("Retrieving files: {}", ids);
        if (ids == null) {
            return new ArrayList<>();
        }

        List<FileUpload> fileUploads = fileUploadRepository.findByIdIn(ids);
        log.info("Found {} files.", fileUploads.size());
        return fileUploads;
    }

    @Override
    public void deleteFileUpload(String fileUploadId) {
        log.info("Deleting file upload: {}", fileUploadId);
        FileUpload fileUpload = getFileUpload(fileUploadId);
        deleteGridFSFile(fileUpload.getFileId());
        fileUploadRepository.delete(fileUpload);
    }

    @Override
    public FileUpload save(FileUpload fileUpload) {
        log.info("Saving file upload: {}", fileUpload.getId());
        fileUpload = fileUploadRepository.save(fileUpload);
        log.info("File upload successfully saved: {}", fileUpload.getId());
        return fileUpload;
    }

    @Async
    @Override
    public void setNotLatest(List<String> fileUploads) {
        log.info("Setting latest to false for: {}", fileUploads);
        for (String fileUploadId : fileUploads) {
            FileUpload fileUpload = getFileUpload(fileUploadId);
            fileUpload.setLatest(false);
            save(fileUpload);
        }
    }

    @Override
    public FileUpload getFileUploadByCallbackId(String callbackId) {
        log.info("Retrieving file upload by callback ID: {}", callbackId);
        Optional<FileUpload> fileUploadOptional = fileUploadRepository.findByCallbackId(callbackId);
        if (fileUploadOptional.isPresent()) {
            log.info("Found file upload: {}", fileUploadOptional.get().getId());
            return fileUploadOptional.get();
        }
        log.error("Unable to find file upload for callback ID: {}", callbackId);
        return null;
    }

    private void deleteGridFSFile(String fileId) {
        log.info("Received call to delete GridFSFile with id: {}", fileId);
        Query query = new Query(Criteria.where("_id").is(fileId));
        GridFSFile file = gridFsOperations.findOne(query);
        if (file == null) {
            log.error("No file found in DB for id: {}", fileId);
            throw new EntityNotFoundException("No file found in DB for id: " + fileId);
        }
        gridFsOperations.delete(query);
    }

    private GridFSFile getGridFsdbFileForFileId(String fileId) {
        log.info("Received call to get GridFSFile for id: {}", fileId);
        Query query = new Query(Criteria.where("_id").is(fileId));
        GridFSFile file = gridFsOperations.findOne(query);
        if (file == null) {
            log.error("No file found in DB for id: {}", fileId);
            throw new EntityNotFoundException("No file found in DB for id: " + fileId);
        }
        return file;
    }

    private InputStream getFileDownloadStream(ObjectId objectId) {
        log.info("Retrieving file for download: {}", objectId);
        GridFSDownloadStream stream = GridFSBuckets.create(mongoTemplate.getDb())
                .openDownloadStream(objectId);
        log.info("Retrieved download stream {}", objectId);
        return stream;

    }
}
