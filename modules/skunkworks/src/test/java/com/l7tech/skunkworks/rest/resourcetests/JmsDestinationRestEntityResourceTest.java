package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.gateway.api.*;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.gateway.common.transport.jms.JmsProviderType;
import com.l7tech.objectmodel.*;
import com.l7tech.server.transport.jms.JmsConnectionManager;
import com.l7tech.server.transport.jms.JmsEndpointManager;
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
public class JmsDestinationRestEntityResourceTest extends RestEntityTests<JmsEndpoint, JMSDestinationMO> {

    private JmsEndpointManager jmsEndpointManager;
    private JmsConnectionManager jmsConnectionManager;
    private List<JmsConnection> connections = new ArrayList<>();
    private List<JmsEndpoint> endpoints = new ArrayList<>();
    private Map<Goid,Goid> endpointConnection = new HashMap<>(); // endpoint connection


    @Before
    public void before() throws ObjectModelException {

        jmsConnectionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jmsConnectionManager", JmsConnectionManager.class);
        jmsEndpointManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("jmsEndpointManager", JmsEndpointManager.class);

        //Create the Jms, outbound, disabled
        JmsConnection jmsConnection = new JmsConnection();
        jmsConnection.setName( "JMS Connection 1" );
        jmsConnection.setQueueFactoryUrl("queueFactory");
        jmsConnection.setInitialContextFactoryClassname("contextClassname");
        jmsConnection.setJndiUrl("jndiUrl");
        jmsConnection.setProviderType(JmsProviderType.Tibco);
        jmsConnection.setUsername("user");
        jmsConnection.setPassword("password");
        jmsConnectionManager.save(jmsConnection);
        connections.add(jmsConnection);

        JmsEndpoint jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setConnectionGoid(jmsConnection.getGoid());
        jmsEndpoint.setName("queueName1");
        jmsEndpoint.setDestinationName("queueName1");
        jmsEndpoint.setUsername("user");
        jmsEndpoint.setPassword("password");
        jmsEndpoint.setMessageSource(false);
        jmsEndpoint.setDisabled(true);
        jmsEndpoint.setTemplate(false);
        jmsEndpointManager.save(jmsEndpoint);
        endpoints.add(jmsEndpoint);
        endpointConnection.put(jmsEndpoint.getGoid(),jmsEndpoint.getConnectionGoid());

        //Create the Jms , inbound, template, enabled
        jmsConnection = new JmsConnection();
        jmsConnection.setName("JMS Connection 2");
        jmsConnection.setQueueFactoryUrl("queueFactory");
        jmsConnection.setInitialContextFactoryClassname("contextClassname");
        jmsConnection.setJndiUrl("jndiUrl");
        jmsConnection.setProviderType(JmsProviderType.Tibco);
        jmsConnection.setUsername("user");
        jmsConnection.setPassword("password");
        jmsConnectionManager.save(jmsConnection);
        connections.add(jmsConnection);

        jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setConnectionGoid(jmsConnection.getGoid());
        jmsEndpoint.setName("queueName2");
        jmsEndpoint.setDestinationName("queueName2");
        jmsEndpoint.setUsername("user");
        jmsEndpoint.setPassword("password");
        jmsEndpoint.setMessageSource(true);
        jmsEndpoint.setTemplate(true);
        jmsEndpoint.setDisabled(false);
        jmsEndpointManager.save(jmsEndpoint);
        endpoints.add(jmsEndpoint);
        endpointConnection.put(jmsEndpoint.getGoid(),jmsEndpoint.getConnectionGoid());

        //Create the Jms, inbound, disabled
        jmsConnection = new JmsConnection();
        jmsConnection.setName( "JMS Connection 3" );
        jmsConnection.setQueueFactoryUrl("queueFactory");
        jmsConnection.setInitialContextFactoryClassname("contextClassname");
        jmsConnection.setJndiUrl("jndiUrl");
        jmsConnection.setProviderType(JmsProviderType.Tibco);
        jmsConnection.setUsername("user");
        jmsConnection.setPassword("password");
        jmsConnectionManager.save(jmsConnection);
        connections.add(jmsConnection);

        jmsEndpoint = new JmsEndpoint();
        jmsEndpoint.setConnectionGoid(jmsConnection.getGoid());
        jmsEndpoint.setName( "queueName3," );
        jmsEndpoint.setDestinationName( "queueName3" );
        jmsEndpoint.setUsername("user");
        jmsEndpoint.setPassword("password");
        jmsEndpoint.setMessageSource(true);
        jmsEndpoint.setTemplate(false);
        jmsEndpoint.setDisabled(true);
        jmsEndpointManager.save(jmsEndpoint);
        endpoints.add(jmsEndpoint);
        endpointConnection.put(jmsEndpoint.getGoid(),jmsEndpoint.getConnectionGoid());
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<JmsEndpoint> all = jmsEndpointManager.findAll();
        for (JmsEndpoint jmsEndpoint : all) {
            jmsEndpointManager.delete(jmsEndpoint.getGoid());
        }

        Collection<JmsConnection> allc = jmsConnectionManager.findAll();
        for (JmsConnection jmsConnection : allc) {
            jmsConnectionManager.delete(jmsConnection.getGoid());
        }

    }

