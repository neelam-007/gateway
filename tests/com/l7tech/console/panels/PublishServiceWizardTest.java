/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.panels;

import com.l7tech.adminws.service.ServiceManager;
import com.l7tech.adminws.service.ServiceManagerStub;
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
import org.apache.log4j.Category;

import javax.swing.*;
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
        private StubDataStore myStubDataStore;

        public IdentityProviderConfigManager getProviderConfigManager() {
            return new IdentityProviderConfigManagerStub();
        }

        public IdentityProvider getInternalProvider() {
            return new IdentityProviderStub();
        }

        public UserManager getInternalUserManager() {
            return new UserManagerStub(getMyStubDataStore());
        }

        public GroupManager getInternalGroupManager() {
            return new GroupManagerStub(getMyStubDataStore());
        }

        private StubDataStore getMyStubDataStore() {
            return StubDataStore.defaultStore();
        }

        public ServiceManager getServiceManager() {
            return new ServiceManagerStub();
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

    public static void main(String[] args) {
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
        System.exit(0);
    }
}
