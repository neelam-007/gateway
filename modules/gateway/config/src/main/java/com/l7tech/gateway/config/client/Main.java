package com.l7tech.gateway.config.client;

import com.l7tech.util.InetAddressUtil;
import com.l7tech.config.client.beans.ConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.NodeConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.StateConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.NodeManagementApiFactory;
import com.l7tech.gateway.config.client.beans.StatusCodeSource;
import com.l7tech.gateway.config.client.beans.NodeDeleteConfigurationBeanProvider;
import com.l7tech.gateway.config.client.beans.SoftwareNodeConfigurationBeanProvider;
import com.l7tech.config.client.options.OptionSet;
import com.l7tech.config.client.options.Option;
import com.l7tech.config.client.ConfigurationException;
import com.l7tech.config.client.ConfigurationFactory;
import com.l7tech.config.client.ConfigurationClient;
import com.l7tech.config.client.InvalidConfigurationStateException;
import com.l7tech.util.JceUtil;
import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.objectmodel.FindException;

import java.io.Console;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.security.GeneralSecurityException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Iterator;
import java.util.Set;
import java.util.Collection;

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
     *   0 - command (auto, wizard, edit, show, headless), default is auto
     *   1 - type (appliance, status), default is appliance
     *
     * @param args The program arguments
     */
    public static void main( String[] args ) {
        boolean success = false;
        int successCode = 0;
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        JdkLoggerConfigurator.configure("com.l7tech.logging", "com/l7tech/gateway/config/client/logging.properties", "configlogging.properties");

        if ( !ConfigUtils.isStrongCryptoEnabledInJvm() ) {
            String message = "The Java virtual machine does not have strong cryptography enabled.  The unlimited strength jurisdiction JCE policy files may need to be installed.";
            logger.warning( message );
            System.out.println( message );
            System.exit( 1 );
        }

        String typeName = "appliance";
        if ( args.length > 1 ) {
            typeName = args[1];
        }

        if ( args.length > 0 && args[0].equals("-lifecycle") ) {
            if ( args.length > 1 ) {
                int resultCode = doGatewayControl( args[1] );
                System.exit(resultCode);
            }
        } else {
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

                        clearConsole();
                        ConfigurationClient client = new ConfigurationClient( provider, os, command );
                        if ( client.doInteraction() ) {
                            success = true;
                            if ( provider instanceof StatusCodeSource) {
                                successCode = ((StatusCodeSource)provider).getStatusCode();
                            }
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
            } else {
                System.exit(successCode);
            }
        }
    }
    
    //- PRIVATE

    private static final Logger logger = Logger.getLogger(Main.class.getName());

    private static final String pcUrl = "https://" + InetAddressUtil.getLocalHostUrlAddress() + ":8765/services/nodeManagementApi";
    private static final String PROP_NODE_PROPS = "com.l7tech.server.config.nodePropsPath";
    private static final String DEFAULT_NODE_PROPS = "../node/default/etc/conf/node.properties";

    private static int doGatewayControl( String command ) {
        NodeManagementApiFactory nodeManagementApiFactory = new NodeManagementApiFactory( pcUrl );

        try {
            if ( command.equals("start") ) {
                try {
                    nodeManagementApiFactory.getManagementService().startNode("default");
                    System.out.println("Start requested.");
                } catch (NodeManagementApi.StartupException se) {
                    logger.log( Level.WARNING, "Error during node start '"+ExceptionUtils.getMessage(se)+"'.", ExceptionUtils.getDebugException(se) );
                    System.out.println("Node start failed due to '"+ExceptionUtils.getMessage(se)+"'.");
                    return 75;
                }
                return 0;
            } else if ( command.equals("stop") ) {
                nodeManagementApiFactory.getManagementService().stopNode("default", 20000);
                System.out.println("Stop requested.");
                return 0;
            } else if ( command.equals("ensureStopped") ) {
                final NodeStateType state = getNodeState(nodeManagementApiFactory);
                if (state == null || state == NodeStateType.STOPPED)
                    return 0;
                return stopNodeAndWait(nodeManagementApiFactory, false) ? 0 : 77;
            } else if ( command.equals("restart") ) {
                return stopNodeAndWait(nodeManagementApiFactory, true) ? 0 : 77;
            } else {
                System.out.println("Unrecognized lifecycle command.");
                return 80;
            }
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Exception during node control.", e );
            return 79;
        }
    }

    private static NodeStateType getNodeState(NodeManagementApiFactory nodeManagementApiFactory) throws FindException {
        NodeManagementApi nma = nodeManagementApiFactory.getManagementService();
        Collection<NodeManagementApi.NodeHeader> config = nma.listNodes();
        if (config != null) for (NodeManagementApi.NodeHeader nodeHeader : config) {
            if ( "default".equals( nodeHeader.getName() ) )
                return nodeHeader.getState();
        }
        return null;
    }

    private static boolean stopNodeAndWait(NodeManagementApiFactory nodeManagementApiFactory, boolean restart) throws FindException, InterruptedException, NodeManagementApi.StartupException {
        NodeManagementApi nma = nodeManagementApiFactory.getManagementService();
        nma.stopNode("default", 20000);
        System.out.print("Stop requested, waiting for shutdown ");

        for ( int i=0; i< 5; i++ ) {
            Thread.sleep( 1000L );
            System.out.print(".");
        }

        boolean handled = false;
        loop:
                for ( int i=0; i< 25; i++ ) {
            Thread.sleep( 1000L );
            System.out.print(".");

                    Collection<NodeManagementApi.NodeHeader> config = nma.listNodes();
            for ( NodeManagementApi.NodeHeader header : config ) {
                if ( "default".equals( header.getName() ) ) {
                    if ( header.getState() == NodeStateType.STOPPED) {
                        handled = true;
                        System.out.println( " stopped" );

                        if (restart) {
                            nodeManagementApiFactory.getManagementService().startNode("default");
                            System.out.println("Restart requested.");
                        }
                        break loop;
                    } else if ( header.getState() == NodeStateType.STOPPING) {
                        continue loop;
                    } else {
                        handled = true;
                        System.out.println( " shutdown failed." );
                        break loop;
                    }
                }
            }
        }

        if (!handled)
            System.out.println( " shutdown timedout." );
        return handled;
    }

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

    private static void clearConsole() {
        final Console console = System.console();
        if ( console != null &&
             SyspropUtil.getBoolean( "com.l7tech.gateway.config.clearConsole", false ) &&
             SyspropUtil.getString( "os.name", "Unknown" ).startsWith( "Linux" ) ) {
            final PrintWriter writer = console.writer();
            writer.write( "\u001b[2J\u001b[H" );
            writer.flush();
        }
    }

    private static final ConfigurationType[] configurationTypes = {
        new ConfigurationType( "appliance-full", "configTemplates/GatewayApplianceConfiguration.xml", true ){
            @Override
            public ConfigurationBeanProvider getProvider() {
                return new NodeConfigurationBeanProvider( new NodeManagementApiFactory( pcUrl ) );
            }
        },
        new ConfigurationType( "appliance", "configTemplates/GatewayApplianceConfiguration.xml", false ){
            @Override
            public ConfigurationBeanProvider getProvider() {
                return new NodeConfigurationBeanProvider( new NodeManagementApiFactory( pcUrl ) );
            }
        },
        new ConfigurationType( "appliance-delete", "configTemplates/GatewayApplianceDeleteConfiguration.xml", true ){
            @Override
            public ConfigurationBeanProvider getProvider() {
                return new NodeDeleteConfigurationBeanProvider( new NodeManagementApiFactory( pcUrl ) );
            }
        },
        new ConfigurationType( "software", "configTemplates/GatewaySoftwareConfiguration.xml", true ){
            @Override
            public ConfigurationBeanProvider getProvider() {
                return new SoftwareNodeConfigurationBeanProvider(new File(SyspropUtil.getString(PROP_NODE_PROPS, DEFAULT_NODE_PROPS)));
            }
        },
        new ConfigurationType( "status", "configTemplates/NodeStatus.xml", true ){
            @Override
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
