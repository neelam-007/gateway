package com.ca.siteminder;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The package private helper/bean class that loads the Siteminder Agent
 * definitions from the dedicated cluster property
 *
 * @author emil, jbufu
 */
public class Config {




    // agent ID -> AgentConfig
    private Map agentConfigs = new LinkedHashMap();

    // - PACKAGE

    Config(String textConfig) throws SiteMinderAgentConfigurationException {

        Properties props = new Properties();
        try {
            props.load(new StringReader(textConfig));
        } catch (IOException e) {
            // shouldn't happen on StringReader
            throw new SiteMinderAgentConfigurationException("Error reading Siteminder configuration: " + e.getMessage(), e);
        }

        readConfig(props);
    }

    /**
     * Gets the single expected agent configuration, throws otherwise.
     */
    SiteMinderAgentConfig getOnlyOneAgentConfig() throws SiteMinderAgentConfigurationException {
        if (agentConfigs.size() != 1) {
            throw new SiteMinderAgentConfigurationException("Expected exactly one Siteminder agent configuration, found: " + agentConfigs.size());
        } else {
            return (SiteMinderAgentConfig) agentConfigs.values().toArray()[0];
        }
    }

    SiteMinderAgentConfig getAgentConfig(String agentId) throws SiteMinderAgentConfigurationException {
        SiteMinderAgentConfig config = (SiteMinderAgentConfig) agentConfigs.get(agentId);
        if (config == null) {
            config = new SiteMinderAgentConfig(agentId);
            agentConfigs.put(agentId, config);
        }
        return config;
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(Config.class.getName());

    private void readConfig(Properties props) throws SiteMinderAgentConfigurationException{
        Enumeration propNames = props.propertyNames();
        while(propNames.hasMoreElements()) {
            String propName = (String) propNames.nextElement();
            readProperty(propName, props.getProperty(propName));
        }
        logger.log(Level.FINE, "Loaded configuration for " + agentConfigs.size() + " agents.");
        Iterator agents = agentConfigs.values().iterator();
        while(agents.hasNext()) {
            SiteMinderAgentConfig agentConfig = (SiteMinderAgentConfig) agents.next();
            logger.log(Level.FINE, "Validating agent configuration: " + agentConfig);
            agentConfig.validate();
        }
    }

    private void readProperty(String propName, String value) throws SiteMinderAgentConfigurationException{
        getAgentConfig(extractAgentId(propName)).setProperty(extractAgentPropertyName(propName), value);
    }

    private String extractAgentId(String propName) throws SiteMinderAgentConfigurationException {
        int firstDot = propName.indexOf(".");
        if (firstDot < 1)
            throw new SiteMinderAgentConfigurationException("Invalid agent configuration entry: " + propName);

        return propName.substring(0, firstDot);
    }

    private String extractAgentPropertyName(String propName) throws SiteMinderAgentConfigurationException {
        int firstDot = propName.indexOf(".");
        if (firstDot < 1 || firstDot == propName.length()-1)
            throw new SiteMinderAgentConfigurationException("Invalid agent configuration entry: " + propName);

        return propName.substring(firstDot+1);
    }
}
