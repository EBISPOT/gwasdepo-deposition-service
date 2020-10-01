package uk.ac.ebi.spot.gwas.deposition.util;

import org.springframework.data.rest.webmvc.support.BaseUriLinkBuilder;
import org.springframework.hateoas.LinkBuilder;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import uk.ac.ebi.spot.gwas.deposition.domain.Author;
import uk.ac.ebi.spot.gwas.deposition.domain.Study;

import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.spot.gwas.deposition.constants.GWASDepositionBackendConstants.VALUE_NR;

public class BackendUtil {

    public static LinkBuilder underBasePath(ControllerLinkBuilder linkBuilder, String prefix) {
        URI uri = linkBuilder.toUri();
        try {
            URI origin = new URI(uri.getScheme(), uri.getAuthority(), null, null, null);
            URI suffix = new URI(null, null, uri.getPath(), uri.getQuery(), uri.getFragment());
            return prefix != null ?
                    BaseUriLinkBuilder.create(origin)
                            .slash(prefix)
                            .slash(suffix) :
                    BaseUriLinkBuilder.create(origin)
                            .slash(suffix);
        } catch (Exception e) {
            return linkBuilder;
        }
    }

    public static String normalize(String s) {
        String str = Normalizer.normalize(s, Normalizer.Form.NFD);
        return str.replaceAll("[^\\p{ASCII}]", "").replaceAll(" ", "_");
    }

    public static List<String> sToList(String s) {
        List<String> list = new ArrayList<>();
        if (s == null) {
            return list;
        }

        String[] parts = s.split(",");
        for (String part : parts) {
            part = part.trim();
            if (!"".equals(part)) {
                list.add(part);
            }
        }
        return list;
    }

    public static String extractName(Author author) {
        if (author.getLastName() != null) {
            return author.getLastName();
        }
        if (author.getFirstName() != null) {
            return author.getFirstName();
        }
        if (author.getGroup() != null) {
            return author.getGroup();
        }

        return "N/A";
    }

    public static boolean ssIsNR(Study study) {
        return sssIsNR(study.getSummaryStatisticsFile()) &&
                sssIsNR(study.getChecksum()) &&
                sssIsNR(study.getSummaryStatisticsAssembly());
    }

    private static boolean sssIsNR(String entry) {
        if (entry != null) {
            if (entry.equalsIgnoreCase(VALUE_NR)) {
                return true;
            }
        }
        return false;
    }
}
