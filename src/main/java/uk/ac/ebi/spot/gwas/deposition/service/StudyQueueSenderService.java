package uk.ac.ebi.spot.gwas.deposition.service;


public interface StudyQueueSenderService {

    public void sendStudiesToQueue(String submissionId);
}
