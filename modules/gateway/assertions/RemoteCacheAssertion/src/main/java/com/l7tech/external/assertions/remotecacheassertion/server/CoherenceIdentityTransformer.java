package com.l7tech.external.assertions.remotecacheassertion.server;

import com.tangosol.net.Service;
import com.tangosol.net.security.IdentityTransformer;

import javax.security.auth.Subject;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 6/20/12
 * Time: 10:55 AM
 * To change this template use File | Settings | File Templates.
 */
public class CoherenceIdentityTransformer implements IdentityTransformer {

    @Override
    public Object transformIdentity(Subject subject, Service service) throws SecurityException {
        // Disable security between extend client and extend proxies.
        //
        return null;
    }
}