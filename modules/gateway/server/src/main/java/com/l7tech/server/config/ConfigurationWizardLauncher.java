package com.l7tech.server.config;

import com.l7tech.server.partition.PartitionManager;

/**
 * User: megery
 * Date: Feb 22, 2006
 * Time: 3:30:33 PM
 */
public class ConfigurationWizardLauncher {
    private static final String EOL_CHAR = System.getProperty("line.separator");
    public static final String PARTITION_UPGRADE = "-partitionMigrate";
    public static final String CONSOLE_MODE = "-console";
    public static final String EXPORT_SHARED_KEY = "-exportsharedkey";
    public static final String CHANGE_MASTER_PASSPHRASE = "-changeMasterPassphrase";

    private static final String USAGE_STATEMENT = "usage: ConfigurationWizardLauncher [options]" + EOL_CHAR +
                        "If no options are specified, console mode is assumed" + EOL_CHAR +
                        "\t-console" + EOL_CHAR +
                        "\t\trun the configuration wizard in console only mode" + EOL_CHAR +

                        "\t-partitionMigrate" + EOL_CHAR +
                        "\t\tmigrate ssg configuration in all partitions to the latest format" + EOL_CHAR +

                        "\t-exportSharedKey" + EOL_CHAR +
                        "\t\texport the cluster shared key to STDOUT" + EOL_CHAR +

                        "\t-changeMasterPassphrase" + EOL_CHAR +
                        "\t\tchange the master passphrase used to encrypt passwords" + EOL_CHAR +
                        "\t\tin the SSG configuration files" + EOL_CHAR;

    public static void main(String[] args) {
        String[] newArgs;
        String launchType;

        if (args.length > 0) {
            launchType = args[0];
            newArgs = new String[args.length -1];
            System.arraycopy(args, 1, newArgs, 0, args.length -1);
        } else {
            launchType = CONSOLE_MODE;
            newArgs = args;
        }

        if (EXPORT_SHARED_KEY.equalsIgnoreCase(launchType)) {
                SharedKeyGetter.main(newArgs);
        } else if (PARTITION_UPGRADE.equalsIgnoreCase(launchType)) {
            //if they have asked for partitionMigrate explicitly, we've give them just that.
            PartitionManager.doMigration(false);
        } else {
            //otherwise we'll migrate and then do what they ask.
            PartitionManager.doMigration(true);
            if (CHANGE_MASTER_PASSPHRASE.equalsIgnoreCase(launchType)) {
                MasterPassphraseChanger.main(newArgs);
            } else if (null == launchType || "".equals(launchType) || CONSOLE_MODE.equalsIgnoreCase(launchType)) {
                ConsoleConfigWizardLauncher.launch(newArgs);
            } else {
                System.out.println("Invalid argument: " + launchType);
                System.out.println(usage());
                System.exit(1);
            }
        }
    }

    private static String usage() {
        return USAGE_STATEMENT;
    }
}
