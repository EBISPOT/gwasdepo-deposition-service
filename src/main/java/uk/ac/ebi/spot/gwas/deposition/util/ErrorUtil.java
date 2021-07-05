package uk.ac.ebi.spot.gwas.deposition.util;

import uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants;
import uk.ac.ebi.spot.gwas.template.validator.util.ErrorMessageTemplateProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ErrorUtil {

    public static List<String> transform(Map<String, List<String>> errorMap, ErrorMessageTemplateProcessor errorMessageTemplateProcessor) {
        List<String> errors = new ArrayList<>();
        errorMap.forEach((sheet, errorList) -> {
            String sheetName = errorMessageTemplateProcessor.processGenericError(GWASDepositionBackendConstants.PREFIX_LABEL + "_" + sheet.toUpperCase(), null).get(0);
            errorList.stream().map(error -> sheetName + ": " + error).forEach(errors::add);
        });

        return errors;
    }
}
