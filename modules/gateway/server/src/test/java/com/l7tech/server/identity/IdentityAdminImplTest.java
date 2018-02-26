package com.l7tech.server.identity;

import com.google.common.collect.Lists;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.internal.PasswordChangeRecord;
import com.l7tech.objectmodel.*;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.test.BugId;
import com.l7tech.util.Pair;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IdentityAdminImplTest {
    private static final Goid PROVIDER_ID = new Goid(0,1L);
    private static final String GROUP_ID = "1234";
    private static final Goid USER_ID = new Goid(0, 100L);
    private static final String USER_LOGIN = "gateway";
    private static final String USER_PASSWORD = "abcabcabcabcabcabcabcabc";
    private static final String USER_PREV_PASSWORD = "xyzxyzxyzxyzxyzxyz";
    private static final String USER_HTTP_DIGEST = "digest";

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
    @Mock
    private UserManager userManager;

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
        when(provider.getUserManager()).thenReturn(userManager);
        when(userManager.findByLogin(USER_LOGIN)).thenReturn(mockUser(true, true, true));
        when(userManager.findByPrimaryKey(USER_ID.toString())).thenReturn(mockUser(true, true, true));
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

    @Test
    @BugId("DE303896")
    public void findUserByIDNoPasswordReturned() throws FindException {
        User user = admin.findUserByID(PROVIDER_ID, USER_ID.toString());
        assertNotNull("null user", user);
        assertTrue("user is not InternalUser, it is " + user.getClass().getName(), user instanceof InternalUser);
        assertNull("password was present", ((InternalUser)user).getHashedPassword());
        assertTrue("password history was present", CollectionUtils.isEmpty(((InternalUser) user).getPasswordChangesHistory()));
    }

    @Test
    @BugId("DE303896")
    public void findUserByLoginNoPasswordReturned() throws FindException {
        User user = admin.findUserByLogin(PROVIDER_ID, USER_LOGIN);
        assertNotNull("null user", user);
        assertTrue("user is not InternalUser, it is " + user.getClass().getName(), user instanceof InternalUser);
        assertNull("password was present", ((InternalUser)user).getHashedPassword());
        assertTrue("password history was present", CollectionUtils.isEmpty(((InternalUser) user).getPasswordChangesHistory()));
    }

    @Test(expected = UpdateException.class)
    @BugId("DE303896")
    public void saveUserWithPassword() throws Exception {
        admin.saveUser(PROVIDER_ID, mockUser(true, false, false), Collections.emptySet());
    }

    @Test(expected = UpdateException.class)
    @BugId("DE303896")
    public void saveUserWithPasswordHistory() throws Exception {
        admin.saveUser(PROVIDER_ID, mockUser(false, true, false), Collections.emptySet());
    }

    @Test(expected = UpdateException.class)
    @BugId("DE303896")
    public void saveUserWithHttpDigest() throws Exception {
        admin.saveUser(PROVIDER_ID, mockUser(false, false, true), Collections.emptySet());
    }

    @Test
    @BugId("DE303896")
    public void saveUserCorrectly() throws Exception {
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        admin.saveUser(PROVIDER_ID, mockUser(false, false, false), null);
        verify(userManager).update(captor.capture(), (Set) isNull());
        assertTrue(captor.getValue() instanceof InternalUser);

        InternalUser user = (InternalUser) captor.getValue();
        assertEquals(USER_PASSWORD, user.getHashedPassword());
        assertNotNull(user.getPasswordChangesHistory());
        assertEquals(1, user.getPasswordChangesHistory().size());
        assertEquals(USER_HTTP_DIGEST, user.getHttpDigest());

        PasswordChangeRecord record = user.getPasswordChangesHistory().iterator().next();
        assertEquals(USER_PREV_PASSWORD, record.getPrevHashedPassword());
    }

    private static InternalUser mockUser(boolean withPassword, boolean withHistory, boolean withHttpDigest) {
        InternalUser user = new InternalUser();
        user.setGoid(USER_ID);
        user.setName("User");
        user.setProviderId(PROVIDER_ID);
        user.setLogin(USER_LOGIN);
        user.setEmail("gateway@ca.com");
        user.setFirstName("Gateway");
        user.setLastName("User");
        if (withPassword) {
            user.setHashedPassword(USER_PASSWORD);
        }
        if (withHistory) {
            user.setPasswordChangesHistory(Lists.newArrayList(new PasswordChangeRecord(user, DateUtils.addDays(new Date(), -7).getTime(), USER_PREV_PASSWORD)));
        }
        if (withHttpDigest) {
            user.setHttpDigest(USER_HTTP_DIGEST);
        }
        return user;
    }


}
