package com.l7tech.security.xml.processor;

import java.security.GeneralSecurityException;
import java.net.InetAddress;

import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import org.w3c.dom.Element;

/**
 * Security token implementation for Kerberos.
 *
 * <p>Created from a BST or reference to a BST used in a previous request.</p>
 */
public class KerberosSigningSecurityTokenImpl extends SigningSecurityTokenImpl implements KerberosSigningSecurityToken {

    //- PUBLIC

    public KerberosSigningSecurityTokenImpl(KerberosServiceTicket ticket, String wsuId) {
        super(null);
        this.kerberosServiceTicket = ticket;
        this.wsuId = wsuId;
    }

    public KerberosSigningSecurityTokenImpl( final KerberosGSSAPReqTicket ticket,
                                             final InetAddress clientAddress,
                                             final String wsuId,
                                             final Element element ) throws GeneralSecurityException {
        super(element);
        this.wsuId = wsuId;

        try {
            KerberosClient client = new KerberosClient();
            String spn;
            try {
                spn = KerberosClient.getKerberosAcceptPrincipal(false);
            }
            catch(KerberosException ke) { // fallback to system property name
                spn = KerberosClient.getGSSServiceName();
            }
            kerberosServiceTicket = client.getKerberosServiceTicket(spn, clientAddress, ticket);
        }
        catch(KerberosException ke) {
            throw new GeneralSecurityException("Error processing Kerberos Binary Security Token.", ke);
        }
    }

    @Override
    public KerberosServiceTicket getServiceTicket() {
        return kerberosServiceTicket;
    }

    @Override
    public KerberosGSSAPReqTicket getTicket() {
        return kerberosServiceTicket.getGSSAPReqTicket();
    }

    @Override
    public String getElementId() {
        return wsuId;
    }

    @Override
    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_KERBEROS_BST;
    }

    //- PRIVATE

    private final KerberosServiceTicket kerberosServiceTicket;
    private final String wsuId;
}
