/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Test application context
 * @author emil
 * @version Dec 13, 2004
 */
public class ApplicationContexts {
    static ClassPathXmlApplicationContext current;

    public static synchronized ApplicationContext getTestApplicationContext() {
        if (current !=null) {
            return current;
        }
        current = createTestApplicationContext();
        return current;
    }
    /**
     * Create thest application context
     * @return
     */
    public static ClassPathXmlApplicationContext createTestApplicationContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{DEFAULT_TEST_BEAN_DEFINITIONS});
        return context;
    }
    public static final String DEFAULT_TEST_BEAN_DEFINITIONS = "com/l7tech/common/testApplicationContext.xml";
}