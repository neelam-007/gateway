package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.GroupMappingConfig;
import com.l7tech.identity.ldap.LdapGroup;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.MemberStrategy;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Tests for com.l7tech.server.identity.ldap.LdapGroupManagerImpl
 */
@RunWith(PowerMockRunner.class)
public class LdapGroupManagerTest {

    @Mock
    private NamingEnumeration<SearchResult> searchResults;
    @Mock
    private SearchResult searchResult;
    @Mock
    private Attributes ldapAttributes;
    @Mock
    private DirContext dirContext;
    @Mock
    private LdapIdentityProviderConfig idProviderConfig;
    @Mock
    private LdapIdentityProvider idProvider;

    /**
     * Test user lookup / finding / searching
     */
    @Test
    public void testGetGroup() throws Exception {
        String groupName = "groupName", groupDescription = "group description";

        GroupMappingConfig groupMappingConfig = new GroupMappingConfig();
        groupMappingConfig.setObjClass("posixGroup");
        groupMappingConfig.setNameAttrName("cn");
        groupMappingConfig.setMemberAttrName("memberUid");
        groupMappingConfig.setMemberStrategy(MemberStrategy.MEMBERS_ARE_LOGIN);

        when(idProviderConfig.getGroupMappings()).thenReturn(new GroupMappingConfig[]{groupMappingConfig});
        when(idProvider.getConfig()).thenReturn(idProviderConfig);
        when(idProvider.getBrowseContext()).thenReturn(dirContext);
        when(dirContext.search( any(Name.class), anyString(), any(SearchControls.class))).thenReturn(searchResults);
        when(searchResults.next()).thenReturn(searchResult);
        when(searchResults.hasMore()).thenReturn(true);
        when(searchResult.getAttributes()).thenReturn(ldapAttributes);
        when(ldapAttributes.get(LdapIdentityProvider.OBJECTCLASS_ATTRIBUTE_NAME)).thenReturn(new SingleAttribute(groupMappingConfig.getObjClass()));
        when(ldapAttributes.get(LdapIdentityProvider.DESCRIPTION_ATTRIBUTE_NAME)).thenReturn(new SingleAttribute(groupDescription));
        when(ldapAttributes.get("cn")).thenReturn(new SingleAttribute(groupName));

        LdapGroupManager groupManager = new LdapGroupManagerImpl();
        groupManager.configure(idProvider);
        LdapGroup group = groupManager.findByName(groupName);
        Assert.assertEquals(groupName, group.getName());
        Assert.assertEquals(groupDescription, group.getDescription());
    }

    @Test
    public void testGetGroupNoDescription() throws Exception {
        String groupName = "groupName";

        GroupMappingConfig groupMappingConfig = new GroupMappingConfig();
        groupMappingConfig.setObjClass("posixGroup");
        groupMappingConfig.setNameAttrName("cn");
        groupMappingConfig.setMemberAttrName("memberUid");
        groupMappingConfig.setMemberStrategy(MemberStrategy.MEMBERS_ARE_LOGIN);

        LdapIdentityProviderConfig idProviderConfig  = mock(LdapIdentityProviderConfig.class);
        LdapIdentityProvider idProvider = mock(LdapIdentityProvider.class);
        when(idProviderConfig.getGroupMappings()).thenReturn(new GroupMappingConfig[]{groupMappingConfig});
        when(idProvider.getConfig()).thenReturn(idProviderConfig);
        when(idProvider.getBrowseContext()).thenReturn(dirContext);
        when(dirContext.search( any(Name.class), anyString(), any(SearchControls.class))).thenReturn(searchResults);
        when(searchResults.next()).thenReturn(searchResult);
        when(searchResults.hasMore()).thenReturn(true);
        when(searchResult.getAttributes()).thenReturn(ldapAttributes);
        when(ldapAttributes.get(LdapIdentityProvider.OBJECTCLASS_ATTRIBUTE_NAME)).thenReturn(new SingleAttribute(groupMappingConfig.getObjClass()));
        when(ldapAttributes.get("cn")).thenReturn(new SingleAttribute(groupName));

        LdapGroupManager groupManager = new LdapGroupManagerImpl();
        groupManager.configure(idProvider);
        LdapGroup group = groupManager.findByName(groupName);
        Assert.assertEquals(groupName, group.getName());
        Assert.assertNull(group.getDescription());
    }

    class SingleAttribute implements Attribute {

        String value;

        SingleAttribute(String value) {
            this.value = value;
        }

        @Override
        public NamingEnumeration<?> getAll() throws NamingException {
            return null;
        }

        @Override
        public Object get() throws NamingException {
            return value;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public String getID() {
            return null;
        }

        @Override
        public boolean contains(Object attrVal) {
            return attrVal.equals(value);
        }

        @Override
        public boolean add(Object attrVal) {
            return false;
        }

        @Override
        public boolean remove(Object attrval) {
            return false;
        }

        @Override
        public void clear() {

        }

        @Override
        public DirContext getAttributeSyntaxDefinition() throws NamingException {
            return null;
        }

        @Override
        public DirContext getAttributeDefinition() throws NamingException {
            return null;
        }

        @Override
        public Object clone() {
            return null;
        }

        @Override
        public boolean isOrdered() {
            return false;
        }

        @Override
        public Object get(int ix) throws NamingException {
            if( ix == 0)
                return value;
            return null;
        }

        @Override
        public Object remove(int ix) {
            return null;
        }

        @Override
        public void add(int ix, Object attrVal) {

        }

        @Override
        public Object set(int ix, Object attrVal) {
            return null;
        }
    }
}
