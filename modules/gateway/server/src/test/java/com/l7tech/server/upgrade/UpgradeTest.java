package com.l7tech.server.upgrade;

import com.l7tech.test.BugNumber;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 */
public class UpgradeTest {

    /**
     * This test may fail due to false positives, the intent is to prevent use of comments
     * that are incompatible with the MySQL command line client. 
     */
    @Test
    @BugNumber(7853)
    public void testSQLComments() throws Exception {
        final String badComment = "---";

        for ( final File sqlFile : getSqlFiles()) {
            final BufferedReader reader = new BufferedReader( new FileReader( sqlFile ) );

            String line;
            while ( ( line = reader.readLine() ) != null ) {
                if ( line.contains( badComment ) ) {
                    fail( "Invalid SQL comment ('---') in '"+sqlFile.getAbsolutePath()+"'" );
                }
            }

            reader.close();
        }
    }

    /**
     * Tests the insert statements specify the columns that they insert into. This helps avoid problems introduced when
     * column ordering changes or new columns are inserted.
     *
     * @throws IOException
     */
    @Test
    public void testInsertsContainsColumnNames() throws IOException {
        Pattern pattern = Pattern.compile("insert\\s+into\\s+\\w+\\s+values", Pattern.CASE_INSENSITIVE);

        // exclude pre 8.0.0 upgrade files when checking insert statements. We don't want to change existing upgrade files.
        for ( final File sqlFile : getSqlFiles(pre8_0_0Files)) {
            final BufferedReader reader = new BufferedReader( new FileReader( sqlFile ) );

            String line;
            int lineNum = 1;
            while ( ( line = reader.readLine() ) != null ) {
                Matcher matcher = pattern.matcher(line);
                if ( matcher.find()) {
                    fail( "Insert into should specify the columns it needs to insert into. \nFile: " + sqlFile.getAbsolutePath() + " \nLine: " + lineNum + " \nSql: " + line);
                }
                lineNum++;
            }

            reader.close();
        }
    }

    private static Set<String> pre8_0_0Files = new HashSet<>(Arrays.asList(new String[] {
            "upgrade_2.1-3.0.sql",
            "upgrade_3.0-3.0.1.sql",
            "upgrade_3.0.1-3.0.2.sql",
            "upgrade_3.0-3.1.sql",
            "upgrade_3.1-3.2.sql",
            "upgrade_3.2-3.3.sql",
            "upgrade_3.3-3.4.sql",
            "upgrade_3.4-3.5.sql",
            "upgrade_3.5-3.6.sql",
            "upgrade_3.6.5-3.7.0.sql",
            "upgrade_3.6-3.6.5.sql",
            "upgrade_3.7.0-4.0.0.sql",
            "upgrade_4.0.0-4.2.0.sql",
            "upgrade_4.2.0-4.3.0.sql",
            "upgrade_4.3.0-4.4.0.sql",
            "upgrade_4.4.0-4.5.0.sql",
            "upgrade_4.5.0-4.6.0.sql",
            "upgrade_4.6.0-4.6.5.sql",
            "upgrade_4.6.5-5.0.0.sql",
            "upgrade_5.0.0-5.1.0.sql",
            "upgrade_5.0.1-5.1.0.sql",
            "upgrade_5.1.0-5.2.0.sql",
            "upgrade_5.2.0-5.3.0.sql",
            "upgrade_5.3.0-5.3.1.sql",
            "upgrade_5.3.1-5.4.0.sql",
            "upgrade_5.4.0-5.4.1.sql",
            "upgrade_5.4.1-6.0.0.sql",
            "upgrade_6.0.0-6.1.0.sql",
            "upgrade_6.1.0-6.1.5.sql",
            "upgrade_6.1.5-6.2.0.sql",
            "upgrade_6.2.0-7.0.0.sql",
            "upgrade_7.0.0-7.0.2_success.sql",
            "upgrade_7.0.0-7.0.2_try.sql",
            "upgrade_7.0.2-7.1.0.sql",
            "upgrade_7.1.0-7.1.1_success.sql",
            "upgrade_7.1.0-7.1.1_try.sql",
            "upgrade_7.1.0-7.1.1_success.sql"
    }));

    private static List<File> getSqlFiles(){
        return getSqlFiles(Collections.<String>emptySet());
    }

    /**
     * Returns a list of all the sql files that will need to be checked, excluding explicitly provided excludes.
     *
     * @param ignoreFiles files to not include in the returned list
     * @return the list of sql files that should be checked excluding the provided ignore files.
     */
    private static List<File> getSqlFiles(Set<String> ignoreFiles){
        FileFilter fileFilter = new IgnoringFileFilter(ignoreFiles);

        ArrayList<File> sqlFiles = new ArrayList<>();

        final File mysqlScriptDir = new File( "etc/db/mysql/" );
        assertTrue( "SQL script dir not found " + mysqlScriptDir.getAbsolutePath(), mysqlScriptDir.isDirectory() );
        sqlFiles.addAll(Arrays.asList(mysqlScriptDir.listFiles( fileFilter )));

        final File derbyScriptDir = new File( "modules/gateway/server/src/main/resources/com/l7tech/server/resources/derby/" );
        assertTrue( "SQL script dir not found " + derbyScriptDir.getAbsolutePath(), derbyScriptDir.isDirectory() );
        sqlFiles.addAll(Arrays.asList(derbyScriptDir.listFiles( fileFilter )));

        final File derbySqlFile = new File( "modules/gateway/server/src/main/resources/com/l7tech/server/resources/ssg_embedded.sql" );
        assertTrue( "SQL file not found " + derbySqlFile.getAbsolutePath(), derbySqlFile.isFile() );
        sqlFiles.add(derbySqlFile);

        return sqlFiles;
    }

    private static class IgnoringFileFilter implements FileFilter {
        private Set<String> ignoreFiles;

        public IgnoringFileFilter(@NotNull Set<String> ignoreFiles) {
            this.ignoreFiles = ignoreFiles;
        }

        @Override
        public boolean accept( final File file ) {
            return file.isFile() && file.getName().endsWith( ".sql" ) && !ignoreFiles.contains(file.getName());
        }
    }
}
