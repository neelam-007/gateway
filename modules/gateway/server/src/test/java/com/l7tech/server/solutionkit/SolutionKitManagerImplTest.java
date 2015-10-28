package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.util.Pair;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.Callable;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Test the Solution Kit Manager
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitManagerImplTest {
    private final SolutionKitManagerImpl solutionKitManager = new SolutionKitManagerImpl();
    private final static String REQUEST = "ignored for these tests";

    @Mock
    private RestmanInvoker restmanInvoker;
    @Mock
    private ProtectedEntityTracker protectedEntityTracker;
    @Mock
    private Callable<Pair<AssertionStatus, RestmanMessage>> callable;
    @Mock
    private PolicyEnforcementContext pec;
    @Mock
    private SolutionKit metadata;

    @Before
    public void setupSolutionKitManager() {
        // importBundle() requires the following
        solutionKitManager.setRestmanInvoker(restmanInvoker);
        solutionKitManager.setProtectedEntityTracker(protectedEntityTracker);
        solutionKitManager.setProtectedEntityTrackerCallable(callable);
        when(restmanInvoker.getContext(REQUEST)).thenReturn(pec);
    }

    @Test
    public void importBundleSuccess() throws Exception {
        // simulate success response
        String response =
                "<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:Name>Bundle mappings</l7:Name>\n" +
                "    <l7:Type>BUNDLE MAPPINGS</l7:Type>\n" +
                "    <l7:TimeStamp>2015-07-31T09:06:18.772-07:00</l7:TimeStamp>\n" +
                "    <l7:Link rel=\"self\" uri=\"/1.0/bundle?test=true\"/>\n" +
                "    <l7:Resource>\n" +
                "        <l7:Mappings>" +
                "           ..." +
                "        </l7:Mappings>\n" +
                "    </l7:Resource>\n" +
                "</l7:Item>";
        Pair<AssertionStatus, RestmanMessage> restmanResponse = new Pair<>(AssertionStatus.NONE, new RestmanMessage(response));
        when(restmanInvoker.callManagementCheckInterrupted(pec, REQUEST)).thenReturn(restmanResponse);
        when(callable.call()).thenReturn(restmanResponse);
        when(protectedEntityTracker.doWithEntityProtectionDisabled(callable)).thenReturn(restmanResponse);

        Assert.assertEquals(response, solutionKitManager.importBundle(REQUEST, metadata, false));
    }

    @Test
    public void importBundleErrorAssertionStatus() throws Exception {
        // simulate error assertion status
        Pair<AssertionStatus, RestmanMessage> restmanResponse = new Pair<>(AssertionStatus.SERVER_ERROR, null);
        when(restmanInvoker.callManagementCheckInterrupted(pec, REQUEST)).thenReturn(restmanResponse);
        when(callable.call()).thenReturn(restmanResponse);
        when(protectedEntityTracker.doWithEntityProtectionDisabled(callable)).thenReturn(restmanResponse);

        try {
            solutionKitManager.importBundle(REQUEST, metadata, true);
            fail("Expected: server error response.");
        } catch (SolutionKitException e) {
            assertThat(e.getMessage(), CoreMatchers.containsString(restmanResponse.left.getMessage()));
        }
    }

    @Test
    public void importBundleMappingErrors() throws Exception {
        // simulate mapping errors
        String response =
                "<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:Name>Bundle mappings</l7:Name>\n" +
                "    <l7:Type>BUNDLE MAPPINGS</l7:Type>\n" +
                "    <l7:TimeStamp>2015-07-31T11:51:53.700-07:00</l7:TimeStamp>\n" +
                "    <l7:Link rel=\"self\" uri=\"/1.0/bundle?test=true\"/>\n" +
                "    <l7:Resource>\n" +
                "        <l7:Mappings>\n" +
                "            <l7:Mapping action=\"NewOrExisting\" errorType=\"TargetNotFound\" srcId=\"f1649a0664f1ebb6235ac238a6f71a6d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/passwords/f1649a0664f1ebb6235ac238a6f71a6d\" type=\"SECURE_PASSWORD\">\n" +
                "                <l7:Properties>\n" +
                "                    <l7:Property key=\"ErrorMessage\">\n" +
                "                        <l7:StringValue>Could not locate entity: Fail on new specified and could not locate existing target. Source Entity: EntityHeader. Name=mysql_root, id=f1649a0664f1ebb6235ac238a6f71a6d, description=null, type = SECURE_PASSWORD</l7:StringValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"FailOnNew\">\n" +
                "                        <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                </l7:Properties>\n" +
                "            </l7:Mapping>\n" +
                "            <l7:Mapping action=\"NewOrExisting\" errorType=\"TargetNotFound\" srcId=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections/0567c6a8f0c4cc2c9fb331cb03b4de6f\" type=\"JDBC_CONNECTION\">\n" +
                "                <l7:Properties>\n" +
                "                    <l7:Property key=\"ErrorMessage\">\n" +
                "                        <l7:StringValue>Could not locate entity: Fail on new specified and could not locate existing target. Source Entity: EntityHeader. Name=SSG, id=0567c6a8f0c4cc2c9fb331cb03b4de6f, description=null, type = JDBC_CONNECTION</l7:StringValue>\n" +
                "                    </l7:Property>\n" +
                "                    <l7:Property key=\"FailOnNew\">\n" +
                "                        <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "                    </l7:Property>\n" +
                "                </l7:Properties>\n" +
                "            </l7:Mapping>\n" +
                "        </l7:Mappings>\n" +
                "    </l7:Resource>\n" +
                "</l7:Item>";
        Pair<AssertionStatus, RestmanMessage> restmanResponse = new Pair<>(AssertionStatus.NONE, new RestmanMessage(response));
        when(restmanInvoker.callManagementCheckInterrupted(pec, REQUEST)).thenReturn(restmanResponse);
        when(callable.call()).thenReturn(restmanResponse);
        when(protectedEntityTracker.doWithEntityProtectionDisabled(callable)).thenReturn(restmanResponse);

        try {
            solutionKitManager.importBundle(REQUEST, metadata, false);
            fail("Expected: server error response.");
        } catch (SolutionKitException e) {
            assertEquals(response, e.getMessage());
        }
    }

    @Test
    public void importBundleRestmanAccessDeniedManagementResponse() throws Exception {
        // AccessDeniedManagementResponse
        when(protectedEntityTracker.doWithEntityProtectionDisabled(callable)).thenThrow(
                new GatewayManagementDocumentUtilities.AccessDeniedManagementResponse("", ""));
        try {
            //test
            solutionKitManager.importBundle(REQUEST, metadata, false);
            fail("Expected: server error response.");
        } catch (Exception e) {
            assertThat(e, CoreMatchers.instanceOf(SolutionKitException.class));
        }
    }

    @Test
    public void importBundleRestmanInterruptedException() throws Exception {
        // InterruptedException is swallowed in SolutionKitManagerImpl.importBundle(); ignore for now
        when(protectedEntityTracker.doWithEntityProtectionDisabled(callable)).thenThrow(new InterruptedException(""));
        solutionKitManager.importBundle(REQUEST, metadata, false);
    }

    @Test
    public void importBundleRestmanRuntimeException() throws Exception {
        // RuntimeException
        when(protectedEntityTracker.doWithEntityProtectionDisabled(callable)).thenThrow(new RuntimeException(""));
        try {
            //test
            solutionKitManager.importBundle(REQUEST, metadata, false);
            fail("Expected: server error response.");
        } catch (Exception e) {
            assertThat(e, CoreMatchers.instanceOf(RuntimeException.class));
        }
    }

    @Test
    public void importBundleRestmanUnexpectedManagementResponse() throws Exception {
        // UnexpectedManagementResponse
        when(protectedEntityTracker.doWithEntityProtectionDisabled(callable)).thenThrow(
                new GatewayManagementDocumentUtilities.UnexpectedManagementResponse(""));
        try {
            //test
            solutionKitManager.importBundle(REQUEST, metadata, false);
            fail("Expected: server error response.");
        } catch (Exception e) {
            assertThat(e, CoreMatchers.instanceOf(GatewayManagementDocumentUtilities.UnexpectedManagementResponse.class));
        }
    }

    @Test
    public void findBySolutionKitGuidAndIMSuccess() throws Exception{
        SolutionKitManagerImpl skManager = Mockito.spy(new SolutionKitManagerImpl());
        List<SolutionKit> solutionKits = new ArrayList<>();
        SolutionKit solutionKit1 = new SolutionKit();
        solutionKit1.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "one");
        SolutionKit solutionKit2 = new SolutionKit();
        solutionKit2.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "two");
        solutionKits.add(solutionKit1);
        solutionKits.add(solutionKit2);

        Mockito.doReturn(solutionKits).when(skManager).findBySolutionKitGuid("mock guid");

        //test
        SolutionKit solutionKitReturned = skManager.findBySolutionKitGuidAndIM("mock guid", "one");

        //expect the solutionKit with IM "one" is returned
        assertEquals(solutionKitReturned, solutionKit1);
    }

    @Test
    public void findBySolutionKitGuidNoMatch() throws Exception{
        SolutionKitManagerImpl skManager = Mockito.spy(new SolutionKitManagerImpl());
        List<SolutionKit> solutionKits = new ArrayList<>();
        SolutionKit solutionKit1 = new SolutionKit();
        solutionKit1.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "one");
        SolutionKit solutionKit2 = new SolutionKit();
        solutionKit2.setProperty(SolutionKit.SK_PROP_INSTANCE_MODIFIER_KEY, "two");
        solutionKits.add(solutionKit1);
        solutionKits.add(solutionKit2);

        Mockito.doReturn(solutionKits).when(skManager).findBySolutionKitGuid("mock guid");

        //test
        SolutionKit solutionKitReturned = skManager.findBySolutionKitGuidAndIM("mock guid", "three");

        //expect that no solution kit is returned
        assertEquals(solutionKitReturned, null);
    }

    @Test
    public void convertToHTListSuccess() {
        SolutionKit solutionKit1 = new SolutionKit();
        solutionKit1.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aafe");
        SolutionKit solutionKit2 = new SolutionKit();
        solutionKit2.setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaaa");
        List<SolutionKit> solutionKits = new ArrayList<>(2);
        solutionKits.add(solutionKit1);
        solutionKits.add(solutionKit2);

        //test
        List<SolutionKitHeader> solutionKitHeaders = solutionKitManager.convertToHTList(solutionKits);

        //expect solutionKit to be converted to solutionKitHeaders
        assertEquals(solutionKitHeaders.get(0).getSolutionKitGuid(), solutionKit1.getSolutionKitGuid());
        assertEquals(solutionKitHeaders.get(1).getSolutionKitGuid(), solutionKit2.getSolutionKitGuid());

    }
}
