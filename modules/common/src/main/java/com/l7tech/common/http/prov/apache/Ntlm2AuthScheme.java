package com.l7tech.common.http.prov.apache;

import com.l7tech.ntlm.protocol.*;
import com.l7tech.util.HexUtils;

import org.apache.http.FormattedHeader;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.*;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.CharArrayBuffer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 */
public class Ntlm2AuthScheme implements ContextAwareAuthScheme {
    public static final String NTLM = "ntlm";

    private static String DEFAULT_FLAGS = null;

    private static String LOCALHOST_NETBIOS_NAME;
    private NtlmAuthenticationClient client;
    private String ntlmchallenge = null;

    private ChallengeState challengeState = null;

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



    protected void parseChallenge(
            final CharArrayBuffer buffer,
            int beginIndex, int endIndex) throws MalformedChallengeException {
        String challenge = buffer.substringTrimmed(beginIndex, endIndex);
        if (challenge.length() != 0) {
            this.ntlmchallenge = challenge;
            client.getNtlmAuthenticationState().setState(NtlmAuthenticationProvider.State.CHALLENGE);
        } else {
            this.ntlmchallenge = "";
            if (client.getNtlmAuthenticationState().getState() != NtlmAuthenticationProvider.State.NEGOTIATE) {
                client.getNtlmAuthenticationState().setState(NtlmAuthenticationProvider.State.FAILED);
            }
        }
    }
    /**
     * Processes the given challenge token. Some authentication schemes
     * may involve multiple challenge-response exchanges. Such schemes must be able
     * to maintain the state information when dealing with sequential challenges
     *
     * @param header the challenge header
     */
    @Override
    public void processChallenge(Header header) throws MalformedChallengeException {
        if (header == null) {
            throw new IllegalArgumentException("Header may not be null");
        }
        String authheader = header.getName();
        if (authheader.equalsIgnoreCase(AUTH.WWW_AUTH)) {
            this.challengeState = ChallengeState.TARGET;
        } else if (authheader.equalsIgnoreCase(AUTH.PROXY_AUTH)) {
            this.challengeState = ChallengeState.PROXY;
        } else {
            throw new MalformedChallengeException("Unexpected header name: " + authheader);
        }

        CharArrayBuffer buffer;
        int pos;
        if (header instanceof FormattedHeader) {
            buffer = ((FormattedHeader) header).getBuffer();
            pos = ((FormattedHeader) header).getValuePos();
        } else {
            String s = header.getValue();
            if (s == null) {
                throw new MalformedChallengeException("Header value is null");
            }
            buffer = new CharArrayBuffer(s.length());
            buffer.append(s);
            pos = 0;
        }
        while (pos < buffer.length() && HTTP.isWhitespace(buffer.charAt(pos))) {
            pos++;
        }
        int beginIndex = pos;
        while (pos < buffer.length() && !HTTP.isWhitespace(buffer.charAt(pos))) {
            pos++;
        }
        int endIndex = pos;
        String s = buffer.substring(beginIndex, endIndex);
        if (!s.equalsIgnoreCase(getSchemeName())) {
            throw new MalformedChallengeException("Invalid scheme identifier: " + s);
        }

        parseChallenge(buffer, pos, buffer.length());
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
    public boolean isConnectionBased() {
        return true;
    }

    @Override
    public boolean isComplete() {
        return client.getNtlmAuthenticationState().getState() == NtlmAuthenticationProvider.State.COMPLETE || client.getNtlmAuthenticationState().getState() == NtlmAuthenticationProvider.State.FAILED;
    }

    /**
     * Produces an authorization string for the given set of {@link org.apache.http.auth.Credentials}.
     *
     * @param credentials The set of credentials to be used for athentication
     * @param request     The request being authenticated
     * @return the authorization string
     * @throws org.apache.http.auth.AuthenticationException
     *          if authorization string cannot
     *          be generated due to an authentication failure
     * @deprecated (4.1)  Use {@link org.apache.http.auth.ContextAwareAuthScheme#authenticate(org.apache.http.auth.Credentials, org.apache.http.HttpRequest, org.apache.http.protocol.HttpContext)}
     */
    @Override
    public Header authenticate(Credentials credentials, HttpRequest request) throws AuthenticationException {
        return authenticate(credentials, request);
    }


    /**
     * Produces an authorization string for the given set of
     * {@link org.apache.http.auth.Credentials}.
     *
     * @param credentials The set of credentials to be used for athentication
     * @param request     The request being authenticated
     * @param context     HTTP context
     * @return the authorization string
     * @throws org.apache.http.auth.AuthenticationException
     *          if authorization string cannot
     *          be generated due to an authentication failure
     */
    @Override
    public Header authenticate(Credentials credentials, HttpRequest request, HttpContext context) throws AuthenticationException {
        try {
            byte[] token = HexUtils.decodeBase64(ntlmchallenge);
            token = client.requestAuthentication(token, convertCredentials(credentials));
            String response = HexUtils.encodeBase64(token, true);
            //once response is base64 encoded create and return an Authorization header with NTLM response
            CharArrayBuffer buffer = new CharArrayBuffer(32);
            buffer.append(AUTH.WWW_AUTH_RESP);
            buffer.append(": NTLM ");
            buffer.append(response);
            return new BufferedHeader(buffer);
        } catch (Exception e) {
            throw new AuthenticationException(e.getMessage());
        }
    }

    private NtlmCredential convertCredentials(Credentials credentials) throws AuthenticationException, AuthenticationManagerException {
        if(credentials instanceof NTCredentials) {
            NTCredentials ntCredentials = (NTCredentials)credentials;
            return new NtlmCredential(ntCredentials.getWorkstation(), ntCredentials.getDomain(), ntCredentials.getUserName(), ntCredentials.getPassword());
        }
        else if(credentials instanceof UsernamePasswordCredentials) {
            UsernamePasswordCredentials usernamePasswordCredentials = (UsernamePasswordCredentials)credentials;
            return new NtlmCredential(usernamePasswordCredentials.getUserName(), usernamePasswordCredentials.getPassword());
        }
        throw new AuthenticationException("Expected NTCredentials or UsernamePasswordCredentials");
    }
}
