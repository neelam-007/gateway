/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.credential.wss;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.CredentialFormat;
import com.l7tech.message.Request;
import com.l7tech.util.SAXParsingCompleteException;
import com.l7tech.logging.LogManager;
import com.l7tech.identity.User;
import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.XMLReaderFactory;

import java.util.logging.Level;
import java.io.IOException;
import java.io.StringReader;

/**
 */
public class WssBasicCredentialFinder extends WssCredentialFinder {
    public PrincipalCredentials findCredentials(Request request) throws CredentialFinderException {
        String xmlreq = null;
        try {
            xmlreq = request.getRequestXml();
        } catch (IOException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception getting a xml request " + e.getMessage(), e);
            throw new CredentialFinderException("Exception getting the xml request " + e.getMessage(), e);
        }
        // in order to get WSS credentials, we need a soap document
        if (xmlreq == null || xmlreq.length() < 1) {
            LogManager.getInstance().getSystemLogger().log(Level.WARNING, "No xml request provided to extract wss element from");
            return null;
        }

        // get xml parser
        // todo, should this be set in some sort of startup class somewhere ?
        System.setProperty("org.xml.sax.driver", "org.apache.xerces.parsers.SAXParser");
        XMLReader reader = null;
        try {
            reader = XMLReaderFactory.createXMLReader();
        } catch (SAXException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception getting an XMLReader " + e.getMessage(), e);
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
                LogManager.getInstance().getSystemLogger().log(Level.WARNING, "parsing completed but no password available.");
                return null;
            }
            // make sure we have a clear text passwd
            if (!handler.DEFAULT_PASSWORD_TYPE.equals(handler.getPasswdType())) return null;

            // return the whole thing
            // this is good, we got what we needed
            User u = new User();
            u.setLogin(handler.getParsedUsername());
            return new PrincipalCredentials(u, passwd.getBytes(), CredentialFormat.CLEARTEXT);
        } catch (IOException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception parsing xml request " + e.getMessage(), e);
            throw new CredentialFinderException("Exception parsing xml request " + e.getMessage(), e);
        } catch (SAXException e) {
            LogManager.getInstance().getSystemLogger().log(Level.SEVERE, "Exception parsing xml request " + e.getMessage(), e);
            throw new CredentialFinderException("Exception parsing xml request " + e.getMessage(), e);
        } finally {
            stringReader.close();
        }
        // note: the actual result is returned in the handling of the SAXParsingCompleteException
        return null;
    }
}
