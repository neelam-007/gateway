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
    public static final String GRAPHICAL_MODE = "-graphical";
    public static final String CONSOLE_MODE = "-console";
    public static final String EXPORT_SHARED_KEY = "-exportsharedkey";

    private static final String USAGE_STATEMENT = "usage: ConfigurationWizardLauncher [-console | -graphical]" + EOL_CHAR +
                        "If no parameter is specified, graphical mode is assumed" + EOL_CHAR +
                        "\t-console\t\trun the configuration wizard in console only mode" + EOL_CHAR +
                        "\t-graphical\t\trun the configuration wizard in graphical mode" + EOL_CHAR;

    public static void main(String[] args) {
        String[] newArgs;
        String launchType;

        if (args.length > 0) {
            launchType = args[0];
            newArgs = new String[args.length -1];
            System.arraycopy(args, 1, newArgs, 0, args.length -1);
        } else {
            launchType = GRAPHICAL_MODE;
            newArgs = args;
        }

        if (CONSOLE_MODE.equalsIgnoreCase(launchType)) {
            ConsoleConfigWizardLauncher.launch(newArgs);
        } else if (PARTITION_UPGRADE.equalsIgnoreCase(launchType)) {
            PartitionManager.doMigration();
        } else if (EXPORT_SHARED_KEY.equalsIgnoreCase(launchType)) {
            SharedKeyGetter.main(newArgs);
        } else if (null == launchType || "".equals(launchType) || GRAPHICAL_MODE.equalsIgnoreCase(launchType)) {
            GuiConfigWizardLauncher.launch(newArgs);
        }
        else {
            System.out.println("invalid argument: " + launchType);
            System.out.println(usage());
            System.exit(1);
        }
    }

    private static String usage() {
        return USAGE_STATEMENT;
    }
}
