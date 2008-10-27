package com.l7tech.gateway.config.client;

import com.l7tech.gateway.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.DatabaseConfigBeanProvider;
import com.l7tech.gateway.config.client.beans.StateConfigurationBeanProvider;
import com.l7tech.gateway.config.client.options.OptionSet;
import com.l7tech.util.JdkLoggerConfigurator;

import java.io.IOException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class for configuration wizard.
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
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);
        
        String typeName = "appliance";
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
                    JAXBContext context = JAXBContext.newInstance("com.l7tech.gateway.config.client.options");
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    OptionSet os = (OptionSet) unmarshaller.unmarshal( Main.class.getResourceAsStream( type.getResource() ) );

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
                } catch ( JAXBException je ) {
                    logger.log(Level.WARNING, "Error during configuration", je );
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

    private static final String pcUrl = "https://127.0.0.1:8765/services/nodeManagementApi";
    
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
        new ConfigurationType( "appliance", "configTemplates/NewAppliance.xml" ){
            public ConfigurationBeanProvider getProvider() {
                return new DatabaseConfigBeanProvider(pcUrl);
            }
        },
        new ConfigurationType( "status", "configTemplates/NodeStatus.xml" ){
            public ConfigurationBeanProvider getProvider() {
                return new StateConfigurationBeanProvider(pcUrl);
            }
        }
    };

    /**
     * 
     */
    private static abstract class ConfigurationType {
        private final String name;
        private final String resource;

        ConfigurationType( final String name,
                           final String resource) {
            this.name = name;
            this.resource = resource;
        }

        public String getName() {
            return name;
        }

        public String getResource() {
            return resource;
        }

        public abstract ConfigurationBeanProvider getProvider();
    }      
}
