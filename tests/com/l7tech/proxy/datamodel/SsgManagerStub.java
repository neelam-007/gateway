package com.l7tech.proxy.datamodel;

import java.util.*;
import java.io.IOException;

/**
 * Stub implementation of the SsgManager.
 * This version provides no persistence.. it just stores a list of SSGs in core.
 * Used so client proxy tests can be set up without having to touch the on-disk SSG config.
 * User: mike
 * Date: Jun 2, 2003
 * Time: 12:12:11 PM
 */
public class SsgManagerStub implements SsgManager {
    List ssgs = new ArrayList(Arrays.asList(new Ssg[] {
        new Ssg(0, "Default SSG", "http://127.0.0.1:5555"),
        new Ssg(1, "Alternate SSG", "http://127.0.0.1:5556")
    }));
    private long nextId = 2;

    public List getSsgList() {
        return Collections.unmodifiableList(ssgs);
    }

    public Ssg getSsgByName(String name) throws SsgNotFoundException {
        for (Iterator i = ssgs.iterator(); i.hasNext(); ) {
            Ssg ssg = (Ssg)i.next();
            if (ssg.getName().equals(name))
                return ssg;
        }
        throw new SsgNotFoundException();
    }

    public Ssg getSsgByEndpoint(String endpoint) throws SsgNotFoundException {
        for (Iterator i = ssgs.iterator(); i.hasNext(); ) {
            Ssg ssg = (Ssg)i.next();
            if (ssg.getLocalEndpoint().equals(endpoint))
                return ssg;
        }
        throw new SsgNotFoundException();
    }

    /**
     * Get the next unused Id.
     */
    public synchronized long nextId() {
        return nextId++;
    }

    /**
     * Create a new Ssg instance, but do not yet register it.
     */
    public Ssg createSsg() {
        return new Ssg(nextId());
    }

    public boolean add(Ssg ssg) {
        if (ssg.getId() == 0)
            throw new IllegalArgumentException("Unable to register ssg: it has not been assigned an ID");
        return ssgs.add(ssg);
    }

    public void remove(Ssg ssg) throws SsgNotFoundException {
        if (!ssgs.remove(ssg))
            throw new SsgNotFoundException();
    }

    public void save() throws IOException {
    }

    public void clear() {
        ssgs.clear();
    }
}
