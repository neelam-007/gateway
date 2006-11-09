package com.l7tech.server.flasher;

/**
 * Methods for dumping
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class DBDumpUtil {

    /**
     * outputs a database dump
     * @param databaseHost database host
     * @param databaseUser database user
     * @param databasePasswd database password
     * @param includeAudit whether or not audit tables should be included
     * @param outputPath the path where the dump should go to
     */
    public static void dump(String databaseHost, String databaseUser, String databasePasswd, boolean includeAudit, String outputPath) {
        // todo, find the mysql client on this system (remember this could be running on windows) and output the dump
        System.out.println("todo dumping at " + outputPath);
    }
}
