/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common;

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
    static FileSystemXmlApplicationContext prodApplicationContext;

    public static synchronized ApplicationContext getTestApplicationContext() {
        if (testApplicationContext !=null) {
            return testApplicationContext;
        }
        testApplicationContext = createTestApplicationContext();
        return testApplicationContext;
    }

    public static ApplicationContext getProdApplicaitonContext() {
            if (prodApplicationContext !=null) {
            return prodApplicationContext;
        }
        prodApplicationContext = createProdApplicationContext();
        return prodApplicationContext;

    }

    private static FileSystemXmlApplicationContext createProdApplicationContext() {
        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext(PRODUCTION_BEAN_DEFINITIONS);
        return context;
    }

    private static ClassPathXmlApplicationContext createTestApplicationContext() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{DEFAULT_TEST_BEAN_DEFINITIONS});
        return context;
    }
    public static final String DEFAULT_TEST_BEAN_DEFINITIONS = "com/l7tech/common/testApplicationContext.xml";

    public static final String[] PRODUCTION_BEAN_DEFINITIONS = {"etc/dataAccessContext.xml", "etc/webApplicationContext.xml", "etc/adminContext.xml"};
}