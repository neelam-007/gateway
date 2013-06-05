package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for upgrading ssg schemas. Used by mysql and embedded schemas.
 */
public final class DbUpgradeUtil {
    private static final Logger logger = Logger.getLogger(DbUpgradeUtil.class.getName());

    /**
     * Retrieves the ssg version from a database connection.
     *
     * @param connection the database connection.
     * @return the database ssg version or null if unable to retrieve it.
     */
    public static String checkVersionFromDatabaseVersion(@NotNull final Connection connection) {
        Statement stmt = null;
        ResultSet rs = null;
        String version = null;
        try {
            stmt = connection.createStatement();

            rs = stmt.executeQuery("select current_version from ssg_version");
            while (rs.next()) {
                version = rs.getString("current_version");
            }
        } catch (SQLException e) {
            logger.warning("Error while checking the version of the ssg in the database: " + ExceptionUtils.getMessage(e));
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(stmt);
        }
        return version;
    }

    /**
     * Retrieves SQL statements from a file.
     *
     * @param sqlScriptFilename the file which contains SQL statements.
     * @return an array of SQL statements.
     * @throws IOException if unable to read the given file.
     */
    public static String[] getStatementsFromFile(@NotNull final String sqlScriptFilename) throws IOException {
        String[] statements;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(sqlScriptFilename));
            statements = SqlUtils.getStatementsFromReader(reader);
        } finally {
            ResourceUtils.closeQuietly(reader);
        }

        return statements;
    }

    /**
     * Creates an upgrade map from a directory which contains upgrade scripts.
     * <p/>
     * Each script must follow the naming convention: upgrade_x-y.sql
     * <p/>
     * Example: upgrade_7.0.0-7.1.0.sql
     *
     * @param parentDir the directory which contains upgrade scripts.
     * @return an upgrade map where key = starting version and value = [destination version][upgrade file path].
     *         Example: key = 7.0.0 value = [7.1.0][upgrade_7.0.0-7.1.0.sql]
     */
    public static Map<String, String[]> buildUpgradeMap(@NotNull File parentDir) {
        File[] upgradeScripts = parentDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.toUpperCase().startsWith("UPGRADE") &&
                        s.toUpperCase().endsWith("SQL");
            }
        });

        Map<String, String[]> upgradeMap = new HashMap<String, String[]>();

        for (File upgradeScript : upgradeScripts) {
            final Triple<String, String, String> upgradeInfo = isUpgradeScript(upgradeScript.getName());
            if (upgradeInfo != null) {
                upgradeMap.put(upgradeInfo.left, new String[]{upgradeInfo.middle, upgradeScript.getAbsolutePath(),upgradeInfo.right});
            }
        }
        return upgradeMap;
    }

    /**
     * Determines if the given file name is valid for an upgrade script.
     *
     * Valid upgrade script names must follow a convention upgrade_x-y.sql or upgrade_x-y_mayFail.sql or
     *          upgrade_x-y_checkSuccess.sql where x = the start version and y = the destination version.
     *
     * @param fileName the file name of the potential upgrade script.
     * @return a Triple where left = start version
     *                        middle = destination version or null if the file name is not valid for an upgrade script
     *                        right = "mayFail" if the sql file is tagged as a mayFail/checkSuccess
     */
    @Nullable
    public static Triple<String, String, String> isUpgradeScript(@NotNull final String fileName) {
        Triple<String, String, String> upgradeInfo = null;

        final Pattern optionPattern = Pattern.compile(UPGRADE_SQL_PATTERN_OPTION);
        final Matcher optionMatcher = optionPattern.matcher(fileName);
        if (optionMatcher.matches()) {
            String startVersion = optionMatcher.group(1);
            String destinationVersion = optionMatcher.group(2);
            upgradeInfo = new Triple<String, String, String>(startVersion, destinationVersion, UPGRADE_TRY_SUFFIX);
        }
        else {
            final Pattern pattern = Pattern.compile(UPGRADE_SQL_PATTERN);
            final Matcher matcher = pattern.matcher(fileName);
            if (matcher.matches()) {
                String startVersion = matcher.group(1);
                String destinationVersion = matcher.group(2);
                if(destinationVersion.contains("_")){
                    return null;
                }
                upgradeInfo = new Triple<String, String, String>(startVersion, destinationVersion,null);
            }
        }
        return upgradeInfo;
    }

    public static final String UPGRADE_TRY_SUFFIX = "try";
    public static final String UPGRADE_SUCCESS_SUFFIX = "success";
    private static final String UPGRADE_SQL_PATTERN = "^upgrade_(.*)-(.*).sql$";
    private static final String UPGRADE_SQL_PATTERN_OPTION = "^upgrade_(.*)-(.*)_("+ UPGRADE_TRY_SUFFIX +"|"+ UPGRADE_SUCCESS_SUFFIX +").sql$";
}
