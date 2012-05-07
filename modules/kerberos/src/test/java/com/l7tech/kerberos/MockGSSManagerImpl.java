package com.l7tech.kerberos;

import org.ietf.jgss.*;
import sun.security.jgss.GSSManagerImpl;

/**
 * User: awitrisna
 */
public class MockGSSManagerImpl extends GSSManagerImpl {

    @Override
    public GSSContext createContext(GSSName gssName, Oid oid, GSSCredential gssCredential, int i) throws GSSException {
        GSSContext context = super.createContext(gssName, oid, gssCredential, i);    //To change body of overridden methods use File | Settings | File Templates.
        return new MockGSSContext(context);
    }

    @Override
    public GSSCredential createCredential(GSSName gssName, int i, Oid oid, int i1) throws GSSException {
        return null;
    }
}
