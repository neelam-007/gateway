package com.l7tech.kerberos.delegate;

import sun.security.krb5.*;
import sun.security.krb5.internal.*;
import sun.security.krb5.internal.crypto.*;

import java.io.IOException;

/**
 * Implementation of TGS_REQ for S4U2Self and S4U2Proxy,
 * construct a TGS_REQ message to KDC and obtain service ticket.
 */
public class DelegateKrbTgsReq {

    private EncryptionKey tgsReqKey;
    private Credentials creds;

    public static final int CONSTRAINED_DELEGATION = 14;

    /**
     * Constructor for S4U2Self TGS_REQ message
     */
    public DelegateKrbTgsReq(Credentials tgtCreds,
                             PrincipalName sname, PrincipalName userName)
            throws KrbException, IOException {
        this(getForwardable(),
                tgtCreds,
                sname,
                userName,
                null);
    }

    /**
     * Constructor for S4U2Proxy TGS_REQ message
     */
    public DelegateKrbTgsReq(Credentials tgtCreds,
                             PrincipalName sname, Ticket[] additionalTickets)
            throws KrbException, IOException {
        this(getConstrainedDelegation(),
                tgtCreds,
                sname,
                null,
                additionalTickets);
    }

    private static KDCOptions getForwardable() {
        KDCOptions o = new KDCOptions();
        o.set(KDCOptions.FORWARDABLE, true);
        return o;
    }

    private static KDCOptions getConstrainedDelegation() {
        KDCOptions o = new KDCOptions();
        o.set(CONSTRAINED_DELEGATION, true);
        return o;
    }

    protected DelegateKrbTgsReq(
            KDCOptions options,
            Credentials tgtCreds,
            PrincipalName sname,
            PrincipalName uname,
            Ticket[] additionalTickets)
            throws KrbException, IOException {

        PrincipalName princName = tgtCreds.getClient();
        PrincipalName servName = sname;
        PrincipalName userName = uname;
        tgsReqKey = tgtCreds.getSessionKey();

        if (!(tgtCreds.getTicketFlags().get(Krb5.TKT_OPTS_FORWARDABLE))) {
            throw new KrbException("TGT is not forwardable.");
        }

        TGSReq tgsReqMessg = populateTGSReq(
                options,
                tgtCreds.getTicket(),
                tgtCreds.getSessionKey(),
                new KerberosTime(KerberosTime.NOW),
                princName,
                princName.getRealm(),
                servName,
                userName,
                additionalTickets
        );
        byte[] obuf = tgsReqMessg.asn1Encode();

        //Send message to KDC
        String realmStr = null;
        if (servName != null)
            realmStr = servName.getRealmString();
        KdcComm comm = new KdcComm(realmStr);
        byte[] ibuf = comm.send(obuf);
        DelegateKrbTgsRep tgs_rep = new DelegateKrbTgsRep(ibuf, this);
        creds = tgs_rep.getCreds();
    }

    public Credentials getCreds() {
        return creds;
    }

    private TGSReq populateTGSReq(
            KDCOptions kdc_options,
            Ticket ticket,
            EncryptionKey key,
            KerberosTime ctime,
            PrincipalName cname,
            Realm crealm,
            PrincipalName sname,
            PrincipalName uname,
            Ticket[] additionalTickets)
            throws Asn1Exception, IOException, KdcErrException, KrbApErrException,
            KrbCryptoException {

        int[] eTypes = null;
        eTypes = EType.getDefaults("default_tgs_enctypes");
        if (eTypes == null) {
            throw new KrbCryptoException("Not supported encryption types");
        }

        KDCReqBody reqBody = new KDCReqBody(
                kdc_options,
                cname,
                sname.getRealm(),
                sname,
                null,
                new KerberosTime(0),
                null,
                Nonce.value(),
                eTypes,
                null,
                null,
                additionalTickets);

        byte[] temp = reqBody.asn1Encode(Krb5.KRB_TGS_REQ);

        Checksum cksum = new Checksum(Checksum.CKSUMTYPE_RSA_MD5, temp);

        byte[] tgs_ap_req = new DelegateKrbApReq(
                new APOptions(),
                ticket,
                key,
                crealm,
                cname,
                cksum,
                ctime,
                null,
                null,
                null).getBytes();

        PAData[] tgsPAData = null;

        //If uname is provided, construct S4U2Self message
        if (uname != null) {
            tgsPAData = new PAData[2];
            S4U2SelfData s4U2SelfData = new S4U2SelfData(uname, uname.getRealm(), key);
            tgsPAData[1] = s4U2SelfData.getPAData();
        } else {
            tgsPAData = new PAData[1];
        }
        tgsPAData[0] = new PAData(Krb5.PA_TGS_REQ, tgs_ap_req);

        return new TGSReq(tgsPAData, reqBody);
    }

    public EncryptionKey getTgsReqKey() {
        return tgsReqKey;
    }
}
