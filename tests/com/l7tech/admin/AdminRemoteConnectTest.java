/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.admin;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URL;

/**
 * @author emil
 * @version Dec 3, 2004
 */
public class AdminRemoteConnectTest extends TestCase {
    static ApplicationContext context = null;

    public AdminRemoteConnectTest(String s) {
        super(s);
    }

    /**
     * create the <code>TestSuite</code>
     */
    public static Test suite() {
        final TestSuite suite = new TestSuite(AdminRemoteConnectTest.class);
        TestSetup wrapper = new TestSetup(suite) {

            protected void setUp() throws Exception {
                context = createApplicationContext();
            }

            protected void tearDown() throws Exception {
                ;
            }

            private ApplicationContext createApplicationContext() {
                String ctxName = System.getProperty("ssm.application.context");
                if (ctxName == null) {
                    ctxName = "com/l7tech/console/resources/beans-context.xml";
                }
                ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{ctxName});
                return context;
            }
        };
        return wrapper;
    }

    public void xtestObtainAdminInterface() throws Exception {
        AdminLogin admin = (AdminLogin)context.getBean("adminLogin");
        AdminContext ai = admin.login("admin", "password");
        System.out.println("the interface is "+ai);
        System.out.println("the version is "+ai.getVersion());
        System.out.println(ai.getInternalProviderConfig());
    }

    public void testScratch() throws Exception {
        String surl = "http://quark:2104";
        URL url = new URL(surl);
        System.out.println(url.getHost());
    }

    public static void main(String[] args) throws  Throwable {
        junit.textui.TestRunner.run(suite());
    }
}