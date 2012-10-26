package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.policy.variable.Syntax;

import com.l7tech.util.HexUtils;
import org.jaaslounge.decoding.kerberos.KerberosAuthData;
import org.jaaslounge.decoding.kerberos.KerberosEncData;
import org.jaaslounge.decoding.kerberos.KerberosPacAuthData;
import org.jaaslounge.decoding.kerberos.KerberosRelevantAuthData;
import org.jaaslounge.decoding.pac.Pac;
import org.jaaslounge.decoding.pac.PacLogonInfo;
import org.jaaslounge.decoding.pac.PacSid;
import org.jaaslounge.decoding.pac.PacSignature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 12/12/11
 * Time: 10:21 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class KerberosAuthorizationDataSelectorTest {

    private static final Logger logger = Logger.getLogger(KerberosAuthorizationDataSelectorTest.class.getName());
    private static final Audit audit = new LoggingAudit(logger);

    private static final String USER_ONE = "userOne";
    private static final String USER_ONE_DISPLAY_NAME = "User One";
    private static final int KDC_SIGNATURE_TYPE = 1;
    private static final int SERVER_SIGNATURE_TYPE = 2;


    @Mock
    KerberosEncData mockData;

    @Mock
    Syntax.SyntaxErrorHandler mockSyntaxErrorHandler;
    Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
    KerberosAuthorizationDataSelector fixture;
    byte[] kdcSignatureBytes = {1,2,3,4,5,6};
    byte[] serverSignatureBytes = {6,5,4,3,2,1};
    private Date logonTime;


    @Before
    public void setUp() throws Exception {
        List<KerberosAuthData> authDataList = new ArrayList<KerberosAuthData>();
        KerberosPacAuthData testAuthDataOne = mock(KerberosPacAuthData.class);
        KerberosRelevantAuthData testAuthDataTwo = mock(KerberosRelevantAuthData.class);
        List<KerberosAuthData> authRelevantDataList = new ArrayList<KerberosAuthData>();
        when(testAuthDataTwo.getAuthorizations()).thenReturn(authRelevantDataList);
        Pac pac = mock(Pac.class);
        PacLogonInfo pacLogonInfo= mock(PacLogonInfo.class);
        when(pacLogonInfo.getServerName()).thenReturn("serverName");
        when(pacLogonInfo.getDomainName()).thenReturn("domain");
        when(pacLogonInfo.getBadPasswordCount()).thenReturn((short)0);
        when(pacLogonInfo.getHomeDirectory()).thenReturn("home");
        when(pacLogonInfo.getHomeDrive()).thenReturn("C");
        when(pacLogonInfo.getUserName()).thenReturn(USER_ONE);
        when(pacLogonInfo.getUserDisplayName()).thenReturn(USER_ONE_DISPLAY_NAME);
        cal.set(2011, 11, 8, 1, 2, 3);
        logonTime = cal.getTime();
        when(pacLogonInfo.getLogonTime()).thenReturn(logonTime);
        //cal.set(2011, 11, 8, 1, 2, 5);
        //when(pacLogonInfo.getLogoffTime()).thenReturn(cal.getTime());
        byte[] bytes = {1,2,3,4,5,6,7,8,1,2,3,4,5,6,7,8};
        PacSid groupSid = new PacSid(bytes);
        when(pacLogonInfo.getGroupSid()).thenReturn(groupSid);
        when(pac.getLogonInfo()).thenReturn(pacLogonInfo);
        PacSignature pacKdcSignature = mock(PacSignature.class);
        when(pacKdcSignature.getType()).thenReturn(KDC_SIGNATURE_TYPE);
        when(pacKdcSignature.getChecksum()).thenReturn(kdcSignatureBytes);
        when(pac.getKdcSignature()).thenReturn(pacKdcSignature);
        PacSignature pacServerSignature = mock(PacSignature.class);
        when(pacServerSignature.getChecksum()).thenReturn(serverSignatureBytes);
        when(pacServerSignature.getType()).thenReturn(SERVER_SIGNATURE_TYPE);
        when(pac.getServerSignature()).thenReturn(pacServerSignature);
        when(testAuthDataOne.getPac()).thenReturn(pac);
        authRelevantDataList.add(testAuthDataOne);
        authDataList.add(testAuthDataOne);
        authDataList.add(testAuthDataTwo);
        when(mockData.getUserAuthorizations()).thenReturn(authDataList);
        fixture = new KerberosAuthorizationDataSelector();
    }

    @Test
    public void shouldReturnUserDisplayName() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations.0.pac.logoninfo.user.displayname", mockSyntaxErrorHandler, false);
        ExpandVariables.Selector.Selection expected = new ExpandVariables.Selector.Selection(USER_ONE_DISPLAY_NAME);
        assertSelection(expected, actual);
    }

    @Test
    public void shouldReturnGroupSid() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations.0.pac.logoninfo.groupid", mockSyntaxErrorHandler, false);
        String expectedValue = "\\01\\02\\03\\04\\05\\06\\07\\08\\01\\02\\03\\04\\05\\06\\07\\08";
        assertSelection(new ExpandVariables.Selector.Selection(expectedValue), actual);
    }

    @Test
    public void shouldReturnLogonTime() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations.0.pac.logoninfo.logontime", mockSyntaxErrorHandler, false);
        String expectedValue = Long.toString(logonTime.getTime());
        assertSelection(new ExpandVariables.Selector.Selection(expectedValue), actual);
    }

    @Test
    public void shouldReturnAuthorizationsObject() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations.0", mockSyntaxErrorHandler, false);
        ExpandVariables.Selector.Selection expected = new ExpandVariables.Selector.Selection(mockData.getUserAuthorizations().get(0));
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(mockData.getUserAuthorizations().get(0).toString(), actual.getSelectedValue().toString());
    }

   @Test
    public void shouldReturnAuthorizationsList() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations", mockSyntaxErrorHandler, false);
        ExpandVariables.Selector.Selection expected = new ExpandVariables.Selector.Selection(mockData.getUserAuthorizations());
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(mockData.getUserAuthorizations().size(), ((List<KerberosAuthData>)actual.getSelectedValue()).size());
    }

    @Test
    public void shouldReturnKdsSignature() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations.0.pac.kdc.signature.type", mockSyntaxErrorHandler, false);
        ExpandVariables.Selector.Selection expected = new ExpandVariables.Selector.Selection(KDC_SIGNATURE_TYPE);
        assertSelection(expected, actual);

        actual = fixture.select("kerberos.data", mockData, "authorizations.0.pac.kdc.signature.checksum", mockSyntaxErrorHandler, false);
        expected = new ExpandVariables.Selector.Selection(HexUtils.encodeBase64(kdcSignatureBytes));
        assertSelection(expected, actual);
    }

    @Test
    public void shouldReturnServerSignature() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations.0.pac.server.signature.type", mockSyntaxErrorHandler, false);
        ExpandVariables.Selector.Selection expected = new ExpandVariables.Selector.Selection(SERVER_SIGNATURE_TYPE);
        assertSelection(expected, actual);

        actual = fixture.select("kerberos.data", mockData, "authorizations.0.pac.server.signature.checksum", mockSyntaxErrorHandler, false);
        expected = new ExpandVariables.Selector.Selection(HexUtils.encodeBase64(serverSignatureBytes));
        assertSelection(expected, actual);
    }


    @Test
    public void shouldReturnRelevantData() throws Exception {
        ExpandVariables.Selector.Selection actual = fixture.select("kerberos.data", mockData, "authorizations.1.relevant.authorizations.0.pac.logoninfo.user.name", mockSyntaxErrorHandler, false);
        ExpandVariables.Selector.Selection expected = new ExpandVariables.Selector.Selection(USER_ONE);
        assertSelection(expected, actual);
    }

    public void shouldReturnNullWhenVariableNotSupported() throws Exception {
        assertTrue(null == fixture.select("kerberos.data", mockData, "autorization", mockSyntaxErrorHandler, false));
    }

    @Test(expected=RuntimeException.class)
    public void shouldThrowExceptionWhenAuthorizationIndexIncorrect() throws Exception {
        fixture.select("kerberos.data", mockData, "authorizations.11.pac.logoninfo.user", mockSyntaxErrorHandler, false);
    }


    public void shouldReturnNullWhenVariableNameIncorrect() throws Exception {
        assertTrue(null == fixture.select("kerberos.data", mockData, "authorizations.0.pack.logoninfo.user", mockSyntaxErrorHandler, false));
    }


    private void assertSelection(final ExpandVariables.Selector.Selection expected, final ExpandVariables.Selector.Selection actual) {
        assertEquals(expected.getClass(), actual.getClass());
        assertEquals(expected.getSelectedValue(),actual.getSelectedValue());
    }

    @Test
    public void testGetContextObjectClass() throws Exception {
        assertTrue(fixture.getContextObjectClass() == KerberosEncData.class);
    }

    @Test
    public void testSelectorWithExpandVariables() throws Exception {
        Map<String, Object> vars = new HashMap<String, Object>() {{
            put("kerberos.data", mockData);
        }};
        assertEquals(Integer.toString(SERVER_SIGNATURE_TYPE), ExpandVariables.process("${kerberos.data.authorizations.0.pac.server.signature.type}", vars, audit));
    }
}
