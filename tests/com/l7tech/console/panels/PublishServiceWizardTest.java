/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.service.ServiceAdminStub;
import com.l7tech.common.util.Locator;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.GroupManagerStub;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.IdentityProviderConfigManagerStub;
import com.l7tech.identity.IdentityProviderStub;
import com.l7tech.identity.StubDataStore;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.UserManagerStub;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceAdminStub;
import org.apache.log4j.Category;

import javax.swing.*;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * User: mike
 * Date: Oct 1, 2003
 * Time: 2:59:36 PM
 */
public class PublishServiceWizardTest extends Locator {
    private static final Category log = Category.getInstance(PublishServiceWizardTest.class);

    public PublishServiceWizardTest() {
        log.info("New PSWT");
    }

    private static class TestRegistry extends Registry {
        public static final StubDataStore STUB_DATA_STORE = StubDataStore.defaultStore();
        public static final UserManagerStub USER_MANAGER_STUB = new UserManagerStub(STUB_DATA_STORE);
        public static final GroupManagerStub GROUP_MANAGER_STUB = new GroupManagerStub(STUB_DATA_STORE);
        public static final IdentityProviderConfigManagerStub IDENTITY_PROVIDER_CONFIG_MANAGER_STUB = new IdentityProviderConfigManagerStub();
        public static final IdentityProviderStub IDENTITY_PROVIDER_STUB = new IdentityProviderStub();
        public static final ServiceAdminStub SERVICE_MANAGER_STUB = new ServiceAdminStub();

        public IdentityProviderConfigManager getProviderConfigManager() {
            return IDENTITY_PROVIDER_CONFIG_MANAGER_STUB;
        }

        public IdentityProvider getInternalProvider() {
            return IDENTITY_PROVIDER_STUB;
        }

        public UserManager getInternalUserManager() {
            return USER_MANAGER_STUB;
        }

        public GroupManager getInternalGroupManager() {
            return GROUP_MANAGER_STUB;
        }

        public ServiceAdmin getServiceManager() {
            return SERVICE_MANAGER_STUB;
        }
    }

    public Object lookup(Class clazz) {
        log.info("Lookup: " + clazz);
        if (clazz.isAssignableFrom(Registry.class)) {
            return new TestRegistry();
        }

        return null;
    }

    public Locator.Matches lookup(final Locator.Template template) {
        log.info("Lookup");
        return new Locator.Matches() {
            public Collection allInstances() {
                Object got = lookup(template.getType());
                if (got == null)
                    return Collections.EMPTY_LIST;
                List list = new LinkedList();
                list.add(got);
                return list;
            }
        };
    }

    public static void main(String[] args) throws IOException {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.console.panels.PublishServiceWizardTest");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            log.warn(e);
        }
        log.info("Property: " + System.getProperty("com.l7tech.common.locator"));
        PublishServiceWizard w = new PublishServiceWizard(new JFrame(), true);
        w.setWsdlUrl("http://192.168.1.118/ACMEWarehouseWS/Service1.asmx?WSDL");
        w.show();

        EntityHeader[] services = TestRegistry.SERVICE_MANAGER_STUB.findAllPublishedServices();
        for (int i = 0; i < services.length; i++) {
            EntityHeader serviceHeader = services[i];
            PublishedService service = TestRegistry.SERVICE_MANAGER_STUB.findServiceByPrimaryKey(serviceHeader.getOid());
            String policyXml = service.getPolicyXml();
            Assertion assertion = WspReader.parse(policyXml);
            log.info("--------------------------------\nService: " + service.getName() + "\n------------\n");
            log.info(assertion);
            log.info("--------------------------------\n");
        }

        System.exit(0);
    }
}
