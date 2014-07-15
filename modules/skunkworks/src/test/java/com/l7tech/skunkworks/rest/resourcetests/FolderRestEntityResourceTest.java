package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.FolderMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class FolderRestEntityResourceTest extends RestEntityTests<Folder, FolderMO> {
    private static final Logger logger = Logger.getLogger(FolderRestEntityResourceTest.class.getName());

    private FolderManager folderManager;
    private Folder rootFolder;
    private List<Folder> folders = new ArrayList<>();

    @Before
    public void before() throws SaveException, FindException {
        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        rootFolder = folderManager.findRootFolder();
        //Create the active connectors

        Folder folder = new Folder("Folder 1",rootFolder);
        folderManager.save(folder);
        folderManager.createRoles(folder);
        folders.add(folder);

        folder = new Folder("Folder 2",rootFolder);
        folderManager.save(folder);
        folderManager.createRoles(folder);
        folders.add(folder);
        
        folder = new Folder("Folder 3",rootFolder);
        folderManager.save(folder);
        folderManager.createRoles(folder);
        folders.add(folder);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<Folder> all = folderManager.findAll();
        for (Folder folder : all) {
            if(folder.getFolder()!=null) {
                folderManager.delete(folder.getGoid());
            }
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(folders, new Functions.Unary<String, Folder>() {
            @Override
            public String call(Folder folder) {
                return folder.getId();
            }
        });
    }

    @Override
    public List<FolderMO> getCreatableManagedObjects() {
        List<FolderMO> folderMOs = new ArrayList<>();

        FolderMO folder = ManagedObjectFactory.createFolder();
        folder.setId(getGoid().toString());
        folder.setName("Folder created");
        folder.setFolderId(rootFolder.getId());
        folderMOs.add(folder);

        return folderMOs;
    }

    @Override
    public List<FolderMO> getUpdateableManagedObjects() {
        List<FolderMO> folderMOs = new ArrayList<>();

        Folder folder = this.folders.get(0);
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setId(folder.getId());
        folderMO.setName(folder.getName() + " Updated");
        folderMO.setFolderId(folder.getFolder().getId());
        folderMOs.add(folderMO);

        //update twice
        folderMO = ManagedObjectFactory.createFolder();
        folderMO.setId(folder.getId());
        folderMO.setName(folder.getName() + " Updated");
        folderMO.setFolderId(folder.getFolder().getId());
        folderMOs.add(folderMO);
        return folderMOs;
    }

    @Override
    public Map<FolderMO, Functions.BinaryVoid<FolderMO, RestResponse>> getUnCreatableManagedObjects() {
        return Collections.emptyMap();
    }

    @Override
    public Map<FolderMO, Functions.BinaryVoid<FolderMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<FolderMO, Functions.BinaryVoid<FolderMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //same name as another connection
        Folder folder = folders.get(0);
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setId(folder.getId());
        folderMO.setName(folders.get(1).getName());
        folderMO.setFolderId(folder.getFolder().getId());
        builder.put(folderMO, new Functions.BinaryVoid<FolderMO, RestResponse>() {
            @Override
            public void call(FolderMO folderMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        builder.put(rootFolder.getId(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String id, RestResponse restResponse) {
                Assert.assertEquals(403, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(folders, new Functions.Unary<String, Folder>() {
            @Override
            public String call(Folder folder) {
                return folder.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "folders";
    }

    @Override
    public String getType() {
        return EntityType.FOLDER.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        Folder entity = folderManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        Folder entity = folderManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, FolderMO managedObject) throws FindException {
        Folder entity = folderManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getFolder().getId(), managedObject.getFolderId());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Arrays.asList(rootFolder.getId(),folders.get(0).getId(),folders.get(1).getId(),folders.get(2).getId()))
                .put("name=" + URLEncoder.encode(folders.get(0).getName()), Arrays.asList(folders.get(0).getId()))
                .put("name=" + URLEncoder.encode(folders.get(0).getName()) + "&name=" + URLEncoder.encode(folders.get(1).getName()), Functions.map(folders.subList(0, 2), new Functions.Unary<String, Folder>() {
                    @Override
                    public String call(Folder folder) {
                        return folder.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("parentFolder.id="+URLEncoder.encode(rootFolder.getId()) , Arrays.asList(folders.get(0).getId(), folders.get(1).getId(), folders.get(2).getId()))
                .put("name=" + URLEncoder.encode(folders.get(0).getName()) + "&name=" + URLEncoder.encode(folders.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(folders.get(1).getId(), folders.get(0).getId()))
                .map();
    }

    @BugId("SSG-8052")
    @Test
    public void testRecursiveFolderDeletion() throws Exception {
        String bundle1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:References>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>canga</l7:Name>\n" +
                "            <l7:Id>e7f96aeb531dcc79c0dfa699d714967a</l7:Id>\n" +
                "            <l7:Type>FOLDER</l7:Type>\n" +
                "            <l7:TimeStamp>2014-07-09T12:01:32.434-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:Folder folderId=\"0000000000000000ffffffffffffec76\" id=\"e7f96aeb531dcc79c0dfa699d714967a\" version=\"1\">\n" +
                "                    <l7:Name>canga</l7:Name>\n" +
                "                </l7:Folder>\n" +
                "            </l7:Resource>\n" +
                "        </l7:Item>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>and</l7:Name>\n" +
                "            <l7:Id>e7f96aeb531dcc79c0dfa699d71496bd</l7:Id>\n" +
                "            <l7:Type>FOLDER</l7:Type>\n" +
                "            <l7:TimeStamp>2014-07-09T12:01:32.434-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:Folder folderId=\"e7f96aeb531dcc79c0dfa699d714967a\" id=\"e7f96aeb531dcc79c0dfa699d71496bd\" version=\"1\">\n" +
                "                    <l7:Name>and</l7:Name>\n" +
                "                </l7:Folder>\n" +
                "            </l7:Resource>\n" +
                "        </l7:Item>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>roo</l7:Name>\n" +
                "            <l7:Id>e7f96aeb531dcc79c0dfa699d71496ff</l7:Id>\n" +
                "            <l7:Type>FOLDER</l7:Type>\n" +
                "            <l7:TimeStamp>2014-07-09T12:01:32.434-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:Folder folderId=\"e7f96aeb531dcc79c0dfa699d714967a\" id=\"e7f96aeb531dcc79c0dfa699d71496ff\" version=\"1\">\n" +
                "                    <l7:Name>roo</l7:Name>\n" +
                "                </l7:Folder>\n" +
                "            </l7:Resource>\n" +
                "        </l7:Item>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>p1</l7:Name>\n" +
                "            <l7:Id>e7f96aeb531dcc79c0dfa699d71497b2</l7:Id>\n" +
                "            <l7:Type>POLICY</l7:Type>\n" +
                "            <l7:TimeStamp>2014-07-09T12:01:32.435-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:Policy guid=\"814b67a7-c457-465f-b44e-7f9a908545fb\" id=\"e7f96aeb531dcc79c0dfa699d71497b2\" version=\"1\">\n" +
                "                    <l7:PolicyDetail folderId=\"e7f96aeb531dcc79c0dfa699d71496ff\" guid=\"814b67a7-c457-465f-b44e-7f9a908545fb\" id=\"e7f96aeb531dcc79c0dfa699d71497b2\" version=\"1\">\n" +
                "                        <l7:Name>p1</l7:Name>\n" +
                "                        <l7:PolicyType>Include</l7:PolicyType>\n" +
                "                        <l7:Properties>\n" +
                "                            <l7:Property key=\"revision\">\n" +
                "                                <l7:LongValue>1</l7:LongValue>\n" +
                "                            </l7:Property>\n" +
                "                            <l7:Property key=\"soap\">\n" +
                "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                            </l7:Property>\n" +
                "                        </l7:Properties>\n" +
                "                    </l7:PolicyDetail>\n" +
                "                    <l7:Resources>\n" +
                "                        <l7:ResourceSet tag=\"policy\">\n" +
                "                            <l7:Resource type=\"policy\"><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:AuditDetailAssertion>\n" +
                "            <L7p:Detail stringValue=\"Policy Fragment: POLICY_FRAGMENT_11_5_8_1\"/>\n" +
                "        </L7p:AuditDetailAssertion>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>]]></l7:Resource>\n" +
                "                        </l7:ResourceSet>\n" +
                "                    </l7:Resources>\n" +
                "                </l7:Policy>\n" +
                "            </l7:Resource>\n" +
                "        </l7:Item>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>p1 alias</l7:Name>\n" +
                "            <l7:Id>e7f96aeb531dcc79c0dfa699d71497c9</l7:Id>\n" +
                "            <l7:Type>POLICY_ALIAS</l7:Type>\n" +
                "            <l7:TimeStamp>2014-07-09T12:01:32.436-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:PolicyAlias folderId=\"e7f96aeb531dcc79c0dfa699d71496bd\" id=\"e7f96aeb531dcc79c0dfa699d71497c9\" version=\"0\">\n" +
                "                    <l7:PolicyReference id=\"e7f96aeb531dcc79c0dfa699d71497b2\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
                "                </l7:PolicyAlias>\n" +
                "            </l7:Resource>\n" +
                "        </l7:Item>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>s1</l7:Name>\n" +
                "            <l7:Id>e7f96aeb531dcc79c0dfa699d714975b</l7:Id>\n" +
                "            <l7:Type>SERVICE</l7:Type>\n" +
                "            <l7:TimeStamp>2014-07-09T12:01:32.437-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:Service id=\"e7f96aeb531dcc79c0dfa699d714975b\" version=\"4\">\n" +
                "                    <l7:ServiceDetail folderId=\"e7f96aeb531dcc79c0dfa699d714967a\" id=\"e7f96aeb531dcc79c0dfa699d714975b\" version=\"4\">\n" +
                "                        <l7:Name>s1</l7:Name>\n" +
                "                        <l7:Enabled>true</l7:Enabled>\n" +
                "                        <l7:ServiceMappings>\n" +
                "                            <l7:HttpMapping>\n" +
                "                                <l7:UrlPattern>/s1</l7:UrlPattern>\n" +
                "                                <l7:Verbs>\n" +
                "                                    <l7:Verb>GET</l7:Verb>\n" +
                "                                    <l7:Verb>POST</l7:Verb>\n" +
                "                                    <l7:Verb>PUT</l7:Verb>\n" +
                "                                    <l7:Verb>DELETE</l7:Verb>\n" +
                "                                </l7:Verbs>\n" +
                "                            </l7:HttpMapping>\n" +
                "                        </l7:ServiceMappings>\n" +
                "                        <l7:Properties>\n" +
                "                            <l7:Property key=\"policyRevision\">\n" +
                "                                <l7:LongValue>1</l7:LongValue>\n" +
                "                            </l7:Property>\n" +
                "                            <l7:Property key=\"wssProcessingEnabled\">\n" +
                "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                            </l7:Property>\n" +
                "                            <l7:Property key=\"soap\">\n" +
                "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                            </l7:Property>\n" +
                "                            <l7:Property key=\"internal\">\n" +
                "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                            </l7:Property>\n" +
                "                            <l7:Property key=\"tracingEnabled\">\n" +
                "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
                "                            </l7:Property>\n" +
                "                        </l7:Properties>\n" +
                "                    </l7:ServiceDetail>\n" +
                "                    <l7:Resources>\n" +
                "                        <l7:ResourceSet tag=\"policy\">\n" +
                "                            <l7:Resource type=\"policy\" version=\"1\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "&lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    &lt;wsp:All wsp:Usage=\"Required\"/>\n" +
                "&lt;/wsp:Policy></l7:Resource>\n" +
                "    </l7:ResourceSet>\n" +
                "</l7:Resources>\n" +
                "</l7:Service>\n" +
                "</l7:Resource>\n" +
                "</l7:Item>\n" +
                "<l7:Item>\n" +
                "    <l7:Name>s1 alias</l7:Name>\n" +
                "    <l7:Id>e7f96aeb531dcc79c0dfa699d71497a2</l7:Id>\n" +
                "    <l7:Type>SERVICE_ALIAS</l7:Type>\n" +
                "    <l7:TimeStamp>2014-07-09T12:01:32.438-07:00</l7:TimeStamp>\n" +
                "    <l7:Resource>\n" +
                "        <l7:ServiceAlias folderId=\"e7f96aeb531dcc79c0dfa699d71496bd\" id=\"e7f96aeb531dcc79c0dfa699d71497a2\" version=\"0\">\n" +
                "            <l7:ServiceReference id=\"e7f96aeb531dcc79c0dfa699d714975b\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/services\"/>\n" +
                "        </l7:ServiceAlias>\n" +
                "    </l7:Resource>\n" +
                "</l7:Item>\n" +
                "</l7:References>\n" +
                "<l7:Mappings>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"0000000000000000ffffffffffffec76\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/folders/0000000000000000ffffffffffffec76\" type=\"FOLDER\">\n" +
                "        <l7:Properties>\n" +
                "            <l7:Property key=\"FailOnNew\">\n" +
                "                <l7:BooleanValue>true</l7:BooleanValue>\n" +
                "            </l7:Property>\n" +
                "        </l7:Properties>\n" +
                "    </l7:Mapping>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"e7f96aeb531dcc79c0dfa699d714967a\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/folders/e7f96aeb531dcc79c0dfa699d714967a\" type=\"FOLDER\"/>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"e7f96aeb531dcc79c0dfa699d71496bd\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/folders/e7f96aeb531dcc79c0dfa699d71496bd\" type=\"FOLDER\"/>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"e7f96aeb531dcc79c0dfa699d71496ff\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/folders/e7f96aeb531dcc79c0dfa699d71496ff\" type=\"FOLDER\"/>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"e7f96aeb531dcc79c0dfa699d71497b2\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/policies/e7f96aeb531dcc79c0dfa699d71497b2\" type=\"POLICY\"/>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"e7f96aeb531dcc79c0dfa699d71497c9\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/policyAliases/e7f96aeb531dcc79c0dfa699d71497c9\" type=\"POLICY_ALIAS\"/>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"e7f96aeb531dcc79c0dfa699d714975b\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/services/e7f96aeb531dcc79c0dfa699d714975b\" type=\"SERVICE\"/>\n" +
                "    <l7:Mapping action=\"NewOrExisting\" srcId=\"e7f96aeb531dcc79c0dfa699d71497a2\" srcUri=\"http://ssg82spp.ca.com:8080/restman/1.0/serviceAliases/e7f96aeb531dcc79c0dfa699d71497a2\" type=\"SERVICE_ALIAS\"/>\n" +
                "</l7:Mappings>\n" +
                "</l7:Bundle>\n";


        RestResponse response = processRequest("bundle", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), bundle1);
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());

        response = processRequest("folders/e7f96aeb531dcc79c0dfa699d714967a?force=true", HttpMethod.DELETE, ContentType.APPLICATION_XML.toString(), bundle1);

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(204, response.getStatus());
    }

}
