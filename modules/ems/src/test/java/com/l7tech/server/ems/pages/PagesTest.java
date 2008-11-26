package com.l7tech.server.ems.pages;

import org.junit.Before;
import org.junit.Test;
import org.apache.wicket.util.tester.WicketTester;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.Session;
import org.apache.wicket.Request;
import org.apache.wicket.Response;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.l7tech.server.ems.EmsSession;
import com.l7tech.server.ems.NavigationModel;

public class PagesTest {

    private WicketTester tester;

    @Before
    public void setup() {
        tester = new WicketTester(new WebApplication() {
            @Override
            protected void init() {
                AbstractApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String[] {
                        "com/l7tech/server/ems/resources/testEmsApplicationContext.xml",
                });
                addComponentInstantiationListener(new SpringComponentInjector(this, applicationContext));
            }

            @Override
            public Class getHomePage() {
                return Login.class;
            }

            @Override
            public Session newSession(Request request, Response response) {
                return new EmsSession(request);
            }
        });

        tester.getApplication().getMarkupSettings().setStripWicketTags(true);
        tester.getApplication().getResourceSettings().setResourceStreamLocator(
            new ResourceStreamLocator() {
                @Override
                public IResourceStream locate(final Class clazz, final String path) {
                    // logger.info("Processing locate call for path '"+path+"'.");
                    return super.locate(clazz, path.replace("pages", "resources/templates"));
                }
            }
        );
    }

    @Test
    public void testNavigationModel() {
        NavigationModel model = new NavigationModel("com.l7tech.server.ems.pages");
        for ( String section : model.getNavigationSections() ) {
            System.out.println("Found section: " + section);
            for ( String page : model.getNavigationPages() ) {
                System.out.println("   page: " + page);
            }
        }
    }

    @Test
    public void testAuditPage() {
        tester.startPage(Audits.class);
        tester.assertRenderedPage(Audits.class);
    }

    @Test
    public void testConfigurePage() {
        tester.startPage(Configure.class);
        tester.assertRenderedPage(Configure.class);
    }

    @Test
    public void testEnterpriseUserPage() {
        tester.startPage(EnterpriseUsers.class);
        tester.assertRenderedPage(EnterpriseUsers.class);
    }

    @Test
    public void testLoginPage() {
        tester.startPage(Login.class);
        tester.assertRenderedPage(Login.class);
        tester.assertVisible("loginForm");
    }

    @Test
    public void testLogsPage() {
        tester.startPage(Logs.class);
        tester.assertRenderedPage(Logs.class);
    }

    @Test
    public void testSystemSettingsPage() {
        tester.startPage(SystemSettings.class);
        tester.assertRenderedPage(SystemSettings.class);
    }

    @Test
    public void testUserRolesPage() {
        tester.startPage(UserRoles.class);
        tester.assertRenderedPage(UserRoles.class);
    }

    @Test
    public void testUserSettingsPage() {
        tester.startPage(UserSettings.class);
        tester.assertRenderedPage(UserSettings.class);
    }

    @Test
    public void testReportsPage() {
        tester.startPage(StandardReports.class);
        tester.assertRenderedPage(StandardReports.class);
    }

}