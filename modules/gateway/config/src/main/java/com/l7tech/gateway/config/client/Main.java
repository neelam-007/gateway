package com.l7tech.gateway.config.client;

import com.l7tech.gateway.config.client.beans.ConfigurationBean;
import com.l7tech.gateway.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.DatabaseConfigBeanProvider;
import com.l7tech.gateway.config.client.options.Option;
import com.l7tech.gateway.config.client.options.OptionGroup;
import com.l7tech.gateway.config.client.options.OptionSet;
import com.l7tech.util.JdkLoggerConfigurator;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Console;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Main class for configuration wizard.
 *
 * //TODO [steve] complete implementation for configuration
 *
 * @author steve
 */
public class Main {

    //- PUBLIC

    public static void main( String[] args ) {
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties", false, true);

        boolean success = false;
        try {
            ConfigurationBeanProvider provider = new DatabaseConfigBeanProvider(new URL("https://127.0.0.1:8765/services/nodeManagementApi"));
            if ( !provider.isValid() ) {
                System.exit(3);
            } else if ( !provider.loadConfiguration().isEmpty() ) {
                System.exit(2);
            }

            Collection<ConfigurationBean> configBeanList = Collections.emptyList();

            Map<String,ConfigurationBean> configBeans = new TreeMap<String,ConfigurationBean>();
            JAXBContext context = JAXBContext.newInstance("com.l7tech.gateway.config.client.options");
            Unmarshaller unmarshaller = context.createUnmarshaller();
            OptionSet os = (OptionSet) unmarshaller.unmarshal( Main.class.getResourceAsStream("configTemplates/NewAppliance.xml") );

            initConfig( os, configBeans, configBeanList );

            doWizard( os, configBeans );
            //doEdit( os, configBeans );

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

            System.out.print( "Press <Enter> to continue.");
            System.console().readLine();

            System.out.println( "Please wait while the configuration is applied ...\n" +
                    "\n" +
                    "---------------------\n" +
                    "Configuration Results\n" +
                    "---------------------\n" +
                    "\n" +
                    "The configuration was successfully applied.\n" +
                    "You must restart the SSG in order for the configuration to take effect." );

            provider.storeConfiguration( configBeans.values() );
            success = true;
        } catch ( IOException ioe ) {
            logger.log(Level.WARNING, "Error during configuration", ioe );
        } catch ( ConfigurationException ce ) {
            logger.log(Level.WARNING, "Error during configuration", ce );
        } catch ( JAXBException je ) {
            logger.log(Level.WARNING, "Error during configuration", je );
        }

        if (!success) System.exit(5);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(Main.class.getName());

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

    @SuppressWarnings({"UnusedDeclaration"})
    private static void doEdit( OptionSet os,
                                Map<String,ConfigurationBean> configBeans ) throws Exception {
        Console console = System.console();
        System.out.println( os.getDescription() );
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

            String selected = console.readLine();
            if ( count == Integer.parseInt(selected) ) { edit = false; continue; }
            String selectedOption = optionMap.get(Integer.parseInt(selected));
            if ( selectedOption == null ) continue;
            
            for ( Option option : os.getOptions(selectedOption) ) {
                System.out.println();
                System.out.println( option.getDescription() );
                System.out.println();
                System.out.print( option.getPrompt() );
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
                        new String(console.readPassword()) : 
                        console.readLine();
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
                        bean.setConfigName( option.getConfigName() );
                        bean.setConfigValue( value );
                        configBeans.put( option.getId(), bean );
                    } else {
                        System.out.print("Invalid value, please try again: ");
                    } 
                }
            }  
        }             
    }    
    
    private static void doWizard( OptionSet os, 
                                  Map<String,ConfigurationBean> configBeans ) {
        Console console = System.console();
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
                System.out.println( option.getPrompt() );
            }
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
                    new String(console.readPassword()) : 
                    console.readLine();
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
                    bean.setConfigName( option.getConfigName() );
                    bean.setConfigValue( value );
                    configBeans.put( option.getId(), bean );
                } else {
                    System.out.print("Invalid value, please try again: ");
                } 
            }
        }        
    }
    
    private static void initConfig(OptionSet os, 
                                   Map<String,ConfigurationBean> configBeans,
                                   Collection<ConfigurationBean> configBeanList ) {
        if ( configBeanList != null ) {
            for ( Option option : os.getOptions() ) {
                for ( ConfigurationBean configBean : configBeanList ) {
                    if ( option.getConfigName().equals(configBean.getConfigName()) ) {
                        configBean.setId( option.getId() );
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
