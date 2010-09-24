package com.l7tech.proxy.datamodel;

import com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException;

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
        new Ssg(1, "127.0.0.1"),
        new Ssg(2, "127.0.0.1")
    }));
    private long nextId = 3;

    public List getSsgList() {
        return Collections.unmodifiableList(ssgs);
    }

    public Ssg getSsgByEndpoint(String endpoint) throws SsgNotFoundException {
        for (Iterator i = ssgs.iterator(); i.hasNext(); ) {
            Ssg ssg = (Ssg)i.next();
            if (ssg.getLocalEndpoint().equals(endpoint))
                return ssg;
        }
        throw new SsgNotFoundException();
    }

    public Ssg getSsgByHostname(String hostname) throws SsgNotFoundException {
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg) i.next();
            if (ssg.getSsgAddress().equals(hostname))
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

    /**
     * Get the default SSG.
     * Returns the first SSG that has its Default flag set.  Usually there is only one such SSG.
     * @return the Default SSG
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException if no Default SSG was found
     */
    public Ssg getDefaultSsg() throws SsgNotFoundException {
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg ssg = (Ssg) i.next();
            if (ssg.isDefaultSsg())
                return ssg;
        }
        throw new SsgNotFoundException();
    }

    public void onSsgUpdated(Ssg ssg) {
    }

    /**
     * Set the default SSG.
     * If this method returns, it's guaranteed that the specified Ssg
     * is in the Ssg list and is the only one with its Default flag set to true.
     * @param ssg the SSG that should be made the new default ssg
     * @throws com.l7tech.proxy.datamodel.exceptions.SsgNotFoundException if the specified SSG is not registered
     */
    public void setDefaultSsg(Ssg ssg) throws SsgNotFoundException {
        boolean found = false;
        for (Iterator i = ssgs.iterator(); i.hasNext();) {
            Ssg s = (Ssg) i.next();
            if (s.equals(ssg)) {
                s.setDefaultSsg(true);
                found = true;
            } else {
                s.setDefaultSsg(false);
            }
        }
        if (!found)
            throw new SsgNotFoundException();
    }

    public void save() throws IOException {
    }

    public void clear() {
        ssgs.clear();
    }
}
