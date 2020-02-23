package uk.ac.ebi.spot.gwas.deposition.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;

public class AuditHelper {

    public static final String toAuditHash(String entityType,
                                           String objectHash,
                                           String contextId,
                                           String messageType) {
        QueueAuditDto queueAuditDto = new QueueAuditDto(entityType, objectHash, contextId, messageType);
        String content = null;
        try {
            content = new ObjectMapper().writeValueAsString(queueAuditDto);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return content == null ? null : DigestUtils.sha1Hex(content);
    }
}
