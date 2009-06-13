package com.l7tech.security.xml.processor;

import java.security.GeneralSecurityException;

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
 *
 * @author $Author$
 * @version $Revision$
 */
public class KerberosSecurityTokenImpl extends SigningSecurityTokenImpl implements KerberosSigningSecurityToken {

    //- PUBLIC

    public KerberosSecurityTokenImpl(KerberosServiceTicket ticket, String wsuId) {
        super(null);
        this.kerberosServiceTicket = ticket;
        this.wsuId = wsuId;
    }

    public KerberosSecurityTokenImpl(KerberosGSSAPReqTicket ticket, String wsuId, Element element) throws GeneralSecurityException {
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
            kerberosServiceTicket = client.getKerberosServiceTicket(spn, ticket);
        }
        catch(KerberosException ke) {
            throw new GeneralSecurityException("Error processing Kerberos Binary Security Token.", ke);
        }
    }

    public KerberosServiceTicket getServiceTicket() {
        return kerberosServiceTicket;
    }

    public KerberosGSSAPReqTicket getTicket() {
        return kerberosServiceTicket.getGSSAPReqTicket();
    }

    public String getElementId() {
        return wsuId;
    }

    public SecurityTokenType getType() {
        return SecurityTokenType.WSS_KERBEROS_BST;
    }

    //- PRIVATE

    private final KerberosServiceTicket kerberosServiceTicket;
    private final String wsuId;
}
