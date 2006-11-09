package com.l7tech.server.flasher;

import java.util.Map;

/**
 * The utility that imports an SSG image
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class Importer {
    // importer options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image", "location of image file to import");
    public static final CommandLineOption MODE = new CommandLineOption("-mode", "[clone | stage]");
    public static final CommandLineOption PARTITION = new CommandLineOption("-p", "partition index to import to");
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-mapping", "location of the staging mapping file");
    public static final CommandLineOption DB_HOST_NAME = new CommandLineOption("-dbh", "database host name");
    public static final CommandLineOption DB_NAME = new CommandLineOption("-db", "database name");
    public static final CommandLineOption DB_PASSWD = new CommandLineOption("-dbp", "database root password");
    public static final CommandLineOption DB_USER = new CommandLineOption("-dbu", "database root username");

    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, MODE, PARTITION, MAPPING_PATH, DB_HOST_NAME, DB_NAME, DB_PASSWD, DB_USER};

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException {
        // todo
    }

}
