package com.l7tech.server.identity;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.GroupManager;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderPasswordPolicy;
import com.l7tech.identity.IdentityProviderPasswordPolicyManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdentityAdminImplTest {
    private static final Goid PROVIDER_ID = new Goid(0,1L);
    private static final String GROUP_ID = "1234";
    private IdentityAdminImpl admin;
    @Mock
    private RoleManager roleManager;
    @Mock
    private IdentityProviderPasswordPolicyManager passwordPolicyManager;
    @Mock
    private PasswordEnforcerManager passwordEnforcerManager;
    @Mock
    private DefaultKey defaultKey;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private LdapConfigTemplateManager templateManager;
    @Mock
    private IdentityProviderFactory providerFactory;
    @Mock
    private IdentityProvider provider;
    @Mock
    private GroupManager groupManager;

    @Before
    public void setup() throws Exception {
        admin = new IdentityAdminImpl(roleManager,
                passwordPolicyManager,
                passwordEnforcerManager,
                defaultKey,
                passwordHasher,
                Collections.<Pair<IdentityAdmin.AccountMinimums, IdentityProviderPasswordPolicy>>emptyList(),
                templateManager);
        admin.setIdentityProviderFactory(providerFactory);
        when(providerFactory.getProvider(PROVIDER_ID)).thenReturn(provider);
        when(provider.getGroupManager()).thenReturn(groupManager);
    }

    @Test
    public void getGroupHeadersForGroup() throws Exception {
        final Set<IdentityHeader> headers = new HashSet<>();
        headers.add(new IdentityHeader(PROVIDER_ID, GROUP_ID, EntityType.GROUP, null, null, null, 0));
        when(groupManager.getGroupHeadersForNestedGroup(GROUP_ID)).thenReturn(headers);
        final Set<IdentityHeader> result = admin.getGroupHeadersForGroup(PROVIDER_ID, GROUP_ID);
        assertEquals(headers, result);
    }

    @Test(expected = FindException.class)
    public void getGroupHeadersForGroupFindException() throws Exception {
        when(groupManager.getGroupHeadersForNestedGroup(GROUP_ID)).thenThrow(new FindException("mocking exception"));
        admin.getGroupHeadersForGroup(PROVIDER_ID, GROUP_ID);
    }
}
