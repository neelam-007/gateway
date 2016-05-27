package com.l7tech.server.processcontroller.patching.client;

import com.l7tech.server.processcontroller.patching.PatchServiceApi;
import com.l7tech.server.processcontroller.patching.PatchStatus;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

/**
 * Added to test the new list -sort arg
 * TODO: add more tests as needed
 */
@RunWith(MockitoJUnitRunner.class)
public class PatchActionTest {

    @Mock
    PatchServiceApi api;

    PatchCli.PatchAction listAction;
    List<PatchStatus> sampleApiPatchStatuses;

    @Before
    public void setUp() throws Exception {
        // use set for random order
        long timer = 10000L;
        sampleApiPatchStatuses = new ArrayList<>(
                Arrays.asList(
                        /*0*/ createPatchStatusMock("b", "desc for b", PatchStatus.State.INSTALLED, ++timer, null, null),
                        /*1*/ createPatchStatusMock("a", "desc for a", PatchStatus.State.UPLOADED, ++timer, null, null),
                        /*2*/ createPatchStatusMock("d", "desc for d", PatchStatus.State.ERROR, ++timer, null, null),
                        /*3*/ createPatchStatusMock("1", "desc for 1", PatchStatus.State.INSTALLED, ++timer, null, null),
                        /*4*/ createPatchStatusMock("z1", "desc for z1", PatchStatus.State.ROLLED_BACK, ++timer, null, "a"),
                        /*5*/ createPatchStatusMock("c", "desc for c", PatchStatus.State.UPLOADED, ++timer, null, null),
                        /*6*/ createPatchStatusMock("d1", "desc for d1", PatchStatus.State.ERROR, ++timer, null, "some status message for d1"),
                        /*7*/ createPatchStatusMock("z", "desc for d1", PatchStatus.State.ROLLED_BACK, ++timer, "1", "some status message for z"),
                        /*8*/ createPatchStatusMock("aa", "desc for aa", PatchStatus.State.INSTALLED, ++timer, null, null),
                        /*9*/ createPatchStatusMock("bb", "desc for bb", PatchStatus.State.UPLOADED, ++timer, null, null)
                )
        );
        Mockito.doReturn(sampleApiPatchStatuses).when(api).listPatches();
    }

    @After
    public void tearDown() throws Exception {
        cleanAction(listAction);
    }

    private static PatchStatus createPatchStatusMock(
            final String id,
            final String desc,
            final PatchStatus.State state,
            final Long last_mod,
            final String rollbackForId,
            final String statusMsg
    ) throws Exception {
        final PatchStatus patchStatus = PatchStatus.newPatchStatus(id, desc, state);
        if (last_mod != null)
            patchStatus.setField(PatchStatus.Field.LAST_MOD, last_mod.toString());
        if (rollbackForId != null)
            patchStatus.setField(PatchStatus.Field.ROLLBACK_FOR_ID, rollbackForId);
        if (statusMsg != null)
            patchStatus.setField(PatchStatus.Field.STATUS_MSG, statusMsg);
        return patchStatus;
    }

