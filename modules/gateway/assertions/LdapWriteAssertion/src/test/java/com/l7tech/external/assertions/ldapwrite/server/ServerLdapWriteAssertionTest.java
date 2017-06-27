package com.l7tech.external.assertions.ldapwrite.server;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import com.l7tech.external.assertions.ldapwrite.LdapChangetypeEnum;
import com.l7tech.external.assertions.ldapwrite.LdapWriteAssertion;
import com.l7tech.external.assertions.ldapwrite.LdifAttribute;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.ApplicationContext;

import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Test the LdapWriteAssertion.
 */
@RunWith(PowerMockRunner.class)
public class ServerLdapWriteAssertionTest {


    private static String VALID_WRITE_BASE = "ou=employee, dc=company, dc=com";
    private static String INVALID_WRITE_BASE = "ou=contractor, dc=company, dc=com";

    private static String base64EncodedPic = "/9j/4AAQSkZJRgABAQEAeAB4AAD/2wBDAAIBAQIBAQICAgICAgICAwUDAwMDAwYEBAMFBwYHBwcGBwcICQsJCAgKCAcHCg0KCgsMDAwMBwkODw0MDgsMDAz/2wBDAQICAgMDAwYDAwYMCAcIDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAwMDAz/wAARCAAWABcDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD92734s+FtMuGhufEnh+3mjJVkl1GFGUjrkFs14x8NvitNrHxiv9cnkaO11Bgm2Q7fKtQSI0weECKfOc9ikv8AeOcv9oH4KL8M5or7Sro2+i6pci3jgXb5thKQz4j3Aq0RVG4IynQZGNvC+CdUSTT7Kxj1TUodYt2332wW7XVoqqzE3AaPaEbCBWRFLk7kJTc1fxb4geKXFlHinD5djMNHDxwdT2r5ZuSqwcZRi3LlVouLno1fmesbpH6Zl2S5fHAyq0ZOftVyptJWs9Vbve3Xpo7an2JpHjHSdfk8uw1PTr5wM7be5SQ/kpNFeS/CH4Tr8RNH/tbXJpLvTb3dstWmLeZtYr82D8gDL0U7iVGSo3IxX9PcLZ9m2bZZSzGthY0vaLmUXUd+V6xbtBpXWtr6HxeNweDw9aVF1ZNx0dop69Vdyj+Re/aR8Lal488X+ENH09rJfP8Atk+Lp3WPcgh5O0EnCNIAOM7sZHWq2t/sr3n/AAi+2x8SXU+rtuaUXUYWyuMnJUIoLQ8/xAueTkPxgor4XG8A5BnWb5zi80w6q1LQhduV1H2MHaNpLl1b1jZ6vWzZ7VPNMThcJgoUJWT5m9E7++11T/r0Rt/so3kkvwxmtZNoGn6nc264OedwdufTe749sdOlFFFfdeHMVHhbL4LaNGml6KKS/BHi8QJLMq9v5mf/2Q==";

    private static String VALID_GOID = "a5a3d5aba3ea0236f52677478cdcafac";

    private static ApplicationContext applicationContext;
    private PolicyEnforcementContext peCtx;
    private LdapWriteAssertion assertion;
    private ServerLdapWriteAssertion serverLdapWriteAssertion;

    @Mock
    private IdentityProviderFactory identityProviderFactory;

    @Mock
    private LdapIdentityProvider ldapIdentityProvider;

    @Mock
    private DirContext dirContext;

    @Mock
    private Config config;

    @Mock
    private LdapIdentityProviderConfig ldapIdentityProviderConfig;


    @Before
    public void setUp() throws Exception {

        // Get the spring app context
        if (applicationContext == null) {
            applicationContext = mock(ApplicationContext.class);
            assertNotNull("Fail - Unable to get applicationContext instance", applicationContext);
        }

        peCtx = mock(PolicyEnforcementContext.class);

        Mockito.when(applicationContext.getBean("identityProviderFactory", IdentityProviderFactory.class)).thenReturn(identityProviderFactory);
        Mockito.when(identityProviderFactory.getProvider(Mockito.any(Goid.class))).thenReturn(ldapIdentityProvider);
        Mockito.when(ldapIdentityProvider.getBrowseContext()).thenReturn(dirContext);

        Mockito.when(ldapIdentityProvider.getConfig()).thenReturn(ldapIdentityProviderConfig);

    }

