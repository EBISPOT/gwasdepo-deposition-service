package uk.ac.ebi.spot.gwas.deposition.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.spot.gwas.deposition.constants.IDPConstants;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

public class HeadersUtil {

    private static final Logger log = LoggerFactory.getLogger(HeadersUtil.class);

    public static String extractJWT(HttpServletRequest httpServletRequest) {
        String authHeader = httpServletRequest.getHeader(IDPConstants.AUTH_HEADER);
        if (authHeader == null) {
            String jwt = httpServletRequest.getHeader(IDPConstants.JWT_TOKEN);
            if (jwt == null && httpServletRequest.getCookies() != null) {
                jwt = Arrays.stream(httpServletRequest.getCookies())
                        .filter(cookie -> cookie.getName().equalsIgnoreCase(IDPConstants.COOKIE_ACCESSTOKEN)
                                || cookie.getName().equalsIgnoreCase(IDPConstants.JWT_TOKEN))
                        .findFirst().map(Cookie::getValue).orElse(jwt);
            }
            return jwt;
        }
        String[] parts = authHeader.split(" ");
        if (parts.length == 2 && parts[0].equalsIgnoreCase(IDPConstants.AUTH_BEARER)) {
            if ((parts[1] == null) || parts[1].equalsIgnoreCase("null")) {
                return null;
            }

            return parts[1];
        }

        return null;
    }
}
