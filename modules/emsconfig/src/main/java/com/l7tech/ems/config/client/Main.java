package com.l7tech.ems.config.client;

import com.l7tech.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.ConfigurationFactory;
import com.l7tech.config.client.ConfigurationClient;
import com.l7tech.config.client.InvalidConfigurationStateException;
import com.l7tech.util.JdkLoggerConfigurator;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Set;

/**
 * Main class for ESM configuration wizard.
 *
 * @author steve
 */
public class Main {

    //- PUBLIC

    /**
     * Arguments are:
     *
     *   0 - command (auto, wizard, edit, show), default is auto
     *   1 - type (appliance, status), default is appliance
     *
     * @param args The program arguments
     */
    public static void main( String[] args ) {
        boolean success = false;
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/ems/config/client/logging.properties", "configlogging.properties", false, true);

        String typeName = "ems";
        if ( args.length > 1 ) {
            typeName = args[1];
        }

        ConfigurationType type = getConfigurationType(configurationTypes, typeName);
        if ( type == null ) {
            System.out.println( "Unknown configuration type '"+typeName+"'." );
        } else {
            String command = ConfigurationClient.getDefaultCommand();
            if ( args.length > 0 ) {
                command = args[0];
            }

            if ( ConfigurationClient.isValidCommand(command) ) {
                ConfigurationBeanProvider provider = type.getProvider();
                if ( !provider.isValid() ) {
                    logger.warning("Cannot perform '"+command+"' at this time (configuration service not available).");
                    System.exit(3);
                }

                try {
                    OptionSet os = ConfigurationFactory.newConfiguration( Main.class, type.getResource() );
                    if ( !type.isIncludeCreateOnlyOptions() ) {
                        Set<Option> options = os.getOptions();
                        for (Iterator<Option> optionIter = options.iterator(); optionIter.hasNext(); ) {
                            Option option = optionIter.next();
                            if ( !option.isUpdatable() ) {
                                optionIter.remove();
                            }
                        }
                    }

                    ConfigurationClient client = new ConfigurationClient( provider, os, command );
                    if ( client.doInteraction() ) {
                        success = true;
                    } else {
                        System.exit(5);
                    }
                } catch ( InvalidConfigurationStateException ce ) {
                    System.out.println( ce.getMessage() );
                    logger.log(Level.WARNING, ce.getMessage());
                    System.exit(2);
                } catch ( ConfigurationException ce ) {
                    logger.log(Level.WARNING, "Error during configuration", ce );
                } catch ( IOException ioe ) {
                    logger.log(Level.WARNING, "Error during configuration", ioe );
                }
            } else {
                System.out.println( "Unknown command  '"+command+"'." );
            }
        }

        if ( !success ) {
            System.exit(1);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static ConfigurationType getConfigurationType( final ConfigurationType[] types, final String name ) {
        ConfigurationType type = null;

        if ( name != null ) {
            for ( ConfigurationType configType : types ) {
                if ( name.equalsIgnoreCase(configType.getName()) ) {
                    type = configType;
                    break;
                }
            }
        }

        return type;
    }

    private static final ConfigurationType[] configurationTypes = {
        new ConfigurationType( "ems", "configTemplates/EsmConfiguration.xml", true ){
            @Override
            public ConfigurationBeanProvider getProvider() {
                return new EsmConfigurationBeanProvider();
            }
        }
    };

    /**
     *
     */
    private static abstract class ConfigurationType {
        private final String name;
        private final String resource;
        private final boolean includeCreateOnlyOptions;

        ConfigurationType( final String name,
                           final String resource,
                           final boolean includeCreateOnlyOptions ) {
            this.name = name;
            this.resource = resource;
            this.includeCreateOnlyOptions = includeCreateOnlyOptions;
        }

        public String getName() {
            return name;
        }

        public String getResource() {
            return resource;
        }

        public boolean isIncludeCreateOnlyOptions() {
            return includeCreateOnlyOptions;
        }

        public abstract ConfigurationBeanProvider getProvider();
    }
}