/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.policy.assertion.credential.wss;

import com.l7tech.proxy.policy.assertion.ClientAssertion;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.SsgResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.util.SoapUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.StringReader;
import java.io.IOException;

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
public class ClientWssBasic extends ClientWssCredentialSource implements ClientAssertion {
    public ClientWssBasic( WssBasic data ) {
        this.data = data;
    }

    /**
     * Decorate the xml soap message with a WSS header containing the username and password.
     *
     * @param request
     * @return
     * @throws PolicyAssertionException
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        Document soapmsg = request.getSoapEnvelope();
        Element headerel = SoapUtil.getOrMakeHeader(soapmsg);

        // get the username and passwords
        String username = request.getSsg().getUsername();
        char[] password = request.getSsg().password();
        if (username == null || password == null || username.length() < 1) {
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.AUTH_REQUIRED;
        }

        Element secElement = null;
        // todo, handle case where the security element is already present in the header
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            String secElStr = "<wsse:Security xmlns:wsse=\"" + SECURITY_NAMESPACE + "\">\n<wsse:UsernameToken Id=\"MyID\">\n<wsse:Username>" + username + "</wsse:Username>\n<Password>" + new String(password) + "</Password>\n</wsse:UsernameToken>\n</wsse:Security>";
            Document doc2 = builder.parse(new InputSource(new StringReader(secElStr)));
            secElement = doc2.getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new PolicyAssertionException(e.getMessage(), e);
        } catch (IOException e) {
            throw new PolicyAssertionException(e.getMessage(), e);
        } catch (SAXException e) {
            throw new PolicyAssertionException(e.getMessage(), e);
        }

        secElement = (Element)soapmsg.importNode(secElement, true);
        headerel.appendChild(secElement);

        return AssertionStatus.NONE;
    }

    public AssertionStatus unDecorateReply(PendingRequest request, SsgResponse response) throws PolicyAssertionException {
        // no action on response
        return AssertionStatus.NONE;
    }

    protected static final String USERNAME_TOKEN_ELEMENT_NAME = "UsernameToken";
    protected WssBasic data;
}
