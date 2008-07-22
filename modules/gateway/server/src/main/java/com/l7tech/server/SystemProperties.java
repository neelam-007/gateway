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

    private final ServerConfig serverConfig;

    private void setSystemProperties(ServerConfig config) throws IOException {
        // Set system properties
        String sysPropsPath = config.getPropertyCached(ServerConfig.PARAM_SYSTEMPROPS);
        File propsFile = new File(sysPropsPath);
        Properties props = new Properties();

        // Set default properties
        props.setProperty("com.sun.jndi.ldap.connect.pool.timeout", Integer.toString(30 * 1000));
        props.setProperty("com.sun.jndi.ldap.connect.pool.protocol", "plain ssl");

        InputStream is = null;
        try {
            if (propsFile.exists()) {
                is = new FileInputStream(propsFile);
            } else {
                is = SystemProperties.class.getResourceAsStream("system.properties");
            }

            if (is != null) props.load(is);

            for (Object o : props.keySet()) {
                String name = (String) o;
                String value = (String) props.get(name);
                logger.config("Setting system property " + name + "=" + value);
                System.setProperty(name, value);
            }
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }


}
