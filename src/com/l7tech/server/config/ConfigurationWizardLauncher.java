package com.l7tech.server.config;

import com.l7tech.server.config.ui.gui.ConfigurationWizard;
import com.l7tech.server.config.ui.console.*;

import java.util.List;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * User: megery
 * Date: Feb 22, 2006
 * Time: 3:30:33 PM
 */
public class ConfigurationWizardLauncher {
    private static final String GRAPHICAL_MODE = "-graphical";
    private static final String CONSOLE_MODE = "-console";

    private static final String USAGE_STATEMENT = "usage: ConfigurationWizardLauncher [-console | -graphical]\n" +
                        "If no parameter is specified, graphical mode is assumed\n" +
                        "\t-console\t\trun the configuration wizard in console only mode\n" +
                        "\t-graphical\t\trun the configuration wizard in graphical mode\n";

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
        } else if (GRAPHICAL_MODE.equalsIgnoreCase(launchType)) {
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
