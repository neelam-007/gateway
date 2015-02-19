package com.l7tech.gateway.common.module;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.Charsets;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test for {@link ServerModuleFile}
 */
public class ServerModuleFileTest {

    private ServerModuleFile createTestServerModuleFile() {
        final ServerModuleFile entity = new ServerModuleFile();
        entity.setName("module_name");
        entity.setVersion(12);
        entity.setModuleType(ModuleType.MODULAR_ASSERTION);
        entity.createData("test_data".getBytes(Charsets.UTF8), "sha_for_data");
        entity.setStateForNode("node1", ModuleState.UPLOADED);
        entity.setStateForNode("node2", ModuleState.STAGED);
        entity.setStateErrorMessageForNode("node2", "error message");
        entity.setProperty(ServerModuleFile.PROP_FILE_NAME, "module_file_name");
        entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(100));
        entity.setProperty(ServerModuleFile.PROP_ASSERTIONS, "assertion1,assertion2");
        final Goid goid = new Goid(100, 101);
        entity.setGoid(goid);
        Assert.assertEquals(goid, entity.getGoid());

        return entity;
    }

    private void testCopyAgainstOriginal(final ServerModuleFile original, final ServerModuleFile copy, final boolean dataIncluded, final boolean metadataIncluded, final boolean statesIncluded) {
        Assert.assertNotNull(original);
        Assert.assertNotNull(original.getGoid());
        Assert.assertNotNull(copy);
        Assert.assertNotNull(copy.getGoid());
        Assert.assertNotSame(original, copy);

        // test attributes (should always equal)
        Assert.assertEquals(original.getGoid(), copy.getGoid());
        Assert.assertEquals(original.getVersion(), copy.getVersion());
        Assert.assertEquals(original.getName(), copy.getName());
        Assert.assertEquals(original.getModuleType(), copy.getModuleType());

        // test data (if dataIncluded then should equal otherwise should not)
        Assert.assertNotNull(original.getData());
        Assert.assertNotNull(copy.getData());
        Assert.assertNotNull(original.getData().getDataBytes()); // original always has bytes
        if (dataIncluded) {
            Assert.assertTrue(Arrays.equals(original.getData().getDataBytes(), copy.getData().getDataBytes()));
        } else {
            Assert.assertNull(copy.getData().getDataBytes());
        }

        // test metadata
        assertThat(original.getModuleSha256(), metadataIncluded ? equalTo(copy.getModuleSha256()) : not(equalTo(copy.getModuleSha256())));
        assertThat(original.getXmlProperties(), metadataIncluded ? equalTo(copy.getXmlProperties()) : not(equalTo(copy.getXmlProperties())));
        assertThat(original.getProperty(ServerModuleFile.PROP_SIZE), metadataIncluded ? equalTo(copy.getProperty(ServerModuleFile.PROP_SIZE)) : not(equalTo(copy.getProperty(ServerModuleFile.PROP_SIZE))));
        assertThat(original.getProperty(ServerModuleFile.PROP_FILE_NAME), metadataIncluded ? equalTo(copy.getProperty(ServerModuleFile.PROP_FILE_NAME)) : not(equalTo(copy.getProperty(ServerModuleFile.PROP_FILE_NAME))));
        assertThat(original.getProperty(ServerModuleFile.PROP_ASSERTIONS), metadataIncluded ? equalTo(copy.getProperty(ServerModuleFile.PROP_ASSERTIONS)) : not(equalTo(copy.getProperty(ServerModuleFile.PROP_ASSERTIONS))));
        assertThat(original.getHumanReadableFileSize(), metadataIncluded ? equalTo(copy.getHumanReadableFileSize()) : not(equalTo(copy.getHumanReadableFileSize())));

        // test states
        Assert.assertNotNull(original.getStates()); // original always has state
        if (statesIncluded) {
            assertThat(original.getStates(), equalTo(copy.getStates()));
        } else {
            Assert.assertNull(copy.getStates());
            assertThat(original.getStates(), not(equalTo(copy.getStates())));
        }
    }

    @Test
    public void test_copy_from() throws Exception {
        final ServerModuleFile entity = createTestServerModuleFile();
        Assert.assertNotNull(entity);
        Assert.assertNotNull(entity.getGoid());

        // copy nothing
        ServerModuleFile copy = new ServerModuleFile();
        copy.copyFrom(entity, false, false, false);
        testCopyAgainstOriginal(entity, copy, false, false, false);

        // copy data-bytes
        copy = new ServerModuleFile();
        copy.copyFrom(entity, true, false, false);
        testCopyAgainstOriginal(entity, copy, true, false, false);

        // copy metadata
        copy = new ServerModuleFile();
        copy.copyFrom(entity, false, true, false);
        testCopyAgainstOriginal(entity, copy, false, true, false);

        // copy states
        copy = new ServerModuleFile();
        copy.copyFrom(entity, false, false, true);
        testCopyAgainstOriginal(entity, copy, false, false, true);

        // copy data-bytes and metadata
        copy = new ServerModuleFile();
        copy.copyFrom(entity, true, true, false);
        testCopyAgainstOriginal(entity, copy, true, true, false);

        // copy metadata and states
        copy = new ServerModuleFile();
        copy.copyFrom(entity, false, true, true);
        testCopyAgainstOriginal(entity, copy, false, true, true);

        // copy data-bytes and states
        copy = new ServerModuleFile();
        copy.copyFrom(entity, true, false, true);
        testCopyAgainstOriginal(entity, copy, true, false, true);

        // copy all
        copy = new ServerModuleFile();
        copy.copyFrom(entity, true, true, true);
        testCopyAgainstOriginal(entity, copy, true, true, true);
    }

    @Test
    public void test_human_readable_bytes() throws Exception {
        ServerModuleFile entity = new ServerModuleFile();
        Assert.assertEquals(StringUtils.EMPTY, entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, null);
        Assert.assertEquals(StringUtils.EMPTY, entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, StringUtils.EMPTY);
        Assert.assertEquals(StringUtils.EMPTY, entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, "not a number");
        Assert.assertEquals(StringUtils.EMPTY, entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, "100KB");
        Assert.assertEquals(StringUtils.EMPTY, entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, "100 KB");
        Assert.assertEquals(StringUtils.EMPTY, entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, "100.0 KB");
        Assert.assertEquals(StringUtils.EMPTY, entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(1024*100));
        Assert.assertEquals("100.0 KB", entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(100000));
        Assert.assertEquals("97.7 KB", entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(100));
        Assert.assertEquals("100 B", entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(1023));
        Assert.assertEquals("1023 B", entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(1024));
        Assert.assertEquals("1.0 KB", entity.getHumanReadableFileSize());

        entity = new ServerModuleFile();
        entity.setProperty(ServerModuleFile.PROP_SIZE, String.valueOf(1025));
        Assert.assertEquals("1.0 KB", entity.getHumanReadableFileSize());
    }

    @Test
    public void test_create_data() throws Exception {
        ServerModuleFile entity = new ServerModuleFile();
        Assert.assertNull(entity.getData());
        Assert.assertNull(entity.getModuleSha256());

        entity = new ServerModuleFile();
        try {
            //noinspection ConstantConditions
            entity.createData(null, "sha");
            Assert.fail("null not allowed for data-bytes param.");
        } catch (IllegalArgumentException e) {
            /* this is expected */
        }
        Assert.assertNull(entity.getData());
        Assert.assertNull(entity.getModuleSha256());

        entity = new ServerModuleFile();
        try {
            //noinspection ConstantConditions
            entity.createData("test data".getBytes(Charsets.UTF8), null);
            Assert.fail("null not allowed for sha256 param");
        } catch (IllegalArgumentException e) {
            /* this is expected */
        }
        Assert.assertNull(entity.getData());
        Assert.assertNull(entity.getModuleSha256());

        entity = new ServerModuleFile();
        entity.createData("test data".getBytes(Charsets.UTF8), "test sha256");
        Assert.assertNotNull(entity.getData());
        Assert.assertTrue(Arrays.equals("test data".getBytes(Charsets.UTF8), entity.getData().getDataBytes()));
        Assert.assertEquals("test sha256", entity.getModuleSha256());

        entity = new ServerModuleFile();
        try {
            entity.updateData("test data".getBytes(Charsets.UTF8), "test sha256");
            Assert.fail("updateData not allowed when data-bytes are not created first.");
        } catch (IllegalStateException e) {
            /* this is expected */
        }
        Assert.assertNull(entity.getData());
        Assert.assertNull(entity.getModuleSha256());
    }

    @Test
    public void test_state() throws Exception {
        ServerModuleFile entity = new ServerModuleFile();
        Assert.assertNull(entity.getStates());

        entity = new ServerModuleFile();
        entity.setStateForNode("node1", ModuleState.STAGED);
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(1, entity.getStates().size());
        ServerModuleFileState state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.STAGED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));

        entity = new ServerModuleFile();
        entity.setStateErrorMessageForNode("node1", "error");
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(1, entity.getStates().size());
        state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.ERROR, state.getState());
        Assert.assertEquals("error", state.getErrorMessage());


        entity = new ServerModuleFile();
        entity.setStateForNode("node1", ModuleState.LOADED);
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(1, entity.getStates().size());
        state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.LOADED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        entity.setStateForNode("node1", ModuleState.DEPLOYED);
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(1, entity.getStates().size());
        state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.DEPLOYED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        entity.setStateErrorMessageForNode("node1", "error");
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(1, entity.getStates().size());
        state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.ERROR, state.getState());
        Assert.assertEquals("error", state.getErrorMessage());

        entity = new ServerModuleFile();
        entity.setStateForNode("node1", ModuleState.UPLOADED);
        entity.setStateForNode("node2", ModuleState.DEPLOYED);
        entity.setStateForNode("node3", ModuleState.STAGED);
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(3, entity.getStates().size());

        state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        state = entity.getStates().get(1);
        Assert.assertEquals("node2", state.getNodeId());
        Assert.assertEquals(ModuleState.DEPLOYED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        state = entity.getStates().get(2);
        Assert.assertEquals("node3", state.getNodeId());
        Assert.assertEquals(ModuleState.STAGED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));

        entity.setStateErrorMessageForNode("node2", "error");
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(3, entity.getStates().size());

        state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        state = entity.getStates().get(1);
        Assert.assertEquals("node2", state.getNodeId());
        Assert.assertEquals(ModuleState.ERROR, state.getState());
        Assert.assertEquals("error", state.getErrorMessage());
        state = entity.getStates().get(2);
        Assert.assertEquals("node3", state.getNodeId());
        Assert.assertEquals(ModuleState.STAGED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));

        entity.setStateForNode("node2", ModuleState.UPLOADED);
        Assert.assertNotNull(entity.getStates());
        Assert.assertFalse(entity.getStates().isEmpty());
        Assert.assertEquals(3, entity.getStates().size());

        state = entity.getStates().get(0);
        Assert.assertEquals("node1", state.getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        state = entity.getStates().get(1);
        Assert.assertEquals("node2", state.getNodeId());
        Assert.assertEquals(ModuleState.UPLOADED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
        state = entity.getStates().get(2);
        Assert.assertEquals("node3", state.getNodeId());
        Assert.assertEquals(ModuleState.STAGED, state.getState());
        Assert.assertTrue(StringUtils.isBlank(state.getErrorMessage()));
    }

    @Test
    public void test_equal_and_hash() throws Exception {
        ServerModuleFile entity = new ServerModuleFile();
        ServerModuleFile another = new ServerModuleFile();
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));

        entity = new ServerModuleFile();
        entity.setGoid(new Goid(1, 1));
        another = new ServerModuleFile();
        another.setGoid(new Goid(1, 1));
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));
        entity.setGoid(new Goid(1, 2));
        assertThat(entity, not(equalTo(another)));
        assertThat(entity.hashCode(), not(equalTo(another.hashCode())));

        entity = new ServerModuleFile();
        another = new ServerModuleFile();
        entity.createData("entity bytes".getBytes(Charsets.UTF8), "sha1");
        another.createData("another bytes".getBytes(Charsets.UTF8), "sha1");
        assertThat(entity, equalTo(another)); // data is not checked only the sha256
        assertThat(entity.hashCode(), equalTo(another.hashCode()));

        entity = new ServerModuleFile();
        another = new ServerModuleFile();
        entity.setModuleSha256("sha1");
        another.setModuleSha256("sha2");
        assertThat(entity, not(equalTo(another)));
        assertThat(entity.hashCode(), not(equalTo(another.hashCode())));

        entity = new ServerModuleFile();
        another = new ServerModuleFile();
        entity.setModuleType(ModuleType.MODULAR_ASSERTION);
        another.setModuleType(ModuleType.MODULAR_ASSERTION);
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));

        entity = new ServerModuleFile();
        another = new ServerModuleFile();
        entity.setModuleType(ModuleType.MODULAR_ASSERTION);
        another.setModuleType(ModuleType.CUSTOM_ASSERTION);
        assertThat(entity, not(equalTo(another)));
        assertThat(entity.hashCode(), not(equalTo(another.hashCode())));

        entity = createTestServerModuleFile();
        another = new ServerModuleFile();
        another.copyFrom(entity, true, true, true);
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));

        entity = createTestServerModuleFile();
        another = new ServerModuleFile();
        another.copyFrom(entity, false, true, false);
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));

        entity = createTestServerModuleFile();
        another = new ServerModuleFile();
        another.copyFrom(entity, true, true, true);
        another.setStateForNode("node2", ModuleState.UPLOADED);
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));
        another.setStateForNode("node10", ModuleState.DEPLOYED);
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));

        entity = createTestServerModuleFile();
        another = new ServerModuleFile();
        another.copyFrom(entity, true, true, true);
        another.getData().setDataBytes("another bytes changed".getBytes(Charsets.UTF8));
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));
        another.getData().setDataBytes(null);
        assertThat(entity, equalTo(another));
        assertThat(entity.hashCode(), equalTo(another.hashCode()));
    }
}
