package com.l7tech.server.module.ca_wsdm;

import com.ca.wsdm.monitor.ObserverProperties;
import com.l7tech.server.ServerConfig;
import com.l7tech.gateway.common.cluster.ClusterProperty;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

/**
 * Converts cluster properties into CA WSDM properties.
 */
public class CaWsdmPropertiesAdaptor {
    protected static final Logger logger = Logger.getLogger(CaWsdmPropertiesAdaptor.class.getName());

    /** Default maximum number of characters per message body to send to WSDM Manager. */
    public static final int MESSAGE_BODY_LIMIT_DEFAULT = 5000;

    /** Default observer type (a.k.a. handler type); for display in WSDM Manager. */
    public static final int OBSERVER_TYPE_DEFAULT = 777;
    
    /** The default (unconfigured) SOAP endpoint, until the admin customizes wsdm.managerSoapEndpoint. */
    static final String DEFAULT_SOAP_ENDPOINT = "http://hostname:8282/wsdm35mmi/services/WSDM35MMI";

    /** Filename that WSDM will try to load from the classpath to find the SOMMA properties. */
    static final String SOMMA_FILENAME = "WsdmSOMMA_Basic.properties";

    // Serverconfig variable names for the ones added by layer 7 that aren't from ObserverProperties
    private static final String PARAM_OBSERVER_TYPE = "cawsdmObserverType";
    private static final String PARAM_MESSAGE_BODY_LIMIT = "cawsdmMessageBodyLimit";

    private static final Set<String> caProps = new HashSet<String>();
    private static final Set<String> sommaProps = new HashSet<String>();
    private final ServerConfig serverConfig;

    /**
     * Create an adaptor that will build a {@link com.ca.wsdm.monitor.ObserverProperties} instance
     * using cluster properties loaded from the specified ServerConfig instance.
     *
     * @param serverConfig  the ServerConfig instance from which to load the properties to build the
     *                      CA WSDM ObserverProperties instance.  Required.
     */
    public CaWsdmPropertiesAdaptor(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;

        // Make sure the caProps is populated
        if (caProps.size() < 1)
            addClusterPropertiesMetadata(new HashMap<String, String[]>());
    }

