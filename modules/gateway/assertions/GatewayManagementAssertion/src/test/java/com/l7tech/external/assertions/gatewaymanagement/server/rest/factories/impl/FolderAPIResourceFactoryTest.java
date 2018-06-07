package com.l7tech.external.assertions.gatewaymanagement.server.rest.factories.impl;

import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory;
import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.test.BugId;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FolderAPIResourceFactoryTest {

    private FolderAPIResourceFactory folderAPIResourceFactory = new FolderAPIResourceFactory();

    @Test
    @BugId("DE345531")
    public void deleteResource_rootFolderForced_disallowed() {
        try {
            folderAPIResourceFactory.deleteResource(Folder.ROOT_FOLDER_ID.toString(), true);
        } catch (ResourceFactory.ResourceNotFoundException e) {
            Assert.fail("Unable to find root folder");
        } catch (ResourceFactory.ResourceAccessException e) {
            Assert.assertThat(e.getCause(), Matchers.instanceOf(ConstraintViolationException.class));
            return;
        }
        Assert.fail("Should have encountered a ResourceAccessException");
    }
}
