package com.l7tech.server.saml;

import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * A class acting as a SAML requester
 * @author emil
 * @version 5-Aug-2004
 */
public class Requester {
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String TEXT_XML = "text/xml";
    public static final String ENCODING = "UTF-8";
    private SoapResponseGenerator responseGenerator = new SoapResponseGenerator();

    private final String serviceUrl;
    HttpClient client = new HttpClient();

    public Requester(String serviceUrl) throws MalformedURLException {
        new URL(serviceUrl);
        this.serviceUrl = serviceUrl;
    }

    public ResponseDocument sendRequest(RequestDocument doc) throws IOException {
        PostMethod postMethod = new PostMethod(serviceUrl);
        postMethod.setRequestHeader(CONTENT_TYPE, TEXT_XML + "; charset=" + ENCODING.toLowerCase());
        postMethod.setRequestBody(doc.newInputStream(Utilities.xmlOptions()));
        int status = client.executeMethod(postMethod);
        if (status !=200) {
            throw new IOException(serviceUrl +" returns status "+status);
        }
        try {
            return responseGenerator.fromSoapInputStream(postMethod.getResponseBodyAsStream());
        } catch (XmlException e) {
            throw new CausedIOException(e);
        } catch (InvalidDocumentFormatException e) {
            throw new CausedIOException(e);
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }
    }
}
