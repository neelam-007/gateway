package com.l7tech.identity;

import com.l7tech.identity.imp.IdentityProviderConfigImp;
import com.l7tech.identity.imp.IdentityProviderTypeImp;
import com.l7tech.identity.internal.imp.GroupImp;
import com.l7tech.identity.internal.imp.UserImp;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import java.beans.XMLDecoder;
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
    /** default data store patch */
    public static final String PATH = "tests/com/l7tech/identity";

    StubDataStore() {
    }

    /**
     * Instantiate path
     * @param path
     */
    public StubDataStore(String path) throws FileNotFoundException {
        this.reconstituteFrom(path);

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
    /**
     * @return the next sequence
     */
    long nextObjectId() {
        return ++objectIdSequence;
    }


    private long initialInternalProvider(XMLEncoder encoder) {
        IdentityProviderConfig config = new IdentityProviderConfigImp();
        config.setOid(nextObjectId());
        config.setDescription("Internal identity provider (stub)");

        IdentityProviderType type = new IdentityProviderTypeImp();
        type.setDescription("Internal identity provider test stub");
        type.setClassName(IdentityProviderStub.class.getName());
        config.setType(type);
        encoder.writeObject(config);

        return config.getOid();
    }

    private void initialUsers(XMLEncoder encoder, long providerId) {
        User user = new UserImp();
        user.setOid(nextObjectId());
        user.setProviderOid(providerId);
        user.setLogin("fred");
        user.setFirstName("Fred");
        user.setLastName("Bunky");
        user.setEmail("fred@layer7-tech.com");
        encoder.writeObject(user);
        users.put(new Long(user.getOid()), user);

        user = new UserImp();
        user.setProviderOid(providerId);
        user.setOid(nextObjectId());
        user.setLogin("don");
        user.setFirstName("Don");
        user.setLastName("Freeman");
        user.setEmail("don@layer7-tech.com");
        encoder.writeObject(user);
        users.put(new Long(user.getOid()), user);

        user = new UserImp();
        user.setProviderOid(providerId);
        user.setOid(nextObjectId());
        user.setLogin("schwartz");
        user.setFirstName("Hertz");
        user.setLastName("Schwartz");
        user.setEmail("hertz@layer7-tech.com");
        encoder.writeObject(user);
        users.put(new Long(user.getOid()), user);
    }

    private void initialGroups(XMLEncoder encoder, long providerId) {
        Group group = new GroupImp();
        group.setProviderOid(providerId);
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

        group = new GroupImp();
        group.setProviderOid(providerId);
        group.setOid(nextObjectId());
        group.setName("marketing");
        group.setDescription("Marketing group");
        encoder.writeObject(group);

        group = new GroupImp();
        group.setProviderOid(providerId);
        group.setOid(nextObjectId());
        group.setName("engineering");
        group.setDescription("Engineering group");
        encoder.writeObject(group);
    }

    private void reconstituteFrom(String path) throws FileNotFoundException {
        XMLDecoder decoder = null;
        try {
            decoder = new XMLDecoder(
                    new BufferedInputStream(
                            new FileInputStream(path + File.separator + "data.xml")));
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
        } else {
            System.err.println("Don't know how to handle " + o.getClass());
        }
    }

    private void initializeSeedData(String path)
            throws FileNotFoundException {
        XMLEncoder encoder = null;
        try {
            File target = new File(path + "/data.xml");
            if (target.exists()) target.delete();
            encoder =
                    new XMLEncoder(
                            new BufferedOutputStream(new FileOutputStream(path + File.separator + "data.xml")));
            long providerId = initialInternalProvider(encoder);
            initialUsers(encoder, providerId);
            initialGroups(encoder, providerId);
        } finally {
            if (encoder != null)
                encoder.close();
        }
    }


    private EntityHeader fromUser(User u) {
        return
                new EntityHeader(u.getOid(), EntityType.USER, u.getName(), null);
    }

    private Map providerConfigs = new HashMap();
    private Map users = new HashMap();
    private Map groups = new HashMap();
    private long objectIdSequence = 0;

    /**
     * initial seed data load.
     * @param args command lien arguments
     */
    public static void main(String[] args) {
        try {
            System.out.println("Generating stub data stor in '"+PATH+File.separator+"data.xml'");
            new StubDataStore().initializeSeedData(PATH);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
