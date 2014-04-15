package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.EmailListenerMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.gateway.common.transport.email.EmailListener;
import com.l7tech.gateway.common.transport.email.EmailServerType;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.transport.email.EmailListenerManager;
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

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class EmailListenerRestEntityResourceTest extends RestEntityTests<EmailListener, EmailListenerMO> {
    private EmailListenerManager emailListenerManager;
    private ServiceManager serviceManager;

    private static final String POLICY = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:AuditAssertion/></wsp:All></wsp:Policy>";


    private static final PublishedService service = new PublishedService();
    private List<EmailListener> emailListeners = new ArrayList<>();

    @Before
    public void before() throws SaveException, FindException {
        emailListenerManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("emailListenerManager", EmailListenerManager.class);
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);

        FolderManager folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        Folder rootFolder = folderManager.findRootFolder();

        service.setName("Service1");
        service.setRoutingUri("/test");
        service.setPolicy(new Policy(PolicyType.INCLUDE_FRAGMENT, "Service1 Policy", POLICY, false));
        service.setFolder(rootFolder);
        service.setSoap(false);
        service.getPolicy().setGuid(UUID.randomUUID().toString());
        serviceManager.save(service);

        //Create the active connectors

        EmailListener emailListener = new EmailListener();
        emailListener.setName("Test Email Listener");
        emailListener.setActive(false);
        emailListener.setDeleteOnReceive(false);
        emailListener.setFolder("Inbox");
        emailListener.setHost("localhost");
        emailListener.setPassword("myPass");
        emailListener.setPollInterval(1000);
        emailListener.setPort(8080);
        emailListener.setUseSsl(true);
        emailListener.setUsername("User");
        emailListener.setServerType(EmailServerType.IMAP);
        Properties properties = new Properties();
        properties.setProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE, (Boolean.TRUE).toString());
        properties.setProperty(EmailListener.PROP_HARDWIRED_SERVICE_ID, service.getId());
        emailListener.properties(properties);

        emailListenerManager.save(emailListener);
        emailListeners.add(emailListener);

        emailListener = new EmailListener();
        emailListener.setName("Test Email Listener 2");
        emailListener.setActive(true);
        emailListener.setDeleteOnReceive(false);
        emailListener.setFolder("Inbox");
        emailListener.setHost("localhost");
        emailListener.setPassword("myPass");
        emailListener.setPollInterval(1000);
        emailListener.setPort(8080);
        emailListener.setUseSsl(true);
        emailListener.setUsername("User");
        emailListener.setServerType(EmailServerType.POP3);

        emailListenerManager.save(emailListener);
        emailListeners.add(emailListener);
    }

    @After
    public void after() throws FindException, DeleteException {
        ArrayList<ServiceHeader> services = new ArrayList<>(serviceManager.findAllHeaders());
        for (EntityHeader service : services) {
            serviceManager.delete(service.getGoid());
        }

        Collection<EmailListener> all = emailListenerManager.findAll();
        for (EmailListener emailListener : all) {
            emailListenerManager.delete(emailListener.getGoid());
        }
    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(emailListeners, new Functions.Unary<String, EmailListener>() {
            @Override
            public String call(EmailListener emailListener) {
                return emailListener.getId();
            }
        });
    }

    @Override
    public List<EmailListenerMO> getCreatableManagedObjects() {
        List<EmailListenerMO> emailListenerMOs = new ArrayList<>();

        EmailListenerMO emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setId(getGoid().toString());
        emailListenerMO.setName("Test Email listener created");
        emailListenerMO.setActive(true);
        emailListenerMO.setHostname("remoteHost");
        emailListenerMO.setPort(123);
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.POP3);
        emailListenerMO.setDeleteOnReceive(false);
        emailListenerMO.setUsername("AUser");
        emailListenerMO.setPassword("UserPass");
        emailListenerMO.setFolder("MyFolder");
        emailListenerMO.setPollInterval(5000);
        emailListenerMO.setUseSsl(false);
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(EmailListener.PROP_IS_HARDWIRED_SERVICE, (Boolean.TRUE).toString())
                .put(EmailListener.PROP_HARDWIRED_SERVICE_ID, service.getId())
                .map());
        emailListenerMOs.add(emailListenerMO);

        return emailListenerMOs;
    }

    @Override
    public List<EmailListenerMO> getUpdateableManagedObjects() {
        List<EmailListenerMO> emailListenerMOs = new ArrayList<>();

        EmailListener emailListener = this.emailListeners.get(0);
        EmailListenerMO emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setId(emailListener.getId());
        emailListenerMO.setName(emailListener.getName() + "Updated");
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(emailListener.getPort());
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(EmailListener.PROP_IS_HARDWIRED_SERVICE, emailListener.properties().getProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE))
                .put(EmailListener.PROP_HARDWIRED_SERVICE_ID, emailListener.properties().getProperty(EmailListener.PROP_HARDWIRED_SERVICE_ID))
                .map());
        emailListenerMOs.add(emailListenerMO);
        return emailListenerMOs;
    }

    @Override
    public Map<EmailListenerMO, Functions.BinaryVoid<EmailListenerMO, RestResponse>> getUnCreatableManagedObjects() {
        CollectionUtils.MapBuilder<EmailListenerMO, Functions.BinaryVoid<EmailListenerMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        EmailListener emailListener = this.emailListeners.get(0);

        EmailListenerMO emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName(emailListener.getName());
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(emailListener.getPort());
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(EmailListener.PROP_IS_HARDWIRED_SERVICE, emailListener.properties().getProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE))
                .put(EmailListener.PROP_HARDWIRED_SERVICE_ID, emailListener.properties().getProperty(EmailListener.PROP_HARDWIRED_SERVICE_ID))
                .map());

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8175 create an email listener with an invalid hardwired service id
        emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName(emailListener.getName() + "different");
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(emailListener.getPort());
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));
        emailListenerMO.setProperties(CollectionUtils.MapBuilder.<String, String>builder()
                .put(EmailListener.PROP_IS_HARDWIRED_SERVICE, emailListener.properties().getProperty(EmailListener.PROP_IS_HARDWIRED_SERVICE))
                .put(EmailListener.PROP_HARDWIRED_SERVICE_ID, new Goid(0,0).toString())
                .map());

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8167 create an email listener with poll interval 0
        emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName(emailListener.getName() + "different");
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(0);
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(emailListener.getPort());
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8166 create an email listener with an invalid port
        emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName(emailListener.getName() + "different");
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(-123);
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName(emailListener.getName() + "different");
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(65536);
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8165 create an email listener with an empty hostname
        emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName(emailListener.getName() + "different");
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname("");
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(emailListener.getPort());
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8164 create an email listener with an name
        emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName("");
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setUseSsl(emailListener.isUseSsl());
        emailListenerMO.setPort(emailListener.getPort());
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        //SSG-8150 create an email listener missing use ssl
        emailListenerMO = ManagedObjectFactory.createEmailListener();
        emailListenerMO.setName(emailListener.getName());
        emailListenerMO.setActive(emailListener.isActive());
        emailListenerMO.setDeleteOnReceive(emailListener.isDeleteOnReceive());
        emailListenerMO.setFolder(emailListener.getFolder());
        emailListenerMO.setHostname(emailListener.getHost());
        emailListenerMO.setPassword(emailListener.getPassword());
        emailListenerMO.setPollInterval(emailListener.getPollInterval());
        emailListenerMO.setPort(emailListener.getPort());
        emailListenerMO.setUsername(emailListener.getUsername());
        emailListenerMO.setServerType(EmailListenerMO.EmailServerType.valueOf(emailListener.getServerType().toString()));

        builder.put(emailListenerMO, new Functions.BinaryVoid<EmailListenerMO, RestResponse>() {
            @Override
            public void call(EmailListenerMO activeConnectorMO, RestResponse restResponse) {
                Assert.assertEquals(400, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<EmailListenerMO, Functions.BinaryVoid<EmailListenerMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<EmailListenerMO, Functions.BinaryVoid<EmailListenerMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();
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
        return Functions.map(emailListeners, new Functions.Unary<String, EmailListener>() {
            @Override
            public String call(EmailListener emailListener) {
                return emailListener.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "emailListeners";
    }

    @Override
    public String getType() {
        return EntityType.EMAIL_LISTENER.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        EmailListener entity = emailListenerManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        EmailListener entity = emailListenerManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, EmailListenerMO managedObject) throws FindException {
        EmailListener entity = emailListenerManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            Assert.assertEquals(entity.isActive(), managedObject.getActive());
            Assert.assertEquals(entity.isDeleteOnReceive(), managedObject.getDeleteOnReceive());
            Assert.assertEquals(entity.getFolder(), managedObject.getFolder());
            Assert.assertEquals(entity.getHost(), managedObject.getHostname());
            Assert.assertEquals(entity.getPollInterval(), managedObject.getPollInterval());
            Assert.assertEquals(entity.getPort(), managedObject.getPort());
            Assert.assertEquals(entity.getUsername(), managedObject.getUsername());
            Assert.assertEquals(entity.getServerType().toString(), managedObject.getServerType().toString());
            Assert.assertEquals(entity.getName(), managedObject.getName());
            for (Object key : entity.properties().keySet()) {
                Assert.assertEquals(entity.properties().get(key), managedObject.getProperties().get(key));
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(emailListeners, new Functions.Unary<String, EmailListener>() {
                    @Override
                    public String call(EmailListener emailListener) {
                        return emailListener.getId();
                    }
                }))
                .put("name=" + URLEncoder.encode(emailListeners.get(0).getName()), Arrays.asList(emailListeners.get(0).getId()))
                .put("name=banName", Collections.<String>emptyList())
                .put("active=false", Arrays.asList(emailListeners.get(0).getId()))
                .put("serverType=IMAP", Arrays.asList(emailListeners.get(0).getId()))
                .map();
    }
}
