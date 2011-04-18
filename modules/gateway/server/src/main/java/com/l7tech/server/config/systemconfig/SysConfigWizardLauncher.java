package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import com.l7tech.server.config.wizard.ConfigWizardConsoleStep;
import com.l7tech.server.config.wizard.ConfigWizardConsoleSummaryStep;
import com.l7tech.server.config.wizard.ConfigWizardConsoleResultsStep;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:04:51 AM
 */
public class SysConfigWizardLauncher {
    private static final String ARG_PRINT_CONFIG = "-printConfig";
    private static final String ARG_SYS_CONFIG = "-systemConfig";
    private static final String ARG_AUTH_CONFIG = "-authConfig";
    private static final String ARG_KEYBOARD_CONFIG = "-keyboardConfig";

    public static void main(String[] args) {
        try {
            launch(args);
        } catch (IllegalArgumentException iaex) {
            System.err.println(iaex.getMessage());
            usage(System.err);
            System.exit(1);
        }
    }

    private static void usage(PrintStream ps) {
        ps.println(SysConfigWizardLauncher.class.getSimpleName() + ": <arg>");
        ps.println("<arg> is one of: ");
        for (ConfigType configType : ConfigType.values()) {
                ps.println("\t" + configType.getArgName() + ": " + configType.getDescription());
        }
    }

    private static void launch(String[] args) {
        SystemConfigurationWizard sysWizard = null;

        if (args.length != 0) {
            String mode = args[0];
            if (mode.equalsIgnoreCase(ARG_PRINT_CONFIG)) {
                sysWizard = new SystemConfigurationWizard();
                sysWizard.printConfigOnly();
            } else {
                ConfigType cType = ConfigType.parseMode(mode);
                if (cType == null)
                    throw new IllegalArgumentException("Invalid configuration type " + mode);

                sysWizard = cType.buildWizard();
                sysWizard.startWizard();
            }
        } else {
            throw new IllegalArgumentException("Incorrect usage. This wizard requires a configuration type.");
        }
    }


     private static enum ConfigType {
        SystemConfig( ARG_SYS_CONFIG, "","") {
            @Override
            List<ConfigWizardConsoleStep> getSteps(SystemConfigurationWizard sysWizard) {
                List<ConfigWizardConsoleStep> stepsList = new ArrayList<ConfigWizardConsoleStep>();
//              stepsList.add(new SystemConfigWizardKeyboardStep(sysWizard));
                stepsList.add(new SystemConfigWizardNetworkingStep(sysWizard));
                stepsList.add(new SystemConfigWizardNtpStep(sysWizard));
                stepsList.add(new ConfigWizardConsoleSummaryStep(sysWizard, "System Configuration Summary"));

                ConfigWizardConsoleResultsStep resultsStep = new ConfigWizardConsoleResultsStep(sysWizard, "Networking Configuration Results");
                resultsStep.setSuccessMessage("The configuration was successfully applied." + BaseConfigurationBean.EOL + "You must restart the SSG Appliance in order for the configuration to take effect." + BaseConfigurationBean.EOL);
                stepsList.add(resultsStep);
                return stepsList;
            }
        },

        KeyboardConfig( ARG_KEYBOARD_CONFIG, "","") {
            @Override
            List<ConfigWizardConsoleStep> getSteps(SystemConfigurationWizard kbdWizard) {
               List<ConfigWizardConsoleStep> stepsList = new ArrayList<ConfigWizardConsoleStep>();
               stepsList.add(new SystemConfigWizardKeyboardStep(kbdWizard));
               stepsList.add(new ConfigWizardConsoleSummaryStep(kbdWizard, "Keyboard Configuration Summary"));

               ConfigWizardConsoleResultsStep resultsStep = new ConfigWizardConsoleResultsStep(kbdWizard, "Keyboard Configuration Results");
               resultsStep.setSuccessMessage("The configuration was successfully applied." + BaseConfigurationBean.EOL);
               stepsList.add(resultsStep);
               return stepsList;
            }
        },

        AuthenticationConfig (ARG_AUTH_CONFIG, "", "") {
            @Override
            List<ConfigWizardConsoleStep> getSteps(SystemConfigurationWizard authWizard) {
                List<ConfigWizardConsoleStep> stepsList = new ArrayList<ConfigWizardConsoleStep>();
                stepsList.add(new SystemConfigurationWizardAuthenticationStep(authWizard) );
                stepsList.add(new ConfigWizardConsoleSummaryStep(authWizard, "Authentication Configuration Summary"));

                ConfigWizardConsoleResultsStep resultsStep = new ConfigWizardConsoleResultsStep(authWizard, "Authentication Configuration Results");
                resultsStep.setSuccessMessage("The configuration was successfully applied." + BaseConfigurationBean.EOL + "You must restart the SSG Appliance in order for the configuration to take effect." + BaseConfigurationBean.EOL);
                stepsList.add(resultsStep);
                return stepsList;
            }
        };


        private static Map<String,ConfigType> possibleArgs = new HashMap<String, ConfigType>();
        static {
           for (ConfigType ct : values())
               possibleArgs.put(ct.getArgName(), ct);
        }

        abstract List<ConfigWizardConsoleStep> getSteps(SystemConfigurationWizard sysWizard);


        public SystemConfigurationWizard buildWizard(){
            SystemConfigurationWizard scw = new SystemConfigurationWizard();
            scw.setSteps(getSteps(scw));
            return scw;
        }

        private ConfigType(String argName, String usage, String description ) {
            this.argName = argName;
            this.usage = usage;
            this.description = description;
        }

        public static ConfigType parseMode(String modeArg) {
            return possibleArgs.get(modeArg);
        }

        public String getArgName() {
            return argName;
        }

        public String getUsage() {
            return usage;
        }

        public String getDescription() {
            return description;
        }

        private String argName;
        private String usage;
        private String description;
     }
}
