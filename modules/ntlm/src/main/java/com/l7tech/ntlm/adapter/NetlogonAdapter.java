package com.l7tech.ntlm.adapter;

import com.l7tech.ntlm.protocol.AuthenticationManagerException;
import com.l7tech.ntlm.protocol.NtlmServerResponse;
import org.ntlmv2.liferay.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 6/5/12
 */
public class NetlogonAdapter implements AuthenticationAdapter {
    private static final Logger log = Logger.getLogger(NetlogonAdapter.class.getName());
    private static final String DEFAULT_VAL = "";

    private Netlogon netlogon;

    private String hostname;
    private final String serverName;
    private final NtlmServiceAccount serviceAccount;

    public NetlogonAdapter(Map properties) throws AuthenticationManagerException{
        serverName = (String)getProperty(properties, "server.dns.name");
        String serviceName = (String)getProperty(properties, "service.account");
        if(!serviceName.matches(".+\\$@.+")) {
            throw new AuthenticationManagerException("Invalid service account");
        }
        String servicePassword = (String)getProperty(properties, "service.password");

        hostname = (String)getProperty(properties, "localhost.netbios.name");

        netlogon = new Netlogon();
        serviceAccount = new NtlmServiceAccount(serviceName, servicePassword);
        netlogon.setConfiguration(serverName, serverName, serviceAccount);

    }

    public void testConnect() throws AuthenticationManagerException {
        NetlogonConnection connection = new NetlogonConnection();

        try {
            connection.connect(serverName, serverName, serviceAccount, new SecureRandom());
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to bind to " + serverName);
            throw new AuthenticationManagerException("Unable to bind", e);
        } catch (NtlmLogonException e) {
            throw  new AuthenticationManagerException("NetrServerAuthenticate3 credential check failed");
        } catch (NoSuchAlgorithmException e) {
            log.log(Level.SEVERE, "Unable to calculate session key");
            throw new AuthenticationManagerException("Unable to calculate session key", e);
        } finally {
            try {
                connection.disconnect();
            } catch (IOException e) {
               log.log(Level.FINE, "disconnect failed" + e.getMessage());
            }
        }

    }

    protected Object getProperty(Map map, Object key) {
        Object value = map.get(key);
        return value != null? value : DEFAULT_VAL;
    }

    /**
     * validates supplied credentials
     *
     *
     * @param response
     * @param acct
     * @return object containing additional authorization data
     *         the type of data is determinate by the implementing adapter
     * @throws com.l7tech.ntlm.protocol.AuthenticationManagerException
     *
     */
    @Override
    public Object validate(NtlmServerResponse response, byte[] challenge,  Map acct) throws AuthenticationManagerException {
        byte[] sessionKey = null;
        try {
            NtlmUserAccount authenticatedUser = netlogon.logon(response.getDomain(), response.getUsername(), hostname, challenge, response.getNtResponse(),response.getLmResponse());
            Map accountInfo = authenticatedUser.getAccountInfo();
            if(accountInfo.containsKey("session.key")){
                sessionKey = (byte[])accountInfo.get("session.key");
            }

            acct.putAll(authenticatedUser.getAccountInfo());
        } catch (NtlmLogonException e) {
           if(log.isLoggable(Level.FINE)){
               log.log(Level.FINE, "Unable to authenticate the user: " + response.getUsername() + " from domain " + response.getDomain());
           }
           throw new AuthenticationManagerException("Unable to authenticate user");
        }
        return sessionKey;
    }
}
