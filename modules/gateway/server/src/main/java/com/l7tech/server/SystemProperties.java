package com.l7tech.server;

import com.l7tech.util.ResourceUtils;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Bean that sets system properties from values in a file.
 *
 * @author Steve Jones
 */
public class SystemProperties implements InitializingBean {

    //- PUBLIC

    public SystemProperties(final ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void afterPropertiesSet() throws Exception  {
        setSystemProperties(this.serverConfig);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SystemProperties.class.getName());
    private static final String DEFAULT_PROPS_RES = "resources/system.properties";

    private final ServerConfig serverConfig;

    private void setSystemProperties(ServerConfig config) throws IOException {
        // Set system properties
        String sysPropsPath = config.getPropertyCached(ServerConfig.PARAM_SYSTEMPROPS);
        File propsFile = new File(sysPropsPath);
        Properties props = new Properties();

        // Set default properties
        InputStream in = null;
        try {
            in = SystemProperties.class.getResourceAsStream(DEFAULT_PROPS_RES);
            if ( in != null ) {
                props.load( in );
            }
        } finally {
            ResourceUtils.closeQuietly(in);
        }


        InputStream is = null;
        try {
            if (propsFile.exists()) {
                is = new FileInputStream(propsFile);
            } else {
                is = SystemProperties.class.getResourceAsStream("system.properties");
            }

            if (is != null) props.load(is);

            setSystemProperties(props, null, true);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    public void setSystemProperties(Properties props, String prefix, boolean log) {
        final String realPrefix = prefix == null ? "" : (prefix.endsWith(".") ? prefix : prefix + ".");
        for (String unprefixedPropertyName : props.stringPropertyNames()) {
            String value = props.getProperty(unprefixedPropertyName);
            String sysPropName = realPrefix + unprefixedPropertyName;
            if (log) logger.config("Setting system property " + sysPropName + "=" + value);
            System.setProperty(sysPropName, value);
        }
    }


}
