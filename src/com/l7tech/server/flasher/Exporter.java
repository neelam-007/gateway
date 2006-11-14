package com.l7tech.server.flasher;

import com.l7tech.server.config.PropertyHelper;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.beans.SsgDatabaseConfigBean;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;

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
        PartitionManager partitionManager = PartitionManager.getInstance();
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

        //parititons have names like "dev", "prod" etc. so we'll use that instead of integers.
        String partitionName = arguments.get(PARTITION.name);

        // check if the system has more than one partition on it
        boolean multiplePartitionSystem = partitionManager.isPartitioned();

        if (multiplePartitionSystem) {
            // option PARTITION now mandatory
            if (StringUtils.isEmpty(partitionName))
                throw new FlashUtilityLauncher.InvalidArgumentException("this system is partitioned. The \"" + PARTITION.name + "\" parameter is required");

            PartitionInformation partitionInfo = partitionManager.getPartition(partitionName);
            if (partitionInfo == null)
                throw new FlashUtilityLauncher.InvalidArgumentException("this system is partitioned but the partition \"" + partitionName + "\" is not present.");

            //todo, FRANCO: do your stuff
        } else {
            // make sure user did not ask for a partition that did not exist
            if (StringUtils.isNotEmpty(partitionName))
                throw new FlashUtilityLauncher.InvalidArgumentException("this system is not partitioned. cannot act on partition number " + partitionName);
        }

        tmpDirectory = createTmpDirectory();

        // todo Egery, i bet you already have code to get this info somewhere (especially given the partition number)
        // todo, should not hardcode these
        //EGERY: here you go

        // Read database connection settings for the partition at hand
        OSSpecificFunctions osFunctions = OSDetector.getOSSpecificFunctions(partitionName);
        Map<String, String> dbProps = PropertyHelper.getProperties(osFunctions.getDatabaseConfig(), new String[] {
            SsgDatabaseConfigBean.PROP_DB_USERNAME,
            SsgDatabaseConfigBean.PROP_DB_PASSWORD,
            SsgDatabaseConfigBean.PROP_DB_URL,
        });
        String databaseURL = dbProps.get(SsgDatabaseConfigBean.PROP_DB_URL);
        String databaseUser = dbProps.get(SsgDatabaseConfigBean.PROP_DB_USERNAME);
        String databasePasswd = dbProps.get(SsgDatabaseConfigBean.PROP_DB_PASSWORD);
        //EGERY: I don't think you need this if you already have the URL but here you go anyway
        String databaseHost = getDbHostNameFromUrl(databaseURL);

        String dbDumpTempFile = tmpDirectory + File.separator + "dbdump.sql";
        // dump the database
        DBDumpUtil.dump(databaseHost, databaseUser, databasePasswd, includeAudit, dbDumpTempFile);

        // produce template mapping if necessary
        partitionName = arguments.get(MAPPING_PATH.name);
        if (partitionName != null) {
            if (!testCanWrite(partitionName)) {
                throw new FlashUtilityLauncher.InvalidArgumentException("cannot write to the mapping template path provided: " + partitionName);
            }
            // read policy files from this dump, collect all potential mapping in order to produce mapping template file
            try {
                MappingUtil.produceTemplateMappingFileFromDatabaseConnection(databaseURL, databaseUser, databasePasswd, partitionName);
            } catch (SQLException e) {
                // should not happen
                throw new RuntimeException("problem producing template mapping file", e);
            } catch (SAXException e) {
                // should not happen
                throw new RuntimeException("problem producing template mapping file", e);
            }
        }

        // copy all config files we want into this temp directory
        // todo get path for all config files, copy these files into

        //MEGERY: this should be the base of the configuration tree for you, regardless of if this is a partition or not.
        String configPath = osFunctions.getConfigurationBase();

        // zip the temp directory into the requested image file (outputpathval)

        /* TODO, FRANCO - take a look at BaseConfigurationCommand which has a (currently protected) backupFiles method.
             We could use this logic if either:
            1) it's extracted out into a utils class
            2) you extend this class in your exporter (this is the worker class of the configurators)
        */
        // todo
    }

    private String getDbHostNameFromUrl(String databaseURL) {
        String hostname = null;
        Matcher matcher = SsgDatabaseConfigBean.dbUrlPattern.matcher(databaseURL);
        if (matcher.matches()) {
            hostname = matcher.group(1);
        }
        return hostname;
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
