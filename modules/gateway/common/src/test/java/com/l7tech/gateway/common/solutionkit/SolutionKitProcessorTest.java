package com.l7tech.gateway.common.solutionkit;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.Triple;
import org.hamcrest.CoreMatchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Solution Kit Processor Tests
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitProcessorTest {
    private static SolutionKitsConfig solutionKitsConfig;
    private static SolutionKitAdmin solutionKitAdmin;
    private static SkarProcessor skarProcessor;

    @BeforeClass
    public static void load() throws Exception {
        solutionKitsConfig = mock(SolutionKitsConfig.class);
        solutionKitAdmin = mock(SolutionKitAdmin.class);
        skarProcessor = mock(SkarProcessor.class);
    }

    @Test
    public void testInstallOrUpgrade() throws Throwable {

        // solution kits for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        SolutionKit solutionKit1 = new SolutionKit();
        solutionKit1.setName("SK1");
        selectedSolutionKits.add(solutionKit1);
        SolutionKit solutionKit2 = new SolutionKit();
        solutionKit2.setName("SK2");
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        // install or upgrade
        final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(solutionKitsConfig, solutionKitAdmin, skarProcessor);
        final AtomicBoolean doTestInstallExecuted = new AtomicBoolean(false);
        solutionKitProcessor.testInstallOrUpgrade(true, new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
            @Override
            public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                doTestInstallExecuted.set(true);
            }
        });

        // make sure doTestInstall was executed
        assertTrue("Expected testInstallOrUpgrade() to have executed the doTestInstall code.", doTestInstallExecuted.get());

        // make sure custom callbacks are invoked
        verify(skarProcessor, times(numberOfSolutionKits)).invokeCustomCallback(any(SolutionKit.class));

        // make sure updateResolvedMappingsIntoBundle called
        verify(solutionKitsConfig, times(numberOfSolutionKits)).updateResolvedMappingsIntoBundle(any(SolutionKit.class), anyBoolean());

        // fail with BadRequestException
        when(solutionKitsConfig.isUpgrade()).thenReturn(true);
        try {
            solutionKitProcessor.testInstallOrUpgrade(true, new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Throwable>() {
                @Override
                public void call(Triple<SolutionKit, String, Boolean> loaded) throws Throwable {
                    // do nothing
                }
            });
            fail("Expected upgrade with no provided upgrade information to fail with BadRequestException.");
        } catch (BadRequestException e) {
            assertThat(e.getMessage(), CoreMatchers.endsWith("cannot be used for upgrade due to that its SKAR file does not include UpgradeBundle.xml."));
        }
    }

    @Test
    public void installOrUpgrade() throws Exception {

        // need local mock to avoid state contamination from other tests
        SolutionKitsConfig solutionKitsConfig = mock(SolutionKitsConfig.class);
        SolutionKitAdmin solutionKitAdmin = mock(SolutionKitAdmin.class);

        // solution kits for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        SolutionKit solutionKit1 = new SolutionKit();
        solutionKit1.setName("SK1");
        selectedSolutionKits.add(solutionKit1);
        SolutionKit solutionKit2 = new SolutionKit();
        solutionKit2.setName("SK2");
        selectedSolutionKits.add(solutionKit2);

        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);
        when(skarProcessor.getAsSolutionKitTriple(solutionKit1)).thenReturn(new Triple<>(solutionKit1, "doesn't matter", false));
        when(skarProcessor.getAsSolutionKitTriple(solutionKit2)).thenReturn(new Triple<>(solutionKit2, "doesn't matter", false));
        final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(solutionKitsConfig, solutionKitAdmin, skarProcessor);
        solutionKitProcessor.installOrUpgrade(null, null);

        // make sure updateResolvedMappingsIntoBundle() called
        verify(solutionKitsConfig).updateResolvedMappingsIntoBundle(solutionKit1);
        verify(solutionKitsConfig).updateResolvedMappingsIntoBundle(solutionKit2);

        // make sure solutionKitAdmin.install() called
        verify(solutionKitAdmin, times(numberOfSolutionKits)).install(any(SolutionKit.class), anyString(), anyBoolean());

        // test doAsyncInstall was executed (when provided)
        final AtomicBoolean doAsyncInstallExecuted = new AtomicBoolean(false);
        solutionKitProcessor.installOrUpgrade(null, new Functions.UnaryVoidThrows<Triple<SolutionKit, String, Boolean>, Exception>() {
            @Override
            public void call(Triple<SolutionKit, String, Boolean> loaded) throws Exception {
                doAsyncInstallExecuted.set(true);
            }
        });
        assertTrue("Expected installOrUpgrade() to have executed the doAsyncInstall code.", doAsyncInstallExecuted.get());

        // test solutionKitAdmin.install() throws exception
        when(solutionKitAdmin.install(any(SolutionKit.class), anyString(), anyBoolean())).thenThrow(new Exception());
        try {
            solutionKitProcessor.installOrUpgrade(null, null);
            fail("Expected installOrUpgrade(...) to throw Exception.");
        } catch (Exception e) {
            // do nothing, expected
        }

        // test exceptions can be returned as an error list (when provided)
        final List<Pair<String, SolutionKit>> errorKitList = new ArrayList<>(2);
        try {
            solutionKitProcessor.installOrUpgrade(errorKitList, null);
            assertEquals(2, errorKitList.size());
        } catch (Exception e) {
            fail("Expected exceptions from installOrUpgrade(...) can be returned as an error list.");
        }
    }

    @Test
    public void installOrUpgradeWithParent() throws Exception {

        // need local mock to avoid state contamination from other tests
        SolutionKitsConfig solutionKitsConfig = mock(SolutionKitsConfig.class);
        SolutionKitAdmin solutionKitAdmin = mock(SolutionKitAdmin.class);

        // parent skar for the test
        SolutionKit parentSolutionKit = new SolutionKit();
        parentSolutionKit.setName("ParentSK");
        parentSolutionKit.setSolutionKitGuid("zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz");
        parentSolutionKit.setGoid(new Goid(0, 1));
        when(solutionKitsConfig.getParentSolutionKitLoaded()).thenReturn(parentSolutionKit);

        // skar of skar for the test
        final int numberOfSolutionKits = 2;
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(numberOfSolutionKits);
        SolutionKit solutionKit1 = new SolutionKit();
        solutionKit1.setName("SK1");
        solutionKit1.setParentGoid(parentSolutionKit.getGoid());
        selectedSolutionKits.add(solutionKit1);
        SolutionKit solutionKit2 = new SolutionKit();
        solutionKit2.setName("SK2");
        solutionKit2.setParentGoid(parentSolutionKit.getGoid());
        selectedSolutionKits.add(solutionKit2);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);
        when(skarProcessor.getAsSolutionKitTriple(solutionKit1)).thenReturn(new Triple<>(solutionKit1, "doesn't matter", false));
        when(skarProcessor.getAsSolutionKitTriple(solutionKit2)).thenReturn(new Triple<>(solutionKit2, "doesn't matter", false));

        final SolutionKitProcessor solutionKitProcessor = new SolutionKitProcessor(solutionKitsConfig, solutionKitAdmin, skarProcessor);

        // test parent not yet saved on Gateway calls solutionKitAdmin.save()
        solutionKitProcessor.installOrUpgrade(null, null);
        verify(solutionKitAdmin).save(parentSolutionKit);

        // test parent already saved on Gateway calls solutionKitAdmin.update() - install code path
        when(solutionKitAdmin.find(parentSolutionKit.getSolutionKitGuid())).thenReturn(Collections.singleton(parentSolutionKit));
        solutionKitProcessor.installOrUpgrade(null, null);
        verify(solutionKitAdmin).update(parentSolutionKit);

        // test parent already saved on Gateway calls solutionKitAdmin.update() - upgrade code path
        when(solutionKitsConfig.isUpgrade()).thenReturn(true);
        final List<SolutionKit> solutionKitsToUpgrade = new ArrayList<>(3);
        solutionKitsToUpgrade.add(parentSolutionKit);
        solutionKitsToUpgrade.add(solutionKit1);
        solutionKitsToUpgrade.add(solutionKit2);
        when(solutionKitsConfig.getSolutionKitsToUpgrade()).thenReturn(solutionKitsToUpgrade);
        solutionKitProcessor.installOrUpgrade(null, null);
        verify(solutionKitAdmin, times(2)).update(parentSolutionKit);
    }
}