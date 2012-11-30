package com.l7tech.util;

import org.jetbrains.annotations.NotNull;

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

        Pattern upgradePattern = Pattern.compile(UPGRADE_SQL_PATTERN);
        Map<String, String[]> upgradeMap = new HashMap<String, String[]>();

        for (File upgradeScript : upgradeScripts) {
            Matcher matcher = upgradePattern.matcher(upgradeScript.getName());
            if (matcher.matches()) {
                String startVersion = matcher.group(1);
                String destinationVersion = matcher.group(2);
                upgradeMap.put(startVersion, new String[]{destinationVersion, upgradeScript.getAbsolutePath()});
            }
        }
        return upgradeMap;
    }

    private static final String UPGRADE_SQL_PATTERN = "^upgrade_(.*)-(.*).sql$";
}
