package com.l7tech.common.security.xml.processor;

import java.security.GeneralSecurityException;

import com.l7tech.common.security.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.common.security.kerberos.KerberosClient;
import com.l7tech.common.security.kerberos.KerberosServiceTicket;
import com.l7tech.common.security.kerberos.KerberosException;
import com.l7tech.common.security.token.KerberosSecurityToken;
import com.l7tech.common.security.token.SecurityTokenType;
import org.w3c.dom.Element;

/**
 * Security token implementation for Kerberos.
 *
 * <p>Created from a BST or reference to a BST used in a previous request.</p>
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosSecurityTokenImpl extends SigningSecurityTokenImpl implements KerberosSecurityToken {

    //- PUBLIC

    public KerberosSecurityTokenImpl(KerberosGSSAPReqTicket ticket, String wsuId) {
        super(null);
        this.ticket = ticket;
        this.wsuId = wsuId;
    }

    public KerberosSecurityTokenImpl(KerberosGSSAPReqTicket ticket, String wsuId, Element element) throws GeneralSecurityException {
        super(element);
        this.ticket = ticket;
        this.wsuId = wsuId;

        if(element!=null && ticket.getServiceTicket()==null) { // check element since we don't do auth for virtual token
            try {
                KerberosClient client = new KerberosClient();
                String spn;
                try {
                    spn = KerberosClient.getKerberosAcceptPrincipal(false);
                }
                catch(KerberosException ke) { // fallback to system property name
                    spn = KerberosClient.getGSSServiceName();
                }
                KerberosServiceTicket kerberosServiceTicket = client.getKerberosServiceTicket(spn, ticket);
                ticket.setServiceTicket(kerberosServiceTicket);
            }
            catch(KerberosException ke) {
                throw new GeneralSecurityException("Error processing Kerberos Binary Security Token.", ke);
            }
        }
    }

    public KerberosGSSAPReqTicket getTicket() {
        return ticket;
    }

    public String getElementId() {
        return wsuId;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_KERBEROS_BST;
    }

    //- PRIVATE

    private final KerberosGSSAPReqTicket ticket;
    private final String wsuId;
}
