package com.l7tech.server.ems.pages;

import org.junit.Before;
import org.junit.Test;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.logging.Logger;

public class PagesTest {

    private Logger logger = Logger.getLogger(PagesTest.class.getName());
    private WicketTester tester;

    @Before
    public void setup() {
        tester = new WicketTester(new WebApplication() {
            protected void init() {
                AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] {
                        "com/l7tech/server/ems/resources/testEmsApplicationContext.xml",
                });
                addComponentInstantiationListener(new SpringComponentInjector(this, applicationContext));
            }

            public Class getHomePage() {
                return Login.class;
            }
        });

        tester.getApplication().getMarkupSettings().setStripWicketTags(true);
        tester.getApplication().getResourceSettings().setResourceStreamLocator(
            new ResourceStreamLocator() {
                public IResourceStream locate(final Class clazz, final String path) {
                    // logger.info("Processing locate call for path '"+path+"'.");
                    return super.locate(clazz, path.replace("pages", "resources/templates"));
                }
            }
        );
    }

    @Test
    public void testLoginPage() {
        tester.startPage(Login.class);
        tester.assertRenderedPage(Login.class);
        tester.assertVisible("loginForm");
    }

    @Test
    public void testSetupPage() {
        tester.startPage(Setup.class);
        tester.assertRenderedPage(Setup.class);
    }

    @Test
    public void testSystemSettingsPage() {
        tester.startPage(SystemSettings.class);
        tester.assertRenderedPage(SystemSettings.class);
    }

    @Test
    public void testUserSettingsPage() {
        tester.startPage(UserSettings.class);
        tester.assertRenderedPage(UserSettings.class);
    }

    @Test
    public void testAuditPage() {
        tester.startPage(Audits.class);
        tester.assertRenderedPage(Audits.class);
    }
}