    /**
     * Add metadata for the new serverconfig cluster properties we will be declaring to support configuration
     * of CA WSDM.
     *
     * @param props a Map ready to receive the new properties, in the format described by
     *             {@link com.l7tech.policy.assertion.AssertionMetadata#CLUSTER_PROPERTIES}.
     */
    public static void addClusterPropertiesMetadata(Map<String, String[]> props) {
        // Add layer 7 additional properties
        props.put("cawsdm.observerType", new String[] {
                "Observer type to show in WSDM Manager (integer).\n" +
                "Add this line to \\Program Files\\CA\\Unicenter WSDM\\server\\default\\conf\\WsdmSOMMA_Basic.properties\n" +
                "on the WSDM Manager.\n" +
                "    observertype.777=SecureSpan Gateway",
                String.valueOf(OBSERVER_TYPE_DEFAULT)
        });

        props.put("cawsdm.messageBodyLimit", new String[] {
                "Maximum number of characters per message body to send to WSDM Manager; if sending is enabled. The purpose is to limit excessive network usage. (Characters)",
                String.valueOf(MESSAGE_BODY_LIMIT_DEFAULT)
        });

        // Add CA ObserverProperties
        addCaProp(props, ObserverProperties.CONFIG_MANAGER_SOAP_ENDPOINT,
                  "WSDM Manager endpoint address.  (URL)\n" +
                  "(REQUIRED) If not specified, the Observer will be disabled.",
                  DEFAULT_SOAP_ENDPOINT);

        addCaProp(props, ObserverProperties.CONFIG_AUTO_DISCOVER,
                  "Determines whether the Observer reports all newly discovered WSDLs and\n" +
                  "registers them with the Catalog. (yes/no)\n" +
                  "If set to no, all new WSDL files must be manually imported into the Catalog\n" +
                  "for the services to be registered.\n" +
                  "For a description of this functionality, see Maintaining the Catalog in the\n" +
                  "CA WSDM online help.",
                  "yes");

        addCaProp(props, ObserverProperties.CONFIG_BLOCK_UNKNOWN,
                  "Determines whether all requests arriving at the Observer will be allowed\n" +
                  "through, even if the request was not previously identified in the Manager\n" +
                  "Catalog. (yes/no)\n" +
                  "If you change the setting to yes, the Observer will not process unidentified requests.",
                  "no");

        addCaProp(props, ObserverProperties.CONFIG_LOG_SOAP,
                  "If this parameter is set to yes, all SOAP messages passing through the\n" +
                  "observer will be sent to the manager for logging (and SOAP message is included\n" +
                  "in locallog records).\n" +
                  "If set to no, only messages which have violated single transaction threshold\n" +
                  "monitor(s) will be sent to the manager (and SOAP messages are omitted from\n" +
                  "local log records).",
                  "no");

        addCaProp(props, ObserverProperties.CONFIG_QUEUE_SIZE_MAX,
                  "The maximum number of messages allowed in the queue.\n" +
                  "Change as desired if system resources permit.",
                  "0");

        addCaProp(props, ObserverProperties.CONFIG_QUEUE_SIZE_MIN,
                  "The minimum number of messages that must be stored before messages are transmitted.\n" +
                  "Allows for transaction-based buffering of data at the Observer level before it\n" +
                  "is sent to the Manager.",
                  "1");

        addCaProp(props, ObserverProperties.CONFIG_SEND_SOAP,
                  "If this parameter is set to no, then SOAP messages will not be sent to the manager.\n" +
                  "When set to no, this parameter takes precedence over the logSOAP parameter to\n" +
                  "ensure that no SOAP messages are sent to manager.\n" +
                  "When set to yes, sending of SOAP messages to manager is determined by logSOAP\n" +
                  "parameter.\n" +
                  "Note that setting this to yes will increase CPU and network load on the SSG.",
                  "no");

        addCaProp(props, ObserverProperties.CONFIG_STANDALONE_MODE,
                  "Specifies whether the Observer logs messages without Manager availability.\n" +
                  "Change as desired if system resources permit.\n" +
                  "Allows users to shut off sending every transaction to manager unless\n" +
                  "transaction produces a fault or transaction statistics exceeds a single\n" +
                  "transaction monitor threshold.\n" +
                  "If the parameter is set to yes, the observer sends only the service request\n" +
                  "information for the transaction that produced faults and those exceeds a\n" +
                  "single transaction monitor threshold.",
                  "no");

        addCaProp(props, ObserverProperties.CONFIG_WAIT_PERIOD,
                  "The maximum amount of time in minutes before the Observer will try to resend\n" +
                  "data to the Manager if the connection between the Observer and Manager has\n" +
                  "been broken. Change as desired if system resources permit.",
                  "5");

        // Add logging config properties

        addSommaProp(props, "log.file.maxsize", "Maximum size of observer log file (bytes)", "10485760");
        addSommaProp(props, "log.echo.debug", "Logs debug information to the console (/var/log/messages)", "false");
        addSommaProp(props, "log.echo.info", "Logs info information to the console (/var/log/messages)", "false");
        addSommaProp(props, "log.echo.warn", "Logs warning information to the console (/var/log/messages)", "true");
        addSommaProp(props, "log.echo.error", "Logs error information to the console (/var/log/messages)", "true");
        addSommaProp(props, "log.enable.debug", "Logs debug information to the log file", "false");
        addSommaProp(props, "log.enable.info", "Logs info details to the log file", "false");
        addSommaProp(props, "log.enable.warn", "Logs warning information to the log file", "true");
        addSommaProp(props, "log.enable.error", "Logs error information to the log file", "true");

        // These two are generated automatically based on the current SSG_HOME value.
        //addSommaProp(props, "log.file.path", "Observer log file path.", "/ssg/logs/ca_wsdm_observer");
        //addSommaProp(props, "log.file.ext", "Observer log file extension", ".log");
    }


