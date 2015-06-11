package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.logging.Logger;

/**
 * These test migration in the local jvm. This test class is used to test some exceptional cases.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class LocalMigrationTestCases extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(LocalMigrationTestCases.class.getName());

    private FolderManager folderManager;
    private Folder rootFolder;

    @Before
    public void before() throws Exception {
        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);

        rootFolder = folderManager.findRootFolder();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        RestEntityTestBase.beforeClass();
    }

    /**
     * Test importing a bundle where we update the parent folder of a folder. This is an update of a foreign key
     * referenced entity. And an update of an entity that is eagerly loaded.
     */
    @Test
    public void testUpdatingForeignKeyReferencedEntity() throws Exception {
        String parentFolderName = "Parent Folder";
        String childFolderName = "Child Folder";

        Folder parentFolder = new Folder(parentFolderName, rootFolder);
        folderManager.save(parentFolder);

        Folder childFolder = new Folder(childFolderName, parentFolder);
        folderManager.save(childFolder);

        try {
            // export
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle/folder/" + parentFolder.getId() + "?defaultAction=NewOrUpdate&includeRequestFolder=true", HttpMethod.GET, ContentType.APPLICATION_XML.toString(), "");
            assertOkResponse(response);
            StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item<Bundle> item = MarshallingUtils.unmarshal(Item.class, source);

            // update folders
            parentFolder.setName(parentFolder.getName() + "update name");
            folderManager.update(parentFolder);
            childFolder.setName(childFolder.getName() + "update name");
            folderManager.update(childFolder);

            // import and update
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            final StreamResult result = new StreamResult(bout);
            MarshallingUtils.marshal(item.getContent(), result, false);
            String bundleXml = bout.toString();
            response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), bundleXml);
            assertOkResponse(response);

            Folder parentFolderAfterImport = folderManager.findByPrimaryKey(parentFolder.getGoid());
            Folder childFolderAfterImport = folderManager.findByPrimaryKey(childFolder.getGoid());

            Assert.assertNotNull(parentFolderAfterImport);
            Assert.assertNotNull(childFolderAfterImport);
            Assert.assertEquals(parentFolderAfterImport.getGoid(), childFolderAfterImport.getFolder().getGoid());
            Assert.assertEquals(parentFolderName, parentFolderAfterImport.getName());
            Assert.assertEquals(childFolderName, childFolderAfterImport.getName());
        } finally {
            folderManager.delete(childFolder.getGoid());
            folderManager.delete(parentFolder.getGoid());
        }
    }

    protected void assertOkResponse(RestResponse response) {
        org.junit.Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        org.junit.Assert.assertEquals(200, response.getStatus());
    }
}
