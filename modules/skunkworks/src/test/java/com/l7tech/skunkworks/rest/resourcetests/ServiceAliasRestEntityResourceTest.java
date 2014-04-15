package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.ServiceAliasMO;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.api.impl.ManagedObjectReference;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.PublishedServiceAlias;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceAliasManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;

import java.util.*;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ServiceAliasRestEntityResourceTest extends RestEntityTests<PublishedServiceAlias, ServiceAliasMO> {
    private ServiceAliasManager serviceAliasManager;
    private PolicyVersionManager policyVersionManager;
    private ServiceManager serviceManager;
    private FolderManager folderManager;

    private List<PublishedServiceAlias> publishedServiceAliases = new ArrayList<>();
    private Folder rootFolder;
    private Folder myServiceFolder;
    private Folder myAliasFolder;
    private Folder myEmptyFolder;
    private List<PublishedService> services = new ArrayList<>();

    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";

    @Before
    public void before() throws ObjectModelException {

        folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);
        serviceAliasManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceAliasManager", ServiceAliasManager.class);

        //create a test folder
        rootFolder = folderManager.findRootFolder();
        myServiceFolder = new Folder("MyServiceFolder", rootFolder);
        folderManager.save(myServiceFolder);
        myAliasFolder = new Folder("MyAliasFolder", rootFolder);
        folderManager.save(myAliasFolder);
        myEmptyFolder = new Folder("MyEmptyFolder", rootFolder);
        folderManager.save(myEmptyFolder);

        //create the published service
        for (int i = 0; i < 3; i++) {
            PublishedService service = new PublishedService();
            service.setName("Service" + i);
            service.setRoutingUri("/test" + i);
            service.getPolicy().setXml(POLICY);
            service.setFolder(myServiceFolder);
            service.setSoap(false);
            service.getPolicy().setGuid(UUID.randomUUID().toString());
            serviceManager.save(service);
            policyVersionManager.checkpointPolicy(service.getPolicy(), true, true);
            services.add(service);
        }


        //Create the PublishedServiceAliases
        PublishedServiceAlias publishedServiceAlias = new PublishedServiceAlias(services.get(0), myAliasFolder);
        serviceAliasManager.save(publishedServiceAlias);
        publishedServiceAliases.add(publishedServiceAlias);

        publishedServiceAlias = new PublishedServiceAlias(services.get(0), rootFolder);
        serviceAliasManager.save(publishedServiceAlias);
        publishedServiceAliases.add(publishedServiceAlias);

        publishedServiceAlias = new PublishedServiceAlias(services.get(1), myAliasFolder);
        serviceAliasManager.save(publishedServiceAlias);
        publishedServiceAliases.add(publishedServiceAlias);

        publishedServiceAlias = new PublishedServiceAlias(services.get(1), rootFolder);
        serviceAliasManager.save(publishedServiceAlias);
        publishedServiceAliases.add(publishedServiceAlias);

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<PublishedServiceAlias> all = serviceAliasManager.findAll();
        for (PublishedServiceAlias publishedServiceAlias : all) {
            serviceAliasManager.delete(publishedServiceAlias.getGoid());
        }

        for (PublishedService publishedService : services) {
            serviceManager.delete(publishedService.getGoid());
        }

        Collection<Folder> folders = folderManager.findAll();
        for (Folder folder : folders) {
            if (!Folder.ROOT_FOLDER_ID.equals(folder.getGoid())) {
                folderManager.delete(folder);
            }
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(publishedServiceAliases, new Functions.Unary<String, PublishedServiceAlias>() {
            @Override
            public String call(PublishedServiceAlias publishedServiceAlias) {
                return publishedServiceAlias.getId();
            }
        });
    }

    @Override
    public List<ServiceAliasMO> getCreatableManagedObjects() {
        List<ServiceAliasMO> serviceAliasMOs = new ArrayList<>();

        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setId(getGoid().toString());
        serviceAliasMO.setFolderId(myAliasFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, services.get(2).getId()));
        serviceAliasMOs.add(serviceAliasMO);

        serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setId(getGoid().toString());
        serviceAliasMO.setFolderId(rootFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, services.get(2).getId()));
        serviceAliasMOs.add(serviceAliasMO);

        return serviceAliasMOs;
    }

    @Override
    public List<ServiceAliasMO> getUpdateableManagedObjects() {
        List<ServiceAliasMO> serviceAliasMOs = new ArrayList<>();

        PublishedServiceAlias publishedServiceAlias = publishedServiceAliases.get(0);

        //change folder
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setId(publishedServiceAlias.getId());
        serviceAliasMO.setFolderId(myEmptyFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, publishedServiceAlias.getEntityGoid().toString()));
        serviceAliasMOs.add(serviceAliasMO);

        return serviceAliasMOs;
    }

    @Override
    public Map<ServiceAliasMO, Functions.BinaryVoid<ServiceAliasMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<ServiceAliasMO, Functions.BinaryVoid<ServiceAliasMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //already existing alias in folder
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId(myAliasFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, services.get(0).getId()));
        builder.put(serviceAliasMO, new Functions.BinaryVoid<ServiceAliasMO, RestResponse>() {
            @Override
            public void call(ServiceAliasMO serviceAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //service in same folder
        serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId(myServiceFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, services.get(0).getId()));
        builder.put(serviceAliasMO, new Functions.BinaryVoid<ServiceAliasMO, RestResponse>() {
            @Override
            public void call(ServiceAliasMO serviceAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //bad service
        serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId(myAliasFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, new Goid(123, 456).toString()));
        builder.put(serviceAliasMO, new Functions.BinaryVoid<ServiceAliasMO, RestResponse>() {
            @Override
            public void call(ServiceAliasMO serviceAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //bad folder
        serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setFolderId(new Goid(123, 456).toString());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, services.get(0).getId()));
        builder.put(serviceAliasMO, new Functions.BinaryVoid<ServiceAliasMO, RestResponse>() {
            @Override
            public void call(ServiceAliasMO serviceAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<ServiceAliasMO, Functions.BinaryVoid<ServiceAliasMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<ServiceAliasMO, Functions.BinaryVoid<ServiceAliasMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //move to service folder
        ServiceAliasMO serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setId(publishedServiceAliases.get(0).getId());
        serviceAliasMO.setFolderId(myServiceFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, publishedServiceAliases.get(0).getEntityGoid().toString()));
        builder.put(serviceAliasMO, new Functions.BinaryVoid<ServiceAliasMO, RestResponse>() {
            @Override
            public void call(ServiceAliasMO serviceAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //move to folder with alias already there
        serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setId(publishedServiceAliases.get(0).getId());
        serviceAliasMO.setFolderId(rootFolder.getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, publishedServiceAliases.get(0).getEntityGoid().toString()));
        builder.put(serviceAliasMO, new Functions.BinaryVoid<ServiceAliasMO, RestResponse>() {
            @Override
            public void call(ServiceAliasMO serviceAliasMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //try to change backing service
        serviceAliasMO = ManagedObjectFactory.createServiceAlias();
        serviceAliasMO.setId(publishedServiceAliases.get(0).getId());
        serviceAliasMO.setFolderId(publishedServiceAliases.get(0).getFolder().getId());
        serviceAliasMO.setServiceReference(new ManagedObjectReference(ServiceMO.class, services.get(2).getId()));
        builder.put(serviceAliasMO, new Functions.BinaryVoid<ServiceAliasMO, RestResponse>() {
            @Override
            public void call(ServiceAliasMO serviceAliasMO, RestResponse restResponse) {
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
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Functions.map(publishedServiceAliases, new Functions.Unary<String, PublishedServiceAlias>() {
            @Override
            public String call(PublishedServiceAlias publishedServiceAlias) {
                return publishedServiceAlias.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "serviceAliases";
    }

    @Override
    public String getType() {
        return EntityType.SERVICE_ALIAS.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        PublishedServiceAlias entity = serviceAliasManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        PublishedService publishedService = serviceManager.findByPrimaryKey(entity.getEntityGoid());
        Assert.assertNotNull(publishedService);
        return publishedService.getName() + " alias";
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        PublishedServiceAlias entity = serviceAliasManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, ServiceAliasMO managedObject) throws FindException {
        PublishedServiceAlias entity = serviceAliasManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getEntityGoid().toString(), managedObject.getServiceReference().getId());
            Assert.assertEquals(entity.getFolder().getId(), managedObject.getFolderId());
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(publishedServiceAliases, new Functions.Unary<String, PublishedServiceAlias>() {
                    @Override
                    public String call(PublishedServiceAlias publishedServiceAlias) {
                        return publishedServiceAlias.getId();
                    }
                }))
                .put("service.id=" + services.get(0).getId(), Arrays.asList(publishedServiceAliases.get(0).getId(), publishedServiceAliases.get(1).getId()))
                .put("service.id=" + services.get(0).getId() + "&service.id=" + services.get(1).getId(), Functions.map(publishedServiceAliases.subList(0, 4), new Functions.Unary<String, PublishedServiceAlias>() {
                    @Override
                    public String call(PublishedServiceAlias publishedServiceAlias) {
                        return publishedServiceAlias.getId();
                    }
                }))
                .put("service.id=" + new Goid(0, 0).toString(), Collections.<String>emptyList())
                .put("folder.id=" + myAliasFolder.getId(), Arrays.asList(publishedServiceAliases.get(0).getId(), publishedServiceAliases.get(2).getId()))
                .put("folder.id=" + rootFolder.getId(), Arrays.asList(publishedServiceAliases.get(1).getId(), publishedServiceAliases.get(3).getId()))
                .put("service.id=" + services.get(0).getId() + "&service.id=" + services.get(1).getId() + "&sort=service.id&order=desc", Arrays.asList(publishedServiceAliases.get(3).getId(), publishedServiceAliases.get(2).getId(), publishedServiceAliases.get(1).getId(), publishedServiceAliases.get(0).getId()))
                .map();
    }
}
