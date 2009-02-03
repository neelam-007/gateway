package com.l7tech.skunkworks.kerberos;

import com.l7tech.util.IOUtils;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.HexUtils;
import org.ietf.jgss.Oid;
import sun.security.jgss.GSSHeader;
import sun.security.krb5.EncryptedData;
import sun.security.krb5.EncryptionKey;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;

/**
 * Utility to dump information from a kerberos ticket.
 *
 * <p>This should work for SPNEGO and GSS wrapped tickets so will be good for
 * Windows Integrated authentication and Kerberos Token Profile.</p>
 *
 * <p>This is basically a viewer for Kerbreos AP_REQ messages.</p>
 */
public class KerberosTicketDump extends JFrame {

    //- PUBLIC

    public static void main(String[] args) {
        KerberosTicketDump kerberosTicketDump = new KerberosTicketDump();
        kerberosTicketDump.setVisible(true);
    }

    public KerberosTicketDump() {
        super(TITLE);
        init();
    }

    //- PRIVATE

    // App constants
    private static final String TITLE = "Kerberos Ticket Dump v0.1";

    // Kerb constants
    private static final String KERBEROS_5_OID = "1.2.840.113554.1.2.2";
    private static final String OID_SPNEGO = "1.3.6.1.5.5.2";
    private static final String[] ETYPE_NAMES = {
        null,
        "des-cbc-crc",
        "des-cbc-md4",
        "des-cbc-md5",
        null,
        "des3-cbc-md5",
        null,
        "des3-cbc-sha1",
        null,
        "dsaWithSHA1-CmsOID",
        "md5WithRSAEncryption-CmsOID",
        "sha1WithRSAEncryption-CmsOID",
        "rc2CBC-EnvOID",
        "rsaEncryption-EnvOID",
        "rsaES-OAEP-ENV-OID",
        "des-ede3-cbc-Env-OID",
        "des3-cbc-sha1-kd",
        "aes128-cts-hmac-sha1-96",
        "aes256-cts-hmac-sha1-96",
        null,
        null,
        null,
        null,
        "rc4-hmac",
    };

    // GUI
    private JButton decodeButton;
    private JPasswordField ticketPasswordField;
    private JTextArea ticketTextArea;
    private JTextArea resultTextArea;
    private JPanel mainPanel;

