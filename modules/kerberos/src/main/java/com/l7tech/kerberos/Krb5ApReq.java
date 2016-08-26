package com.l7tech.kerberos;

import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.HexUtils;
import com.l7tech.util.JdkLoggerConfigurator;
import org.bouncycastle.asn1.*;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Created by balro10 on 2016-08-18.
 */

/*
 * Implementation of KRB5_AP_REQ ticket parsing
 *
 * The JDK implements parsing but only as part of authentication of the ticket.
 * It is not possible in the JDK to peek into the KRB5_AP_REQ ticket without authenticating
 * it.
 *
 * This class parses the non-encrypted portions of the tickets so they can be examined prior
 * to authentication.
 *
 *
 *
 *
 */


// SSG-13640 - When the SSG has multiple service keys (SPNs) in its keytab it attempts to:
//     guess the SPN required to authenticate the via in the bound URL (KerberosUtils.extractSpnFromRequest())
//     or by reverse lookup the remote calling endpoint and do the same (to support Load Balancer Proxies)
//     or failing those uses the first key in the keytab.
//     This class supports the parsing of the plaintext portion of the inbound ticket so that the SPN
//     can be obtained before the Gateway needs to know which SPN to authenticate the request with.


public class Krb5ApReq {
    public static Logger logger = Logger.getLogger(Krb5ApReq.class.getName());

    public static final Config config = ConfigFactory.getCachedConfig();

    public static final String GSSAPI_TOKEN_OID = "1.2.840.113554.1.2.2";
    public static final int GSSAPI_APP_ID = 0;
    public static final int KRB5_APREQ_ID = 14;
    public static final int KRB5_TICKET_ID = 1;

    private String protocol;
    private String host;
    private String realm;

    // Constructor expects GSSAPI Wrapped as byte array
    public Krb5ApReq(byte[] ticket) {

        if (JdkLoggerConfigurator.debugState() ) {
            logger.warning("Decoding Ticket: " + HexUtils.encodeBase64(ticket));
        }

        try {
            ASN1InputStream ain = new ASN1InputStream(ticket);
            DERApplicationSpecific gssApiWrap = (DERApplicationSpecific) ain.readObject();

            if ( ! (gssApiWrap.isConstructed() && gssApiWrap.getApplicationTag() == GSSAPI_APP_ID ) ) {
                throw new Krb5ApReqException();
            }

            byte[] gssapiContents = gssApiWrap.getContents();
            ASN1InputStream wrappedStream = new ASN1InputStream(gssapiContents);
            DERObjectIdentifier gssapioid = (DERObjectIdentifier)wrappedStream.readObject();

            if ( gssapioid == null || gssapiContents == null  || ! gssapioid.toString().equals(GSSAPI_TOKEN_OID) ) {
                throw new Krb5ApReqException();
            }

            int tag = (wrappedStream.read() << 8) | wrappedStream.read();
            if ( tag != 0x100 ) {
                throw new Krb5ApReqException();
            }

            DERApplicationSpecific apReq = (DERApplicationSpecific)wrappedStream.readObject();

            if ( apReq.getApplicationTag() != KRB5_APREQ_ID ) {
                throw new Krb5ApReqException();
            }

            byte[] apReqContents = apReq.getContents();
            ASN1InputStream apReqStream = new ASN1InputStream(apReqContents);
            DERSequence apReqSeq = (DERSequence)apReqStream.readObject();
            ASN1Integer pvno = (ASN1Integer)((DERTaggedObject)apReqSeq.getObjectAt(0).getDERObject()).getObject();
            ASN1Integer msgtype = (ASN1Integer)((DERTaggedObject)apReqSeq.getObjectAt(1).getDERObject()).getObject();
            DERApplicationSpecific innerTicket = (DERApplicationSpecific)((DERTaggedObject)apReqSeq.getObjectAt(3).getDERObject()).getObject();

            if ( innerTicket.getApplicationTag() != KRB5_TICKET_ID ) {
                throw new Krb5ApReqException();
            }

            ASN1InputStream innerTicketStream = new ASN1InputStream(innerTicket.getContents());
            DERSequence ticketSeq = (DERSequence)innerTicketStream.readObject();
            DERInteger tktvno = (DERInteger)((DERTaggedObject)ticketSeq.getObjectAt(0)).getObject();
            DERString realm = (DERString)((DERTaggedObject)ticketSeq.getObjectAt(1)).getObject();
            this.realm = realm.getString();
            DERSequence sname = (DERSequence)((DERTaggedObject)ticketSeq.getObjectAt(2)).getObject();
            DERInteger nametype = (DERInteger)((DERTaggedObject)sname.getObjectAt(0)).getObject();
            DERSequence namelist = (DERSequence)((DERTaggedObject)sname.getObjectAt(1)).getObject();

            int namesz = namelist.size();
            if ( namesz != 2 ) {
                throw new Krb5ApReqException();
            }

            DERGeneralString protocol = (DERGeneralString)namelist.getObjectAt(0);
            DERGeneralString host = (DERGeneralString)namelist.getObjectAt(1);

            this.protocol = protocol.getString();
            this.host = host.getString();

        } catch (IOException e) {
            throw new Krb5ApReqException();
        }

        if (JdkLoggerConfigurator.debugState() ) {
            logger.warning("Decoded protocol: " + protocol +", host: " + host + ", realm: " + realm );
        }
    }

    public String getProtocol() { return protocol; }

    public String getHost() { return host; }

    public String getRealm() { return realm; }

    public String getSpn() { return protocol + "/" + host + "@" + realm; }
}
