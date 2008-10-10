package com.l7tech.kerberos;

import javax.security.auth.kerberos.KerberosTicket;

/**
 * Represents a kerberos service ticket.
 *
 * <p>This is part of the information in the AP REQ and is only created
 * if the clients authenticator is validated.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public final class KerberosServiceTicket {

    //- PUBLIC

    /**
     * Create a new KerberosServiceTicket
     *
     * @param client the client principal name
     * @param service the service principal name
     * @param key the sub or session key
     * @param ticket the ticket bytes
     */
    public KerberosServiceTicket(String client, String service, byte[] key, long expiry, KerberosGSSAPReqTicket ticket) {
        this(client, service, key, expiry, ticket, null);
    }

    /**
     * Create a new KerberosServiceTicket
     *
     * @param client the client principal name
     * @param service the service principal name
     * @param key the sub or session key
     * @param ticket the ticket bytes
     * @param delegatedKerberosTicket the delegated ticket (may be null)
     */
    public KerberosServiceTicket(String client, String service, byte[] key, long expiry, KerberosGSSAPReqTicket ticket, KerberosTicket delegatedKerberosTicket) {
        clientPrincipalName = client;
        servicePrincipalName = service;
        sessionOrSubKey = key;
        expires = expiry;
        gssApReqTicket = ticket;
        this.delegatedKerberosTicket = delegatedKerberosTicket;
    }

    /**
     * Get the name of the client.
     *
     * @return the principal name
     */
    public String getClientPrincipalName() {
        return clientPrincipalName;
    }

    /**
     * Get the name of the service.
     *
     * @return the principal name
     */
    public String getServicePrincipalName() {
        return servicePrincipalName;
    }

    /**
     * This is the sub key or session key used for data integrity / confidentiality.
     *
     * @return the key
     */
    public byte[] getKey() {
        return sessionOrSubKey;
    }

    /**
     * Get the expiry time for this ticket.
     *
     * @return the time.
     */
    public long getExpiry() {
        return expires;
    }

    /**
     * Get the kerberos AP REQ GSS ticket for this principal for the client/service.
     *
     * @return the ticket
     */
    public KerberosGSSAPReqTicket getGSSAPReqTicket() {
        return gssApReqTicket;
    }

    /**
     * Get the delegated KerberosTicket.
     *
     * @return The ticket or null if not set
     */
    public KerberosTicket getDelegatedKerberosTicket() {
        return delegatedKerberosTicket;
    }

    /**
     * Create a string representation of this ticket.
     */
    public String toString() {
        return "KerberosServiceTicket[client='"+clientPrincipalName+"'; service='"+servicePrincipalName+"'];";
    }

    //- PRIVATE

    private final String clientPrincipalName;
    private final String servicePrincipalName;
    private final byte[] sessionOrSubKey;
    private final long expires;
    private final KerberosGSSAPReqTicket gssApReqTicket;
    private final KerberosTicket delegatedKerberosTicket;
}
