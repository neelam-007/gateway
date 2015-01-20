package com.l7tech.kerberos.delegate;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.KeyUsage;

import java.io.IOException;

public class DelegateKrbApReq {

    private byte[] value;

    public DelegateKrbApReq(APOptions apOptions,
                            Ticket ticket,
                            EncryptionKey eKey,
                            Realm crealm,
                            PrincipalName cname,
                            Checksum cksum,
                            KerberosTime ctime,
                            EncryptionKey subKey,
                            SeqNumber seqNumber,
                            AuthorizationData authorizationData)
            throws Asn1Exception, IOException,
            KdcErrException, KrbCryptoException {

        Authenticator authenticator = new Authenticator(
                cname,
                cksum,
                ctime.getMicroSeconds(),
                ctime,
                subKey,
                (seqNumber != null ? new Integer(seqNumber.current()) : null),
                authorizationData);

        EncryptedData encryptedData = new EncryptedData(eKey,
                authenticator.asn1Encode(),
                KeyUsage.KU_PA_TGS_REQ_AUTHENTICATOR);

        APReq apReq = new APReq(apOptions, ticket, encryptedData);
        value = apReq.asn1Encode();

    }

    public byte[] getBytes() {
        return value;
    }

}
