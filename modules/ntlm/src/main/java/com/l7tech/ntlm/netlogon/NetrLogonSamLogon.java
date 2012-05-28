package com.l7tech.ntlm.netlogon;

import jcifs.dcerpc.DcerpcMessage;
import jcifs.dcerpc.ndr.NdrBuffer;
import jcifs.dcerpc.ndr.NdrException;
import jcifs.dcerpc.ndr.NdrObject;

/**
 * Copyright: Layer 7 Technologies, 2012
 * User: ymoiseyenko
 * Date: 3/12/12
 */
public class NetrLogonSamLogon extends DcerpcMessage {

    public int retval;
    public String logonServer;
    public String computerName;
    public NetlogonAuthenticator authenticator;
    public NetlogonAuthenticator returnAuthenticator;
    public short logonLevel;
    public NdrObject logonInformation;
    public short validationLevel;
    public NdrObject validationInformation;
    public byte authoritative;

    public int getOpnum() {
        return 2;
    }

    public NetrLogonSamLogon(String logonServer, String computerName, NetlogonAuthenticator authenticator, NetlogonAuthenticator returnAuthenticator, short logonLevel, NdrObject logonInformation, short validationLevel, NdrObject validationInformation, byte authoritative) {
        this.logonServer = logonServer;
        this.computerName = computerName;
        this.authenticator = authenticator;
        this.returnAuthenticator = returnAuthenticator;
        this.logonLevel = logonLevel;
        this.logonInformation = logonInformation;
        this.validationLevel = validationLevel;
        this.validationInformation = validationInformation;
        this.authoritative = authoritative;
        this.ptype = 0;
        this.flags = DCERPC_FIRST_FRAG | DCERPC_LAST_FRAG;
    }

    @Override
    public void encode_in(NdrBuffer buffer) throws NdrException {
        buffer.enc_ndr_referent(logonServer, 1);
        if (logonServer != null) {
            buffer.enc_ndr_string(logonServer);
        }

        buffer.enc_ndr_referent(computerName, 1);
        if (computerName != null) {
            buffer.enc_ndr_string(computerName);
        }

        buffer.enc_ndr_referent(authenticator, 1);
        if (authenticator != null) {
            authenticator.encode(buffer);
        }

        buffer.enc_ndr_referent(returnAuthenticator, 1);
        if (returnAuthenticator != null) {
            returnAuthenticator.encode(buffer);
        }

        buffer.enc_ndr_short(logonLevel);
        short descriptor = logonLevel;
        buffer.enc_ndr_short(descriptor);
        buffer.enc_ndr_referent(logonInformation, 1);
        if (logonInformation != null) {
            buffer = buffer.deferred;
            logonInformation.encode(buffer);
        }

        buffer.enc_ndr_short(validationLevel);
    }

    @Override
    public void decode_out(NdrBuffer buffer) throws NdrException {
        int returnAuthenticatorPtr = buffer.dec_ndr_long();
        if (returnAuthenticatorPtr != 0) {
            if (returnAuthenticator == null) {
                returnAuthenticator = new NetlogonAuthenticator();
            }
            returnAuthenticator.decode(buffer);
        }

        buffer.dec_ndr_short();
        int validationInformationPtr = buffer.dec_ndr_long();
        if (validationInformationPtr != 0) {
            if (validationInformation == null) {
                validationInformation = new NetlogonValidationSamInfo2();
            }
            buffer = buffer.deferred;
            validationInformation.decode(buffer);
        }

        authoritative = (byte) buffer.dec_ndr_small();
        retval = buffer.dec_ndr_long();
    }
}
