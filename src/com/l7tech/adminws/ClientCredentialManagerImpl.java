package com.l7tech.adminws;

import com.l7tech.adminws.identity.Client;
import com.l7tech.adminws.identity.Service;
import java.io.IOException;
import java.net.PasswordAuthentication;
import java.rmi.RemoteException;
import javax.security.auth.login.LoginException;
import javax.xml.namespace.QName;
import javax.xml.rpc.ServiceException;
import org.apache.axis.client.Call;

/**
 * Default <code>ClientCredentialManager</code> implementaiton that validates
 * the credentials against the live SSG.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class ClientCredentialManagerImpl extends ClientCredentialManager {
    /**
     * Determines if the passed credentials will grant access to the admin web service.
     * If successful, those credentials will be cached for future admin ws calls.
     *
     * This requires the URL to be available in com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
     * IOException might be thrown otherwise.
     */
    public void login(PasswordAuthentication creds)
      throws LoginException, VersionException {
        resetCredentials();
        try {
            setCredentials(creds);
            // test client call to make sure creds are valid
            org.apache.axis.client.Service service = new org.apache.axis.client.Service();
            Call call = null;
            call = (Call)service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(getServiceURL()));
            call.setUsername(creds.getUserName());
            call.setPassword(new String(creds.getPassword()));
            call.setOperationName(new QName(Client.IDENTITY_URN, "echoVersion"));
            call.setReturnClass(String.class);
            call.setMaintainSession(true);
            String remoteVersion = (String)call.invoke(new Object[]{});
            if (remoteVersion == null) {
                throw new VersionException("Unknown version");
            }
            if (!remoteVersion.equals(Service.VERSION)) {
                throw new VersionException("Version mismatch ", Service.VERSION, remoteVersion);
            }
            axisSessionCall = call;
        } catch (RemoteException e) {
            LoginException le = new LoginException();
            le.initCause(e); // no constructor with nested throwable
            throw le;
        } catch (IOException e) {
            LoginException le = new LoginException("Unable to obtain the SSG service url");
            le.initCause(e); // no constructor with nested throwable
            throw le;
        } catch (ServiceException e) {
            LoginException le = new LoginException("Unable to establish connection with remote service");
            le.initCause(e); // no constructor with nested throwable
            throw le;
        }

    }

    protected void resetCredentials() {
        super.resetCredentials();
        axisSessionCall = null;
    }

    public Call getAxisSession() {
        return axisSessionCall;
    }

    private String getServiceURL() throws IOException {
        String prefUrl = com.l7tech.console.util.Preferences.getPreferences().getServiceUrl();
        if (prefUrl == null || prefUrl.length() < 1 || prefUrl.equals("null/ssg")) {
            throw new IOException("com.l7tech.console.util.Preferences.getPreferences does not resolve a server address");
        }
        prefUrl += Service.SERVICE_DEPENDENT_URL_PORTION;
        return prefUrl;
    }

    private Call axisSessionCall = null;

}
