package com.l7tech.logging;

import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.util.Locator;

import java.net.PasswordAuthentication;

/**
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 2:42:17 PM
 */
public class RemoteLogProxyTest {
    public static void main(String[] args) throws Exception {
        ClientCredentialManager credsManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        PasswordAuthentication creds = new PasswordAuthentication("ssgadmin", "ssgadminpasswd".toCharArray());
        credsManager.login(creds);

        RemoteLogProxy proxy = new RemoteLogProxy();
        String[] ssglogs = proxy.getSSGLogs(0, 5000);
        for (int i = 0; i < ssglogs.length; i++) System.out.println(ssglogs[i]);
        System.exit(0);
    }
}
