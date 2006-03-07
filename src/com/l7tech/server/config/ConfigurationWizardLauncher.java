package com.l7tech.server.config;

import com.l7tech.server.config.ui.gui.ConfigurationWizard;

/**
 * User: megery
 * Date: Feb 22, 2006
 * Time: 3:30:33 PM
 */
public class ConfigurationWizardLauncher {
    private static final String GRAPHICAL_MODE = "-graphical";
    private static final String CONSOLE_MODE = "-console";

    private static final String USAGE_STATEMENT = "usage: ConfigurationWizardLauncher [-console | -graphical]\n" +
                        "If no parameter is spoecified, graphical mode is assumed\n" +
                        "\t-console\t\trun the configuration wizard in console only mode\n" +
                        "\t-graphical\t\trun the configuration wizard in graphical mode\n";

    public static void main(String[] args) {
        boolean isConsole = false;
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
            isConsole = true;
        } else if (GRAPHICAL_MODE.equalsIgnoreCase(launchType)) {
            isConsole = false;
        }
        else {
            System.out.println("invalid argument: " + launchType);
            System.out.println(usage());
            System.exit(1);
        }

        launch(newArgs, isConsole);
    }

    private static void launch(String[] newArgs, boolean isConsole) {
        if (isConsole) {
            launchWithConsole(newArgs);
        } else {
            launchWithGui(newArgs);
        }
    }

    private static void launchWithConsole(String[] args) {
        System.out.println("Starting Configuration Wizard in Console Mode");
        com.l7tech.server.config.ui.console.ConfigurationWizard.startWizard(args);
    }

    private static void launchWithGui(String[] args) {
        System.out.println("Starting Configuration Wizard in Graphical Mode");
        ConfigurationWizard.startWizard(args);
    }

    private static String usage() {
        return USAGE_STATEMENT;
    }
}
