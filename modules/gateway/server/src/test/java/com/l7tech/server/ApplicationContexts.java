/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
<<<<<<< ApplicationContexts.java
=======
 *
 * $Id$
>>>>>>> 1.5.14.1
 */
package com.l7tech.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * A bag of application contexts
 * @author emil
 * @version Dec 13, 2004
 */
public class ApplicationContexts {
    static ClassPathXmlApplicationContext testApplicationContext;
    static ClassPathXmlApplicationContext prodApplicationContext;

    public static synchronized ApplicationContext getTestApplicationContext() {
        if (testApplicationContext !=null) {
            return testApplicationContext;
        }
        testApplicationContext = createTestApplicationContext();
        return testApplicationContext;
    }

    public static ApplicationContext getProdApplicationContext() {
            if (prodApplicationContext !=null) {
            return prodApplicationContext;
        }
        prodApplicationContext = createProdApplicationContext();
        return prodApplicationContext;

    }

    private static ClassPathXmlApplicationContext createProdApplicationContext() {
        return new ClassPathXmlApplicationContext(PRODUCTION_BEAN_DEFINITIONS);
    }

    private static ClassPathXmlApplicationContext createTestApplicationContext() {
        return new ClassPathXmlApplicationContext(new String[]{DEFAULT_TEST_BEAN_DEFINITIONS});
    }
    public static final String DEFAULT_TEST_BEAN_DEFINITIONS = "com/l7tech/server/resources/testApplicationContext.xml";

    public static final String[] PRODUCTION_BEAN_DEFINITIONS = {
            "com/l7tech/server/resources/dataAccessContext.xml",
            "com/l7tech/server/resources/ssgApplicationContext.xml",
            "com/l7tech/server/resources/adminContext.xml"
    };
}