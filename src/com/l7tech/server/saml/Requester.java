package com.l7tech.server.saml;

import com.l7tech.common.security.saml.Constants;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.xmlbeans.XmlException;
import org.xml.sax.SAXException;
import x0Assertion.oasisNamesTcSAML1.NameIdentifierType;
import x0Assertion.oasisNamesTcSAML1.SubjectType;
import x0Protocol.oasisNamesTcSAML1.AuthenticationQueryType;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.RequestType;
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

    /**
     * Returns the authentication statements for a subject name, optionally specifying
     * the authentication method filter.
     * @param subjectName
     * @param authMethod
     * @return the response document containing the authentication statements for the
     *         given subjectand authenticatin method
     *
     * @throws SamlException wrapping the cause or
     * @throws IllegalArgumentException if the subject name is null
     */
    public ResponseDocument getAuthenticationStatements(String subjectName, String authMethod)
      throws SamlException, IllegalArgumentException {
        if (subjectName == null) {
            throw new IllegalArgumentException();
        }
        RequestDocument rdoc = RequestDocument.Factory.newInstance();
        RequestType rt = rdoc.addNewRequest();
        AuthenticationQueryType at = rt.addNewAuthenticationQuery();
        if (authMethod == null) {
            at.setAuthenticationMethod(authMethod);
        }
        SubjectType subject = at.addNewSubject();
        NameIdentifierType nameIdentifier = subject.addNewNameIdentifier();
        nameIdentifier.setFormat(Constants.NAMEIDENTIFIER_UNSPECIFIED);
        nameIdentifier.setStringValue(subjectName);

        return sendRequest(rdoc);

    }

    /**
     * General method for SAML protocol request/response exchange with the saml authorty.
     * @param doc the request document
     * @return  the response document
     * @throws SamlException on error
     */
    public ResponseDocument sendRequest(RequestDocument doc) throws SamlException {
        PostMethod postMethod = new PostMethod(serviceUrl);
        postMethod.setRequestHeader(CONTENT_TYPE, TEXT_XML + "; charset=" + ENCODING.toLowerCase());
        postMethod.setRequestBody(doc.newInputStream(Utilities.xmlOptions()));
        try {
            int status = client.executeMethod(postMethod);
            if (status !=200) {
                throw new SamlException(serviceUrl +" returns status "+status);
            }
            return responseGenerator.fromSoapInputStream(postMethod.getResponseBodyAsStream());
        } catch (XmlException e) {
            throw new SamlException(e);
        } catch (InvalidDocumentFormatException e) {
            throw new SamlException(e);
        } catch (SAXException e) {
            throw new SamlException(e);
        } catch (IOException e) {
            throw new SamlException(e);
        }
    }
}
