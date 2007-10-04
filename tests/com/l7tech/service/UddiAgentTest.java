package com.l7tech.service;

import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;

import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class UddiAgentTest {

    private final Logger logger = Logger.getLogger(getClass().getName());

    public static void main(String[] args) {
        new UddiAgentTest().testGetWsdlUrls();
    }

    private void testGetWsdlUrls() {
        String searchString = "%";
        ServiceAdmin seriveAdmin = Registry.getDefault().getServiceManager();
        if (seriveAdmin == null) throw new RuntimeException("Service Admin reference not found");

        try {
            WsdlInfo[] urls = seriveAdmin.findWsdlUrlsFromUDDIRegistry("http://whale.l7tech.com:8085", searchString, false);
            if (urls != null) {
                logger.info("Number of URLs on the list is: " + urls.length);
                for (int i = 0; i < urls.length; i++) {
                    System.out.println("Service Name: " + urls[i].getName());
                    System.out.println("Service URL: " + urls[i].getWsdlUrl());
                }
            } else {
                logger.warning("ServiceAdmin returns a NULL list");
            }
        } catch (FindException e) {
            e.printStackTrace();
        }
    }
}
