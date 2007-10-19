package com.l7tech.server.service.uddi;

import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.Service;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.util.FilterClassLoader;
import com.l7tech.server.util.ModuleClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory for UddiAgent implementation and Properties.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class UddiAgentFactory {

    //- PUBLIC

    /**
     *
     */
    public UddiAgentFactory() {
    }

    /**
     * Set ServerConfig reference.
     *
     * @param serverConfig The server config to use.
     */
    public void setServerConfig(ServerConfig serverConfig) {
        if (this.serverConfig!=null) throw new IllegalStateException("already set");
        this.serverConfig = serverConfig;
    }

    /**
     * Get the UDDI properties.
     *
     * <p>This could be a mixture of the uddi.properties file and the cluster
     * properties for uddi.</p>
     *
     * @return the Properties to use.
     * @throws UddiAgentException if an error occurs
     */
    public Properties getUddiProperties() throws UddiAgentException {
        Properties properties = new Properties();
        addUDDIConfig(properties);
        addClusterProperties(properties);
        return properties;
    }

    /**
     * Get the UddiAgent.
     *
     * @return The configured agent.
     * @throws UddiAgentException if no agent is available or an error occured
     */
    public UddiAgent getUddiAgent() throws UddiAgentException {
        ServerConfig serverConfig = ServerConfig.getInstance();

        ClassLoader uddiClassLoader = getUddiModuleClassLoader(serverConfig);

        if (uddiClassLoader != null) {
            Iterator serviceIter = Service.providers(UddiAgent.class, uddiClassLoader);
            if (serviceIter.hasNext()) {
                UddiAgent agentImpl = (UddiAgent) serviceIter.next();

                if (serviceIter.hasNext())
                    logger.warning("Multiple UDDI implementations found, using first.");


                agentImpl.init(getUddiProperties());
                return agentImpl;
            }
        }

        throw new UddiAgentException("No UDDI available.");
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(UddiAgentFactory.class.getName());

    private static final String KEY_CUSTOM_MODULES = "custom.assertions.modules";
    private static final String KEY_CUSTOM_MODULES_TEMP = "custom.assertions.temp";
    private static final String UDDI_CONFIG_FILENAME = "uddi.properties";

    private ServerConfig serverConfig;
    private ClassLoader uddiModuleClassLoader = null;

    /**
     * Read config file props into the given properties object.
     */
    private void addUDDIConfig(Properties props) throws UddiAgentException {
        String ssgConfigPath = serverConfig.getPropertyCached("configDirectory");
        String uddiConfigFileName = ssgConfigPath + "/" + UDDI_CONFIG_FILENAME;

        FileInputStream propStream = null;
        try {
            propStream = null;

            File file = new File(uddiConfigFileName);
            if (file.exists()) {
                propStream = new FileInputStream(file);
                props.load(propStream);
                if (logger.isLoggable(Level.FINE))
                    logger.fine("Loaded UDDI Registry properties from '" + uddiConfigFileName + "'.");
            }
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Couldn't load file '" + uddiConfigFileName + "'.", ioe);
        } finally {
            ResourceUtils.closeQuietly(propStream);
        }
    }

    /**
     * Set any UDDI cluster properties on the given properties object.
     */
    private void addClusterProperties(Properties props) {
        String inquiryUrlList = serverConfig.getPropertyCached(UddiAgent.PROP_INQUIRY_URLS);
        String resultBatchSize = serverConfig.getPropertyCached(UddiAgent.PROP_RESULT_BATCH_SIZE);
        String resultRowsMax = serverConfig.getPropertyCached(UddiAgent.PROP_RESULT_ROWS_MAX);

        if (inquiryUrlList != null) {
            // then remove any existing urls
            for (int p=1; p<101; p++) {
                props.remove(UddiAgent.PROP_INQUIRY_URLS + "." + p);
            }

            // add urls
            StringTokenizer strtok = new StringTokenizer(inquiryUrlList, " \n\r\t");
            int index = 1;
            while (strtok.hasMoreTokens()) {
                props.setProperty(UddiAgent.PROP_INQUIRY_URLS + "." + index,
                        strtok.nextToken());
                index++;
            }
        }

        if (resultBatchSize != null) {
            try {
                props.setProperty(UddiAgent.PROP_RESULT_BATCH_SIZE,
                        Integer.toString(Integer.parseInt(resultBatchSize)));
            }
            catch(NumberFormatException nfe) {
                logger.warning("Ignoring cluster property for '"+
                        UddiAgent.PROP_RESULT_BATCH_SIZE+
                        "' value '"+
                        resultBatchSize+
                        "', expected integer.");
            }
        }

        if (resultRowsMax != null) {
            try {
                props.setProperty(UddiAgent.PROP_RESULT_ROWS_MAX,
                        Integer.toString(Integer.parseInt(resultRowsMax)));
            }
            catch(NumberFormatException nfe) {
                logger.warning("Ignoring cluster property for '"+
                        UddiAgent.PROP_RESULT_ROWS_MAX+
                        "' value '"+
                        resultRowsMax+
                        "', expected integer.");
            }
        }
    }

    private ClassLoader getUddiModuleClassLoader(ServerConfig serverConfig) throws UddiAgentException {
        if (uddiModuleClassLoader == null) {
            String moduleDirectory = serverConfig.getPropertyCached(KEY_CUSTOM_MODULES);
            String moduleWorkDirectory = serverConfig.getPropertyCached(KEY_CUSTOM_MODULES_TEMP);

            if (moduleDirectory == null) {
                logger.config("'" + KEY_CUSTOM_MODULES + "' not specified");
                throw new UddiAgentException("Not configured for UDDI.");
            }

            uddiModuleClassLoader = new ModuleClassLoader(
                new FilterClassLoader(UddiAgentFactory.class.getClassLoader(), "com.l7tech"),
                "uddi",
                "META-INF/services/" + UddiAgent.class.getName(),
                new File(moduleDirectory),
                new File(moduleWorkDirectory));
        }

        return uddiModuleClassLoader;
    }
}
