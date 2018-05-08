package com.l7tech.external.assertions.gatewaymanagement.server;

import static junit.framework.Assert.*;
import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.transformers.impl.LogSinkTransformer;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.LogSinkMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.log.GatewayDiagnosticContextKeys;
import com.l7tech.gateway.common.log.SinkConfiguration;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManagerStub;
import com.l7tech.server.log.SinkManagerStub;
import com.l7tech.util.CollectionUtils;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

/**
 *
 */
public class LogSinkRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(LogSinkRestServerGatewayManagementAssertionTest.class.getName());

    private static final SinkConfiguration sinkConfiguration = new SinkConfiguration();
    private static SinkManagerStub sinkManager;
    private static final String logSinkBasePath = "logSinks/";

    private static FolderManagerStub folderManager;
    private static Folder rootFolder;

    @InjectMocks
    protected LogSinkTransformer logSinkTransformer;

    @Before
    public void before() throws Exception {
        super.before();

        folderManager = applicationContext.getBean("folderManager", FolderManagerStub.class);
        rootFolder = new Folder("ROOT FOLDER", null);
        rootFolder.setGoid(folderManager.save(rootFolder));

        sinkManager = assertionContext.getBean("sinkManager", SinkManagerStub.class);
        sinkConfiguration.setGoid(new Goid(0, 1234L));
        sinkConfiguration.setName("sink1");
        sinkConfiguration.setDescription("sink1 desc");
        sinkConfiguration.setCategories("LOG,AUDIT");
        sinkConfiguration.setSeverity(SinkConfiguration.SeverityThreshold.INFO);
        sinkConfiguration.setType(SinkConfiguration.SinkType.SYSLOG);
        sinkConfiguration.syslogHostList().addAll(CollectionUtils.list("host:1234"));
        sinkConfiguration.setProperty("syslog.protocol", "TCP"); // UDP or TCP or SSL
        sinkConfiguration.setProperty("syslog.logHostname", "true");
        sinkConfiguration.setProperty("syslog.facility", "1");
        sinkConfiguration.setProperty("syslog.ssl.clientAuth", "false");
        sinkConfiguration.setProperty("syslog.charSet", "LATIN-1"); // UTF-8 or ASCII or LATIN-1
        sinkConfiguration.setFilters(CollectionUtils.<String,List<String>>mapBuilder().put(GatewayDiagnosticContextKeys.CLIENT_IP, CollectionUtils.list("0.0.0.0","1.1.1.1"))
                .put(GatewayDiagnosticContextKeys.FOLDER_ID, CollectionUtils.list(rootFolder.getId())).map());
        sinkConfiguration.setGoid(sinkManager.save(sinkConfiguration));

    }

    @After
    public void after() throws Exception {
        super.after();
        
        Collection<FolderHeader> folders = new ArrayList<>(folderManager.findAllHeaders());
        for (EntityHeader entity : folders) {
            folderManager.delete(entity.getGoid());
        }

        Collection<EntityHeader> entities = new ArrayList<>(sinkManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            sinkManager.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(logSinkBasePath + sinkConfiguration.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        assertEquals("Log sink identifier:", sinkConfiguration.getId(), item.getId());
        assertEquals("Log sink name:", sinkConfiguration.getName(), ((LogSinkMO) item.getContent()).getName());
        assertEquals("Log sink type:", sinkConfiguration.getType().toString(), ((LogSinkMO) item.getContent()).getType().toString());
    }

    @Test
    public void createEntityTest() throws Exception {

        LogSinkMO createObject = logSinkTransformer.convertToMO(sinkConfiguration);
        createObject.setId(null);
        createObject.setName("New Log sink");
        createObject.setDescription(createObject.getName()+" desc");
        createObject.setVersion(null);

        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(logSinkBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        SinkConfiguration sinkConfiguration = sinkManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Log sink name:", sinkConfiguration.getName(), createObject.getName());
        assertEquals("Log sink description:", sinkConfiguration.getDescription(), createObject.getDescription());
        assertEquals("Log sink type:", sinkConfiguration.getType().toString(), createObject.getType().toString());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        LogSinkMO createObject = logSinkTransformer.convertToMO(sinkConfiguration);
        createObject.setId(null);
        createObject.setVersion(null);
        createObject.setName("New Log sink");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(logSinkBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.log(Level.INFO, response.toString());

        assertEquals("Created Log sink goid:", goid.toString(), getFirstReferencedGoid(response));

        SinkConfiguration createdConnector = sinkManager.findByPrimaryKey(goid);
        assertEquals("Log sink name:", createdConnector.getName(), createObject.getName());
        assertEquals("Log sink type:", createdConnector.getType().toString(), createObject.getType().toString());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(logSinkBasePath + sinkConfiguration.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        LogSinkMO entityGot = (LogSinkMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.setName(entityGot.getName() + "_mod");
        RestResponse response = processRequest(logSinkBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Log sink goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SinkConfiguration updatedConnector = sinkManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Log sink id:", updatedConnector.getId(), sinkConfiguration.getId());
        assertEquals("Log sink name:", updatedConnector.getName(), entityGot.getName());
        assertEquals("Log sink type:", updatedConnector.getType().toString(), entityGot.getType().toString());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(logSinkBasePath + sinkConfiguration.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(sinkManager.findByPrimaryKey(sinkConfiguration.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(logSinkBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<LogSinkMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(1, item.getContent().size());
    }


    @Test
    public void listFiltersTest() throws Exception {

        RestResponse response = processRequest(logSinkBasePath + "?" +
                "type=FILE", HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

    }
}
