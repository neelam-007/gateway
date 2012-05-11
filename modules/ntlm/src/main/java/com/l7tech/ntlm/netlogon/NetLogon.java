package com.l7tech.ntlm.netlogon;

import com.l7tech.ntlm.adapter.AuthenticationAdapter;
import com.l7tech.ntlm.netlogon.NetlogonConstants.*;
import com.l7tech.ntlm.protocol.*;
import jcifs.dcerpc.DcerpcBinding;
import jcifs.dcerpc.DcerpcHandle;
import jcifs.dcerpc.UnicodeString;
import jcifs.dcerpc.msrpc.samr;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SID;
import jcifs.smb.SmbException;
import jcifs.util.DES;
import jcifs.util.Encdec;
import jcifs.util.HMACT64;
import jcifs.util.Hexdump;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/9/12
 */
public class NetLogon extends HashMap implements AuthenticationAdapter{
    private static Logger log = Logger.getLogger(NetLogon.class.getName());
    static final byte[] BYTES4 = { 0, 0, 0, 0 };
    static final SecureRandom random = new SecureRandom();

    private static final int NEGOTIATE_FLAGS = 0x600FFFFF;

    DcerpcHandle handle = null;
    byte[] sessionKey;
    byte[] clientCredentials;
    byte[] serverCredentials;

    public static String getSyntax() {
        return "12345678-1234-abcd-ef00-01234567cffb:1.0";
    }


    static byte[] computeSessionKey(byte[] nTOWFv1, byte[] clientChallenge, byte[] serverChallenge)
    {
        try
        {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(BYTES4, 0, 4);
            md5.update(clientChallenge, 0, 8);
            md5.update(serverChallenge, 0, 8);

            HMACT64 hmac = new HMACT64(nTOWFv1);
            hmac.update(md5.digest());
            return hmac.digest();
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("MD5", gse);
        }
    }

    static byte[] computeNetlogonCredential(byte[] inBytes, byte[] sessionKey)
    {
        byte[] k1 = new byte[7];
        byte[] k2 = new byte[7];

        System.arraycopy(sessionKey, 0, k1, 0, 7);
        System.arraycopy(sessionKey, 7, k2, 0, 7);

        DES k3 = new DES(k1);
        DES k4 = new DES(k2);

        byte[] tmp = new byte[8];
        byte[] outBytes = new byte[8];

        k3.encrypt(inBytes, tmp);
        k4.encrypt(tmp, outBytes);

        return outBytes;
    }

    public NetLogon(Map properties){
        super(properties);
        DcerpcBinding.addInterface("netlogon", getSyntax());
    }

    protected Object get(Object key, Object def) {
        Object val = get(key);
        return val != null? val : def;
    }

