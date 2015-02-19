package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.ClusterPropertyMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.objectmodel.*;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ClusterPropertyRestEntityResourceTest extends RestEntityTests<ClusterProperty, ClusterPropertyMO> {
    private static final Logger logger = Logger.getLogger(ClusterPropertyRestEntityResourceTest.class.getName());

    private ClusterPropertyManager clusterPropertyManager;
    private List<ClusterProperty> clusterProperties = new ArrayList<>();

    @Before
    public void before() throws SaveException {
        clusterPropertyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("clusterPropertyManager", ClusterPropertyManager.class);
        //Create the cluster properties

        ClusterProperty clusterProperty = new ClusterProperty("prop","value");
        clusterPropertyManager.save(clusterProperty);
        clusterProperties.add(clusterProperty);

        clusterProperty = new ClusterProperty("prop 2","value 2");
        clusterPropertyManager.save(clusterProperty);
        clusterProperties.add(clusterProperty);

        clusterProperty = new ClusterProperty("prop 3","value 3");
        clusterPropertyManager.save(clusterProperty);
        clusterProperties.add(clusterProperty);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<ClusterProperty> all = clusterPropertyManager.findAll();
        for (ClusterProperty clusterProperty : all) {
            if(!Goid.equals(new Goid(0,-700001), clusterProperty.getGoid())) {
                clusterPropertyManager.delete(clusterProperty.getGoid());
            }
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(clusterProperties, new Functions.Unary<String, ClusterProperty>() {
            @Override
            public String call(ClusterProperty clusterProperty) {
                return clusterProperty.getId();
            }
        });
    }

    @Override
    public List<ClusterPropertyMO> getCreatableManagedObjects() {
        List<ClusterPropertyMO> clusterPropertyMOs = new ArrayList<>();

        ClusterPropertyMO clusterProperty = ManagedObjectFactory.createClusterProperty();
        clusterProperty.setId(getGoid().toString());
        clusterProperty.setName("Cluster Property created");
        clusterProperty.setValue("Cluster Property created value");

        clusterPropertyMOs.add(clusterProperty);
        return clusterPropertyMOs;
    }

    @Override
    public List<ClusterPropertyMO> getUpdateableManagedObjects() {
        List<ClusterPropertyMO> clusterPropertyMOs = new ArrayList<>();

        ClusterProperty clusterProperty = this.clusterProperties.get(0);
        ClusterPropertyMO clusterPropertyMO = ManagedObjectFactory.createClusterProperty();
        clusterPropertyMO.setId(clusterProperty.getId());
        clusterPropertyMO.setName(clusterProperty.getName());
        clusterPropertyMO.setValue(clusterProperty.getValue() + " Updated");
        clusterPropertyMOs.add(clusterPropertyMO);

        //update twice
        clusterPropertyMO = ManagedObjectFactory.createClusterProperty();
        clusterPropertyMO.setId(clusterProperty.getId());
        clusterPropertyMO.setName(clusterProperty.getName());
        clusterPropertyMO.setValue(clusterProperty.getValue() + " Updated");
        clusterPropertyMOs.add(clusterPropertyMO);
        return clusterPropertyMOs;
    }

    @Override
    public Map<ClusterPropertyMO, Functions.BinaryVoid<ClusterPropertyMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<ClusterPropertyMO, Functions.BinaryVoid<ClusterPropertyMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        ClusterPropertyMO clusterProperty = ManagedObjectFactory.createClusterProperty();
        clusterProperty.setName(clusterProperties.get(0).getName());
        clusterProperty.setValue("Value");
        builder.put(clusterProperty, new Functions.BinaryVoid<ClusterPropertyMO, RestResponse>() {
            @Override
            public void call(ClusterPropertyMO clusterPropertyMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<ClusterPropertyMO, Functions.BinaryVoid<ClusterPropertyMO, RestResponse>> getUnUpdateableManagedObjects() {
        return Collections.emptyMap();
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
        return Functions.map(clusterProperties, new Functions.Unary<String, ClusterProperty>() {
            @Override
            public String call(ClusterProperty clusterProperty) {
                return clusterProperty.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "clusterProperties";
    }

    @Override
    public String getType() {
        return EntityType.CLUSTER_PROPERTY.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        ClusterProperty entity = clusterPropertyManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        ClusterProperty entity = clusterPropertyManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, ClusterPropertyMO managedObject) throws FindException {
        ClusterProperty entity = clusterPropertyManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.getValue(), managedObject.getValue());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        List<String> allClusterPropertiesIds = Functions.map(clusterProperties, new Functions.Unary<String, ClusterProperty>() {
            @Override
            public String call(ClusterProperty clusterProperty) {
                return clusterProperty.getId();
            }
        });
        //adds the cluster hostname
        allClusterPropertiesIds.add(0, new Goid(0,-700001).toString());
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", allClusterPropertiesIds)
                .put("name=" + URLEncoder.encode(clusterProperties.get(0).getName()), Arrays.asList(clusterProperties.get(0).getId()))
                .put("name=" + URLEncoder.encode(clusterProperties.get(0).getName()) + "&name=" + URLEncoder.encode(clusterProperties.get(1).getName()), Functions.map(clusterProperties.subList(0, 2), new Functions.Unary<String, ClusterProperty>() {
                    @Override
                    public String call(ClusterProperty clusterProperty) {
                        return clusterProperty.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("name=" + URLEncoder.encode(clusterProperties.get(0).getName()) + "&name=" + URLEncoder.encode(clusterProperties.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(clusterProperties.get(1).getId(), clusterProperties.get(0).getId()))
                .map();
    }
}
