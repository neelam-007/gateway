package com.l7tech.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.service.Wsdl;
import com.l7tech.service.PublishedService;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.wsp.WspWriter;

import javax.wsdl.WSDLException;
import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.*;
import java.util.*;
import java.net.MalformedURLException;

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
    /** default data store patch */
    public static final String DEFAULT_STORE_PATH = "tests/com/l7tech/identity/data.xml";

    StubDataStore() {
    }

    /**
     * create the default store
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
     * @param storePath
     */
    protected StubDataStore(String storePath)
      throws FileNotFoundException, WSDLException, MalformedURLException {
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

    Map getIdentityProviderConfigs() {
        return providerConfigs;
    }

    public Map getPublishedServices() {
          return pubServices;
    }


    /**
     * @return the next sequence
     */
    public long nextObjectId() {
        return ++objectIdSequence;
    }


    private IdentityProviderConfig initialInternalProvider(XMLEncoder encoder) {
        IdentityProviderConfig config = new IdentityProviderConfig(IdentityProviderType.INTERNAL);
        config.setOid(nextObjectId());
        config.setDescription("Internal identity provider (stub)");
        encoder.writeObject(config);
        populate(config);
        return config;
    }

    private void initialUsers(XMLEncoder encoder, long providerId) {
        User user = new User();
        user.setOid(nextObjectId());
        user.setLogin("fred");
        user.setName(user.getLogin());
        user.setFirstName("Fred");
        user.setLastName("Bunky");
        user.setEmail("fred@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);

        user = new User();
        user.setOid(nextObjectId());
        user.setLogin("don");
        user.setName(user.getLogin());
        user.setFirstName("Don");
        user.setLastName("Freeman");
        user.setEmail("don@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);

        user = new User();
        user.setOid(nextObjectId());
        user.setLogin("schwartz");
        user.setName(user.getLogin());
        user.setFirstName("Hertz");
        user.setLastName("Schwartz");
        user.setEmail("hertz@layer7-tech.com");
        encoder.writeObject(user);
        populate(user);
    }

    private void initialGroups(XMLEncoder encoder, long providerId) {
        Group group = new Group();
        group.setOid(nextObjectId());
        group.setName("all-staff");
        group.setDescription("All staff group");

        Set members = new HashSet();
        for (Iterator i = users.keySet().iterator(); i.hasNext();) {
            members.add(users.get(i.next()));
        }
        group.setMembers(members);

        Set membersHeaders = new HashSet();
        for (Iterator i = users.keySet().iterator(); i.hasNext();) {
            membersHeaders.add(fromUser((User)users.get(i.next())));
        }


        group.setMemberHeaders(membersHeaders);
        encoder.writeObject(group);
        populate(group);

        group = new Group();
        group.setOid(nextObjectId());
        group.setName("marketing");
        group.setDescription("Marketing group");
        encoder.writeObject(group);
        populate(group);

        group = new Group();
        group.setOid(nextObjectId());
        group.setName("engineering");
        group.setDescription("Engineering group");
        encoder.writeObject(group);
        populate(group);
    }

    private void initialServices(XMLEncoder encoder, IdentityProviderConfig pc)
      throws FileNotFoundException, WSDLException, MalformedURLException {
        String path = "tests/com/l7tech/service/StockQuoteService.wsdl";
        File file = new File(path);
        Wsdl wsdl = Wsdl.newInstance(null, new FileReader(file));
        PublishedService service = new PublishedService();
        service.setName(wsdl.getDefinition().getTargetNamespace());
        service.setUrn(wsdl.getDefinition().getTargetNamespace());
        StringWriter sw = new StringWriter();
        wsdl.toWriter(sw);
        service.setWsdlXml(sw.toString());
        service.setWsdlUrl(file.toURI().toString());
        service.setOid(nextObjectId());
        Group g =
          (Group)groups.values().iterator().next();
        Assertion assertion =
          new MemberOfGroup(makeProvider(pc), g);
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        WspWriter.writePolicy(assertion, bo);

        service.setPolicyXml(bo.toString());
        encoder.writeObject(service);
        populate(service);

    }

    private IdentityProvider makeProvider(IdentityProviderConfig pc) {
        IdentityProvider provider = new IdentityProviderStub();
        provider.initialize(pc);
        return provider;
    }

    private void reconstituteFrom(String path)
      throws FileNotFoundException {
        XMLDecoder decoder = null;
        try {
            decoder = new XMLDecoder(
              new BufferedInputStream(
                new FileInputStream(path)));
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
        if (o instanceof Group) {
            groups.put(new Long(((Group)o).getOid()), o);
        } else if (o instanceof User) {
            users.put(new Long(((User)o).getOid()), o);
        } else if (o instanceof IdentityProviderConfig) {
            providerConfigs.put(new Long(((IdentityProviderConfig)o).getOid()), o);
        } else if (o instanceof PublishedService) {
            pubServices.put(new Long(((PublishedService)o).getOid()), o);
        } else {
            System.err.println("Don't know how to handle " + o.getClass());
        }
    }

    private void initializeSeedData(String storePath)
      throws FileNotFoundException, MalformedURLException, WSDLException {
        XMLEncoder encoder = null;
        try {
            File target = new File(storePath);
            if (target.exists()) target.delete();
            encoder =
              new XMLEncoder(
                new BufferedOutputStream(new FileOutputStream(storePath)));
            IdentityProviderConfig providerConfig
              = initialInternalProvider(encoder);
            long providerId = providerConfig.getOid();
            initialUsers(encoder, providerId);
            initialGroups(encoder, providerId);
            initialServices(encoder, providerConfig);
        } finally {
            if (encoder != null) {
                encoder.close();
            }
        }
    }


    private EntityHeader fromUser(User u) {
        return
          new EntityHeader(u.getOid(), EntityType.USER, u.getName(), null);
    }

    private Map providerConfigs = new HashMap();
    private Map users = new HashMap();
    private Map groups = new HashMap();
    private Map pubServices = new HashMap();
    private long objectIdSequence = 0;

    /**
     * initial seed data load.
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
