package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.gateway.common.service.PublishedService;

import javax.swing.*;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.SyspropUtil;
import org.junit.Ignore;

/**
 * User: mike
 * Date: Oct 1, 2003
 * Time: 2:59:36 PM
 */
@Ignore
public class PublishServiceWizardTest {
    private static final Logger log = Logger.getLogger(PublishServiceWizardTest.class.getName());

    public PublishServiceWizardTest() {
        log.info("New PSWT");
    }

    public static void main(String[] args) throws IOException, FindException {
        SyspropUtil.setProperty( "com.l7tech.common.locator", "com.l7tech.common.locator.StubModeLocator" );
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.log(Level.WARNING, "L&F error", e);
        }
        log.info( "Property: " + ConfigFactory.getProperty( "com.l7tech.common.locator" ) );
        //PublishServiceWizard w = new PublishServiceWizard(new JFrame(), true);
        PublishServiceWizard w = PublishServiceWizard.getInstance(new JFrame());
        w.setVisible(true);
        Registry registry = Registry.getDefault();
        EntityHeader[] services = registry.getServiceManager().findAllPublishedServices();
        for (EntityHeader serviceHeader : services) {
            PublishedService service = registry.getServiceManager().findServiceByID(serviceHeader.getStrId());
            String policyXml = service.getPolicy().getXml();
            Assertion assertion = WspReader.getDefault().parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
            log.info("--------------------------------\nService: " + service.getName() + "\n------------\n");
            log.info(assertion.toString());
            log.info("--------------------------------\n");
        }

        System.exit(0);
    }
}
