package com.l7tech.gateway.common;

import com.l7tech.common.TestDocuments;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.jms.JmsConnection;
import com.l7tech.gateway.common.transport.jms.JmsEndpoint;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.IdentityAssertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.SafeXMLDecoder;
import com.l7tech.util.SafeXMLDecoderBuilder;
import com.l7tech.util.StringClassFilter;
import com.l7tech.wsdl.Wsdl;

import javax.wsdl.WSDLException;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.*;

/**
 * The test in memory datastore with users, groups, providers etc. Used
 * for testing and developing with manager stubs, that is, without the
 * server side component.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public class StubDataStore {
    private static StubDataStore defaultStore = null;
    /**
     * default data store patch
     */
    public static final String DEFAULT_STORE_NAME = "stubdata";
    public static final String DEFAULT_STORE_EXT = ".xml";

    StubDataStore() {
    }

    /**
     * create the default store
     *
     * @return the default data  store
     */
    public synchronized static StubDataStore defaultStore() {
        if (defaultStore != null) return defaultStore;
        try {
            File storeFile = File.createTempFile(DEFAULT_STORE_NAME, DEFAULT_STORE_EXT);
            storeFile.deleteOnExit();
            defaultStore = new StubDataStore(storeFile.getAbsolutePath());
            return defaultStore;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Instantiate path
     *
     * @param storePath
     */
    protected StubDataStore(String storePath)
      throws IOException, WSDLException {
        if (!new File(storePath).exists() || new File(storePath).length()==0) {
            initializeSeedData(storePath);
        }
        this.reconstituteFrom(storePath);

    }

    public Map<String, PersistentUser> getUsers() {
        return users;
    }

    public Map<String, PersistentGroup> getGroups() {
        return groups;
    }

    public Set<GroupMembership> getGroupMemberships() {
        return memberships;
    }

    public Map<Goid, IdentityProviderConfig> getIdentityProviderConfigs() {
        return providerConfigs;
    }

    public Map<Goid, PublishedService> getPublishedServices() {
        return pubServices;
    }

    public Map getJmsConnections() {
        return jmsProviders;
    }

    public Map getJmsEndpoints() {
        return jmsEndpoints;
    }

    /**
     * @return the next sequence
     */
    public long nextObjectId() {
        return ++objectIdSequence;
    }
    public Goid nextGoid() {
        return new Goid(0,nextObjectId());
    }

    static void recycle() {
        defaultStore = null;
    }

    private IdentityProviderConfig initialInternalProvider(XMLEncoder encoder) {
        IdentityProviderConfig config = new IdentityProviderConfig( IdentityProviderType.INTERNAL);
        config.setGoid( IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        config.setDescription("Internal identity provider (stub)");
        config.setName(config.getDescription());
        encoder.writeObject(config);
        populate(config);
        return config;
    }

    private void initialUsers(XMLEncoder encoder) {
        InternalUser user = new InternalUser();
        user.setGoid(nextGoid());
        user.setLogin("fred");
        user.setName(user.getLogin());
        user.setFirstName("Fred");
        user.setLastName("Bunky");
        user.setEmail("fred@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);

        user = new InternalUser();
        user.setGoid(nextGoid());
        user.setLogin("don");
        user.setName(user.getLogin());
        user.setFirstName("Don");
        user.setLastName("Freeman");
        user.setEmail("don@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);

        user = new InternalUser();
        user.setGoid(nextGoid());
        user.setLogin("schwartz");
        user.setName(user.getLogin());
        user.setFirstName("Hertz");
        user.setLastName("Schwartz");
        user.setEmail("hertz@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);
    }

    private void initialGroups(XMLEncoder encoder) {
        InternalGroup group = new InternalGroup();
        group.setGoid(nextGoid());
        group.setName("all-staff");
        group.setDescription("All staff group");

        encoder.writeObject(group);
        populate(group);

        group = new InternalGroup();
        group.setGoid(nextGoid());
        group.setName("marketing");
        group.setDescription("Marketing group");
        encoder.writeObject(group);
        populate(group);

        group = new InternalGroup();
        group.setGoid(nextGoid());
        group.setName("engineering");
        group.setDescription("Engineering group");
        encoder.writeObject(group);
        populate(group);
    }

    private void initialGroupMemberships(XMLEncoder encoder) {
        Iterator groups = getGroups().values().iterator();
        for (; groups.hasNext();) {
            InternalGroup g = (InternalGroup)groups.next();
            for (PersistentUser u : users.values()) {
                InternalGroupMembership gm = InternalGroupMembership.newInternalMembership(g.getGoid(), u.getGoid());
                encoder.writeObject(gm);
                populate(gm);
            }
        }
    }

    private void initialServices(XMLEncoder encoder, IdentityProviderConfig pc)
      throws IOException, WSDLException {
        String[] wsdls = {TestDocuments.WSDL, TestDocuments.WSDL_DOC_LITERAL, TestDocuments.WSDL_DOC_LITERAL2, TestDocuments.WSDL_DOC_LITERAL3};
        for (String fileName : wsdls) {
            String wsdlUrl = TestDocuments.getTestDocumentURL( fileName ).toString();
            Wsdl wsdl = Wsdl.newInstance(wsdlUrl, new InputStreamReader(TestDocuments.getInputStream( fileName )));

            PublishedService service = new PublishedService();
            service.setName(wsdl.getDefinition().getTargetNamespace());
            StringWriter sw = new StringWriter();
            wsdl.toWriter(sw);
            service.setWsdlXml(sw.toString());
            service.setWsdlUrl(wsdlUrl);
            service.setGoid(new Goid(0, nextObjectId()));
            Assertion assertion = sampleAssertion(pc);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(assertion, bo);

            service.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, null, bo.toString(), true));
            service.getPolicy().setGoid(service.getGoid());
            encoder.writeObject(service);
            populate(service);
        }
    }

    private void initialJmsProviders(XMLEncoder encoder) {
        JmsConnection p = new JmsConnection();
        Goid connectionOid = nextGoid();
        p.setGoid(connectionOid);
        p.setJndiUrl("JNDI:foo:bar:baz");
        p.setName("JMS on NowhereInteresting");

        encoder.writeObject(p);
        populate(p);

        JmsEndpoint e = new JmsEndpoint();
        e.setConnectionGoid(connectionOid);
        e.setName( "q1 on foo:bar:baz" );
        e.setDestinationName( "q1" );
        e.setGoid( nextGoid() );
        encoder.writeObject(e);
        populate(e);

        p = new JmsConnection();
        p.setGoid(nextGoid());
        p.setJndiUrl("JNDI:blee:bloo:boof");
        p.setName("JMS on SomewhereElse");

        encoder.writeObject(p);
        populate(p);
    }

    private Assertion sampleAssertion(IdentityProviderConfig pc) {
        Group g = groups.values().iterator().next();
        List<IdentityAssertion> identities = new ArrayList<IdentityAssertion>();

        Goid providerId = pc.getGoid();

        MemberOfGroup memberOfGroup = new MemberOfGroup(providerId, g.getName(), g.getId());
        memberOfGroup.setGroupName(g.getName());
        memberOfGroup.setGroupId(g.getId());
        identities.add(memberOfGroup);
        for (PersistentUser u : users.values()) {
            identities.add(new SpecificUser(providerId, u.getLogin(), u.getId(), u.getName()));
        }
        OneOrMoreAssertion oom = new OneOrMoreAssertion(identities);
        return new AllAssertion(Arrays.asList(new HttpBasic(), oom));
    }

    private void reconstituteFrom(String path)
      throws FileNotFoundException {
        SafeXMLDecoder decoder = null;
        try {
            decoder = new SafeXMLDecoderBuilder(new BufferedInputStream(new FileInputStream(path)))
                    .addClassFilter(new StringClassFilter(classes, constructors, methods))
                    .build();

            //noinspection InfiniteLoopStatement
            while (true) {
                populate(decoder.readObject());
                this.nextObjectId();
            }

        } catch (ArrayIndexOutOfBoundsException e) {
            // swallow, means no more objects, this exceptional
            // flow control brought to you by JDK!
        } finally {
            if (decoder != null) decoder.close();
        }

    }

    private void populate(Object o) {
        if (o instanceof InternalGroup) {
            groups.put(((InternalGroup)o).getId(), (PersistentGroup) o);
        } else if (o instanceof PersistentUser) {
            users.put(((PersistentUser)o).getId(), (PersistentUser) o);
        } else if (o instanceof GroupMembership) {
            memberships.add((GroupMembership) o);
        } else if (o instanceof IdentityProviderConfig) {
            providerConfigs.put(((IdentityProviderConfig) o).getGoid(), (IdentityProviderConfig) o);
        } else if (o instanceof PublishedService) {
            pubServices.put(((PublishedService)o).getGoid(), (PublishedService) o);
        } else if (o instanceof JmsConnection) {
            jmsProviders.put(((JmsConnection)o).getGoid(), (JmsConnection) o);
        } else if (o instanceof JmsEndpoint) {
            jmsEndpoints.put(((JmsEndpoint)o).getGoid(), (JmsEndpoint) o);
        } else if (o != null) {
            System.err.println("Don't know how to handle " + o.getClass());
        }
    }

    private void initializeSeedData(String storePath)
      throws IOException, WSDLException {
        XMLEncoder encoder = null;
        try {
            File target = new File(storePath);
            if (target.exists() && !target.canWrite()) target.delete();
            encoder =
              new XMLEncoder(new BufferedOutputStream(new FileOutputStream(storePath)));
            IdentityProviderConfig providerConfig = initialInternalProvider(encoder);
            initialUsers(encoder);
            initialGroups(encoder);
            initialGroupMemberships(encoder);
            initialServices(encoder, providerConfig);
            initialJmsProviders(encoder);
        } finally {
            if (encoder != null) {
                encoder.close();
            }
        }
    }

    private Map<Goid, IdentityProviderConfig> providerConfigs = new HashMap<Goid, IdentityProviderConfig>();
    private Map<String, PersistentUser> users = new HashMap<String, PersistentUser>();
    private Map<String, PersistentGroup> groups = new HashMap<String, PersistentGroup>();
    private Set<GroupMembership> memberships = new HashSet<GroupMembership>();

    private Map<Goid, PublishedService> pubServices = new HashMap<Goid, PublishedService>();
    private Map<Goid, JmsConnection> jmsProviders = new HashMap<Goid, JmsConnection>();
    private Map<Goid, JmsEndpoint> jmsEndpoints = new HashMap<Goid, JmsEndpoint>();

    private long objectIdSequence = 100;

    private static final Set<String> classes = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.l7tech.identity.IdentityProviderConfig",
            "com.l7tech.identity.internal.InternalUser",
            "com.l7tech.identity.internal.InternalGroup",
            "com.l7tech.gateway.common.service.PublishedService",
            "java.lang.Enum",
            "com.l7tech.common.http.HttpMethod",
            "com.l7tech.gateway.common.transport.jms.JmsConnection",
            "com.l7tech.gateway.common.transport.jms.JmsEndpoint"
    )));

    private static final Set<String> constructors = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.l7tech.identity.IdentityProviderConfig()",
            "com.l7tech.identity.internal.InternalUser()",
            "com.l7tech.identity.internal.InternalGroup()",
            "com.l7tech.gateway.common.service.PublishedService()",
            "com.l7tech.gateway.common.transport.jms.JmsConnection()",
            "com.l7tech.gateway.common.transport.jms.JmsEndpoint()"
    )));

    private static final Set<String> methods = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.l7tech.identity.IdentityProviderConfig.setDescription(java.lang.String)",
            "com.l7tech.objectmodel.imp.PersistentEntityImp.setId(java.lang.String)",
            "com.l7tech.objectmodel.imp.NamedEntityImp.setName(java.lang.String)",
            "com.l7tech.identity.IdentityProviderConfig.setTypeVal(int)",
            "com.l7tech.identity.PersistentUser.setEmail(java.lang.String)",
            "com.l7tech.identity.PersistentUser.setFirstName(java.lang.String)",
            "com.l7tech.identity.PersistentUser.setLastName(java.lang.String",
            "com.l7tech.identity.PersistentUser.setLastName(java.lang.String)",
            "com.l7tech.identity.PersistentUser.setLogin(java.lang.String)",
            "com.l7tech.identity.PersistentGroup.setDescription(java.lang.String)",
            "com.l7tech.gateway.common.service.PublishedService.getHttpMethods()",
            "java.util.RegularEnumSet.clear()",
            "java.lang.Enum.valueOf(java.lang.Class,java.lang.String)",
            "java.util.RegularEnumSet.add(java.lang.Enum)",
            "com.l7tech.gateway.common.service.PublishedService.getPolicy()",
            "com.l7tech.policy.Policy.setSoap(boolean)",
            "com.l7tech.policy.Policy.setXml(java.lang.String)",
            "com.l7tech.gateway.common.service.PublishedService.setWsdlUrl(java.lang.String)",
            "com.l7tech.gateway.common.service.PublishedService.setWsdlXml(java.lang.String)",
            "com.l7tech.gateway.common.transport.jms.JmsConnection.setJndiUrl(java.lang.String)",
            "com.l7tech.gateway.common.transport.jms.JmsEndpoint.setDestinationName(java.lang.String)"
    )));

    /**
     * initial seed data load.
     *
     * @param args command lien arguments
     */
    public static void main(String[] args) {
        try {
            String path = DEFAULT_STORE_NAME + DEFAULT_STORE_EXT;
            if (args.length > 0) {
                path = args[0];
            }
            System.out.println("Generating stub data store in '" + path + "'");
            new StubDataStore().initializeSeedData(path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
