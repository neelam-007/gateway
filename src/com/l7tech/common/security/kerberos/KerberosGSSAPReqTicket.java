package com.l7tech.common.security.kerberos;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import sun.security.jgss.GSSHeader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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

    public KerberosServiceTicket getServiceTicket() {
        return serviceTicket;
    }

    public void setServiceTicket(KerberosServiceTicket kerberosServiceTicket) {
        this.serviceTicket = kerberosServiceTicket;
    }

    //- PACKAGE

    /**
     * Get the ticket body (the byte[] less gss header and token identifier)
     */
    InputStream getTicketBody() throws IOException, GSSException, KerberosException {
        InputStream is = new ByteArrayInputStream(ticketBytes,0,ticketBytes.length);
        GSSHeader header = new GSSHeader(is); // skip header bytes
        if(!new Oid(header.getOid().toString()).equals(KerberosClient.getKerberos5Oid())) throw new GSSException(GSSException.BAD_MECH, -1, "Expected Kerberos v5 mechanism '"+header.getOid()+"'");
        int identifier = ((is.read() << 8) | is.read());
        if (identifier != 256) throw new GSSException(GSSException.DEFECTIVE_TOKEN, -1, "Incorrect token type '"+identifier+"' (expected AP REQ)");
        return is;
    }

    //- PRIVATE

    private final byte[] ticketBytes;
    private KerberosServiceTicket serviceTicket;

}
