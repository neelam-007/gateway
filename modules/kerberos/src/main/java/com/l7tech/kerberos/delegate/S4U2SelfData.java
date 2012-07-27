package com.l7tech.kerberos.delegate;


import sun.security.krb5.*;
import sun.security.krb5.internal.KdcErrException;
import sun.security.krb5.internal.KrbApErrException;
import sun.security.krb5.internal.PAData;
import sun.security.krb5.internal.util.KerberosString;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;

import java.io.IOException;

public class S4U2SelfData {

    private PrincipalName userName;
    private Realm userRealm;
    private Checksum checksum;
    private byte[] checksumAsn;
    private EncryptionKey encryptionKey;
    private final String authPackage = "Kerberos";

    public S4U2SelfData(PrincipalName userName, Realm realm, EncryptionKey encryptionKey) throws IOException, Asn1Exception, KrbCryptoException, KrbApErrException, KdcErrException {

        this.userName = userName;
        this.userRealm = realm;
        this.encryptionKey = encryptionKey;

        byte[] nameType = toByteArray(userName.getNameType());
        String[] nameStrings = userName.getNameStrings();
        StringBuilder checksumString = new StringBuilder();
        for (int i = 0; i < nameStrings.length; i++) {
             checksumString.append(nameStrings[i]);
        }

         checksumString.append(realm.toString()).append(authPackage);
        byte[] checksumBytes = concat(nameType,  checksumString.toString().getBytes("UTF8"));

        checksum = new Checksum(Checksum.CKSUMTYPE_HMAC_MD5_ARCFOUR, checksumBytes, encryptionKey, 17 );
        checksumAsn = checksum.asn1Encode();

    }
    
    public PAData getPAData() throws IOException, Asn1Exception {
        PAData paData = new PAData(129, asn1Encode());
        return paData;
    }

    public byte[] asn1Encode() throws Asn1Exception, IOException {

        DerOutputStream bytes = new DerOutputStream();
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x00), userName.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x01), userRealm.asn1Encode());
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x02), checksum.asn1Encode());
        DerOutputStream temp = new DerOutputStream();
        DerValue der = new KerberosString(authPackage).toDerValue();
        temp.putDerValue(der);
        bytes.write(DerValue.createTag(DerValue.TAG_CONTEXT, true, (byte) 0x03), temp);
        temp = new DerOutputStream();
        temp.write(DerValue.tag_Sequence, bytes);
        return temp.toByteArray();
    }


    private byte[] toByteArray(int paramInt) {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            bytes[i] = (byte) ((paramInt >> i * 8) % 255);
        }
        return bytes;
    }

    private byte[] concat(byte[]... bytes) {
        int size = 0;
        for (int i = 0; i < bytes.length; i++) {
            byte[] aByte = bytes[i];
            size = size + aByte.length;
        }
        byte[] result = new byte[size];
        int pos = 0;
        for (int i = 0; i < bytes.length; i++) {
            byte[] aByte = bytes[i];
            System.arraycopy(aByte, 0, result, pos, aByte.length);
            pos = aByte.length;
        }
        return result;
    }

}
