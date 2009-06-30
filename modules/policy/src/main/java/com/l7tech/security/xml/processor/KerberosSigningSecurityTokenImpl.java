package com.l7tech.security.xml.processor;

import java.security.GeneralSecurityException;
import java.net.InetAddress;

import com.l7tech.kerberos.KerberosGSSAPReqTicket;
import com.l7tech.kerberos.KerberosClient;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.security.token.KerberosSigningSecurityToken;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.DomUtils;
import com.l7tech.util.SoapConstants;
import com.l7tech.util.HexUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

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

    public KerberosSigningSecurityTokenImpl(KerberosServiceTicket ticket, Element element) {
        super(element);
        this.kerberosServiceTicket = ticket;
        this.wsuId = null;
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

    /**
     * Generate a KerberosSigningSecurityTokenImpl for the given info.
     *
     * <p>The <code>asElement</code> method will return a DOM suitable for use
     * with the STR-Transform.</p>
     *
     * @param domFactory The Document that will own the BST fragment.
     * @param kerberosServiceTicket The Kerberos Service Ticket for the BST
     * @param wssePrefix The namespace prefix for the WS-Security namespace
     * @param wssePrefix The namespace URI for the WS-Security namespace
     * @return A new X509SigningSecurityTokenImpl
     */
    public static KerberosSigningSecurityTokenImpl createBinarySecurityToken( final Document domFactory,
                                                                              final KerberosServiceTicket kerberosServiceTicket,
                                                                              final String wssePrefix,
                                                                              final String wsseNs ) {
        final Element bst;
        if (wssePrefix == null) {
            bst = domFactory.createElementNS(wsseNs, "BinarySecurityToken");
            bst.setAttributeNS( DomUtils.XMLNS_NS, "xmlns", wsseNs);
        } else {
            bst = domFactory.createElementNS(wsseNs, wssePrefix+":BinarySecurityToken");
            bst.setAttributeNS(DomUtils.XMLNS_NS, "xmlns:"+wssePrefix, wsseNs);
        }
        bst.setAttribute("ValueType", SoapConstants.VALUETYPE_X509);
        DomUtils.setTextContent(bst, HexUtils.encodeBase64(kerberosServiceTicket.getGSSAPReqTicket().toByteArray(), true));

        return new KerberosSigningSecurityTokenImpl(kerberosServiceTicket, bst);
    }


    //- PRIVATE

    private final KerberosServiceTicket kerberosServiceTicket;
    private final String wsuId;
}
