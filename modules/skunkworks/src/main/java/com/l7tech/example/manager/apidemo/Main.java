/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.example.manager.apidemo;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Demo sample entry point.
 */
public class Main {
    // todo, change these settings to make them relevent to your situation
    // this is the host name where the ssg is deployed (must be same host name as one declared in ssl cert)
    public static final String SSGHOST =  "samplessg.layer7tech.com";
    // administrator account name valid on that ssg
    public static final String ADMINACCOUNT_NAME = "admin";
    // associated password for the abovementioned administrator account name
    public static final String ADMINACCOUNT_PASSWD = "password";

    private static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        // create an admin session
        SsgAdminSession session = new SsgAdminSession(SSGHOST, ADMINACCOUNT_NAME, ADMINACCOUNT_PASSWD);

        // list all available web services
        ServicePublication stub = new ServicePublication(session);
        try {
            String[] res = stub.listPublishedServices();
            logger.info("Published services found (" + res.length + ")");
            for (String line : res) {
                logger.info(line);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "ERROR while listing published service", e);
        }

        // create a bunch of users, then list them
        IdentityProvider stub2 = new IdentityProvider(session);
        try {
            ArrayList<String> res = stub2.createBulkUsers("U-" + System.currentTimeMillis(), "nohcolber", 25);
            logger.info("Users created (" + res.size() + ")");
            for (String id : res) {
                logger.info(id);
            }
            String[] res2 = stub2.listUsers();
            logger.info("Users found (" + res2.length + ")");
            for (String u : res2) {
                logger.info(u);
            }
            stub2.removeBulkUsers("U-");
        } catch (Exception e) {
            logger.log(Level.WARNING, "ERROR while adding users", e);
        }

        // cluster status and service statistics
        ShowClusterStatistics stub3 = new ShowClusterStatistics(session);
        try {
            String[] res = stub3.getClusterStatus();
            for (String s : res) {
                logger.info(s);
            }
            res = stub3.getServiceUsage();
            for (String s : res) {
                logger.info(s);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "ERROR while fetching statistics", e);
        }
    }
}
