package com.l7tech.server;

import com.l7tech.util.Config;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Bean that sets system properties from values in a file.
 *
 * @author Steve Jones
 */
public class SystemProperties implements InitializingBean {

    //- PUBLIC

    public SystemProperties(final Config config ) {
        this.config = config;
    }

    public void afterPropertiesSet() throws Exception  {
        setSystemProperties(this.config );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(SystemProperties.class.getName());
    private static final String DEFAULT_PROPS_RES = "resources/system.properties";
    private static final String PROCESS_CONTROLLER_PRESENT_PROP = "com.l7tech.server.processControllerPresent";

    private final Config config;

    private void setSystemProperties(Config config) throws IOException {
        // Set system properties
        String sysPropsPath = config.getProperty( ServerConfigParams.PARAM_SYSTEMPROPS );
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

        // DE372409 : We don't overwrite existing properties in a docker environment, to ensure that JAVA_EXTRA_ARGS get precedence
        String processControllerProp = PROCESS_CONTROLLER_PRESENT_PROP.replace(realPrefix, "");
        boolean overwriteExistingProperties = SyspropUtil.getBoolean(PROCESS_CONTROLLER_PRESENT_PROP) ||
                Boolean.parseBoolean(props.getProperty(processControllerProp));

        for (String unprefixedPropertyName : props.stringPropertyNames()) {
            String value = props.getProperty(unprefixedPropertyName);
            String sysPropName = realPrefix + unprefixedPropertyName;

            if (overwriteExistingProperties || SyspropUtil.getProperty(sysPropName) == null) {
                if (log) logger.log(Level.CONFIG, "Setting system property {0}={1}", new Object[]{sysPropName, value});
                SyspropUtil.setProperty(sysPropName, value);
            } else if (log) {
                logger.log(Level.FINE, "Not setting system property {0} because it has already been set", sysPropName);
            }
        }
    }


}
