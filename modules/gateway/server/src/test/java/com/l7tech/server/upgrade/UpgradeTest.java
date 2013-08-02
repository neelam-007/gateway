package com.l7tech.server.upgrade;

import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;

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
        final File scriptDir = new File( "etc/db/mysql/" );
        assertTrue( "SQL script dir not found " + scriptDir.getAbsolutePath(), scriptDir.isDirectory() );

        for ( final File sqlFile : scriptDir.listFiles( new FileFilter(){
            @Override
            public boolean accept( final File file ) {
                return file.isFile() && file.getName().endsWith( ".sql" );
            }
        } )) {
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
}
