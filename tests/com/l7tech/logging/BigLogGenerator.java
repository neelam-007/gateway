package com.l7tech.logging;

import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 * Generates a sql file that inserts 2M records in the log table.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Apr 23, 2004<br/>
 * $Id$<br/>
 */
public class BigLogGenerator {
    public static void main(String[] args) throws Exception {
        String outputFileName = "/home/flascell/logrecords.sql";
        OutputStream output = new FileOutputStream(outputFileName);
        output.write("DELETE from ssg_logs;\n".getBytes());
        for (long i = 0; i < 2000000; i++) {
            long recordid = i+1;
            String insertStatement = "INSERT INTO ssg_logs VALUES(" + recordid + "," +
                    "\'00:30:1B:14:12:05\', " +
                    "\'blah fake log record" + recordid + "\', " +
                    "\'FINE\', " +
                    "\'com.l7tech.server.log\', " +
                    System.currentTimeMillis() + ", " +
                    "\'classblah\', " +
                    "\'methodblah\', " +
                    "\'\');\n";
            output.write(insertStatement.getBytes());
        }
        output.flush();
        output.close();
    }
}
