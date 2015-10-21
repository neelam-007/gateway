package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.signer.SignatureVerifier;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.UUID;

/**
 * {@code SolutionKitAdminHelper} unit test.<br/>
 * Feel free to add more {@code SolutionKitAdminHelper} tests here.
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitAdminHelperTest {

    @Mock
    private static SignatureVerifier signatureVerifier;
    @Mock
    private LicenseManager licenseManager;
    @Mock
    private SolutionKitManager solutionKitManager;

    private SolutionKitAdminHelper solutionKitAdminHelper;

    @Before
    public void before() throws Exception {
        solutionKitAdminHelper = new SolutionKitAdminHelper(licenseManager, solutionKitManager, signatureVerifier);
        Assert.assertNotNull("SolutionKitAdminHelper is created", solutionKitAdminHelper);
    }


    @Test
    public void testUpdateEntityOwnershipDescriptors() throws Exception {
        String restmanMappings =
                "<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                        "    <l7:Name>Bundle mappings</l7:Name>\n" +
                        "    <l7:Type>BUNDLE MAPPINGS</l7:Type>\n" +
                        "    <l7:TimeStamp>2015-10-13T16:46:35.422-07:00</l7:TimeStamp>\n" +
                        "    <l7:Link rel=\"self\" uri=\"/1.0/bundle?versionComment=Simple+Server+Module+File+%28v1.1%29\"/>\n" +
                        "    <l7:Resource>\n" +
                        "        <l7:Mappings>\n" +
                        "            <l7:Mapping action=\"AlwaysCreateNew\" actionTaken=\"CreatedNew\" srcId=\"37953ab3e89a8b7df721c055d4aeb6a1\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a1\" targetId=\"37953ab3e89a8b7df721c055d4aeb6a1\" targetUri=\"/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a1\" type=\"SERVER_MODULE_FILE\">\n" +
                        "                <l7:Properties>\n" +
                        "                    <l7:Property key=\"MapBy\"><l7:StringValue>moduleSha256</l7:StringValue></l7:Property>\n" +
                        "                    <l7:Property key=\"SK_ReadOnlyEntity\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                        "                </l7:Properties>\n" +
                        "            </l7:Mapping>\n" +
                        "            <l7:Mapping action=\"NewOrExisting\" actionTaken=\"UsedExisting\" srcId=\"37953ab3e89a8b7df721c055d4aeb6a2\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a2\" targetId=\"37953ab3e89a8b7df721c055d4aeb6a2\" targetUri=\"/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a2\" type=\"SECURE_PASSWORD\">\n" +
                        "                <l7:Properties>\n" +
                        "                    <l7:Property key=\"FailOnNew\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                        "                    <l7:Property key=\"SK_ReadOnlyEntity\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +  // should be ignored
                        "                </l7:Properties>\n" +
                        "            </l7:Mapping>\n" +
                        "            <l7:Mapping action=\"NewOrExisting\" actionTaken=\"CreatedNew\" srcId=\"37953ab3e89a8b7df721c055d4aeb6a3\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a3\" targetId=\"37953ab3e89a8b7df721c055d4aeb6a3\" targetUri=\"/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a3\" type=\"POLICY\">\n" +
                        "                <l7:Properties>\n" +
                        "                    <l7:Property key=\"SK_ReadOnlyEntity\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                        "                </l7:Properties>\n" +
                        "            </l7:Mapping>\n" +
                        "            <l7:Mapping action=\"NewOrExisting\" actionTaken=\"CreatedNew\" srcId=\"37953ab3e89a8b7df721c055d4aeb6a4\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a4\" targetId=\"37953ab3e89a8b7df721c055d4aeb6a4\" targetUri=\"/1.0/serverModuleFiles/37953ab3e89a8b7df721c055d4aeb6a4\" type=\"ENCAPSULATED_ASSERTION\"/>\n" +
                        "        </l7:Mappings>\n" +
                        "    </l7:Resource>\n" +
                        "</l7:Item>";
        final Goid newGoid = new Goid(0, 1);

        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doAnswer(
                new Answer() {
                    @Override
                    public Object answer(final InvocationOnMock invocation) throws Throwable {
                        Assert.assertThat("Two params", invocation.getArguments().length, Matchers.is(2));
                        final Object param1 = invocation.getArguments()[0];
                        Assert.assertThat("First Param is Goid", param1, Matchers.instanceOf(Goid.class));
                        final Goid goid = (Goid)param1;
                        Assert.assertThat(goid, Matchers.notNullValue());
                        // for now just make sure the kit is not null
                        final Object param2 = invocation.getArguments()[1];
                        Assert.assertThat("Second Param is SolutionKit", param2, Matchers.instanceOf(SolutionKit.class));
                        final SolutionKit solutionKit = (SolutionKit)param2;
                        Assert.assertThat(solutionKit, Matchers.notNullValue());
                        return goid;
                    }
                }
        ).when(solutionKitManager).save(Mockito.any(Goid.class), Mockito.any(SolutionKit.class));

        // create a sample Kit without any ownership descriptors
        final SolutionKit solutionKit = new SolutionKitBuilder()
                .name("sk1")
                .skGuid(UUID.randomUUID().toString())
                .skVersion("1.0")
                .build();

        // mock save to return Goid(0, 1)
        Mockito.doReturn(newGoid).when(solutionKitManager).save(solutionKit);

        // make sure both mappings and uninstall bundle are empty
        Assert.assertNull(solutionKit.getMappings());
        Assert.assertNull(solutionKit.getUninstallBundle());
        // make sure initial solutionKit doesn't have any entity ownership descriptors
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.anyOf(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class), Matchers.nullValue()));

        final Goid goid = solutionKitAdminHelper.install(solutionKit, "", false);
        Assert.assertEquals(goid, newGoid);
        // make sure the mappings are set
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

        // test entity ownership descriptors
        // SERVER_MODULE_FILE       - 37953ab3e89a8b7df721c055d4aeb6a1 - read-only
        // POLICY                   - 37953ab3e89a8b7df721c055d4aeb6a3 - read-only
        // ENCAPSULATED_ASSERTION   - 37953ab3e89a8b7df721c055d4aeb6a4 - not read-only
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.hasSize(3), Matchers.notNullValue()));
        for (final EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
            Assert.assertThat(descriptor.getSolutionKit(), Matchers.sameInstance(solutionKit));
            Assert.assertThat(
                    descriptor.getEntityId(),
                    Matchers.anyOf(
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a1"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a3"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a4")
                    )
            );
            switch (descriptor.getEntityId()) {
                case "37953ab3e89a8b7df721c055d4aeb6a1":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVER_MODULE_FILE));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a3":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.POLICY));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a4":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.ENCAPSULATED_ASSERTION));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                default:
                    Assert.fail("Unexpected entity id: " + descriptor.getEntityId());
                    break;
            }
        }
    }

    // TODO: add more tests here
}