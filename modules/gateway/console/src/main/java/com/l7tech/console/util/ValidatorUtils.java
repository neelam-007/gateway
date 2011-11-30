package com.l7tech.console.util;

import com.l7tech.common.protocol.SecureSpanConstants;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 11/30/11
 * Time: 9:45 AM
 */
public class ValidatorUtils {

    private static final Logger logger = Logger.getLogger(ValidatorUtils.class.getName());

    /**
     * validates url relative path against reserved path fragments such as /ssg and /service
     * @param path - relative path to be validated
     * @param soap - true if the subject is a SOAP service
     * @param internal - true if the subject is an internal service
     * @return - null if path is valid and message if path is not valid
     */
    public static String validateResolutionPath(final String path, final boolean soap, final boolean internal) {
        String message = null;
        if (!soap || internal) {
            if (path == null || path.length() <= 0 || path.equals("/")) { // non-soap service cannot have null routing uri
                String serviceType = internal ? "an internal" : "non-soap";
                message = "Cannot set empty URI on " + serviceType + " service";
            } else if (path.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                message = "Custom resolution path cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX;
            } else if (uriConflictsWithServiceOIDResolver(path)) {
                message = "This custom resolution path conflicts with an internal resolution mechanism.";
            }
        } else {
            if (path != null && path.length() > 0 && path.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                message = "Custom resolution path cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX;
            } else if (path != null && path.length() > 0 && uriConflictsWithServiceOIDResolver(path)) {
                message = "This custom resolution path conflicts with an internal resolution mechanism.";
            }
        }
        return message;
    }

    private static boolean uriConflictsWithServiceOIDResolver(String newURI) {
        java.util.List<Pattern> compiled = new ArrayList<Pattern>();

        try {
            for (String s : SecureSpanConstants.RESOLUTION_BY_OID_REGEXES) {
                compiled.add(Pattern.compile(s));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "A Regular Expression failed to compile. " +
                    "This resolver is disabled.", e);
            compiled.clear();
        }

        for (Pattern regexPattern : compiled) {
            Matcher matcher = regexPattern.matcher(newURI);
            if (matcher.find() && matcher.groupCount() == 1) {
                return true;
            }
        }
        return false;
    }

}
