package com.l7tech.util;

import static org.junit.Assert.*;
import org.junit.Test;

import java.lang.reflect.Method;

import static com.l7tech.util.JdkLoggerConfigurator.*;

/**
 *
 */
public class JdkLoggerConfiguratorTest {

    @Test
    public void testLog4JAppender() throws Exception {
        final Class configClass = Class.forName( LOG4J_JDK_LOG_APPENDER_CLASS );
        final Method configMethod = configClass.getMethod( LOG4J_JDK_LOG_APPENDER_INIT, new Class[0]);
        assertNotNull( "Log4J init method", configMethod );
    }
}