    /**
     * perform validation of the client challenge response against AD
     *
     * @param response  challenge response from the client
     * @param challenge
     *@param acct  client account  @return
     * @throws AuthenticationManagerException
     */
    @Override
    public Object validate(NtlmChallengeResponse response, byte[] challenge, Map acct) throws AuthenticationManagerException {
        //first we need to connect to netlogon service
        String hostname = (String)get("localhost.netbios.name", "");
        String serverName = (String)get("server.dns.name", "");
        String serviceName = (String)get("service.account", "");
        String servicePassword = (String)get("service.password", "");

        connect(serverName, hostname, serviceName, servicePassword);

        //validate user account
        int timestamp = (int) (System.currentTimeMillis() / 1000L);
        Encdec.enc_uint32le(Encdec.dec_uint32le(clientCredentials, 0) + timestamp, clientCredentials, 0);
        byte[] cred = computeNetlogonCredential(clientCredentials, sessionKey);

        NetlogonAuthenticator authenticator = new NetlogonAuthenticator(cred, timestamp);
        NetlogonAuthenticator returnAuthenticator = new NetlogonAuthenticator(new byte[8], 0);
        NetlogonNetworkInfo networkInfo = createNetlogonNetworkInfo(response, challenge);
        NetlogonValidationSamInfo2 validationSamInfo2 = new NetlogonValidationSamInfo2();

        try {
            sendNetrLogonSamLogon(serverName, hostname, authenticator, returnAuthenticator, networkInfo, validationSamInfo2);
            //compute credentials and check them against return authenticator credentials
            Encdec.enc_uint32le(Encdec.dec_uint32le(clientCredentials, 0) + 1, clientCredentials, 0);
            cred = computeNetlogonCredential(clientCredentials, sessionKey);

            if (!Arrays.equals(cred, returnAuthenticator.credential)) {
                throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_INVALID_CREDENTIALS, "invalid user credentials");
            }
            //update user account info such as user sids, domain home directory, etc.
            updateUserAccountInfo(acct, validationSamInfo2);
            //set session key
            response.setSessionKey(sessionKey);
        } catch (IOException e) {
            throw  new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "DCP RPC call failed: " + e.getMessage());
        }
        finally {
            disconnect();//release the handle
        }

        return sessionKey;
    }

    public void connect() throws AuthenticationManagerException {
        String hostname = (String)get("localhost.netbios.name","");
        String serverName = (String)get("server.dns.name","");
        String serviceName = (String)get("service.account","");
        String servicePassword = (String)get("service.password","");

        connect(serverName, hostname, serviceName, servicePassword);
    }

    protected void connect(String serverName, String hostname, String serviceName, String servicePassword) throws AuthenticationManagerException {
        DcerpcHandle dcerpcHandle = null;

        dcerpcHandle = bind(serverName, serviceName, servicePassword);

        byte[] clientChallenge = new byte[8];
        byte[] serverChallenge = new byte[8];
        random.nextBytes(clientChallenge);

        NetrServerReqChallenge netrServerReqChallenge = sendNetrServerReqChallenge(serverName, hostname, dcerpcHandle, clientChallenge, serverChallenge);

        byte[] nTOWFv1 = NtlmPasswordAuthentication.nTOWFv1(servicePassword);
        byte[] sessionkey = computeSessionKey(nTOWFv1, clientChallenge, netrServerReqChallenge.serverChallenge);

        clientCredentials = computeNetlogonCredential(clientChallenge, sessionkey);
        serverCredentials = computeNetlogonCredential(netrServerReqChallenge.serverChallenge, sessionkey);

        NetrServerAuthenticate2 netrServerAuthenticate2 = sendNetrServerAuthenticate2(serverName, hostname, serviceName, dcerpcHandle);

        log.log(Level.FINE, "Calculated credentials: " + Hexdump.toHexString(serverCredentials, 0, serverCredentials.length) + " Server credentials: " + Hexdump.toHexString(netrServerAuthenticate2.serverCredential, 0, netrServerAuthenticate2.serverCredential.length));

        if(Arrays.equals(serverCredentials, netrServerAuthenticate2.serverCredential)) {
            this.sessionKey = sessionkey;
            handle = dcerpcHandle;
            log.log(Level.FINE, "Session key: " + Hexdump.toHexString(this.sessionKey, 0, this.sessionKey.length));
        }
        else {
            throw  new AuthenticationManagerException("NetrServerAuthenticate2 credential check failed");
        }

    }

    public void disconnect() {
        if(handle != null) {
            try{
                String serverName = handle.getServer();
                handle.close();
                log.log(Level.FINE, "disconnected from : " + serverName);
            } catch (IOException ex) {
                log.log(Level.SEVERE, "Unable to close DCE RPC handle", ex);
            } finally {
                handle = null;
                sessionKey = null;
                clientCredentials = null;
                serverCredentials = null;
            }
        }
    }



    private NetrServerAuthenticate2 sendNetrServerAuthenticate2(String serverName, String hostname, String serviceName, DcerpcHandle dcerpcHandle) throws AuthenticationManagerException {
        byte[] serverCredBytes = new byte[8];
        int ci = 0;
        String computerAccount = null;

        if (serviceName != null) {
            ci = serviceName.indexOf('@');
            if (ci > 0) {
                computerAccount = serviceName.substring(0, ci);
            } else {
                ci = serviceName.indexOf('\\');
                if (ci > 0) {
                    computerAccount = serviceName.substring(ci + 1);
                }
                else {
                    computerAccount = serviceName;
                }
            }
        }

        NetrServerAuthenticate2 netrServerAuthenticate2 = new NetrServerAuthenticate2(serverName, computerAccount, NetlogonSecureChannelType.WorkstationSecureChannel.value/*(short)2*/, hostname, clientCredentials, serverCredBytes, NEGOTIATE_FLAGS);


        try {
            dcerpcHandle.sendrecv(netrServerAuthenticate2);
            if (netrServerAuthenticate2.retval != 0) {
                throw new SmbException(netrServerAuthenticate2.retval, false);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to send " + NetrServerAuthenticate2.class.getSimpleName() + " message to " + serverName);
            throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "Failed to sendrecv", e);
        }
        return netrServerAuthenticate2;
    }

    private NetrServerReqChallenge sendNetrServerReqChallenge(String serverName, String hostname, DcerpcHandle dcerpcHandle, byte[] client_challenge, byte[] server_challenge) throws AuthenticationManagerException {
        NetrServerReqChallenge netrServerReqChallenge = new NetrServerReqChallenge(serverName, hostname, client_challenge, server_challenge);
        try {
            dcerpcHandle.sendrecv(netrServerReqChallenge);
            if(netrServerReqChallenge.retval != 0) {
                throw new SmbException(netrServerReqChallenge.retval, false);
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to send " + NetrServerReqChallenge.class.getSimpleName() + " message to " + serverName);
            throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "Failed to sendrecv", e);
        }
        return netrServerReqChallenge;
    }

    private DcerpcHandle bind(String serverIp, String serviceName, String servicePassword) throws AuthenticationManagerException {
        DcerpcHandle dcerpcHandle;
        NtlmPasswordAuthentication serviceAuth = new NtlmPasswordAuthentication(null, serviceName, servicePassword);

        String endpoint = "ncacn_np:" + serverIp + "[\\PIPE\\NETLOGON]";

        try {
            dcerpcHandle = DcerpcHandle.getHandle(endpoint, serviceAuth);
            dcerpcHandle.bind();
            log.log(Level.FINE, "Bind to " + serverIp + " was successful");
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to bind to " + serverIp, e);
            throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ERROR, "Unable to bind", e);
        }
        return dcerpcHandle;
    }

    private void sendNetrLogonSamLogon(String logonServer, String computerName, NetlogonAuthenticator authenticator, NetlogonAuthenticator returnAuthenticator,  NetlogonNetworkInfo networkInfo,  NetlogonValidationSamInfo2 samInfo2) throws AuthenticationManagerException, IOException {
        String errInfoEx = "";
        NetrLogonSamLogon rpc = new NetrLogonSamLogon(logonServer, computerName, authenticator, returnAuthenticator, NetlogonLogonInfoClass.NetlogonNetworkInformation.value/*(short) 2*/, networkInfo,  NetlogonValidationInfoClass.NetlogonValidationSamInfo2.value, samInfo2, (byte)0);

        handle.sendrecv(rpc);

        if(networkInfo.identityInfo != null) {
            String domain =  new UnicodeString(networkInfo.identityInfo.logonDomainName, false).toString();
            String user = new UnicodeString(networkInfo.identityInfo.userName, false).toString();
            errInfoEx = "domain="  + domain + ", user=" + user;
        }
        //check returned error codes
        switch (rpc.retval) {
            case 0:
                break;
            case 0xC0000064/*-1073741724*/:  //User logon with misspelled or bad user account
                throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_ACCOUNT_NOT_FOUND, "The account is not found: " + errInfoEx );
            case 0xC000006D: //User logon has incorrect user name
            case 0xC000006A/*-1073741718*/: //User logon with misspelled or bad password
                throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_INVALID_CREDENTIALS, "Invalid user credentials: " + errInfoEx);
            case 0xC000006F/*-1073741713*/: //User logon outside authorized hours
            case 0xC0000070: // User logon from unauthorized workstation
            case 0xC0000071/*-1073741711*/: //User logon with expired password
            case 0xC0000072/*-1073741710*/: //User logon to account disabled by administrator
            case 0xC0000193: //User logon with expired account
            case 0xC0000234/*-1073741260*/: //	 User logon with account locked
                SmbException se = new SmbException(rpc.retval, false);
                throw new AuthenticationManagerException(AuthenticationManagerException.Status.STATUS_INVALID_CREDENTIALS, se.getMessage() + ": " + errInfoEx);
            default:
                throw new SmbException(rpc.retval, false);
        }
    }

    private NetlogonNetworkInfo createNetlogonNetworkInfo(NtlmChallengeResponse response, byte[] challenge) throws AuthenticationManagerException {
        NetlogonNetworkInfo networkInfo = new NetlogonNetworkInfo();

        networkInfo.identityInfo = new NetlogonLogonIdentityInfo();

        networkInfo.identityInfo.logonDomainName = new UnicodeString(response.getDomain()!= null? response.getDomain(): "", false);
        //parameters contain information pertaining to the logon validation processing
        //Allow this account to log on with the computer account.
        //Allow this account to log on with the domain controller account
        networkInfo.identityInfo.parameterControl = 0x820;//2080; //0b100000100000
        networkInfo.identityInfo.userName = new UnicodeString(response.getUsername(), false);
        networkInfo.identityInfo.workstation = new UnicodeString("",false);//new UnicodeString("\\\\" + hostname, false);
        networkInfo.lmChallenge = challenge;
        networkInfo.ntChallengeResponse = new NetlogonChallengeResponse(response.getNtResponse());
        networkInfo.lmChallengeResponse = new NetlogonChallengeResponse(response.getLmResponse());
        return networkInfo;
    }

    private void updateUserAccountInfo(Map acct, NetlogonValidationSamInfo2 samInfo2) {
        SID domainSid = new SID(samInfo2.logonDomainSid, SID.SID_TYPE_DOMAIN, new UnicodeString(samInfo2.logonDomainName, false).toString(), null, false);
        samr.SamrRidWithAttributeArray sids = samInfo2.groups;
        Set groups = new HashSet<SID>();

        for (int i = 0; i < sids.count; i++) {
            SID sid = new SID(domainSid, sids.rids[i].rid);
            groups.add(sid);
        }

        for (int i = 0; i < samInfo2.sidCount; i++) {
            SID sid = new SID(samInfo2.extraSids[i].sid, 0, null, null, false);
            groups.add(sid);
        }

        acct.put("sidGroups", groups);

        acct.put("sAMAccountName", new UnicodeString(samInfo2.effectiveName, false).toString());
        acct.put("userSid", new SID(domainSid, samInfo2.userId));
        acct.put("primaryGroupSid", new SID(domainSid, samInfo2.primaryGroupId));
        acct.put("userAccountFlags", "0x" + Hexdump.toHexString(samInfo2.accountFlags, 8));
        acct.put("session.key", sessionKey);

        if ((samInfo2.fullName != null) && (samInfo2.fullName.buffer != null))
            acct.put("fullName", new UnicodeString(samInfo2.fullName, false).toString());
        if ((samInfo2.homeDirectory != null) && (samInfo2.homeDirectory.buffer != null))
            acct.put("homeDirectory", new UnicodeString(samInfo2.homeDirectory, false).toString());
        if ((samInfo2.homeDirectoryDrive != null) && (samInfo2.homeDirectoryDrive.buffer != null))
            acct.put("homeDirectoryDrive", new UnicodeString(samInfo2.homeDirectoryDrive, false).toString());
        if  ((samInfo2.logonDomainName != null) && (samInfo2.logonDomainName.buffer != null))
            acct.put("logonDomainName", new UnicodeString(samInfo2.logonDomainName, false).toString());
    }
}
