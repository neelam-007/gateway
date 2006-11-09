package com.l7tech.server.flasher;

import java.util.Map;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;

/**
 * The utility that exports an SSG image file.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class Exporter {
    // exporter options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image", "location of image file to export");
    public static final CommandLineOption AUDIT = new CommandLineOption("-ia", "[yes | no] whether or not to include audit tables in resulting image");
    public static final CommandLineOption PARTITION = new CommandLineOption("-p", "partition index to import to");
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-it", "path of the output mapping template");
    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, AUDIT, PARTITION, MAPPING_PATH};

    // do the import
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        // check that we can write output at located asked for
        String outputpathval = arguments.get(IMAGE_PATH.name);
        if (outputpathval == null) {
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i dont know where to output the image to.");
        }
        if (!testCanWrite(outputpathval)) {
            throw new FlashUtilityLauncher.InvalidArgumentException("cannot write to " + outputpathval);
        }

        // check whether or not we are expected to include audit in export
        String auditval = arguments.get(AUDIT.name);
        if (auditval != null && auditval.toLowerCase().equals("yes")) {
            includeAudit = true;
        }

        int desiredPartition = 0;
        String tmp = arguments.get(PARTITION.name);
        if (tmp != null) {
            try {
                desiredPartition = Integer.parseInt(tmp);
            } catch (NumberFormatException e) {
                throw new FlashUtilityLauncher.InvalidArgumentException("the value " + tmp + " for option " + PARTITION.name + " is invalid");
            }
        }
        boolean multiplePartitionSystem = false;
        // check if the system has more than one partition on it
        // todo, Egery how do i look for this?


        if (multiplePartitionSystem) {
            // option PARTITION now mandatory
            // todo, Egery, how do i check that the partition desiredPartition exists on the target system
        } else if (desiredPartition > 0) {
            // make sure user did not ask for a partition that did not exist
            throw new FlashUtilityLauncher.InvalidArgumentException("this system is not partitioned. cannot act on partition number " + desiredPartition);
        }

        tmpDirectory = createTmpDirectory();

        // Read database connection settings for the partition at hand
        // todo Egery, i bet you already have code to get this info somewhere (especially given the partition number)
        String databaseHost = "todo";
        String databaseUser = "todo";
        String databasePasswd = "todo";
        String dbDumpTempFile = tmpDirectory + File.separator + "dbdump.sql";
        // dump the database
        DBDumpUtil.dump(databaseHost, databaseUser, databasePasswd, includeAudit, dbDumpTempFile);

        // produce template mapping if necessary
        tmp = arguments.get(MAPPING_PATH);
        if (tmp != null) {
            if (!testCanWrite(tmp)) {
                throw new FlashUtilityLauncher.InvalidArgumentException("cannot write to the mapping template path provided: " + tmp);
            }
            // read policy files from this dump, collect all potential mapping in order to produce mapping template file
            MappingUtil.produceTemplateMappingFileFromDump(dbDumpTempFile, tmp);
        }

        // copy all config files we want into this temp directory
        // todo get path for all config files, copy these files into

        // zip the temp directory into the requested image file (outputpathval)
        // todo
    }

    private boolean testCanWrite(String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.close();
        } catch (FileNotFoundException e) {
            System.err.println("cannot write to " + path + ". " + e.getMessage());
            return false;
        } catch (IOException e) {
            System.err.println("cannot write to " + path + ". " + e.getMessage());
            return false;
        }
        (new File(path)).delete();
        return true;
    }

    private String createTmpDirectory() throws IOException {
        File tmp = File.createTempFile("ssgflash", "tmp");
        tmp.delete();
        tmp.mkdir();
        return tmp.getPath();
    }

    private boolean includeAudit = false;
    private String tmpDirectory;
}
