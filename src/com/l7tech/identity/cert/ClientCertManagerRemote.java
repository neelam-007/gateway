package com.l7tech.identity.cert;

import com.l7tech.common.util.Locator;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectNotFoundException;
import com.l7tech.objectmodel.UpdateException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Oct 27, 2003
 * Time: 10:32:13 AM
 * $Id$
 *
 * Client side implementation of the ClientCertManager - the internal CA
 */
public class ClientCertManagerRemote implements ClientCertManager {

    public Certificate getUserCert(User user) throws FindException {
        try {
            String certstr = getStub().getUserCert(user);
            if (certstr == null) return null;
            sun.misc.BASE64Decoder base64decoder = new sun.misc.BASE64Decoder();
            byte[] certbytes = base64decoder.decodeBuffer(certstr);
            return CertificateFactory.getInstance("X.509").
                    generateCertificate(new ByteArrayInputStream(certbytes));
        } catch (RemoteException e) {
            throw new FindException("could not resolve remote interface", e);
        } catch (CertificateEncodingException e) {
            throw new FindException("could not encode cert", e);
        } catch (IOException e) {
            throw new FindException("could not decode cert", e);
        } catch (CertificateException e) {
            throw new FindException("could not generate cert", e);
        }
    }

    public void revokeUserCert(User user) throws UpdateException, ObjectNotFoundException {
        try {
            getStub().revokeCert(user);
        } catch (RemoteException e) {
            throw new UpdateException("could not resolve remote interface", e);
        }
    }

    public void forbidCertReset(User user) throws UpdateException {
        throw new RuntimeException("not implemented in stub - this should not be invoked on client side");
    }

    public boolean userCanGenCert(User user) {
        throw new RuntimeException("not implemented in stub - this should not be invoked on client side");
    }

    public void recordNewUserCert(User user, Certificate cert) throws UpdateException {
        throw new RuntimeException("not implemented in stub - this should not be invoked on client side");
    }

    protected IdentityAdmin getStub() throws RemoteException {
        IdentityAdmin svc = (IdentityAdmin)Locator.getDefault().lookup(IdentityAdmin.class);
        if (svc == null) {
            throw new RemoteException("Cannot obtain the identity service");
        }
        return svc;
    }
}
