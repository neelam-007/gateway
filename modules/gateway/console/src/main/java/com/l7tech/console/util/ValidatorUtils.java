package com.l7tech.console.util;

import com.l7tech.common.protocol.SecureSpanConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 11/30/11
 * Time: 9:45 AM
 */
public class ValidatorUtils {

    private static final List<Pattern> compiled;
    static {
        List<Pattern> allCompiled = new ArrayList<Pattern>();
        for (String s : SecureSpanConstants.getResolutionByIdRegexes()) {
            allCompiled.add(Pattern.compile(s));
        }
        compiled = Collections.unmodifiableList(allCompiled);
    }

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
            } else if (uriConflictsWithServiceIDResolver(path)) {
                message = "This custom resolution path conflicts with an internal resolution mechanism.";
            }
        } else {
            if (path != null && path.length() > 0 && path.startsWith(SecureSpanConstants.SSG_RESERVEDURI_PREFIX)) {
                message = "Custom resolution path cannot start with " + SecureSpanConstants.SSG_RESERVEDURI_PREFIX;
            } else if (path != null && path.length() > 0 && uriConflictsWithServiceIDResolver(path)) {
                message = "This custom resolution path conflicts with an internal resolution mechanism.";
            }
        }
        return message;
    }

    private static boolean uriConflictsWithServiceIDResolver(String newURI) {
        for (Pattern regexPattern : compiled) {
            Matcher matcher = regexPattern.matcher(newURI);
            if (matcher.find() && matcher.groupCount() == 1) {
                return true;
            }
        }
        return false;
    }

}
