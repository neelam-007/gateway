package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URLEncoder;
import java.text.ParseException;
import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class InterfaceTagRestEntityResourceTest extends RestEntityTests<InterfaceTag, InterfaceTagMO> {
    private ClusterPropertyManager clusterPropertyManager;
    private List<InterfaceTag> interfaceTags = new ArrayList<>();
    private ClusterProperty interfaceTagClusterProperty;

    @Before
    public void before() throws SaveException {
        clusterPropertyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("clusterPropertyManager", ClusterPropertyManager.class);
        //Create the active connectors

        interfaceTags.add(new InterfaceTag("TestInterfaceTag1", CollectionUtils.set("123.456.789.123", "192.0.0.0/24", "2001:db8:85a3::8a2e:370:7334")));
        interfaceTags.add(new InterfaceTag("TestInterfaceTag2", Collections.<String>emptySet()));
        interfaceTags.add(new InterfaceTag("TestInterfaceTag3", CollectionUtils.set("123.456.789.444")));
        interfaceTags.add(new InterfaceTag("TestInterfaceTag4", CollectionUtils.set("2001:db8:85a3:0:0:8a2e:370:7334")));

        interfaceTagClusterProperty = new ClusterProperty(InterfaceTag.PROPERTY_NAME, InterfaceTag.toString(new HashSet<>(interfaceTags)));
        clusterPropertyManager.save(interfaceTagClusterProperty);
    }

    @After
    public void after() throws FindException, DeleteException {
        clusterPropertyManager.delete(interfaceTagClusterProperty.getGoid());
    }

    private String nameAsIdentifier( final String name ) {
        return UUID.nameUUIDFromBytes(name.getBytes(Charsets.UTF8)).toString();
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(interfaceTags, new Functions.Unary<String, InterfaceTag>() {
            @Override
            public String call(InterfaceTag interfaceTag) {
                return nameAsIdentifier(interfaceTag.getName());
            }
        });
    }

    @Override
    public List<InterfaceTagMO> getCreatableManagedObjects() {
        List<InterfaceTagMO> interfaceTagMOs = new ArrayList<>();

        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier("MyNewInterfaceTag"));
        interfaceTagMO.setName("MyNewInterfaceTag");
        interfaceTagMO.setAddressPatterns(Arrays.asList("127.0.0.1"));
        interfaceTagMOs.add(interfaceTagMO);

        interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier("MyNewInterfaceTag2"));
        interfaceTagMO.setName("MyNewInterfaceTag2");
        interfaceTagMO.setAddressPatterns(Collections.<String>emptyList());
        interfaceTagMOs.add(interfaceTagMO);

        return interfaceTagMOs;
    }

    @BugId("SSG-8203")
    @Override
    @Test
    public void testCreateWithIdEntity() throws Exception {
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier("MyNewInterfaceTag"));
        interfaceTagMO.setName("MyNewInterfaceTag");
        interfaceTagMO.setAddressPatterns(Arrays.asList("127.0.0.1"));

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + interfaceTagMO.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(interfaceTagMO)));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 403, response.getStatus());
    }

    @Override
    @Test
    public void testCreateEntitySpecifyIDFailed() throws Exception {
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier("MyNewInterfaceTag"));
        interfaceTagMO.setName("MyNewInterfaceTag");
        interfaceTagMO.setAddressPatterns(Arrays.asList("127.0.0.1"));

        //specify id in mo but not in url
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(interfaceTagMO)));

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 400, response.getStatus());
    }

    @Override
    public List<InterfaceTagMO> getUpdateableManagedObjects() {
        List<InterfaceTagMO> interfaceTagMOs = new ArrayList<>();

        InterfaceTag interfaceTag = this.interfaceTags.get(0);
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier(interfaceTag.getName()));
        interfaceTagMO.setName(interfaceTag.getName());
        interfaceTagMO.setAddressPatterns(new ArrayList<String>());
        interfaceTagMOs.add(interfaceTagMO);

        interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier(interfaceTag.getName()));
        interfaceTagMO.setName(interfaceTag.getName());
        interfaceTagMO.setAddressPatterns(Arrays.asList("127.0.0.1"));
        interfaceTagMOs.add(interfaceTagMO);

        return interfaceTagMOs;
    }

    @Override
    public Map<InterfaceTagMO, Functions.BinaryVoid<InterfaceTagMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<InterfaceTagMO, Functions.BinaryVoid<InterfaceTagMO, RestResponse>> mapBuilder = CollectionUtils.MapBuilder.builder();

        InterfaceTag interfaceTag = this.interfaceTags.get(0);
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setName(interfaceTag.getName());
        interfaceTagMO.setAddressPatterns(new ArrayList<String>());
        mapBuilder.put(interfaceTagMO, new Functions.BinaryVoid<InterfaceTagMO, RestResponse>() {
            @Override
            public void call(InterfaceTagMO interfaceTagMO, RestResponse restResponse) {
                Assert.assertEquals(403, restResponse.getStatus());
            }
        });

        interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setName("bad name tag");
        interfaceTagMO.setAddressPatterns(new ArrayList<String>());
        mapBuilder.put(interfaceTagMO, new Functions.BinaryVoid<InterfaceTagMO, RestResponse>() {
            @Override
            public void call(InterfaceTagMO interfaceTagMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setName("badiptag");
        interfaceTagMO.setAddressPatterns(Arrays.asList("this is a bad ip"));
        mapBuilder.put(interfaceTagMO, new Functions.BinaryVoid<InterfaceTagMO, RestResponse>() {
            @Override
            public void call(InterfaceTagMO interfaceTagMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return mapBuilder.map();
    }

    @Override
    public Map<InterfaceTagMO, Functions.BinaryVoid<InterfaceTagMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<InterfaceTagMO, Functions.BinaryVoid<InterfaceTagMO, RestResponse>> mapBuilder = CollectionUtils.MapBuilder.builder();

        InterfaceTag interfaceTag = this.interfaceTags.get(0);
        InterfaceTagMO interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier(interfaceTag.getName()));
        interfaceTagMO.setName(interfaceTag.getName());
        interfaceTagMO.setAddressPatterns(Arrays.asList("this is a bad ip"));
        mapBuilder.put(interfaceTagMO, new Functions.BinaryVoid<InterfaceTagMO, RestResponse>() {
            @Override
            public void call(InterfaceTagMO interfaceTagMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8244
        interfaceTagMO = ManagedObjectFactory.createInterfaceTag();
        interfaceTagMO.setId(nameAsIdentifier(interfaceTag.getName()));
        interfaceTagMO.setName(interfaceTag.getName() + "Update");
        interfaceTagMO.setAddressPatterns(new ArrayList<>(interfaceTag.getIpPatterns()));
        mapBuilder.put(interfaceTagMO, new Functions.BinaryVoid<InterfaceTagMO, RestResponse>() {
            @Override
            public void call(InterfaceTagMO interfaceTagMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return mapBuilder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(interfaceTags, new Functions.Unary<String, InterfaceTag>() {
            @Override
            public String call(InterfaceTag interfaceTag) {
                return nameAsIdentifier(interfaceTag.getName());
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "interfaceTags";
    }

    @Override
    public String getType() {
        return "INTERFACE_TAG";
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        ClusterProperty entity = clusterPropertyManager.findByUniqueName(InterfaceTag.PROPERTY_NAME);
        Assert.assertNotNull(entity);
        Set<InterfaceTag> interfaceTagsFound;
        try {
            interfaceTagsFound = InterfaceTag.parseMultiple(entity.getValue());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        for(InterfaceTag interfaceTag : interfaceTagsFound){
            if(id.equals(nameAsIdentifier(interfaceTag.getName()))){
                return interfaceTag.getName();
            }
        }
        Assert.fail("Could not find interface tag with name: " + id);
        return null;
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        ClusterProperty entity = clusterPropertyManager.findByUniqueName(InterfaceTag.PROPERTY_NAME);
        Assert.assertNotNull(entity);
        Set<InterfaceTag> interfaceTagsFound;
        try {
            interfaceTagsFound = InterfaceTag.parseMultiple(entity.getValue());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        for(InterfaceTag interfaceTag : interfaceTagsFound){
            if(id.equals(nameAsIdentifier(interfaceTag.getName()))){
                return;
            }
        }
        Assert.fail("Could not find interface tag with name: " + id);
    }

    @Override
    public void verifyEntity(String id, InterfaceTagMO managedObject) throws FindException {
        ClusterProperty entity = clusterPropertyManager.findByUniqueName(InterfaceTag.PROPERTY_NAME);
        Assert.assertNotNull(entity);
        Set<InterfaceTag> interfaceTagsFound;
        try {
            interfaceTagsFound = InterfaceTag.parseMultiple(entity.getValue());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        for(InterfaceTag interfaceTag : interfaceTagsFound){
            if(id.equals(nameAsIdentifier(interfaceTag.getName()))){
                Assert.assertNotNull("Found interface tag with ID: " + id + " that should not exist.", managedObject);
                Assert.assertEquals(interfaceTag.getName(), managedObject.getName());
                Assert.assertNull("Interface tags should not have version numbers: SSG-8178", managedObject.getVersion());
                org.junit.Assert.assertArrayEquals(interfaceTag.getIpPatterns().toArray(), managedObject.getAddressPatterns().toArray());
                return;
            }
        }
        Assert.assertNull("Could not find interface tag with name: " + id, managedObject);
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Arrays.asList(nameAsIdentifier(interfaceTags.get(2).getName()), nameAsIdentifier(interfaceTags.get(3).getName()), nameAsIdentifier(interfaceTags.get(1).getName()), nameAsIdentifier(interfaceTags.get(0).getName())))
                .put("name=" + URLEncoder.encode(interfaceTags.get(0).getName()), Arrays.asList(nameAsIdentifier(interfaceTags.get(0).getName())))
                .put("name=banName", Collections.<String>emptyList())
                //Bug: SSG-8379
                .put("name=" + URLEncoder.encode(interfaceTags.get(0).getName()) + "&name=" + URLEncoder.encode(interfaceTags.get(1).getName()) + "&sort=name", Arrays.asList(nameAsIdentifier(interfaceTags.get(0).getName()), nameAsIdentifier(interfaceTags.get(1).getName())))
                .put("name=" + URLEncoder.encode(interfaceTags.get(0).getName()) + "&name=" + URLEncoder.encode(interfaceTags.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(nameAsIdentifier(interfaceTags.get(1).getName()), nameAsIdentifier(interfaceTags.get(0).getName())))
                .map();
    }

    @Override
    @Test
    public void testListEntitiesUnprivileged() throws Exception {
        //test unprivileged for interface tags trying to list with an unprivileged user returns a privileged error exception.
        InternalUser user = createUnprivilegedUser();
        try {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "", HttpMethod.GET, null, "", null, user);
            Assert.assertEquals(401, response.getStatus());
        } finally {
            userManager.delete(user);
        }
    }
}
