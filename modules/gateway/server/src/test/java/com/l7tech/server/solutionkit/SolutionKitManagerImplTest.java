package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitException;
import com.l7tech.gateway.common.solutionkit.SolutionKitHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanInvoker;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Pair;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    @Test
    public void testUpdateProtectedEntityTracking() throws Exception {
        final SolutionKitManagerImpl skManager = Mockito.spy(new SolutionKitManagerImpl());
        skManager.setProtectedEntityTracker(protectedEntityTracker);

        // our sample kits
        final Collection<SolutionKit> solutionKits = new ArrayList<>();
        // mock findAll()
        Mockito.doAnswer(new Answer<Collection<SolutionKit>>() {
            @Override
            public Collection<SolutionKit> answer(final InvocationOnMock invocationOnMock) throws Throwable {
                Assert.assertNotNull(invocationOnMock);
                Assert.assertThat(invocationOnMock.getArguments(), Matchers.arrayWithSize(0));
                return solutionKits;
            }
        }).when(skManager).findAll();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // create our sample list:
        //
        // sk1
        //    entity id1 -> FOLDER           -> true  -> 100
        //    entity id2 -> SCHEDULED_TASK   -> false -> 101
        //    entity id3 -> SERVICE          -> true  -> 102
        //    entity id5 -> SECURE_PASSWORD  -> true  -> 103
        //    entity id6 -> CLUSTER_PROPERTY -> false -> 104
        //
        // sk2
        //    entity id1 -> FOLDER           -> false -> 99
        //    entity id2 -> SCHEDULED_TASK   -> true  -> 102
        //    entity id4 -> SERVICE          -> false -> 103
        //    entity id5 -> SECURE_PASSWORD  -> false -> 104
        //    entity id6 -> CLUSTER_PROPERTY -> true  -> 103
        // ----------------------------------------------------------
        // expected result:
        // id1 -> FOLDER            -> true    -> sk1->100  (its grater than sk2->99)
        // id2 -> SCHEDULED_TASK    -> true    -> sk2->102  (grater than sk1->101)
        // id3 -> SERVICE           -> true    -> sk1->102  (the only one)
        // id4 -> SERVICE           -> false   -> sk2->103  (the only one)
        // id5 -> SECURE_PASSWORD   -> false   -> sk2->104  (grater than sk2->103)
        // id6 -> CLUSTER_PROPERTY  -> false   -> sk1->104  (grater than sk2->103)
        //
        doTestUpdateProtectedEntityTracking(
                skManager,
                solutionKits,
                CollectionUtils.list(
                        new SolutionKit() {{
                            setGoid(new Goid(0, 1));
                            setName("sk1");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf1");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id1", EntityType.FOLDER, true) {{
                                        setVersionStamp(100);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id2", EntityType.SCHEDULED_TASK, false) {{
                                        setVersionStamp(101);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id3", EntityType.SERVICE, true) {{
                                        setVersionStamp(102);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id5", EntityType.SECURE_PASSWORD, true) {{
                                        setVersionStamp(103);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id6", EntityType.CLUSTER_PROPERTY, false) {{
                                        setVersionStamp(104);
                                    }}
                            ));
                        }},
                        new SolutionKit() {{
                            setGoid(new Goid(0, 2));
                            setName("sk2");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf2");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id1", EntityType.FOLDER, false) {{
                                        setVersionStamp(99);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id2", EntityType.SCHEDULED_TASK, true) {{
                                        setVersionStamp(102);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id4", EntityType.SERVICE, false) {{
                                        setVersionStamp(103);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id5", EntityType.SECURE_PASSWORD, false) {{
                                        setVersionStamp(104);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id6", EntityType.CLUSTER_PROPERTY, true) {{
                                        setVersionStamp(103);
                                    }}
                            ));
                        }}
                ),
                CollectionUtils.list(
                        Pair.pair(EntityType.FOLDER, "id1"),
                        Pair.pair(EntityType.SCHEDULED_TASK, "id2"),
                        Pair.pair(EntityType.SERVICE, "id3")
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // same test but with different types
        //
        // sk1
        //    entity id1 -> EMAIL_LISTENER      -> true  -> 100
        //    entity id2 -> SCHEDULED_TASK      -> false -> 101
        //    entity id3 -> SERVICE             -> true  -> 102
        //    entity id5 -> SECURE_PASSWORD     -> true  -> 103
        //    entity id6 -> POLICY              -> false -> 104
        //
        // sk2
        //    entity id1 -> FOLDER              -> false -> 99
        //    entity id2 -> SERVER_MODULE_FILE  -> true  -> 102
        //    entity id4 -> SERVICE             -> false -> 103
        //    entity id5 -> POLICY              -> false -> 104
        //    entity id6 -> CLUSTER_PROPERTY    -> true  -> 103
        // ----------------------------------------------------------
        // expected result:
        // id1 -> EMAIL_LISTENER        -> true    -> sk1->100  (its grater than sk2->99)
        // id2 -> SERVER_MODULE_FILE    -> true    -> sk2->102  (grater than sk1->101)
        // id3 -> SERVICE               -> true    -> sk1->102  (the only one)
        // id4 -> SERVICE               -> false   -> sk2->103  (the only one)
        // id5 -> POLICY                -> false   -> sk2->104  (grater than sk2->103)
        // id6 -> POLICY                -> false   -> sk1->104  (grater than sk2->103)
        //
        doTestUpdateProtectedEntityTracking(
                skManager,
                solutionKits,
                CollectionUtils.list(
                        new SolutionKit() {{
                            setGoid(new Goid(0, 1));
                            setName("sk1");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf1");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id1", EntityType.EMAIL_LISTENER, true) {{
                                        setVersionStamp(100);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id2", EntityType.SCHEDULED_TASK, false) {{
                                        setVersionStamp(101);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id3", EntityType.SERVICE, true) {{
                                        setVersionStamp(102);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id5", EntityType.SECURE_PASSWORD, true) {{
                                        setVersionStamp(103);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id6", EntityType.POLICY, false) {{
                                        setVersionStamp(104);
                                    }}
                            ));
                        }},
                        new SolutionKit() {{
                            setGoid(new Goid(0, 2));
                            setName("sk2");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf2");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id1", EntityType.FOLDER, false) {{
                                        setVersionStamp(99);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id2", EntityType.SERVER_MODULE_FILE, true) {{
                                        setVersionStamp(102);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id4", EntityType.SERVICE, false) {{
                                        setVersionStamp(103);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id5", EntityType.POLICY, false) {{
                                        setVersionStamp(104);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id6", EntityType.CLUSTER_PROPERTY, true) {{
                                        setVersionStamp(103);
                                    }}
                            ));
                        }}

                ),
                CollectionUtils.list(
                        Pair.pair(EntityType.EMAIL_LISTENER, "id1"),
                        Pair.pair(EntityType.SERVER_MODULE_FILE, "id2"),
                        Pair.pair(EntityType.SERVICE, "id3")
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // same test but with same version_stamps
        //
        // sk1
        //    entity id1 -> EMAIL_LISTENER      -> true  -> 100
        //    entity id2 -> SCHEDULED_TASK      -> false -> 100
        //    entity id3 -> SERVICE             -> true  -> 100
        //    entity id5 -> SECURE_PASSWORD     -> true  -> 100
        //    entity id6 -> POLICY              -> false -> 100
        //
        // sk2
        //    entity id1 -> FOLDER              -> false -> 100
        //    entity id2 -> SERVER_MODULE_FILE  -> true  -> 100
        //    entity id4 -> SERVICE             -> false -> 100
        //    entity id5 -> POLICY              -> false -> 100
        //    entity id6 -> CLUSTER_PROPERTY    -> true  -> 100
        // ----------------------------------------------------------
        // expected result:
        // id1 -> FOLDER                -> false   -> sk2->100
        // id2 -> SERVER_MODULE_FILE    -> true    -> sk2->100
        // id3 -> SERVICE               -> true    -> sk1->100
        // id4 -> SERVICE               -> false   -> sk2->100
        // id5 -> POLICY                -> false   -> sk2->100
        // id6 -> CLUSTER_PROPERTY      -> true    -> sk2->100
        //
        final long version_stamp = 100;
        doTestUpdateProtectedEntityTracking(
                skManager,
                solutionKits,
                CollectionUtils.list(
                        new SolutionKit() {{
                            setGoid(new Goid(0, 1));
                            setName("sk1");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf1");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id1", EntityType.EMAIL_LISTENER, true) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id2", EntityType.SCHEDULED_TASK, false) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id3", EntityType.SERVICE, true) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id5", EntityType.SECURE_PASSWORD, true) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id6", EntityType.POLICY, false) {{
                                        setVersionStamp(version_stamp);
                                    }}
                            ));
                        }},
                        new SolutionKit() {{
                            setGoid(new Goid(0, 2));
                            setName("sk2");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf2");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id1", EntityType.FOLDER, false) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id2", EntityType.SERVER_MODULE_FILE, true) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id4", EntityType.SERVICE, false) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id5", EntityType.POLICY, false) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id6", EntityType.CLUSTER_PROPERTY, true) {{
                                        setVersionStamp(version_stamp);
                                    }}
                            ));
                        }}

                ),
                CollectionUtils.list(
                        Pair.pair(EntityType.SERVER_MODULE_FILE, "id2"),
                        Pair.pair(EntityType.SERVICE, "id3"),
                        Pair.pair(EntityType.CLUSTER_PROPERTY, "id6")
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // another test with multiple skars without duplicate entities
        //
        // sk1
        //    entity id11 -> EMAIL_LISTENER      -> true  -> 100
        //    entity id12 -> SCHEDULED_TASK      -> false -> 100
        //    entity id13 -> SERVICE             -> true  -> 100
        //    entity id14 -> SECURE_PASSWORD     -> true  -> 100
        //
        // sk2
        //    entity id21 -> FOLDER              -> false -> 100
        //    entity id22 -> SERVER_MODULE_FILE  -> true  -> 100
        //    entity id23 -> SERVICE             -> false -> 100
        //
        // sk3
        //    entity id31 -> POLICY              -> false -> 100
        //    entity id32 -> SECURE_PASSWORD     -> true  -> 100
        // ----------------------------------------------------------
        // expected result:
        // id11 -> EMAIL_LISTENER      -> true  -> 100
        // id12 -> SCHEDULED_TASK      -> false -> 100
        // id13 -> SERVICE             -> true  -> 100
        // id14 -> SECURE_PASSWORD     -> true  -> 100
        // id21 -> FOLDER              -> false -> 100
        // id22 -> SERVER_MODULE_FILE  -> true  -> 100
        // id23 -> SERVICE             -> false -> 100
        // id31 -> POLICY              -> false -> 100
        // id32 -> SECURE_PASSWORD     -> true  -> 100
        //
        //
        doTestUpdateProtectedEntityTracking(
                skManager,
                solutionKits,
                CollectionUtils.list(
                        new SolutionKit() {{
                            setGoid(new Goid(0, 1));
                            setName("sk1");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf1");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id11", EntityType.EMAIL_LISTENER, true) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id12", EntityType.SCHEDULED_TASK, false) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id13", EntityType.SERVICE, true) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id14", EntityType.SECURE_PASSWORD, true) {{
                                        setVersionStamp(version_stamp);
                                    }}
                            ));
                        }},
                        new SolutionKit() {{
                            setGoid(new Goid(0, 2));
                            setName("sk2");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf2");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id21", EntityType.FOLDER, false) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id22", EntityType.SERVER_MODULE_FILE, true) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id23", EntityType.SERVICE, false) {{
                                        setVersionStamp(version_stamp);
                                    }}
                            ));
                        }},
                        new SolutionKit() {{
                            setGoid(new Goid(0, 3));
                            setName("sk3");
                            setSolutionKitGuid("1f87436b-7ca5-41c8-9418-21d7a784aaf3");
                            setSolutionKitVersion("1.0");
                            setEntityOwnershipDescriptors(CollectionUtils.list(
                                    new EntityOwnershipDescriptor(this, "id31", EntityType.POLICY, false) {{
                                        setVersionStamp(version_stamp);
                                    }},
                                    new EntityOwnershipDescriptor(this, "id32", EntityType.SECURE_PASSWORD, true) {{
                                        setVersionStamp(version_stamp);
                                    }}
                            ));
                        }}
                ),
                CollectionUtils.list(
                        Pair.pair(EntityType.EMAIL_LISTENER, "id11"),
                        Pair.pair(EntityType.SERVICE, "id13"),
                        Pair.pair(EntityType.SECURE_PASSWORD, "id14"),
                        Pair.pair(EntityType.SERVER_MODULE_FILE, "id22"),
                        Pair.pair(EntityType.SECURE_PASSWORD, "id32")
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    private void doTestUpdateProtectedEntityTracking(
            final SolutionKitManagerImpl skManager,
            final Collection<SolutionKit> solutionKits,
            final Collection<SolutionKit> sampleData,
            final Collection<Pair<EntityType, String>> expectedRedOnlyEntities
    ) throws Exception {
        Assert.assertNotNull(skManager);
        Assert.assertNotNull(solutionKits);
        Assert.assertNotNull(sampleData);
        Assert.assertNotNull(expectedRedOnlyEntities);

        // clear existing demo data and fill in with sampleData
        solutionKits.clear();
        solutionKits.addAll(sampleData);

        // force updateProtectedEntityTracking
        skManager.handleEvent(new EntityInvalidationEvent(this, SolutionKit.class, new Goid[0], new char[0]));
        // make sure bulkUpdateReadOnlyEntitiesList is called with the correct list
        final ArgumentCaptor<Collection> argument = ArgumentCaptor.forClass(Collection.class);
        //noinspection unchecked
        Mockito.verify(protectedEntityTracker, Mockito.atLeastOnce()).bulkUpdateReadOnlyEntitiesList(argument.capture());
        //noinspection unchecked
        final Collection<Pair<EntityType, String>> readOnlyEntities = argument.getValue();
        Assert.assertThat(readOnlyEntities, Matchers.allOf(Matchers.hasSize(expectedRedOnlyEntities.size()), Matchers.notNullValue()));
        Assert.assertThat(
                readOnlyEntities,
                Matchers.containsInAnyOrder(
                        expectedRedOnlyEntities.toArray()
                )
        );
    }
}
