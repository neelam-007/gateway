/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.locator;

import com.l7tech.common.util.Locator;
import com.l7tech.console.panels.PublishServiceWizardTest;
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
import com.l7tech.jms.JmsAdmin;
import com.l7tech.service.JmsAdminStub;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.ServiceAdminStub;
import org.apache.log4j.Category;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Locator for use during tests, that uses stub versions of all manager classes.
  */
public class StubModeLocator extends Locator {
    private static final Category log = Category.getInstance(PublishServiceWizardTest.class);

    public StubModeLocator() {
        log.info("New StubModeLocator");
    }

    private static class TestRegistry extends Registry {
        public static final StubDataStore STUB_DATA_STORE = StubDataStore.defaultStore();
        public static final UserManagerStub USER_MANAGER_STUB = new UserManagerStub(STUB_DATA_STORE);
        public static final GroupManagerStub GROUP_MANAGER_STUB = new GroupManagerStub(STUB_DATA_STORE);
        public static final IdentityProviderConfigManagerStub IDENTITY_PROVIDER_CONFIG_MANAGER_STUB = new IdentityProviderConfigManagerStub();
        public static final IdentityProviderStub IDENTITY_PROVIDER_STUB = new IdentityProviderStub();
        public static final ServiceAdminStub SERVICE_MANAGER_STUB = new ServiceAdminStub();
        public static final JmsAdminStub JMS_MANAGER_STUB = new JmsAdminStub();

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

        public JmsAdmin getJmsManager() {
            return JMS_MANAGER_STUB;
        }

        public IdentityProvider getIdentityProvider(long idProviderOid) {
            return IDENTITY_PROVIDER_STUB;
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
}