    private static synchronized void addCaProp(Map<String, String[]> props, String caName, String desc, String dflt) {
        props.put(asCpName(caName), new String[] {
                desc,
                dflt
        });
        caProps.add(caName);
    }

    private static synchronized void addSommaProp(Map<String, String[]> props, String caName, String desc, String dflt) {
        if (desc == null)
            desc = "This property configures WSDM logging.";
        props.put(asCpName(caName), new String[] { desc, dflt });
        sommaProps.add(caName);
    }

    /**
     * Given a CA WSDM ObserverProperties name, returns the corresponding Layer 7 cluster property name.
     *
     * @param caName  the name of a CA ObserverProperties property, ie "standaloneMode" or "log.echo.warn".
     * @return the corresponding Layer 7 cluster property name, ie "cawsdm.standaloneMode" or "cawsdm.log.echo.warn".
     */
    public static String asCpName(String caName) {
        return "cawsdm." + caName;
    }

    /**
     * Given a CA WSDM ObserverProperties name, returns the corresponding Layer 7 serverconfig parameter name.
     *
     * @param caName  the name of a CA ObserverProperties property, ie "standaloneMode" or "log.echo.warn".
     * @return the corresponding Layer 7 ServerConfig parameter name, ie "cawsdmStandaloneMode" or "cawsdmLogEchoWarn".
     */
    public static String asScName(String caName) {
        return ClusterProperty.asServerConfigPropertyName(asCpName(caName));
    }


    /**
     * Build CA WSDM ObserverProperties from the current serverconfig cluster properties.
     *
     * @return a new ObserverProperties instance.  Never null.
     */
    public ObserverProperties getObserverProperties() {
        Map<String, Object> properties = new HashMap<String, Object>();
        for (String caProp : caProps) {
            String value = serverConfig.getProperty(asScName(caProp));
            properties.put(caProp, value);
        }

        return new ObserverProperties(properties);
    }

    /**
     * Build a WsdmSOMMA_Basic.properties collection from the current serverconfig cluster properties.
     *
     * @return a Properties object representing the properties from serverconfig that
     *          used to live in the WsdmSOMMA_Basic.properties file.  Never null.
     */
    public Properties getSommaBasicProperties() {
        Properties properties = new Properties();

        properties.put("log.file.path",
                       serverConfig.getProperty(ServerConfig.PARAM_SSG_HOME_DIRECTORY) +
                       File.separator + "logs" + File.separator + "ca_wsdm_observer");
        properties.put("log.file.ext", ".log");

        for (String caProp : sommaProps) {
            String scName = asScName(caProp);
            String value = serverConfig.getProperty(scName);
            if (value != null) {
                properties.put(caProp, value);
            } else {
                logger.warning("null ServerConfig property (not yet registered?): " + scName);
            }
        }
        return properties;
    }

    public int getObserverType() {
        int observerType;
        String s = serverConfig.getProperty(PARAM_OBSERVER_TYPE);
        if (s == null) {
            observerType = OBSERVER_TYPE_DEFAULT;
        } else {
            try {
                observerType = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                observerType = OBSERVER_TYPE_DEFAULT;
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Setting Observer type to " + observerType);
        }
        return observerType;
    }

    public int getMessageBodyLimit() {
        String s = serverConfig.getProperty(PARAM_MESSAGE_BODY_LIMIT);
        int limit;
        if (s == null) {
            limit = MESSAGE_BODY_LIMIT_DEFAULT;
        } else {
            try {
                limit = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                limit = MESSAGE_BODY_LIMIT_DEFAULT;
            }
        }
        if (logger.isLoggable(Level.FINE)) {
            logger.fine("Setting message body sending limit to " + limit + " characters.");
        }
        return limit;
    }
}
