package com.l7tech.server.config;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.text.MessageFormat;

/**
 * User: megery
 * Date: Dec 18, 2007
 * Time: 12:41:51 PM
 */
public class Utilities {
    public static final String EOL_CHAR = System.getProperty("line.separator");
    
    public static String getFormattedMac(String mac) {
        String formattedMac = mac;
        //format the mac with colons
        Pattern macPattern = Pattern.compile("(\\w\\w)(\\w\\w)(\\w\\w)(\\w\\w)(\\w\\w)(\\w\\w)");
        Matcher macMatcher = macPattern.matcher(mac);

        if (macMatcher.matches()) {
            formattedMac = MessageFormat.format("{0}:{1}:{2}:{3}:{4}:{5}",
                macMatcher.group(1),
                macMatcher.group(2),
                macMatcher.group(3),
                macMatcher.group(4),
                macMatcher.group(5),
                macMatcher.group(6));
        }
        return formattedMac;
    }
}
