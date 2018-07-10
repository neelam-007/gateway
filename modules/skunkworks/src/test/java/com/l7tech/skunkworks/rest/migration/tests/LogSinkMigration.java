package com.l7tech.skunkworks.rest.migration.tests;

import static org.junit.Assert.*;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.DependencyListMO;
import com.l7tech.gateway.api.DependencyMO;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.LogSinkFilter;
import com.l7tech.gateway.api.LogSinkMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.api.Mappings;
import com.l7tech.gateway.api.PasswordFormatted;
import com.l7tech.gateway.api.PrivateKeyCreationContext;
import com.l7tech.gateway.api.PrivateKeyMO;
import com.l7tech.gateway.api.UserMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import java.io.StringReader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * This will test migration using the rest api from one gateway to another.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class LogSinkMigration extends MigrationTestBase {
    private static final Logger logger = Logger.getLogger(LogSinkMigration.class.getName());
    public static final String internalIdpId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID.toString();

    private Item<FolderMO> rootFolderItem;
    private Item<FolderMO> folder1Item;
    private Item<FolderMO> folder2Item;

    private Item<UserMO> userItem;
    private Item<UserMO> adminUserItem;
    private Item<PrivateKeyMO> privateKeyItem;
    private Item<LogSinkMO> logSinkItem;

    private Item<Mappings> mappingsToClean;

    final String assXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" + "    <wsp:All wsp:Usage=\"Required\">\n" + "        <L7p:AuditDetailAssertion>\n" + "            <L7p:Detail stringValue=\"HI 2\"/>\n" + "        </L7p:AuditDetailAssertion>\n" + "    </wsp:All>\n" + "</wsp:Policy>";


    @Before
    public void before() throws Exception {
        // get root folder
        RestResponse response = getSourceEnvironment().processRequest("folders/" + Folder.ROOT_FOLDER_ID.toString(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        rootFolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // create folder1
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(rootFolderItem.getId());
        folderMO.setName("Source folder1");
         response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));

        assertOkCreatedResponse(response);

        folder1Item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folder1Item.setContent(folderMO);

        // create folder2
        folderMO.setName("Source folder2");
        response = getSourceEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));

        assertOkCreatedResponse(response);

        folder2Item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        folder2Item.setContent(folderMO);

        // create private key
        PrivateKeyCreationContext createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=srcAlias1");
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ecName", "secp384r1").map());
        response = getSourceEnvironment().processRequest("privateKeys/" + new Goid(0, 2).toString() + ":srcAlias1", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        privateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        privateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        // get admin user
        response = getSourceEnvironment().processRequest("identityProviders/" + internalIdpId + "/users/" + new Goid(0, 3).toString(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        adminUserItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //create user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalIdpId);
        userMO.setLogin("SrcUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getSourceEnvironment().processRequest("identityProviders/" + internalIdpId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        userItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        userItem.setContent(userMO);

        //create logsink
        LogSinkMO logSinkMO = ManagedObjectFactory.createLogSinkMO();
        logSinkMO.setName("Source Sink");
        logSinkMO.setDescription("Source Sink Desc");
        logSinkMO.setType(LogSinkMO.SinkType.SYSLOG);
        logSinkMO.setEnabled(true);
        logSinkMO.setSeverity(LogSinkMO.SeverityThreshold.FINE);
        logSinkMO.setSyslogHosts(CollectionUtils.list("host:123"));
        logSinkMO.setCategories(CollectionUtils.list(LogSinkMO.Category.LOG, LogSinkMO.Category.AUDIT));
        logSinkMO.setProperties(CollectionUtils.<String, String>mapBuilder()
                .put(SinkConfiguration.PROP_SYSLOG_FORMAT, "format").map());
        logSinkMO.setFilters(CollectionUtils.list(
                createFilter(GatewayDiagnosticContextKeys.CLIENT_IP, CollectionUtils.list("0.0.0.0"))));

        response = getSourceEnvironment().processRequest("logSinks/", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(logSinkMO)));
        assertOkCreatedResponse(response);
        logSinkItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        logSinkItem.setContent(logSinkMO);

    }

    private LogSinkFilter createFilter(String type, List<String> values) {
        LogSinkFilter filter = ManagedObjectFactory.createLogSinkFilter();
        filter.setType(type);
        filter.setValues(values);
        return filter;
    }

    @After
    public void after() throws Exception {
        if (mappingsToClean != null)
            cleanupAll(mappingsToClean);

        RestResponse response;

        response = getSourceEnvironment().processRequest("folders/" + folder1Item.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
        response = getSourceEnvironment().processRequest("folders/" + folder2Item.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
        response = getSourceEnvironment().processRequest("identityProviders/" + internalIdpId + "/users/" + userItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
        response = getSourceEnvironment().processRequest("privateKeys/" + privateKeyItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
        response = getSourceEnvironment().processRequest("logSinks/" + logSinkItem.getId(), HttpMethod.DELETE, null, "");
        assertOkEmptyResponse(response);
    }

    @Test
    public void testExport() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        assertEquals("The bundle should have 1 items.", 1, bundleItem.getContent().getReferences().size());
        assertEquals("The bundle should have 1 items.", 1, bundleItem.getContent().getMappings().size());
    }

    @Test
    public void testImportNoDependencies() throws Exception {
        RestResponse response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 items. A log sink", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getReferences().get(0).getType());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 1 mapping after the import", 1, mappings.getContent().getMappings().size());
        validateMapping(mappings.getContent().getMappings().get(0), EntityType.LOG_SINK, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, logSinkItem.getId(), logSinkItem.getId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("logSinks/" + logSinkItem.getId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(0, folderDependencies.size());

        validate(mappings);
    }

    /**
     * Same algorithm is used for dependency handling.  Only need to test one
     * Dependencies include: folder, service, email, jms, listen port
     * @throws Exception
     */
    @Test
    public void testImportFilter() throws Exception {
        // Update logsink
        LogSinkMO logSinkMO = logSinkItem.getContent();
        logSinkMO.setFilters(CollectionUtils.list(
                createFilter(GatewayDiagnosticContextKeys.FOLDER_ID, CollectionUtils.list(folder1Item.getId())),
                createFilter(GatewayDiagnosticContextKeys.CLIENT_IP, CollectionUtils.list("0.0.0.0"))));

        RestResponse response = getSourceEnvironment().processRequest("logSinks/" + logSinkItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(logSinkMO)));
        assertOkResponse(response);
        logSinkItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        logSinkItem.setContent(logSinkMO);

        // Export
        response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A log sink and a folder", 2, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getReferences().get(1).getType());

        Assert.assertEquals("The bundle should have 3 mappings. A log sink and 2 folders", 3, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getMappings().get(2).getType());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        validateMapping(mappings.getContent().getMappings().get(0), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, rootFolderItem.getId(), rootFolderItem.getId());
        validateMapping(mappings.getContent().getMappings().get(1), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, folder1Item.getId(), folder1Item.getId());
        validateMapping(mappings.getContent().getMappings().get(2), EntityType.LOG_SINK, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, logSinkItem.getId(), logSinkItem.getId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("logSinks/" + logSinkItem.getId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(1, folderDependencies.size());
        validateDependency(folderDependencies.get(0),folder1Item.getId(), folder1Item.getName(), EntityType.FOLDER);

        validate(mappings);
    }

    /**
     * Same algorithm is used for dependency handling.  Only need to test one
     * Dependencies include: folder, service, email, jms, listen port
     * @throws Exception
     */
    @Test
    public void testImportFilterMapped() throws Exception {
        // Update logsink
        LogSinkMO logSinkMO = logSinkItem.getContent();
        logSinkMO.setFilters(CollectionUtils.list(
                createFilter(GatewayDiagnosticContextKeys.FOLDER_ID, CollectionUtils.list(folder1Item.getId())),
                createFilter(GatewayDiagnosticContextKeys.CLIENT_IP, CollectionUtils.list("0.0.0.0"))));

        RestResponse response = getSourceEnvironment().processRequest("logSinks/" + logSinkItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(logSinkMO)));
        assertOkResponse(response);
        logSinkItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        logSinkItem.setContent(logSinkMO);

        // Create target folder
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("Target folder");
        response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));

        assertOkCreatedResponse(response);

        Item<FolderMO> targetFolderItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetFolderItem.setContent(folderMO);

        // Export
        response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 2 items. A log sink and a folder", 2, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getReferences().get(1).getType());

        Assert.assertEquals("The bundle should have 3 mappings. A log sink and 2 folders", 3, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getMappings().get(2).getType());

        // Update mapping
        bundleItem.getContent().getMappings().get(1).setTargetId(targetFolderItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        validateMapping(mappings.getContent().getMappings().get(0), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, rootFolderItem.getId(), rootFolderItem.getId());
        validateMapping(mappings.getContent().getMappings().get(1), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, folder1Item.getId(), targetFolderItem.getId());
        validateMapping(mappings.getContent().getMappings().get(2), EntityType.LOG_SINK, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, logSinkItem.getId(), logSinkItem.getId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("logSinks/" + logSinkItem.getId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(1, folderDependencies.size());
        validateDependency(folderDependencies.get(0),targetFolderItem.getId(), targetFolderItem.getName(), EntityType.FOLDER);

        validate(mappings);
    }

    /**
     * Same algorithm is used for dependency handling.  Only need to test one
     * Dependencies include: folder, service, email, jms, listen port
     * @throws Exception
     */
    @Test
    public void testImportFilters() throws Exception {
        // Update logsink
        LogSinkMO logSinkMO = logSinkItem.getContent();
        logSinkMO.setFilters(CollectionUtils.list(
                createFilter(GatewayDiagnosticContextKeys.FOLDER_ID, CollectionUtils.list(folder1Item.getId(), folder2Item.getId())),
                createFilter(GatewayDiagnosticContextKeys.CLIENT_IP, CollectionUtils.list("0.0.0.0"))));

        RestResponse response = getSourceEnvironment().processRequest("logSinks/" + logSinkItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(logSinkMO)));
        assertOkResponse(response);
        logSinkItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        logSinkItem.setContent(logSinkMO);

        // Export
        response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A log sink and a folder", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(1).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getReferences().get(2).getType());

        Assert.assertEquals("The bundle should have 4 mappings. A log sink and 2 folders", 4, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(2).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getMappings().get(3).getType());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        validateMapping(mappings.getContent().getMappings().get(0), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, Folder.ROOT_FOLDER_ID.toString(), Folder.ROOT_FOLDER_ID.toString());
        validateMapping(mappings.getContent().getMappings().get(1), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, folder1Item.getId(), folder1Item.getId());
        validateMapping(mappings.getContent().getMappings().get(2), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, folder2Item.getId(), folder2Item.getId());
        validateMapping(mappings.getContent().getMappings().get(3), EntityType.LOG_SINK, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, logSinkItem.getId(), logSinkItem.getId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("logSinks/" + logSinkItem.getId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(2, folderDependencies.size());
        validateDependency(folderDependencies.get(0),folder1Item.getId(), folder1Item.getName(), EntityType.FOLDER);
        validateDependency(folderDependencies.get(1),folder2Item.getId(), folder2Item.getName(), EntityType.FOLDER);

        validate(mappings);
    }

    /**
     * Same algorithm is used for dependency handling.  Only need to test one
     * Dependencies include: folder, service, email, jms, listen port
     * @throws Exception
     */
    @Test
    public void testImportFiltersMapped() throws Exception {
        // Update logsink
        LogSinkMO logSinkMO = logSinkItem.getContent();
        logSinkMO.setFilters(CollectionUtils.list(
                createFilter(GatewayDiagnosticContextKeys.FOLDER_ID, CollectionUtils.list(folder1Item.getId(), folder2Item.getId())),
                createFilter(GatewayDiagnosticContextKeys.CLIENT_IP, CollectionUtils.list("0.0.0.0"))));

        RestResponse response = getSourceEnvironment().processRequest("logSinks/" + logSinkItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(logSinkMO)));
        assertOkResponse(response);
        logSinkItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        logSinkItem.setContent(logSinkMO);

        // Create target folders
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("Target folder1");
        response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));

        assertOkCreatedResponse(response);

        Item<FolderMO> targetFolder1Item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetFolder1Item.setContent(folderMO);

        folderMO.setName("Target folder2");
        response = getTargetEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));

        assertOkCreatedResponse(response);

        Item<FolderMO> targetFolder2Item = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetFolder2Item.setContent(folderMO);

        // Export
        response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A log sink and a folder", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getReferences().get(1).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getReferences().get(2).getType());

        Assert.assertEquals("The bundle should have 4 mappings. A log sink and 2 folders", 4, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.FOLDER.toString(), bundleItem.getContent().getMappings().get(2).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getMappings().get(3).getType());

        // Update mapping
        bundleItem.getContent().getMappings().get(1).setTargetId(targetFolder1Item.getId());
        bundleItem.getContent().getMappings().get(2).setTargetId(targetFolder2Item.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 4 mappings after the import", 4, mappings.getContent().getMappings().size());
        validateMapping(mappings.getContent().getMappings().get(0), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, Folder.ROOT_FOLDER_ID.toString(), Folder.ROOT_FOLDER_ID.toString());
        validateMapping(mappings.getContent().getMappings().get(1), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, folder1Item.getId(), targetFolder1Item.getId());
        validateMapping(mappings.getContent().getMappings().get(2), EntityType.FOLDER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, folder2Item.getId(), targetFolder2Item.getId());
        validateMapping(mappings.getContent().getMappings().get(3), EntityType.LOG_SINK, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, logSinkItem.getId(), logSinkItem.getId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("logSinks/" + logSinkItem.getId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(2, folderDependencies.size());
        validateDependency(folderDependencies.get(0),targetFolder1Item.getId(), targetFolder1Item.getName(), EntityType.FOLDER);
        validateDependency(folderDependencies.get(1),targetFolder2Item.getId(), targetFolder2Item.getName(), EntityType.FOLDER);

        validate(mappings);
    }

    @Test
    public void testImportUserMapped() throws Exception {
        // Update logsink
        LogSinkMO logSinkMO = logSinkItem.getContent();
        logSinkMO.setFilters(CollectionUtils.list(
                createFilter(GatewayDiagnosticContextKeys.USER_ID, CollectionUtils.list(internalIdpId+":"+adminUserItem.getId(), internalIdpId+":"+userItem.getId())),
                createFilter(GatewayDiagnosticContextKeys.CLIENT_IP, CollectionUtils.list("0.0.0.0"))));

        RestResponse response = getSourceEnvironment().processRequest("logSinks/" + logSinkItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(logSinkMO)));
        assertOkResponse(response);
        logSinkItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        logSinkItem.setContent(logSinkMO);

        // create target user
        UserMO userMO = ManagedObjectFactory.createUserMO();
        userMO.setProviderId(internalIdpId);
        userMO.setLogin("TargetUser");
        PasswordFormatted password = ManagedObjectFactory.createPasswordFormatted();
        password.setFormat("plain");
        password.setPassword("123#@!qwER");
        userMO.setPassword(password);
        response = getTargetEnvironment().processRequest("identityProviders/" + internalIdpId + "/users", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(userMO)));
        assertOkCreatedResponse(response);
        Item<UserMO> targetUserItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        targetUserItem.setContent(userMO);

        // Export
        response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 3 items. A log sink and 2 user", 3, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.USER.toString(), bundleItem.getContent().getReferences().get(0).getType());
        Assert.assertEquals(EntityType.USER.toString(), bundleItem.getContent().getReferences().get(1).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getReferences().get(2).getType());

        Assert.assertEquals("The bundle should have 3 mappings. A log sink and 2 users", 3, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.USER.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.USER.toString(), bundleItem.getContent().getMappings().get(1).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getMappings().get(2).getType());

        // Update mapping
        bundleItem.getContent().getMappings().get(1).setTargetId(targetUserItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 3 mappings after the import", 3, mappings.getContent().getMappings().size());
        validateMapping(mappings.getContent().getMappings().get(0), EntityType.USER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, adminUserItem.getId(), adminUserItem.getId());
        validateMapping(mappings.getContent().getMappings().get(1), EntityType.USER, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, userItem.getId(), targetUserItem.getId());
        validateMapping(mappings.getContent().getMappings().get(2), EntityType.LOG_SINK, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, logSinkItem.getId(), logSinkItem.getId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("logSinks/" + logSinkItem.getId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(2, folderDependencies.size());
        validateDependency(folderDependencies.get(0),adminUserItem.getId(), adminUserItem.getName(), EntityType.USER);
        validateDependency(folderDependencies.get(1),targetUserItem.getId(), targetUserItem.getName(), EntityType.USER);

        validate(mappings);
    }

    @Test
    public void testImportPrivateKeyMapped() throws Exception {
        // Update logsink
        LogSinkMO logSinkMO = logSinkItem.getContent();
        logSinkMO.setProperties(CollectionUtils.<String, String>mapBuilder()
                 .put(SinkConfiguration.PROP_SYSLOG_SSL_KEYSTORE_ID, new Goid(0, 2).toString())
                 .put(SinkConfiguration.PROP_SYSLOG_SSL_KEY_ALIAS, privateKeyItem.getName()).map());
        RestResponse response = getSourceEnvironment().processRequest("logSinks/" + logSinkItem.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(logSinkMO)));
        assertOkResponse(response);
        logSinkItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        logSinkItem.setContent(logSinkMO);

        // create target private key
        PrivateKeyCreationContext createPrivateKey = ManagedObjectFactory.createPrivateKeyCreationContext();
        createPrivateKey.setDn("CN=targetAlias1");
        createPrivateKey.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("ecName", "secp384r1").map());
        response = getTargetEnvironment().processRequest("privateKeys/" + new Goid(0, 2).toString() + ":targetAlias1", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(createPrivateKey)));
        assertOkCreatedResponse(response);
        Item<PrivateKeyMO> targetPrivateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getTargetEnvironment().processRequest("privateKeys/" + targetPrivateKeyItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        targetPrivateKeyItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));


        // Export
        response = getSourceEnvironment().processRequest("bundle?logSink=" + logSinkItem.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<Bundle> bundleItem = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        Assert.assertEquals("The bundle should have 1 item. A log sink.", 1, bundleItem.getContent().getReferences().size());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getReferences().get(0).getType());

        Assert.assertEquals("The bundle should have 2 mappings. A log sink and a private key", 2, bundleItem.getContent().getMappings().size());
        Assert.assertEquals(EntityType.SSG_KEY_ENTRY.toString(), bundleItem.getContent().getMappings().get(0).getType());
        Assert.assertEquals(EntityType.LOG_SINK.toString(), bundleItem.getContent().getMappings().get(1).getType());

        // Update mapping
        bundleItem.getContent().getMappings().get(0).setTargetId(targetPrivateKeyItem.getId());

        //import the bundle
        response = getTargetEnvironment().processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundleItem.getContent()));
        assertOkResponse(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        mappingsToClean = mappings;

        //verify the mappings
        Assert.assertEquals("There should be 2 mappings after the import", 2, mappings.getContent().getMappings().size());
        validateMapping(mappings.getContent().getMappings().get(0), EntityType.SSG_KEY_ENTRY, Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, privateKeyItem.getId(), targetPrivateKeyItem.getId());
        validateMapping(mappings.getContent().getMappings().get(1), EntityType.LOG_SINK, Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, logSinkItem.getId(), logSinkItem.getId());

        // verify dependencies

        response = getTargetEnvironment().processRequest("logSinks/" + logSinkItem.getId() + "/dependencies", "returnType", HttpMethod.GET, null, "");
        assertOkResponse(response);

        Item<DependencyListMO> dependencies = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        List<DependencyMO> folderDependencies = dependencies.getContent().getDependencies();

        Assert.assertNotNull(folderDependencies);
        Assert.assertEquals(1, folderDependencies.size());
        validateDependency(folderDependencies.get(0),targetPrivateKeyItem.getId(), targetPrivateKeyItem.getName(), EntityType.SSG_KEY_ENTRY);

        validate(mappings);
    }

}
