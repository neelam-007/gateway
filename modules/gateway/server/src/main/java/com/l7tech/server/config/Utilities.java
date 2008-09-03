package com.l7tech.server.config;

import com.l7tech.util.FileUtils;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.text.MessageFormat;
import java.io.File;
import java.io.IOException;

/**
 * User: megery
 * Date: Dec 18, 2007
 * Time: 12:41:51 PM
 */
public class Utilities {
    private static final Logger logger = Logger.getLogger(Utilities.class.getName());

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

    public static void renameFile(File srcFile, File destFile) throws IOException {
        String backupName = destFile.getAbsoluteFile() + ".bak";

        logger.info("Renaming: " + destFile + " to: " + backupName);
        File backupFile = new File(backupName);
        if (backupFile.exists()) {
            backupFile.delete();
        }

        //copy the old file to the backup location

        FileUtils.copyFile(destFile, backupFile);
        try {
            destFile.delete();
            FileUtils.copyFile(srcFile, destFile);
            srcFile.delete();
            logger.info("Successfully updated the " + destFile.getName() + " file");
        } catch (IOException e) {
            throw new IOException("You may need to restore the " + destFile.getAbsolutePath() + " file from: " + backupFile.getAbsolutePath() + "reason: " + e.getMessage());
        }
    }

}
