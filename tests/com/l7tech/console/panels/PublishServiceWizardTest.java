/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: mike
 * Date: Oct 1, 2003
 * Time: 2:59:36 PM
 */
public class PublishServiceWizardTest {
    private static final Logger log = Logger.getLogger(PublishServiceWizardTest.class.getName());

    public PublishServiceWizardTest() {
        log.info("New PSWT");
    }

    public static void main(String[] args) throws IOException, FindException {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.log(Level.WARNING, "L&F error", e);
        }
        log.info("Property: " + System.getProperty("com.l7tech.common.locator"));
        //PublishServiceWizard w = new PublishServiceWizard(new JFrame(), true);
        PublishServiceWizard w = PublishServiceWizard.getInstance(new JFrame());
        w.setWsdlUrl("http://data.l7tech.com/ACMEWarehouseWS/Service1.asmx?WSDL");
        w.show();
        Registry registry = Registry.getDefault();
        EntityHeader[] services = registry.getServiceManager().findAllPublishedServices();
        for (int i = 0; i < services.length; i++) {
            EntityHeader serviceHeader = services[i];
            PublishedService service = registry.getServiceManager().findServiceByID(serviceHeader.getStrId());
            String policyXml = service.getPolicyXml();
            Assertion assertion = WspReader.parsePermissively(policyXml);
            log.info("--------------------------------\nService: " + service.getName() + "\n------------\n");
            log.info(assertion.toString());
            log.info("--------------------------------\n");
        }

        System.exit(0);
    }
}
