/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks;

import com.l7tech.objectmodel.HibernatePersistenceManager;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class HypersonicTest {
    public static final Logger logger = Logger.getLogger(HypersonicTest.class.getName());

    public static void main(String[] args) throws Exception {
        Properties p = new Properties();

        p.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        p.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:aname");
        p.setProperty("hibernate.connection.username", "sa");
        p.setProperty("hibernate.connection.password", "");

        p.setProperty("hibernate.dialect", "net.sf.hibernate.dialect.MySQLDialect");
        p.setProperty("hibernate.transaction.factory_class", "net.sf.hibernate.transaction.JDBCTransactionFactory");
        p.setProperty("hibernate.connection.provider_class", "net.sf.hibernate.connection.DBCPConnectionProvider");
        p.setProperty("hibernate.connection.isolation", "2");

        p.setProperty("hibernate.dbcp.validationQuery", "SELECT 1");
        p.setProperty("hibernate.dbcp.testOnBorrow", "true");
        p.setProperty("hibernate.dbcp.testOnReturn", "true");

        p.setProperty("hibernate.dbcp.maxActive", "100");
        p.setProperty("hibernate.dbcp.maxIdle", "10");
        p.setProperty("hibernate.dbcp.maxWait", "120000");
        p.setProperty("hibernate.dbcp.whenExhaustedAction", "1");


        p.setProperty("hibernate.dbcp.ps.maxActive", "100");
        p.setProperty("hibernate.dbcp.ps.maxIdle", "100");
        p.setProperty("hibernate.dbcp.ps.maxWait", "120000");
        p.setProperty("hibernate.dbcp.ps.whenExhaustedAction", "1");

        HibernatePersistenceManager.initialize(p);

        logger.info("Initialization was successful.");
    }
}
