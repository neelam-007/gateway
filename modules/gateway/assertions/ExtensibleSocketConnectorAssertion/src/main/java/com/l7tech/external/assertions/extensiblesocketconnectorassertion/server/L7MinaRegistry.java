package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.classloader.NioSocketWrapper;
import com.l7tech.objectmodel.Goid;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 02/12/11
 * Time: 11:06 AM
 * To change this template use File | Settings | File Templates.
 */
public class L7MinaRegistry {
    private HashMap<Goid, NioSocketWrapper> services = null;

    public L7MinaRegistry() {
        services = new HashMap<Goid, NioSocketWrapper>();
    }

    public void addService(Goid goid, NioSocketWrapper nioSocketWrapper) {
        services.put(goid, nioSocketWrapper);
    }

    public NioSocketWrapper getService(Goid goid) {
        return services.get(goid);
    }

    public HashMap<Goid, NioSocketWrapper> getServices() {
        return services;
    }

    public NioSocketWrapper removeService(Goid goid) {
        return services.remove(goid);
    }
}
