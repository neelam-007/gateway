/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential.wss;

import com.l7tech.common.util.SAXParsingCompleteException;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerWssBasic implements ServerAssertion {
    public ServerWssBasic(WssBasic data) {
        _data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        LoginCredentials creds = null;
        try {
            creds = findCredentials(request, response);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "cannot find credentials", e);
            return AssertionStatus.FALSIFIED;
        } catch (CredentialFinderException e) {
            logger.log(Level.SEVERE, "cannot find credentials", e);
            return AssertionStatus.AUTH_REQUIRED;
        }
        if (creds == null) {
            response.setPolicyViolated(true);
            return AssertionStatus.FALSIFIED;
        }
        request.setPrincipalCredentials(creds);
        return AssertionStatus.NONE;
    }

    public LoginCredentials findCredentials( Request grequest, Response gresponse ) throws IOException, CredentialFinderException {
        XmlRequest request;
        if ( grequest instanceof XmlRequest )
            request = (XmlRequest)grequest;
        else
            throw new CredentialFinderException( "Only XML requests and responses are supported!", AssertionStatus.FALSIFIED );

        String xmlreq = null;
        try {
            xmlreq = request.getRequestXml();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception getting a xml request " + e.getMessage(), e);
            throw new CredentialFinderException("Exception getting the xml request " + e.getMessage(), e);
        }
        // in order to get WSS credentials, we need a soap document
        if (xmlreq == null || xmlreq.length() < 1) {
            logger.log(Level.WARNING, "No xml request provided to extract wss element from");
            return null;
        }

        // get xml parser
        // todo, should this be set in some sort of startup class somewhere ?
        System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        XMLReader reader = null;
        try {
            reader = XMLReaderFactory.createXMLReader();
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Exception getting an XMLReader " + e.getMessage(), e);
            throw new CredentialFinderException("Exception getting an XMLReader " + e.getMessage(), e);
        }
        WsseBasicSaxHandler handler = new WsseBasicSaxHandler();
        reader.setContentHandler(handler);

        // parse the request, look for SAXParsingCompleteException
        StringReader stringReader = new StringReader(xmlreq);
        try {
            reader.parse(new InputSource(stringReader));
        } catch (SAXParsingCompleteException e) {
            String passwd = handler.getParsedPassword();
            // make sure we got a password (avoid a NPE)
            if (passwd == null || passwd.length() < 1) {
                logger.log(Level.WARNING, "parsing completed but no password available.");
                return null;
            }
            // make sure we have a clear text passwd
            if (!handler.DEFAULT_PASSWORD_TYPE.equals(handler.getPasswdType())) {
                logger.warning("password is of wrong type: " + handler.getPasswdType());
                return null;
            }

            // return the whole thing
            // this is good, we got what we needed
            logger.fine("Found credentials for user " + handler.getParsedUsername());
            return new LoginCredentials( handler.getParsedUsername(), passwd.getBytes(), CredentialFormat.CLEARTEXT);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception parsing xml request " + e.getMessage(), e);
            throw new CredentialFinderException("Exception parsing xml request " + e.getMessage(), e);
        } catch (SAXException e) {
            logger.log(Level.SEVERE, "Exception parsing xml request " + e.getMessage(), e);
            throw new CredentialFinderException("Exception parsing xml request " + e.getMessage(), e);
        } finally {
            stringReader.close();
        }
        // note: the actual result is returned in the handling of the SAXParsingCompleteException
        logger.warning("no credentials found");
        return null;
    }


    /*public AssertionStatus checkCredentials(Request request, Response response) throws CredentialFinderException {
        // this is only called once we have credentials
        // there is nothing more to check here, if the creds were not in the right format,
        // the WssBasicCredentialFinder would not have returned credentials

        // (just to make sure)
        LoginCredentials pc = request.getPrincipalCredentials();
        // yes, we're good
        if (pc != null) return AssertionStatus.NONE;
        else return AssertionStatus.AUTH_REQUIRED;
    }*/

    protected WssBasic _data;
    private final Logger logger = Logger.getLogger(getClass().getName());
}