    private List<LdifAttribute> getValidAddAttributeList() {

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();

        ldifAttributeList.add(new LdifAttribute("objectClass", "posixAccount"));
        ldifAttributeList.add(new LdifAttribute("objectClass", "top"));
        ldifAttributeList.add(new LdifAttribute("objectClass", "inetOrgPerson"));
        ldifAttributeList.add(new LdifAttribute("objectClass", "shadowAccount"));
        ldifAttributeList.add(new LdifAttribute("sn", "jsmith"));
        ldifAttributeList.add(new LdifAttribute("phonenumber", "778-123-1234"));// multi-value attribute input
        ldifAttributeList.add(new LdifAttribute("gidNumber", "123"));
        ldifAttributeList.add(new LdifAttribute("jpegPhoto;binary", base64EncodedPic));
        ldifAttributeList.add(new LdifAttribute("description", "line1"));// multi-value attribute input
        ldifAttributeList.add(new LdifAttribute("description", "line2"));// multi-value attribute input
        ldifAttributeList.add(new LdifAttribute("description", "line3"));// multi-value attribute input
        ldifAttributeList.add(new LdifAttribute("phonenumber", "555-123-1234"));// multi-value attribute input

        return ldifAttributeList;
    }

    @Test
    public void testEmptyClusterProperty() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn("");

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith, ou=employee, dc=company,dc=com");
        assertion.setChangetype(LdapChangetypeEnum.ADD);
        assertion.setAttributeList(getValidAddAttributeList());

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);

    }


    @Test
    public void testAddErrorLdapProviderNotPermittedForWrite() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(false);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid("a5a3d5aba3ab0236f52677478cdcafac")); //Goid not in config
        assertion.setDn("uid=jsmith, ou=employee, dc=company,dc=com");
        assertion.setChangetype(LdapChangetypeEnum.ADD);
        assertion.setAttributeList(getValidAddAttributeList());

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);

    }

    @Test
    public void testAddErrorMissingDn() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        //assertion.setDn("uid=jsmith, ou=employee, dc=test,dc=com"); DN is missing
        assertion.setChangetype(LdapChangetypeEnum.ADD);
        assertion.setAttributeList(getValidAddAttributeList());

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);
    }

    @Test
    public void testAddErrorDnOutsideOfWriteBase() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith,"+INVALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.ADD);
        assertion.setAttributeList(getValidAddAttributeList());

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);

    }

    @Test
    public void testAddSuccess() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();

        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.ADD);
        assertion.setAttributeList(getValidAddAttributeList());

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);

    }


    @Test
    public void testAddErrorUserOutsideOfWriteBase() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + INVALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.ADD);
        assertion.setAttributeList(getValidAddAttributeList());

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);

    }


    @Test
    public void testAddErrorUidAlreadyExists() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        Mockito.when(dirContext.createSubcontext(Mockito.anyString(), Mockito.any(BasicAttributes.class))).
                thenThrow(new NameAlreadyBoundException("LDAP: error code 68 - Entry Already Exists"));

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.ADD);
        assertion.setAttributeList(getValidAddAttributeList());

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);

    }


    @Test
    public void testModifyErrorNonMatchingAttributeNameOnDifferentLines() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODIFY);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        ldifAttributeList.add(new LdifAttribute("add", "telephonenumber"));
        ldifAttributeList.add(new LdifAttribute("telephonenumberNOT_MATCHING", "555-5555"));
        ldifAttributeList.add(new LdifAttribute("-", ""));
        ldifAttributeList.add(new LdifAttribute("delete", "description"));
        ldifAttributeList.add(new LdifAttribute("replace", "cn"));
        ldifAttributeList.add(new LdifAttribute("cn", "John R Smith"));
        ldifAttributeList.add(new LdifAttribute("-", ""));
        assertion.setAttributeList(ldifAttributeList);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);

    }


    @Test
    public void testModifyErrorAddAttributeAlreadyExists() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        Mockito.doThrow(new NamingException("LDAP: error code 20 - modify/add: telephoneNumber: value #0 already exists")).
                when(dirContext).modifyAttributes(Mockito.anyString(), Mockito.any(ModificationItem[].class));


        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODIFY);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        ldifAttributeList.add(new LdifAttribute("add", "telephonenumber"));
        ldifAttributeList.add(new LdifAttribute("telephonenumber", "555-5555"));
        ldifAttributeList.add(new LdifAttribute("-", ""));
        assertion.setAttributeList(ldifAttributeList);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);
    }


    @Test
    public void testModifySuccess() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODIFY);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        ldifAttributeList.add(new LdifAttribute("add", "telephonenumber")); // add telephone# and its value
        ldifAttributeList.add(new LdifAttribute("telephonenumber", "555-555-5555"));
        ldifAttributeList.add(new LdifAttribute("-", ""));
        ldifAttributeList.add(new LdifAttribute("delete", "description"));// delete all values of an attribute (without the specifier).
        ldifAttributeList.add(new LdifAttribute("-", ""));
        ldifAttributeList.add(new LdifAttribute("replace", "cn"));
        ldifAttributeList.add(new LdifAttribute("cn", "John R Smith"));
        ldifAttributeList.add(new LdifAttribute("-", ""));
        ldifAttributeList.add(new LdifAttribute("delete", "telephonenumber"));// delete a specific item of a multi-value attribute.
        ldifAttributeList.add(new LdifAttribute("telephonenumber", "235-555-5555"));
        ldifAttributeList.add(new LdifAttribute("-", ""));
        assertion.setAttributeList(ldifAttributeList);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);

    }

    @Test
    public void testModrdnErrorMissingRequiredAttributeKeyword() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODRDN);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        assertion.setAttributeList(ldifAttributeList); // the attribute modification is empty

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);
    }

    @Test
    public void testModrdnErrorRequiredAttributeKeywordIncorrect() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODRDN);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        ldifAttributeList.add(new LdifAttribute("newrdnWrong", "uid=wsmith"));
        assertion.setAttributeList(ldifAttributeList);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);
    }

    @Test
    public void testModrdnErrorDnOutsideOfWriteBase() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODRDN);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        ldifAttributeList.add(new LdifAttribute("newrdn", "uid=wsmith," + INVALID_WRITE_BASE));
        assertion.setAttributeList(ldifAttributeList);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);
    }

    @Test
    public void testModrdnSuccessWithRdn() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODRDN);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        ldifAttributeList.add(new LdifAttribute("newrdn", "uid=wsmith"));
        assertion.setAttributeList(ldifAttributeList);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testModrdnSuccessWithFullyQualifiedDn() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.MODRDN);

        final List<LdifAttribute> ldifAttributeList = new ArrayList<>();
        ldifAttributeList.add(new LdifAttribute("newrdn", "uid=wsmith," + VALID_WRITE_BASE));
        assertion.setAttributeList(ldifAttributeList);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testDeleteErrorDnOutsideOfWriteBase() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith,ou=retired,dc=company,dc=com");
        assertion.setChangetype(LdapChangetypeEnum.DELETE);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.FALSIFIED, status);
    }


    @Test
    public void testDeleteDnSuccess() throws Exception {

        Mockito.when(ldapIdentityProviderConfig.isWritable()).thenReturn(true);
        Mockito.when(ldapIdentityProviderConfig.getWriteBase()).thenReturn(VALID_WRITE_BASE);

        assertion = new LdapWriteAssertion();
        assertion.setLdapProviderId(new Goid(VALID_GOID));
        assertion.setDn("uid=jsmith," + VALID_WRITE_BASE);
        assertion.setChangetype(LdapChangetypeEnum.DELETE);

        serverLdapWriteAssertion = new ServerLdapWriteAssertion(assertion, applicationContext);
        AssertionStatus status = serverLdapWriteAssertion.checkRequest(peCtx);

        assertEquals(AssertionStatus.NONE, status);

    }

}
