package com.l7tech.identity;

import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;
import com.l7tech.service.WsdlTest;

import javax.wsdl.WSDLException;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.net.MalformedURLException;
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
    public static final String DEFAULT_STORE_PATH = "stubdata.xml";

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
            defaultStore = new StubDataStore(StubDataStore.DEFAULT_STORE_PATH);
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
      throws IOException, WSDLException, MalformedURLException {
        if (!new File(storePath).exists()) {
            initializeSeedData(storePath);
        }
        this.reconstituteFrom(storePath);

    }

    Map getUsers() {
        return users;
    }

    Map getGroups() {
        return groups;
    }

    Set getGroupMemberships() {
        return memberships;
    }

    Map getIdentityProviderConfigs() {
        return providerConfigs;
    }

    public Map getPublishedServices() {
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

    static void recycle() {
        defaultStore = null;
    }

    private IdentityProviderConfig initialInternalProvider(XMLEncoder encoder) {
        IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        config.setOid(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID);
        config.setDescription("Internal identity provider (stub)");
        config.setName(config.getDescription());
        encoder.writeObject(config);
        populate(config);
        return config;
    }

    private void initialUsers(XMLEncoder encoder) {
        InternalUser user = new InternalUser();
        user.setOid(nextObjectId());
        user.setLogin("fred");
        user.setName(user.getLogin());
        user.setFirstName("Fred");
        user.setLastName("Bunky");
        user.setEmail("fred@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);

        user = new InternalUser();
        user.setOid(nextObjectId());
        user.setLogin("don");
        user.setName(user.getLogin());
        user.setFirstName("Don");
        user.setLastName("Freeman");
        user.setEmail("don@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);

        user = new InternalUser();
        user.setOid(nextObjectId());
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
        group.setOid(nextObjectId());
        group.setName("all-staff");
        group.setDescription("All staff group");

        Set members = new HashSet();
        for (Iterator i = users.keySet().iterator(); i.hasNext();) {
            members.add(users.get(i.next()));
        }

        encoder.writeObject(group);
        populate(group);

        group = new InternalGroup();
        group.setOid(nextObjectId());
        group.setName("marketing");
        group.setDescription("Marketing group");
        encoder.writeObject(group);
        populate(group);

        group = new InternalGroup();
        group.setOid(nextObjectId());
        group.setName("engineering");
        group.setDescription("Engineering group");
        encoder.writeObject(group);
        populate(group);
    }

    private void initialGroupMemberships(XMLEncoder encoder) {
        Iterator groups = getGroups().values().iterator();
        for (; groups.hasNext();) {
            InternalGroup g = (InternalGroup)groups.next();
            for (Iterator i = users.values().iterator(); i.hasNext();) {
                InternalUser u = (InternalUser)i.next();
                GroupMembership gm = new GroupMembership(u.getOid(), g.getOid());
                encoder.writeObject(gm);
                populate(gm);
            }
        }
    }

    private void initialServices(XMLEncoder encoder, IdentityProviderConfig pc)
      throws IOException, WSDLException, MalformedURLException {
        String[] wsdls = {TestDocuments.WSDL, TestDocuments.WSDL_DOC_LITERAL, TestDocuments.WSDL_DOC_LITERAL2, TestDocuments.WSDL_DOC_LITERAL3};
        for (int i = 0; i < wsdls.length; i++) {
            String fileName = wsdls[i];
            Wsdl wsdl = Wsdl.newInstance(null, new WsdlTest("blah").getWsdlReader(fileName));

            ClassLoader cl = getClass().getClassLoader();
            String wsdlUrl = cl.getResource(fileName).toString();

            PublishedService service = new PublishedService();
            service.setName(wsdl.getDefinition().getTargetNamespace());
            StringWriter sw = new StringWriter();
            wsdl.toWriter(sw);
            service.setWsdlXml(sw.toString());
            service.setWsdlUrl(wsdlUrl);
            service.setOid(nextObjectId());
            Assertion assertion = sampleAssertion(pc);
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            WspWriter.writePolicy(assertion, bo);

            service.setPolicyXml(bo.toString());
            encoder.writeObject(service);
            populate(service);
        }
    }

    private void initialJmsProviders(XMLEncoder encoder) {
        JmsConnection p = new JmsConnection();
        long connectionOid = nextObjectId();
        p.setOid(connectionOid);
        p.setJndiUrl("JNDI:foo:bar:baz");
        p.setName("JMS on NowhereInteresting");

        encoder.writeObject(p);
        populate(p);

        JmsEndpoint e = new JmsEndpoint();
        e.setConnectionOid( connectionOid );
        e.setName( "q1 on foo:bar:baz" );
        e.setDestinationName( "q1" );
        e.setOid( nextObjectId() );
        encoder.writeObject(e);
        populate(e);

        p = new JmsConnection();
        p.setOid(nextObjectId());
        p.setJndiUrl("JNDI:blee:bloo:boof");
        p.setName("JMS on SomewhereElse");

        encoder.writeObject(p);
        populate(p);
    }

    private Assertion sampleAssertion(IdentityProviderConfig pc) {
        Group g =
          (Group)groups.values().iterator().next();
        List identities = new ArrayList();

        long providerId = makeProvider(pc).getConfig().getOid();

        MemberOfGroup memberOfGroup = new MemberOfGroup(providerId, g.getName(), g.getUniqueIdentifier());
        memberOfGroup.setGroupName(g.getName());
        memberOfGroup.setGroupId(g.getUniqueIdentifier());
        identities.add(memberOfGroup);
        for (Iterator i = users.values().iterator(); i.hasNext();) {
            identities.add(new SpecificUser(providerId, ((User)i.next()).getLogin()));
        }
        OneOrMoreAssertion oom = new OneOrMoreAssertion(identities);
        AllAssertion assertion =
          new AllAssertion(Arrays.asList(new Assertion[]{new HttpBasic(), oom}));
        return assertion;
    }

    private IdentityProvider makeProvider(IdentityProviderConfig pc) {
        IdentityProvider provider = new IdentityProviderStub(pc);
        return provider;
    }

    private void reconstituteFrom(String path)
      throws FileNotFoundException {
        XMLDecoder decoder = null;
        try {
            decoder = new XMLDecoder(new BufferedInputStream(new FileInputStream(path)));
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
            groups.put(((InternalGroup)o).getUniqueIdentifier(), o);
        } else if (o instanceof InternalUser) {
            users.put(((InternalUser)o).getUniqueIdentifier(), o);
        } else if (o instanceof GroupMembership) {
            memberships.add(o);
        } else if (o instanceof IdentityProviderConfig) {
            providerConfigs.put(new Long(((IdentityProviderConfig)o).getOid()), o);
        } else if (o instanceof PublishedService) {
            pubServices.put(new Long(((PublishedService)o).getOid()), o);
        } else if (o instanceof JmsConnection) {
            jmsProviders.put(new Long(((JmsConnection)o).getOid()), o);
        } else if (o instanceof JmsEndpoint) {
            jmsEndpoints.put(new Long(((JmsEndpoint)o).getOid()), o);
        } else {
            System.err.println("Don't know how to handle " + o.getClass());
        }
    }

    private void initializeSeedData(String storePath)
      throws IOException, MalformedURLException, WSDLException {
        XMLEncoder encoder = null;
        try {
            File target = new File(storePath);
            if (target.exists()) target.delete();
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

    private Map providerConfigs = new HashMap();
    private Map users = new HashMap();
    private Map groups = new HashMap();
    private Set memberships = new HashSet();

    private Map pubServices = new HashMap();
    private Map jmsProviders = new HashMap();
    private Map jmsEndpoints = new HashMap();

    private long objectIdSequence = 100;

    /**
     * initial seed data load.
     *
     * @param args command lien arguments
     */
    public static void main(String[] args) {
        try {
            String path = DEFAULT_STORE_PATH;
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
