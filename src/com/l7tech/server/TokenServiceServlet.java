package com.l7tech.server;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.secureconversation.SecureConversationSession;
import com.l7tech.server.secureconversation.SecureConversationTokenService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 * The servlet handling WS Trust RequestSecurityToken requests.
 * The SSA requests such a token when it desires to establish a Secure Conversation.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Aug 5, 2004<br/>
 * $Id$<br/>
 */
public class TokenServiceServlet extends HttpServlet {
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        dbf = DocumentBuilderFactory.newInstance();
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        throw new ServletException("Method not supported; context requests must use POST");
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        Document payload = null;
        try {
            payload = extractXMLPayload(req);
        } catch (ParserConfigurationException e) {
            // todo, some error
        } catch (SAXException e) {
            // todo, some error
        }
        SecureConversationTokenService tokenService = new SecureConversationTokenService();
        SecureConversationSession session = tokenService.getNewContext(payload);
        Document responseDoc = tokenService.sessionToRequestSecurityTokenResponse(session);
        outputRequestSecurityTokenResponse(responseDoc, res);
    }

    private void outputRequestSecurityTokenResponse(Document requestSecurityTokenResponse,
                                                    HttpServletResponse res) {
        // todo, send back the RequestSecurityTokenResponse to the requestor
    }



    private Document extractXMLPayload(HttpServletRequest req)
            throws IOException, ParserConfigurationException, SAXException {
        BufferedReader reader = new BufferedReader(req.getReader());
        DocumentBuilder parser = getDomParser();
        return parser.parse( new InputSource(reader));

    }

    private DocumentBuilder getDomParser() throws ParserConfigurationException {
        DocumentBuilder builder = dbf.newDocumentBuilder();
        builder.setEntityResolver(XmlUtil.getSafeEntityResolver());
        return builder;
    }

    private final Logger logger = Logger.getLogger(getClass().getName());
    private DocumentBuilderFactory dbf;

}
