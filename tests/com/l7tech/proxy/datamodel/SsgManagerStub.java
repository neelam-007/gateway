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
        new Ssg("Default SSG", "SSG0", "http://127.0.0.1:5555", "fbunky", "soopersekrit"),
        new Ssg("Alternate SSG", "SSG1", "http://127.0.0.1:5556", "fredb", "p-ass-word")
    }));

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

    public boolean add(Ssg ssg) {
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
