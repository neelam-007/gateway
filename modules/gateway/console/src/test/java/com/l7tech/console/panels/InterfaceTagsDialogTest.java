package com.l7tech.console.panels;

import com.l7tech.console.util.registry.RegistryStub;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.CollectionUtils;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

/**
 * This was created: 9/17/13 as 11:01 AM
 *
 * @author Victor Kazakov
 */
public class InterfaceTagsDialogTest {
    private RegistryStub registry;

    @Before
    public void before() {
        registry = new RegistryStub();
        RegistryStub.setDefault(registry);
    }

    @Test
    public void loadCurrentInterfaceTagsFromServerTest() throws DeleteException, UpdateException, SaveException {
        InterfaceTag test1 = new InterfaceTag("test1", CollectionUtils.set("123.123.123/24"));
        InterfaceTag test2 = new InterfaceTag("test2", CollectionUtils.set("123.123.123.001"));
        registry.getClusterStatusAdmin().saveProperty(new ClusterProperty(InterfaceTag.PROPERTY_NAME, test1.toString() + ";" + test2.toString()));

        Set<InterfaceTag> rtn = InterfaceTagsDialog.loadCurrentInterfaceTagsFromServer();

        Assert.assertEquals(2, rtn.size());
        Assert.assertTrue(rtn.contains(test1));
        Assert.assertTrue(rtn.contains(test2));
    }

    @Test
    public void loadCurrentInterfaceTagsFromServerNullAndEmptyPropertyTest() throws DeleteException, UpdateException, SaveException {
        registry.getClusterStatusAdmin().saveProperty(new ClusterProperty(InterfaceTag.PROPERTY_NAME, ""));

        Set<InterfaceTag> rtn = InterfaceTagsDialog.loadCurrentInterfaceTagsFromServer();

        Assert.assertTrue(rtn.isEmpty());

        registry.getClusterStatusAdmin().saveProperty(new ClusterProperty(InterfaceTag.PROPERTY_NAME, null));

        rtn = InterfaceTagsDialog.loadCurrentInterfaceTagsFromServer();

        Assert.assertTrue(rtn.isEmpty());
    }
}
