/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.proxy.datamodel.exceptions.OperationCanceledException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;

/**
 * decorates a request with a header that looks like that:
 * <wsse:Security xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/04/secext">
 *           <wsse:UsernameToken Id="MyID">
 *                   <wsse:Username>Zoe</wsse:Username>
 *                   <Password>ILoveLlamas</Password>
 *           </wsse:UsernameToken>
 * </wsse:Security>
 * @author flascell
 * @version $Revision$
 */
public class ClientWssBasic extends ClientWssCredentialSource {
    public ClientWssBasic( WssBasic data ) {
        this.data = data;
    }

    /**
     * Decorate the xml soap message with a WSS header containing the username and password.
     *
     * @param request
     * @return
     */
    public AssertionStatus decorateRequest(PendingRequest request)
            throws OperationCanceledException, IOException, SAXException
    {
        Document soapmsg = request.getSoapEnvelope();
        Element headerel = SoapUtil.getOrMakeHeader(soapmsg);

        // get the username and passwords
        String username = request.getUsername();
        char[] password = request.getPassword();

        Element secElement = null;
        // todo, handle case where the security element is already present in the header
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            String secElStr = "<wsse:Security xmlns:wsse=\"" + SECURITY_NAMESPACE + "\">\n<wsse:UsernameToken Id=\"MyID\">\n<wsse:Username>" + username + "</wsse:Username>\n<Password>" + new String(password) + "</Password>\n</wsse:UsernameToken>\n</wsse:Security>";
            Document doc2 = builder.parse(new InputSource(new StringReader(secElStr)));
            secElement = doc2.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage(), e);  // can't happen
        }

        secElement = (Element)soapmsg.importNode(secElement, true);
        headerel.appendChild(secElement);

        request.setSoapEnvelope(soapmsg);
        if (!request.getClientSidePolicy().isPlaintextAuthAllowed())
            request.setSslRequired(true); // force SSL when using WSS basic
        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) {
        // no action on response
        return AssertionStatus.NONE;
    }

    /**
     * @return the human-readable node name that is displayed.
     */
    public String getName() {
        return "Require WS Token Basic Authentication";
    }

    /**
     * subclasses override this method specifying the resource name of the
     * icon to use when this assertion is displayed in the tree view.
     *
     * @param open for nodes that can be opened, can have children
     * @return a string such as "com/l7tech/proxy/resources/tree/assertion.png"
     */
    public String iconResource(boolean open) {
        return "com/l7tech/proxy/resources/tree/authentication.gif";
    }

    protected static final String USERNAME_TOKEN_ELEMENT_NAME = "UsernameToken";
    protected WssBasic data;
}