    @Test
    public void testListSortArg() throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList("list", "-sort", "INSTALLED::UPLOADED|ID::-LAST_MOD", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("INSTALLED::UPLOADED|ID::-LAST_MOD"));
        List<PatchStatus> statuses = new ArrayList<>(listAction.call(api));
        Assert.assertThat(statuses, Matchers.hasSize(sampleApiPatchStatuses.size()));
        // expected order should be 3, 8, 0, 9, 5, 1, 4, 7, 2, 6
        doTestExpectedValues(
                statuses,
                Arrays.asList(
                        Pair.pair("1", "10004"),
                        Pair.pair("aa", "10009"),
                        Pair.pair("b", "10001"),
                        Pair.pair("bb", "10010"),
                        Pair.pair("c", "10006"),
                        Pair.pair("a", "10002"),
                        Pair.pair("z1", "10005"),
                        Pair.pair("z", "10008"),
                        Pair.pair("d", "10003"),
                        Pair.pair("d1", "10007")
                )
        );
        doTestExpectedValuesEx(
                sampleApiPatchStatuses,
                statuses,
                Arrays.asList(3, 8, 0, 9, 5, 1, 4, 7, 2, 6)
        );
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "INSTALLED::UPLOADED|-ID::LAST_MOD", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("INSTALLED::UPLOADED|-ID::LAST_MOD"));
        statuses = new ArrayList<>(listAction.call(api));
        Assert.assertThat(statuses, Matchers.hasSize(sampleApiPatchStatuses.size()));
        doTestExpectedValuesEx(
                sampleApiPatchStatuses,
                statuses,
                Arrays.asList(0, 8, 3, 1, 5, 9, 4, 7, 2, 6)

        );
        cleanAction(listAction);


        args = new ArrayList<>(Arrays.asList("list", "-sort", "ID", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("ID"));
        statuses = new ArrayList<>(listAction.call(api));
        Assert.assertThat(statuses, Matchers.hasSize(sampleApiPatchStatuses.size()));
        doTestExpectedValuesEx(
                sampleApiPatchStatuses,
                statuses,
                Arrays.asList(3, 1, 8, 0, 9, 5, 2, 6, 7, 4)
        );
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "-ID", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("-ID"));
        statuses = new ArrayList<>(listAction.call(api));
        Assert.assertThat(statuses, Matchers.hasSize(sampleApiPatchStatuses.size()));
        doTestExpectedValuesEx(
                sampleApiPatchStatuses,
                statuses,
                Arrays.asList(4, 7, 6, 2, 5, 9, 0, 8, 1, 3)

        );
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "INSTALLED::UPLOADED|-ID::LAST_MOD|something", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("INSTALLED::UPLOADED|-ID::LAST_MOD|something"));
        statuses = new ArrayList<>(listAction.call(api));
        Assert.assertThat(statuses, Matchers.hasSize(sampleApiPatchStatuses.size()));
        doTestExpectedValuesEx(
                sampleApiPatchStatuses,
                statuses,
                Arrays.asList(0, 8, 3, 1, 5, 9, 4, 7, 2, 6)

        );
        cleanAction(listAction);
    }

    @Test
    public void testListSortArgErrorCases() throws Exception {
        List<String> args = new ArrayList<>(Arrays.asList("list"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.isEmptyOrNullString());
        Assert.assertThat(listAction.sortingFormat, Matchers.isEmptyOrNullString());
        Collection<PatchStatus> statuses = listAction.call(api);
        Assert.assertThat(statuses, Matchers.<Collection<PatchStatus>>sameInstance(sampleApiPatchStatuses));
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.isEmptyOrNullString());
        statuses = listAction.call(api);
        Assert.assertThat(statuses, Matchers.<Collection<PatchStatus>>sameInstance(sampleApiPatchStatuses));
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "INSTALLED::UPLOADED|ID::-LAST_MOD"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "INSTALLED::UPLOADED|ID::-LAST_MOD"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("INSTALLED::UPLOADED|ID::-LAST_MOD"));
        Assert.assertThat(listAction.sortingFormat, Matchers.isEmptyOrNullString());
        statuses = listAction.call(api);
        Assert.assertThat(statuses, Matchers.<Collection<PatchStatus>>sameInstance(sampleApiPatchStatuses));
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.isEmptyOrNullString());
        Assert.assertThat(listAction.sortingFormat, Matchers.isEmptyOrNullString());
        statuses = listAction.call(api);
        Assert.assertThat(statuses, Matchers.<Collection<PatchStatus>>sameInstance(sampleApiPatchStatuses));
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "blah", "-sort"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.isEmptyOrNullString());
        statuses = listAction.call(api);
        Assert.assertThat(statuses, Matchers.<Collection<PatchStatus>>sameInstance(sampleApiPatchStatuses));
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.isEmptyOrNullString());
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("blah"));
        try {
            listAction.call(api);
            Assert.fail("should have failed with IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertThat(ex.getMessage(), Matchers.startsWith("No enum constant"));
            Assert.assertThat(ex.getMessage(), Matchers.endsWith("PatchStatus.Field.blah"));
        }
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "blah", "-sort", "sort_options"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("sort_options"));
        try {
            listAction.call(api);
            Assert.fail("should have failed with IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertThat(ex.getMessage(), Matchers.startsWith("No enum constant"));
            Assert.assertThat(ex.getMessage(), Matchers.endsWith("PatchStatus.Field.sort_options"));
        }
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "sort_options", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("sort_options"));
        try {
            listAction.call(api);
            Assert.fail("should have failed with IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertThat(ex.getMessage(), Matchers.startsWith("No enum constant"));
            Assert.assertThat(ex.getMessage(), Matchers.endsWith("PatchStatus.Field.sort_options"));
        }
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "INSTALLED::blah|ID::-LAST_MOD", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("INSTALLED::blah|ID::-LAST_MOD"));
        try {
            listAction.call(api);
            Assert.fail("should have failed with IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertThat(ex.getMessage(), Matchers.startsWith("No enum constant"));
            Assert.assertThat(ex.getMessage(), Matchers.endsWith("PatchStatus.State.blah"));
        }
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "INSTALLED::UPLOADED|ID::-blah", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("INSTALLED::UPLOADED|ID::-blah"));
        try {
            listAction.call(api);
            Assert.fail("should have failed with IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            Assert.assertThat(ex.getMessage(), Matchers.startsWith("No enum constant"));
            Assert.assertThat(ex.getMessage(), Matchers.endsWith("PatchStatus.Field.blah"));
        }
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "INSTALLED::UPLOADED|ID", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("INSTALLED::UPLOADED|ID"));
        statuses = listAction.call(api);
        Assert.assertThat(statuses, Matchers.<Collection<PatchStatus>>sameInstance(sampleApiPatchStatuses));
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "INSTALLED|ID::-LAST_MOD", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("INSTALLED|ID::-LAST_MOD"));
        statuses = listAction.call(api);
        Assert.assertThat(statuses, Matchers.<Collection<PatchStatus>>sameInstance(sampleApiPatchStatuses));
        cleanAction(listAction);
    }

    @Test
    public void testListSortArgLetterCase() throws Exception {
        long timer = 10000L;
        sampleApiPatchStatuses = new ArrayList<>(
                Arrays.asList(
                        /*0*/ createPatchStatusMock("b4", "desc for b4", PatchStatus.State.INSTALLED, ++timer, null, null),
                        /*1*/ createPatchStatusMock("a3", "desc for a3", PatchStatus.State.UPLOADED, ++timer, null, null),
                        /*2*/ createPatchStatusMock("A4", "desc for A4", PatchStatus.State.ERROR, ++timer, null, null),
                        /*3*/ createPatchStatusMock("B3", "desc for B3", PatchStatus.State.INSTALLED, ++timer, null, null),
                        /*4*/ createPatchStatusMock("C1", "desc for C1", PatchStatus.State.ROLLED_BACK, ++timer, null, "a3"),
                        /*5*/ createPatchStatusMock("c2", "desc for c2", PatchStatus.State.UPLOADED, ++timer, null, null)
                )
        );
        Mockito.doReturn(sampleApiPatchStatuses).when(api).listPatches();

        List<String> args = new ArrayList<>(Arrays.asList("list", "-sort", "ID", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("ID"));
        List<PatchStatus> statuses = new ArrayList<>(listAction.call(api));
        doTestExpectedValuesEx(
                sampleApiPatchStatuses,
                statuses,
                Arrays.asList(2, 3, 4, 1, 0, 5)

        );
        cleanAction(listAction);

        args = new ArrayList<>(Arrays.asList("list", "-sort", "-ID", "blah"));
        listAction = PatchCli.PatchAction.fromArgs(args);
        Assert.assertThat(listAction, Matchers.sameInstance(PatchCli.PatchAction.LIST));
        Assert.assertThat(args, Matchers.contains("list", "blah"));
        Assert.assertThat(listAction.getOutputFormat(), Matchers.equalTo("blah"));
        Assert.assertThat(listAction.sortingFormat, Matchers.equalTo("-ID"));
        statuses = new ArrayList<>(listAction.call(api));
        doTestExpectedValuesEx(
                sampleApiPatchStatuses,
                statuses,
                Arrays.asList(5, 0, 1, 4, 3, 2)

        );
        cleanAction(listAction);
    }

    private static void cleanAction(final PatchCli.PatchAction action) {
        Assert.assertNotNull(action);
        action.argument = null;
        action.outputFormat = null;
        action.sortingFormat = null;
        action.target = null;
    }

    private static void doTestExpectedValues(
            final List<PatchStatus> statuses,
            final List<Pair<String, String>> expectedValues
    ) {
        Assert.assertNotNull(statuses);
        Assert.assertNotNull(expectedValues);
        Assert.assertThat(statuses, Matchers.hasSize(expectedValues.size()));
        for (int i = 0; i < expectedValues.size(); ++i) {
            Assert.assertThat(statuses.get(i).getField(PatchStatus.Field.ID), Matchers.equalTo(expectedValues.get(i).left));
            Assert.assertThat(statuses.get(i).getField(PatchStatus.Field.LAST_MOD), Matchers.equalTo(expectedValues.get(i).right));
        }
    }

    private static void doTestExpectedValuesEx(
            final List<PatchStatus> patchStatuses,
            final List<PatchStatus> apiStatuses,
            final List<Integer> posInPatchStatuses
    ) {
        Assert.assertNotNull(patchStatuses);
        Assert.assertNotNull(apiStatuses);
        Assert.assertNotNull(posInPatchStatuses);
        Assert.assertThat(apiStatuses, Matchers.hasSize(patchStatuses.size()));
        Assert.assertThat(posInPatchStatuses, Matchers.hasSize(patchStatuses.size()));
        for (int i = 0; i < posInPatchStatuses.size(); ++i) {
            Assert.assertThat(apiStatuses.get(i), Matchers.sameInstance(patchStatuses.get(posInPatchStatuses.get(i))));
        }
    }
}