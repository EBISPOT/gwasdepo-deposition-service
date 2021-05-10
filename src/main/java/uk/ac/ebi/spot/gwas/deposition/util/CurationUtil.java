package uk.ac.ebi.spot.gwas.deposition.util;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

public class CurationUtil {

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

    public static String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7, headerAuth.length());
        }
        return null;
    }

}