    @Override
    public List<String> getRetrievableEntityIDs() {
        return Functions.map(endpoints, new Functions.Unary<String, JmsEndpoint>() {
            @Override
            public String call(JmsEndpoint jmsEndpoint) {
                return jmsEndpoint.getId();
            }
        });
    }

    @Override
    public List<JMSDestinationMO> getCreatableManagedObjects() {
        List<JMSDestinationMO> destinationMOs = new ArrayList<>();

        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName("New JMS");
        jmsDetail.setDestinationName("New JMS Destination");
        jmsDetail.setTemplate(false);
        JMSConnection jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setTemplate(false);
        jmsConnection.setProviderType(JMSConnection.JMSProviderType.TIBCO_EMS);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname","om.context.Classname")
                .put("jndi.providerUrl","ldap://jndi")
                .put("queue.connectionFactoryName","qcf").map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setId(getGoid().toString());
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);
        destinationMOs.add(jmsMO);

        jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName("New JMS 2");
        jmsDetail.setDestinationName("New JMS Destination 2");
        jmsDetail.setTemplate(false);
        jmsConnection = ManagedObjectFactory.createJMSConnection();
        jmsConnection.setId(getGoid().toString());
        jmsConnection.setTemplate(false);
        jmsConnection.setProviderType(JMSConnection.JMSProviderType.TIBCO_EMS);
        jmsConnection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname","om.context.Classname")
                .put("jndi.providerUrl","ldap://jndi")
                .put("queue.connectionFactoryName","qcf").map());
        jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setId(getGoid().toString());
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(jmsConnection);
        destinationMOs.add(jmsMO);

        return destinationMOs;
    }

    @Override
    public List<JMSDestinationMO> getUpdateableManagedObjects() {
        List<JMSDestinationMO> destinationMOs = new ArrayList<>();

        JmsEndpoint jmsEndpoint = endpoints.get(0);
        JmsConnection jmsConnection = connections.get(0);

        //change name
        JMSDestinationDetail jmsDetail = ManagedObjectFactory.createJMSDestinationDetails();
        jmsDetail.setName(jmsEndpoint.getName() + " Updated");
        jmsDetail.setDestinationName(jmsEndpoint.getDestinationName() + " Updated");
        jmsDetail.setTemplate(jmsEndpoint.isTemplate());
        jmsDetail.setEnabled(!jmsEndpoint.isDisabled());
        jmsDetail.setInbound(jmsEndpoint.isMessageSource());
        jmsDetail.setId(jmsEndpoint.getId());
        JMSConnection connection = ManagedObjectFactory.createJMSConnection();
        connection.setId(jmsConnection.getId());
        connection.setTemplate(jmsConnection.isTemplate());
        connection.setProviderType(JMSConnection.JMSProviderType.Weblogic);
        connection.setProperties(CollectionUtils.<String, Object>mapBuilder()
                .put("jndi.initialContextFactoryClassname",jmsConnection.getInitialContextFactoryClassname())
                .put("jndi.providerUrl",jmsConnection.getJndiUrl())
                .put("queue.connectionFactoryName",jmsConnection.getQueueFactoryUrl()).map());
        JMSDestinationMO jmsMO = ManagedObjectFactory.createJMSDestination();
        jmsMO.setId(jmsEndpoint.getId());
        jmsMO.setJmsDestinationDetail(jmsDetail);
        jmsMO.setJmsConnection(connection);
        destinationMOs.add(jmsMO);

        return destinationMOs;
    }

    @Override
    public Map<JMSDestinationMO, Functions.BinaryVoid<JMSDestinationMO, RestResponse>> getUnCreatableManagedObjects() {

        return Collections.emptyMap();
    }

    @Override
    public Map<JMSDestinationMO, Functions.BinaryVoid<JMSDestinationMO, RestResponse>> getUnUpdateableManagedObjects() {

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
        return Functions.map(endpoints, new Functions.Unary<String, JmsEndpoint>() {
            @Override
            public String call(JmsEndpoint JmsEndpoint) {
                return JmsEndpoint.getId();
            }
        });
    }

    @Override
    public String getResourceUri() {
        return "jmsDestinations";
    }

    @Override
    public String getType() {
        return EntityType.JMS_ENDPOINT.name();
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        JmsEndpoint entity = jmsEndpointManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return entity.getName();
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
        JmsEndpoint entity = jmsEndpointManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
    }

    @Override
    public void verifyEntity(String id, JMSDestinationMO managedObject) throws FindException {
        JmsEndpoint entity = jmsEndpointManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
            JmsConnection connection = jmsConnectionManager.findByPrimaryKey(endpointConnection.get(Goid.parseGoid(id)));
            Assert.assertNull(connection);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            JmsConnection connection = jmsConnectionManager.findByPrimaryKey(entity.getConnectionGoid());
            Assert.assertNotNull(connection);
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(endpoints, new Functions.Unary<String, JmsEndpoint>() {
                    @Override
                    public String call(JmsEndpoint jmsEndpoint) {
                        return jmsEndpoint.getId();
                    }
                }))
                .put("name=" + endpoints.get(0).getName(), Arrays.asList(endpoints.get(0).getId()))
                .put("name=" + endpoints.get(0).getName() + "&name=" + endpoints.get(1).getName(), Functions.map(endpoints.subList(0, 2), new Functions.Unary<String, JmsEndpoint>() {
                    @Override
                    public String call(JmsEndpoint jmsEndpoint) {
                        return jmsEndpoint.getId();
                    }
                }))
                .put("name=banName", Collections.<String>emptyList())
                .put("enabled=true", Arrays.asList(endpoints.get(1).getId()))
                .put("enabled=false", Arrays.asList(endpoints.get(0).getId(), endpoints.get(2).getId()))
                .put("inbound=true",  Arrays.asList(endpoints.get(1).getId(), endpoints.get(2).getId()))
                .put("inbound=false", Arrays.asList(endpoints.get(0).getId()))
                .put("template=true", Arrays.asList(endpoints.get(1).getId()))
                .put("template=false", Arrays.asList(endpoints.get(0).getId(), endpoints.get(2).getId()))
                .put("destination=" + URLEncoder.encode(endpoints.get(0).getDestinationName()) + "&destination=" + URLEncoder.encode(endpoints.get(2).getDestinationName()), Arrays.asList(endpoints.get(0).getId(), endpoints.get(2).getId()))
                .map();
    }
}
