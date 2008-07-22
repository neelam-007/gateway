package com.l7tech.kerberos;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import sun.security.jgss.GSSHeader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.l7tech.util.ArrayUtils;

/**
 * Represents a Kerberos BST (GSS AP REQ)
 *
 * @author $Author$
 * @version $Revision$
 */
public final class KerberosGSSAPReqTicket {

    //- PUBLIC

    /**
     * Create a ticket with the given bytes.
     *
     * @param bytes the GSS wrapped AP REQ ticket bytes (from BST)
     */
    public KerberosGSSAPReqTicket(byte[] bytes) {
        if(bytes==null || bytes.length==0) throw new IllegalArgumentException("no data");
        ticketBytes = new byte[bytes.length];
        System.arraycopy(bytes,0,ticketBytes,0,bytes.length);
    }

    /**
     * Get this GSS AP REQ Ticket as a byte array.
     *
     * @return the ticket bytes
     */
    public byte[] toByteArray() {
        byte[] bytes = new byte[ticketBytes.length];
        System.arraycopy(ticketBytes,0,bytes,0,ticketBytes.length);
        return bytes;
    }

    /**
     * Get the decrypted ticket (if any)
     *
     * @return the ticket if available (may be null)
     */
    public KerberosServiceTicket getServiceTicket() {
        return serviceTicket;
    }

    /**
     * Set the decrypted ticket.
     *
     * @param kerberosServiceTicket the ticket
     */
    public void setServiceTicket(KerberosServiceTicket kerberosServiceTicket) {
        this.serviceTicket = kerberosServiceTicket;
    }

    //- PACKAGE

    /**
     * Get the ticket body (the byte[] less gss header and token identifier)
     */
    InputStream getTicketBody() throws IOException, GSSException, KerberosException {
        // Check for NTLMSSP
        if (ArrayUtils.matchSubarrayOrPrefix(ticketBytes, 0, 1, NTLM_MESSAGE_PREFIX, 0) > -1) {
            throw new KerberosException("Client attempted to negotiate NTLM!");
        }

        // Skip any SPNEGO wrapper
        byte[] token = GSSSpnego.removeSpnegoWrapper(ticketBytes);
        InputStream is = new ByteArrayInputStream(token,0,token.length);

        // Skip header bytes and check mechanism
        GSSHeader header = new GSSHeader(is);
        if(!new Oid(header.getOid().toString()).equals(KerberosClient.getKerberos5Oid())) {
            throw new GSSException(GSSException.BAD_MECH, -1, "Expected Kerberos v5 mechanism '"
                    +header.getOid()+"'");
        }

        // Process the GSS token type
        int identifier = ((is.read() << 8) | is.read());
        if (identifier != 256) {
            throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1, "Incorrect token type '"
                    +identifier+"' (expected AP REQ)");
        }

        return is;
    }

    //- PRIVATE

    private static final byte[] NTLM_MESSAGE_PREFIX =  new byte[]{'N', 'T', 'L', 'M', 'S', 'S', 'P', 0};

    /**
     * The GSS wrapped request ticket / packet
     */
    private final byte[] ticketBytes;

    /**
     * The decrypted ticket
     */
    private KerberosServiceTicket serviceTicket;

}
