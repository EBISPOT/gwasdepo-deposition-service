package uk.ac.ebi.spot.gwas.deposition.util;

import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.template.validator.util.ErrorMessageTemplateProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ErrorUtil {

    public static List<String> transform(Map<String, List<String>> errorMap, ErrorMessageTemplateProcessor errorMessageTemplateProcessor) {
        List<String> errors = new ArrayList<>();
        for (String sheet : errorMap.keySet()) {
            String sheetName = errorMessageTemplateProcessor.processGenericError(GWASDepositionBackendConstants.PREFIX_LABEL + "_" + sheet.toUpperCase(), null).get(0);

            List<String> errorList = errorMap.get(sheet);
            for (String error : errorList) {
                errors.add(sheetName + ": " + error);
            }
        }

        return errors;
    }
}