   private void init() {
        setContentPane(mainPanel);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        decodeButton.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                resultTextArea.setText(decodeTicket(ticketTextArea.getText(), ticketPasswordField.getPassword()));
                resultTextArea.setCaretPosition(0);
            }
        });

        pack();
        Utilities.setEscKeyStrokeDisposes(this);
        Utilities.centerOnScreen(this);
    }

    private byte[] stripSPNEGO(byte[] possiblySpnegoToken, StringBuilder messages) throws IOException {
        byte[] resultToken = null;

        ObjectIdentifier oid = null;
        DerInputStream dis = null;

        try {
            DerValue dv = new DerValue(possiblySpnegoToken);
            if (dv.isConstructed() && dv.isApplication() && (0==(dv.getTag()&31))) {
                dis = new DerInputStream(dv.getDataBytes());
                oid = dis.getOID();
            }
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }

        if (oid != null && OID_SPNEGO.equals(oid.toString())) {
            try {
                DerValue negotationTokenDv = dis.getDerValue();
                if (negotationTokenDv.isContextSpecific((byte)0) &&
                    negotationTokenDv.isConstructed()) {
                    DerInputStream negTokenInitSeqStr = new DerInputStream(negotationTokenDv.getDataBytes());

                    if ((negTokenInitSeqStr.peekByte() & 63)==48) {
                        DerValue[] negTokenInitDerValues = negTokenInitSeqStr.getSequence(4);

                        for (int i=0; i<negTokenInitDerValues.length; i++) {
                            DerValue contextualDerValue = negTokenInitDerValues[i];
                            if (contextualDerValue.isContextSpecific((byte)2)) {
                                resultToken = contextualDerValue.data.getOctetString();
                            }
                        }
                    }
                }
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }

        if (resultToken == null) {
            // then the token is not SPNEGO
            resultToken = possiblySpnegoToken;
        } else {
            messages.append("Removed SPNEGO wrapper.\n");
        }

        return resultToken;
    }

    private byte[] stripGSS(byte[] token, StringBuilder messages) throws IOException {
        InputStream is = new ByteArrayInputStream(token,0,token.length);

        try {
            // Skip header bytes and check mechanism
            GSSHeader header = new GSSHeader(is);
            if(!new Oid(header.getOid().toString()).equals(new Oid(KERBEROS_5_OID))) {
                throw new IOException("Expected Kerberos v5 mechanism '" +header.getOid()+"'");
            }

            // Process the GSS token type
            int identifier = ((is.read() << 8) | is.read());
            if (identifier != 256) {
                throw new IOException("Incorrect token type '" +identifier+"' (expected AP REQ)");
            }

            messages.append("Removed GSS wrapper.\n");

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copyStream(is, baos);
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return token;
    }

    private String getEtypeName(int etype) {
        String name = null;

        if ( etype > 0 && etype < ETYPE_NAMES.length) {
            name = ETYPE_NAMES[etype];
        }

        if (name == null) {
            name = "unknown";
        }

        return name;
    }

    private String getNameTypeName(int nameType) {
        String name;

        switch (nameType) {
            case 0:
                name = "NT-UNKNOWN";
                break;
            case 1:
                name = "NT-PRINCIPAL";
                break;
            case 2:
                name = "NT-SRV-INST";
                break;
            case 3:
                name = "NT-SRV-HST";
                break;
            case 4:
                name = "NT-SRV-XHST";
                break;
            case 5:
                name = "NT-UID";
                break;
            default:
                name = "unknown";
                break;
        }

        return name; 
    }

    private String decodeTicket(final String base64Ticket, final char[] password) {
        StringBuilder sb = new StringBuilder();

        try {
            byte[] decoded = stripGSS(stripSPNEGO(HexUtils.decodeBase64(base64Ticket), sb), sb);

            DerValue dv = new DerValue(decoded);

//            sb.append("Is Application?: " + dv.isApplication() + "\n");
//            sb.append("Tag: " + (dv.getTag() ^ DerValue.TAG_APPLICATION) + "\n");
//            sb.append("Uni: " + dv.isUniversal() + "\n");
//            sb.append("Ctx: " + dv.isContextSpecific() + "\n");
//            sb.append("Cns: " + dv.isConstructed() + "\n");

            DerInputStream dis = new DerInputStream(dv.getDataBytes());
            DerValue[] dvs = dis.getSequence(5);
//            for(int d=0; d<dvs.length; d++) {
//                sb.append("App: " + d + " - " + dvs[d].isApplication()+ "\n");
//                sb.append("Uni: " + d + " - " + dvs[d].isUniversal() + "\n");
//                sb.append("Ctx: " + d + " - " + dvs[d].isContextSpecific() + "\n");
//                sb.append("Cns: " + d + " - " + dvs[d].isConstructed() + "\n");
//                sb.append("Tag: " + (dvs[d].tag & 0x1f) + "\n");
//            }

            sb.append("General:\n");
            sb.append("pvno: " + dvs[0].data.getInteger() + "\n");  // pvno[0]
            sb.append("type: " + dvs[1].data.getInteger() + "\n");  // msg-type[1]
            sb.append("apopts: " + (new BigInteger(dvs[2].data.getBitString()).toString(2)) + "\n");// ap-options[2]

            // ticket der value
            DerValue tdv = dvs[3].data.getDerValue();
            DerInputStream dis2 = new DerInputStream(tdv.getDataBytes());
            DerValue[] dvs2 = dis2.getSequence(5);

            sb.append("tktvno: " + dvs2[0].data.getInteger() + "\n");      // tkt-vno[0]
            String realm = dvs2[1].data.getGeneralString();
            sb.append("realm: " + realm + "\n");// realm[1]

            // principal name
            //NT-UNKNOWN       0     Name type not known
            //NT-PRINCIPAL     1     Just the name of the principal as in
            //                       DCE, or for users
            //NT-SRV-INST      2     Service and other unique instance (krbtgt)
            //NT-SRV-HST       3     Service with host name as instance
            //                       (telnet, rcommands)
            //NT-SRV-XHST      4     Service with host as remaining components
            //NT-UID           5     Unique ID
            DerValue[] pndvs1 = dvs2[2].data.getSequence(5);
            int ntype = pndvs1[0].data.getInteger();
            sb.append("name-type: " + ntype + " (" + getNameTypeName(ntype) + ")\n");  //name-type[0]
            DerValue[] pndvs2 = pndvs1[1].data.getSequence(5);//name-string[1]
            String host = "unknown";
            for(int i=0; i<pndvs2.length; i++) {
                sb.append(pndvs2[i].getGeneralString() + "\n");
                if (i==1)
                    host = pndvs2[i].getGeneralString(); 
            }

            String pname = "http/" +  host + "@" + realm;
            sb.append("SPN: " + pname + "\n");

            //        EncryptedData ::=   SEQUENCE {
            //                       etype[0]     INTEGER, -- EncryptionType
            //                       kvno[1]      INTEGER OPTIONAL,
            //                       cipher[2]    OCTET STRING -- ciphertext
            //        }

            DerValue[] eddvs = dvs2[3].data.getSequence(3);
            sb.append("Encrypted data information:\n");
            int etype = eddvs[0].data.getInteger();
            sb.append("etype: " + etype + " ("+getEtypeName(etype)+")\n");
            sb.append("kvno:  " + eddvs[1].data.getInteger() + "\n");
            byte[] encdata = eddvs[2].data.getOctetString();
            sb.append(encdata.length + "\n");
            sb.append("Ticket Encoded HEX:\n");
            sb.append(HexUtils.hexDump(encdata) + "\n");

            if ( password.length > 0 ) {
                // modify here to support other encryption types
                boolean isDes = etype < 23;
                String algorithm = isDes ?  "DES" : "ArcFourHmac";
                KerberosPrincipal principal = new KerberosPrincipal(pname, KerberosPrincipal.KRB_NT_PRINCIPAL);
                KerberosKey kkey = new KerberosKey(principal, password, algorithm);
                byte[] keyBytes = kkey.getEncoded();

                EncryptedData ed = new EncryptedData(etype, 0, encdata);
                byte[] decrypted = null;
                try {
                    decrypted = ed.decrypt(new EncryptionKey(keyBytes,etype,0), 2);
                } catch (Exception e) {
                    throw new GeneralSecurityException(e);
                }
                //}
                sb.append("Ticket Decoded HEX:\n");
                sb.append(HexUtils.hexDump(decrypted) + "\n");

                //          EncTicketPart ::=     [APPLICATION 3] SEQUENCE {
                //                      flags[0]             TicketFlags,
                //                      key[1]               EncryptionKey,
                //                      crealm[2]            Realm,
                //                      cname[3]             PrincipalName,
                //                      transited[4]         TransitedEncoding,
                //                      authtime[5]          KerberosTime,
                //                      starttime[6]         KerberosTime OPTIONAL,
                //                      endtime[7]           KerberosTime,
                //                      renew-till[8]        KerberosTime OPTIONAL,
                //                      caddr[9]             HostAddresses OPTIONAL,
                //                      authorization-data[10]   AuthorizationData OPTIONAL
                //          }

                DerValue etdv = new DerValue(decrypted);
                DerInputStream ddis = new DerInputStream(etdv.getDataBytes());
                //System.out.println(ddis.available());

                DerValue[] sequence = ddis.getSequence(11);

                for ( int i = 0; i<sequence.length; i++ ) {
                    DerValue sdv = sequence[i];

                    int part = sdv.tag & 0x1f;

                    switch(part) {
                        case 0: // flags
                            byte[] bits = sdv.data.getBitString();
                            long flags = 0;
                            //System.out.println(bits.length);
                            if (bits.length < 5) {
                                flags |= (bits[0] & 0xFF);
                                flags |= (bits[1] & 0xFF) << 8;
                                flags |= (bits[2] & 0xFF) << 16;
                                flags |= (bits[3] & 0xFF) << 24;
                            }
                            sb.append("tkt-flags: " + Long.toBinaryString(flags)  + " ("+describeFlags(flags)+")\n");
                            break;
                        case 1: // key
                            DerValue[] keySeq = sdv.data.getSequence(2);
                            sb.append("session-key-type: " + keySeq[0].data.getInteger() + "\n");
                            sb.append("session-key-data: " + HexUtils.hexDump(keySeq[1].data.getOctetString()) + "\n");
                            break;
                        case 2: // crealm
                            sb.append("client-realm: " + sdv.data.getGeneralString() + "\n");
                            break;
                        case 3: // cname
                            DerInputStream pndis = new DerInputStream(sdv.getDataBytes()); // cname[3]
                            pndis = pndis.getDerValue().data;
                            DerValue pdv = pndis.getDerValue();
                            int type = pdv.data.getInteger();
                            sb.append("client-name-type: " + type + " ("+getNameTypeName(type)+")\n" );
                            pdv = pndis.getDerValue();
                            DerValue[] pnseq = pdv.data.getSequence(4);
                            sb.append("client-name: ");
                            for (int j = 0; j < pnseq.length; j++) {
                                DerValue derValue = pnseq[j];
                                if ( j > 0 )
                                    sb.append(", ");
                                sb.append( derValue.getGeneralString() );
                            }
                            sb.append("\n");
                            break;
                        case 4: // transited
                            sb.append("transited: TODO\n");
                            break;
                        case 5: // authtime
                            sb.append("tkt-authtime: " + sdv.data.getGeneralizedTime() + "\n");
                            break;
                        case 6: // starttime
                            sb.append("tkt-starttime: " + sdv.data.getGeneralizedTime() + "\n");
                            break;
                        case 7: // endtime
                            sb.append("tkt-endtime: " + sdv.data.getGeneralizedTime() + "\n");
                            break;
                        case 8: // renew-till
                            sb.append("tkt-renew-till: " + sdv.data.getGeneralizedTime() + "\n");
                            break;
                        case 9: // caddr
                            sb.append("client-addr: TODO\n");
                            break;
                        case 10: // authorization-data
                            sb.append("authorization-data: TODO\n");
                            break;
                    }
                }

                // TODO output authenticator info
            } else {
                sb.append("No password. Not decoding ticket.\n");
            }
        } catch (IOException ioe) {
            sb.append("Could not decode BASE64 ticket.");
            ioe.printStackTrace();
        } catch (GeneralSecurityException gse) {
            sb.append("Could not decode encrypted part.");
            gse.printStackTrace();
        } 

        return sb.toString();
    }

    private boolean addFlag(StringBuilder string, long flags, long mask, String desc, boolean first) {
        boolean added = false;

        //System.out.println(Long.toBinaryString(mask) + " " + desc + " " + (( (flags & mask) > 0 )));

        if ( (flags & mask) > 0 ) {
            added = true;
            if (!first)
                string.append(", ");
            string.append(desc);
        }

        return first && !added;
    }

    /**
     * Get text for flags
     *
     *     reserved(0),
     *     forwardable(1),
     *     forwarded(2),
     *     proxiable(3),
     *     proxy(4),
     *     may-postdate(5),
     *     postdated(6),
     *     invalid(7),
     *     renewable(8),
     *     initial(9),
     *     pre-authent(10),
     *     hw-authent(11)
     *     Transited Policy Checked: Kdc has NOT performed transited policy checking (12)
     *     Ok As Delegate: This ticket is NOT ok as a delegated ticket (13)
     */
    private String describeFlags(long flags) {
        StringBuilder flagString = new StringBuilder();

        boolean isfirst = true;
        isfirst = addFlag(flagString, flags,   0x02, "forwardable", isfirst);
        isfirst = addFlag(flagString, flags,   0x04, "forwarded", isfirst);
        isfirst = addFlag(flagString, flags,   0x08, "proxiable", isfirst);
        isfirst = addFlag(flagString, flags,   0x10, "proxy", isfirst);
        isfirst = addFlag(flagString, flags,   0x20, "may-postdate", isfirst);
        isfirst = addFlag(flagString, flags,   0x40, "postdated", isfirst);
        isfirst = addFlag(flagString, flags,   0x80, "invalid", isfirst);
        isfirst = addFlag(flagString, flags,  0x100, "renewable", isfirst);
        isfirst = addFlag(flagString, flags,  0x200, "initial", isfirst);
        isfirst = addFlag(flagString, flags,  0x400, "pre-authent", isfirst);
        isfirst = addFlag(flagString, flags,  0x800, "hw-authent", isfirst);
        isfirst = addFlag(flagString, flags, 0x1000, "transited", isfirst);
        isfirst = addFlag(flagString, flags, 0x2000, "ok-as-delegate", isfirst);

        return flagString.toString();
    }
}
