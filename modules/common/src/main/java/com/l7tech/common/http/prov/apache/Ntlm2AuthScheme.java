package com.l7tech.common.http.prov.apache;

import com.l7tech.ntlm.protocol.*;
import com.l7tech.util.HexUtils;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthChallengeParser;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.MalformedChallengeException;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class Ntlm2AuthScheme implements AuthScheme{
    public static final String NTLM = "ntlm";

    private static String DEFAULT_FLAGS = null;

    private static String LOCALHOST_NETBIOS_NAME;
    private NtlmAuthenticationClient client;
    private String ntlmchallenge = null;

    static{
       InetAddress laddr = null;
       String hostname = null;
       try {
         laddr = InetAddress.getLocalHost();
       }
       catch (UnknownHostException uhe) {}

       if (laddr != null) {
         hostname = laddr.getHostName();
         int dot = hostname.indexOf('.');
         if (dot > 0)
           hostname = hostname.substring(0, dot);
         if (hostname.length() > 15)
           hostname = hostname.substring(0, 15);
       }

       LOCALHOST_NETBIOS_NAME = hostname;

    }

    public static void setNegotiateFlags(String flags) {
        Ntlm2AuthScheme.DEFAULT_FLAGS = flags;
    }


    public Ntlm2AuthScheme() {
        super();
        HashMap<String, Object> props = new HashMap<String, Object>();
        if(LOCALHOST_NETBIOS_NAME != null){
            props.put("host.netbios.name", LOCALHOST_NETBIOS_NAME);
        }
        if(DEFAULT_FLAGS != null && DEFAULT_FLAGS.length() > 0){
            props.put("flags", DEFAULT_FLAGS);
        }
        this.client = new NtlmAuthenticationClient(props);
    }
    
    @Override
    public void processChallenge(String challenge) throws MalformedChallengeException {
        String s = AuthChallengeParser.extractScheme(challenge);
        if (!s.equalsIgnoreCase(getSchemeName())) {
            throw new MalformedChallengeException("Invalid NTLM challenge: " + challenge);
        }
        int i = challenge.indexOf(' ');
        if (i != -1) {
            s = challenge.substring(i, challenge.length());
            this.ntlmchallenge = s.trim();

            client.getNtlmAuthenticationState().setState(NtlmAuthenticationProvider.State.CHALLENGE);
        } else {
            this.ntlmchallenge = "";
            if (client.getNtlmAuthenticationState().getState() != NtlmAuthenticationProvider.State.NEGOTIATE) {
                client.getNtlmAuthenticationState().setState(NtlmAuthenticationProvider.State.FAILED);
            }
        }
    }

    @Override
    public String getSchemeName() {
        return NTLM;
    }

    @Override
    public String getParameter(String name) {
        return null;
    }

    @Override
    public String getRealm() {
        return null;
    }

    @Override
    public String getID() {
        return ntlmchallenge;
    }

    @Override
    public boolean isConnectionBased() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return client.getNtlmAuthenticationState().getState() == NtlmAuthenticationProvider.State.COMPLETE || client.getNtlmAuthenticationState().getState() == NtlmAuthenticationProvider.State.FAILED;
    }

    @Override
    public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException {
        throw new UnsupportedOperationException("Depricated!");
    }

    @Override
    public String authenticate(Credentials credentials, HttpMethod method) throws AuthenticationException {
        String response = null;

        try {
            byte[] token = HexUtils.decodeBase64(ntlmchallenge);
            token = client.requestAuthentication(token, convertCredentials(credentials));
            response = HexUtils.encodeBase64(token, true);
        } catch (Exception e) {
            throw new AuthenticationException(e.getMessage());
        }
        return "NTLM " + response;
    }

    private NtlmCredential convertCredentials(Credentials credentials) throws AuthenticationException, AuthenticationManagerException {
        if(credentials instanceof NTCredentials) {
            NTCredentials ntCredentials = (NTCredentials)credentials;
            return new NtlmCredential(ntCredentials.getHost(), ntCredentials.getDomain(), ntCredentials.getUserName(), ntCredentials.getPassword());
        }
        else if(credentials instanceof UsernamePasswordCredentials) {
            UsernamePasswordCredentials usernamePasswordCredentials = (UsernamePasswordCredentials)credentials;
            return new NtlmCredential(usernamePasswordCredentials.getUserName(), usernamePasswordCredentials.getPassword());
        }
        throw new AuthenticationException("Expected NTCredentials or UsernamePasswordCredentials");
    }
}
