package com.l7tech.gateway.config.client;

import com.l7tech.gateway.config.client.beans.ConfigurationBean;
import com.l7tech.gateway.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.DatabaseConfigBeanProvider;
import com.l7tech.gateway.config.client.beans.StateConfigurationBeanProvider;
import com.l7tech.gateway.config.client.options.Option;
import com.l7tech.gateway.config.client.options.OptionGroup;
import com.l7tech.gateway.config.client.options.OptionSet;
import com.l7tech.util.JdkLoggerConfigurator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Console;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;

/**
 * Main class for configuration wizard.
 *
 * //TODO [steve] complete implementation for configuration (navigation "<" / "quit", input validation)
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
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);

        boolean success = false;
        try {
            String typeName = "appliance";
            if ( args.length > 1 ) {
                typeName = args[1];
            }
            ConfigurationType type = getConfigurationType(configurationTypes, typeName);
            if ( type == null ) {
                System.out.println( "Unknown configuration type '"+typeName+"'." );
            } else {
                String command = COMMAND_AUTO;
                if ( args.length > 0 ) {
                    command = args[0];
                }

                if ( COMMANDS.contains(command) ) {
                    ConfigurationBeanProvider provider = type.getProvider();
                    if ( !provider.isValid() ) {
                        logger.warning("Cannot perform '"+command+"' at this time (configuration service not available).");
                        System.exit(3);
                    }

                    Collection<ConfigurationBean> configBeanList = provider.loadConfiguration();
                    Map<String,ConfigurationBean> configBeans = new TreeMap<String,ConfigurationBean>();
                    JAXBContext context = JAXBContext.newInstance("com.l7tech.gateway.config.client.options");
                    Unmarshaller unmarshaller = context.createUnmarshaller();
                    OptionSet os = (OptionSet) unmarshaller.unmarshal( Main.class.getResourceAsStream( type.getResource() ) );

                    initConfig( os, configBeans, configBeanList );

                    boolean store = false;
                    if ( COMMAND_AUTO.equals(command) ) {
                        if ( configBeanList.isEmpty() ) {
                            store = doWizard( os, configBeans );
                        } else {
                            store = doEdit( os, configBeans );
                        }
                    } else if ( COMMAND_WIZARD.equals(command) ) {
                        if ( configBeanList.isEmpty() ) {
                            store = doWizard( os, configBeans );
                        } else {
                            logger.info("Already configured, cannot use wizard.");
                            System.exit(2);
                        }
                    } else if ( COMMAND_EDIT.equals(command) ) {
                        if ( !configBeanList.isEmpty() ) {
                            store = doEdit( os, configBeans );
                        } else {
                            logger.info("Not yet configured, use wizard.");
                            System.exit(2);
                        }
                    } else if ( COMMAND_SHOW.equals(command) ) {
                        if ( !configBeanList.isEmpty() ) {
                            doShow( os, configBeans );
                        } else {
                            logger.info("Not yet configured, use wizard.");
                            System.exit(2);
                        }
                    }

                    if ( store ) {
                        provider.storeConfiguration( configBeans.values() );
                    }

                    success = true;
                } else {
                    System.out.println( "Unknown command  '"+command+"'." );
                }
            }
        } catch ( IOException io ) {
            logger.log(Level.WARNING, "Error during configuration", io );
        } catch ( ParseException pe ) {
            logger.log(Level.WARNING, "Error during configuration", pe );
        } catch ( ConfigurationException ce ) {
            logger.log(Level.WARNING, "Error during configuration", ce );
        } catch ( JAXBException je ) {
            logger.log(Level.WARNING, "Error during configuration", je );
        }

        if (!success) System.exit(5);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static final String pcUrl = "https://127.0.0.1:8765/services/nodeManagementApi";

    private static final String COMMAND_AUTO = "auto"; // wizard if not already configured, else edit
    private static final String COMMAND_WIZARD = "wizard";
    private static final String COMMAND_EDIT = "edit";
    private static final String COMMAND_SHOW = "show";
    private static final Collection COMMANDS = Collections.unmodifiableCollection(Arrays.asList( COMMAND_AUTO, COMMAND_WIZARD, COMMAND_EDIT, COMMAND_SHOW ));

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

    private static String getConfigurationSummary( OptionSet os,
                                                   Map<String,ConfigurationBean> configBeans) {
        StringBuilder summary = new StringBuilder();

        String group = null;
        for ( Option option : os.getOptions() ) {
            if ( option.getType().isHidden() ) {
                continue;
            }

            if ( option.getGroup() != null && !option.getGroup().equals(group) ) {
                group = option.getGroup();
                OptionGroup optionGroup = getOptionGroup( os, option.getGroup() );
                if ( optionGroup != null && optionGroup.getDescription() != null ) {
                    summary.append( "\n" );
                    summary.append( "  " );
                    summary.append( optionGroup.getDescription() );
                    summary.append( "\n" );
                }
            }

            summary.append( "    " );
            summary.append( option.getName() );
            summary.append( " = " );
            String currentValue = getCurrentValue( option, configBeans );
            if ( currentValue != null ) {
                summary.append( currentValue );
            }
            summary.append( "\n" );
        }

        return summary.toString();
    }

    private static String fallbackReadLine( final Console console, final BufferedReader reader ) throws IOException {
        String line;

        if ( console != null ) {
            line = console.readLine();
        } else {
            line = reader.readLine();
        }

        return line;
    }

    private static String fallbackReadPassword( final Console console, final BufferedReader reader ) throws IOException {
        String line;

        if ( console != null ) {
            line = new String(console.readPassword());
        } else {
            line = reader.readLine();
        }

        return line;
    }

    private static void doShow( OptionSet os,
                                Map<String,ConfigurationBean> configBeans ) throws IOException {
        Console console = System.console();
        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );

        if ( os.getDescription() != null ) {
            System.out.println( os.getDescription() );
        }
        System.out.println();

        String summary =
            "Configuration:";

        System.out.println( summary );

        System.out.println( getConfigurationSummary(os, configBeans) );

        System.out.print( "Press [Enter] to continue.");
        fallbackReadLine( console, reader );
    }

    private static boolean doEdit( OptionSet os,
                                   Map<String,ConfigurationBean> configBeans ) throws IOException, ParseException {
        Console console = System.console();
        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );

        if ( os.getPrompt() != null ) {
            System.out.println( os.getPrompt() );
        }
        System.out.println();
        
        boolean edit = true;
        while( edit ) {
            int count = 1;
            Map<Integer,String> optionMap = new HashMap<Integer,String>();
            System.out.println( "Select option to configure:" );
            for ( Option option : os.getOptions() ) {
                optionMap.put(count, option.getId());
                System.out.print( count++ );
                System.out.print( ") " );                
                System.out.println( option.getName() );
            }
            System.out.print( count );
            System.out.print( ") Done\n\nEnter option: " );

            String selected = fallbackReadLine( console, reader );
            if ( count == parseOption(selected, count) ) { edit = false; continue; }
            String selectedOption = optionMap.get(parseOption(selected, count));
            if ( selectedOption == null ) continue;
            
            for ( Option option : os.getOptions(selectedOption) ) {
                System.out.println();
                if ( option.getPrompt() != null ) {
                    System.out.println();
                    System.out.println( option.getPrompt() );
                }
                System.out.println();
                System.out.print( option.getName() );
                String currentValue = getCurrentValue( option, configBeans );
                if ( currentValue != null ) {
                    System.out.print( " [" );
                    System.out.print( currentValue );
                    System.out.print( "]: " );
                } else {
                    System.out.print( ": " );
                }

                String regex = option.getRegex();
                if ( regex == null ) {
                    regex = option.getType().getDefaultRegex();
                }
                Pattern pattern = Pattern.compile(regex);

                boolean read = false;
                while ( !read ) {
                    String value = option.getType().isHidden() ? 
                        fallbackReadPassword( console, reader ) :
                        fallbackReadLine( console, reader );
                    if ( value.trim().length() == 0 ) {
                        if ( currentValue != null ) {
                            value = currentValue;
                        }
                    }

                    Matcher matcher = pattern.matcher(value);
                    if ( matcher.matches() ) {
                        read = true;
                        ConfigurationBean bean = new ConfigurationBean();
                        bean.setId( option.getId() );
                        bean.setFormatter( option.getType().getFormat() );
                        bean.setConfigName( option.getConfigName() );
                        bean.processConfigValueInput( value );
                        configBeans.put( option.getId(), bean );
                    } else {
                        System.out.print("Invalid value, please try again: ");
                    }
                }
            }  
        }

        String summary =
            "---------------------\n" +
            "Configuration Summary\n" +
            "---------------------\n" +
            "At any time type \"quit\" to quit\n" +
            "Press \"<\" to go to the previous step\n" +
            "\n" +
            "The following configuration will be applied:";

        System.out.println( summary );

        System.out.println( getConfigurationSummary(os, configBeans) );
//        *Database Configuration - Configures the database properties for an SSG
//            Setup connection to a database:
//                HOSTNAME = gateway.l7tech.com
//                USERNAME = gateway
//                DATABASE = ssg

        System.out.print( "Press [Enter] to continue.");
        fallbackReadLine( console, reader );

        System.out.println( "Please wait while the configuration is applied ...\n" +
                "\n" +
                "---------------------\n" +
                "Configuration Results\n" +
                "---------------------\n" +
                "\n" +
                "The configuration was successfully applied.\n" +
                "You must restart the SSG in order for the configuration to take effect." );

        return true;
    }    
    
    private static boolean doWizard( OptionSet os,
                                  Map<String,ConfigurationBean> configBeans ) throws IOException, ParseException {
        Console console = System.console();
        BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );

        if ( os.getPrompt() != null ) {
            System.out.println( os.getPrompt() );
        }
        System.out.println();
        String group = null;
        for ( Option option : os.getOptions() ) {
            if ( option.getGroup() != null && !option.getGroup().equals(group) ) {
                group = option.getGroup();
                OptionGroup optionGroup = getOptionGroup( os, option.getGroup() );
                if ( optionGroup != null && optionGroup.getPrompt() != null ) {
                    System.out.println( optionGroup.getPrompt() );
                }
            }

            if ( option.getPrompt() != null ) {
                System.out.println();
                System.out.println( option.getPrompt() );
            }
            System.out.println();
            System.out.print( option.getName() );
            String currentValue = getCurrentValue( option, configBeans );
            if ( currentValue != null ) {
                System.out.print( " [" );
                System.out.print( currentValue );
                System.out.print( "]: " );
            } else {
                System.out.print( ": " );
            }
            
            String regex = option.getRegex();
            if ( regex == null ) {
                regex = option.getType().getDefaultRegex();
            }
            Pattern pattern = Pattern.compile(regex);
            
            boolean read = false;
            while ( !read ) {
                String value = option.getType().isHidden() ? 
                    fallbackReadPassword( console, reader ) :
                    fallbackReadLine( console, reader );
                if ( value.trim().length() == 0 ) {
                    if ( currentValue != null ) {
                        value = currentValue;
                    }
                }
                
                Matcher matcher = pattern.matcher(value);
                if ( matcher.matches() ) {
                    read = true;
                    ConfigurationBean bean = new ConfigurationBean();
                    bean.setId( option.getId() );
                    bean.setFormatter( option.getType().getFormat() );
                    bean.setConfigName( option.getConfigName() );
                    bean.processConfigValueInput( value );
                    configBeans.put( option.getId(), bean );
                } else {
                    System.out.print("Invalid value, please try again: ");
                } 
            }
        }

        String summary =
                "---------------------\n" +
                "Configuration Summary\n" +
                "---------------------\n" +
                "At any time type \"quit\" to quit\n" +
                "Press \"<\" to go to the previous step\n" +
                "\n" +
                "The following configuration will be applied:";

        System.out.println( summary );

        System.out.println( getConfigurationSummary(os, configBeans) );
//        *Database Configuration - Configures the database properties for an SSG
//            Setup connection to a database:
//                HOSTNAME = gateway.l7tech.com
//                USERNAME = gateway
//                DATABASE = ssg

        System.out.print( "Press [Enter] to continue.");
        fallbackReadLine( console, reader );

        System.out.println( "Please wait while the configuration is applied ...\n" +
                "\n" +
                "---------------------\n" +
                "Configuration Results\n" +
                "---------------------\n" +
                "\n" +
                "The configuration was successfully applied.\n" +
                "You must restart the SSG in order for the configuration to take effect." );

        return true;
    }

    private static int parseOption( final String selected, final int count ) {
        int option = count;

        try {
            option = Integer.parseInt(selected);
        } catch( NumberFormatException nfe ) {
            // ok
        }

        return option;
    }

    private static void initConfig(OptionSet os, 
                                   Map<String,ConfigurationBean> configBeans,
                                   Collection<ConfigurationBean> configBeanList ) {
        if ( configBeanList != null ) {
            for ( Option option : os.getOptions() ) {
                for ( ConfigurationBean configBean : configBeanList ) {
                    if ( option.getConfigName().equals(configBean.getConfigName()) ) {
                        configBean.setId( option.getId() );
                        configBean.setFormatter( option.getType().getFormat() );
                        configBeans.put( option.getId(), configBean);
                        break;
                    }
                }
            }
        }
    }
    
    private static OptionGroup getOptionGroup(OptionSet os, String group) {
        OptionGroup optionGroup = null;

        for ( OptionGroup oGroup : os.getOptionGroups() ) {
            if ( group.equals(oGroup.getId()) ) {
                optionGroup = oGroup;
                break;
            }
        }

        return optionGroup;
    }

    private static String getCurrentValue( Option option, Map<String,ConfigurationBean> configBeans ) {
        String value = null;
        
        ConfigurationBean configBean = configBeans.get( option.getId() );
        if ( configBean != null ) {
            value = configBean.getConfigValue().toString();
        }
        
        if ( value == null ) {
            value = option.getConfigValue();
        }
        
        return value;
    }
}
