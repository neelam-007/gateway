package com.l7tech.server.saml;

import com.l7tech.common.security.saml.Constants;
import org.apache.xmlbeans.XmlOptions;
import x0Assertion.oasisNamesTcSAML1.*;
import x0Protocol.oasisNamesTcSAML1.RequestDocument;
import x0Protocol.oasisNamesTcSAML1.RequestType;
import x0Protocol.oasisNamesTcSAML1.ResponseDocument;
import x0Protocol.oasisNamesTcSAML1.ResponseType;

import java.util.Calendar;

/**
 * @author emil
 * @version 28-Jul-2004
 */
public class AuthenticationQueryHandler implements SamlRequestHandler {
    private RequestDocument request;
    private ResponseDocument response;

    /**
     * Package private constructor
     * @param request
     */
    AuthenticationQueryHandler(RequestDocument request) {
        if (request == null) {
            throw new IllegalArgumentException();
        }
        RequestType rt = request.getRequest();
        if (rt == null) {
            throw new CannotProcessException("missing samlp:Request", request);
        }
        if (!rt.isSetAuthenticationQuery()) {
            throw new IllegalArgumentException("Saml request does not contain "+request);
        }
        this.request = request;
    }

    /**
     * @return the saml request that is processed by the handler
     */
    public RequestDocument getRequest() {
        return request;
    }

    /**
     * @return the saml response
     */
    public ResponseDocument getResponse() {
        if (response !=null) {
            return response;
        }
        final XmlOptions xmlOptions = Responses.options();
        response =  ResponseDocument.Factory.newInstance(xmlOptions);
        ResponseType responseType = ResponseType.Factory.newInstance(xmlOptions);
//        StatusType status = StatusType.Factory.newInstance(xmlOptions);
//        StatusCodeType statusCode = StatusCodeType.Factory.newInstance(xmlOptions);
//        statusCode.setValue(new QName(Constants.NS_SAMLP, Constants.STATUS_SUCCESS));
//        status.setStatusCode(statusCode);
//        responseType.setStatus(status);

        AssertionType assertion = responseType.addNewAssertion();
        AuthenticationStatementType authStatement = assertion.addNewAuthenticationStatement();
        authStatement.setAuthenticationInstant(Calendar.getInstance());
        authStatement.setAuthenticationMethod(Constants.PASSWORD_AUTHENTICATION);
        SubjectType subjectType = authStatement.addNewSubject();
        NameIdentifierType nameIdentifier = subjectType.addNewNameIdentifier();
        nameIdentifier.setFormat(Constants.NAMEIDENTIFIER_X509_SUBJECT);
        final String subjectName = getRequestNameIdentifierValue();
        nameIdentifier.setStringValue(subjectName);
        SubjectConfirmationType subjectConfirmation = subjectType.addNewSubjectConfirmation();
        subjectConfirmation.addConfirmationMethod(Constants.CONFIRMATION_HOLDER_OF_KEY);

        response.setResponse(responseType);
        return response;
    }

    private String getRequestNameIdentifierValue() {
        return request.getRequest().getAuthenticationQuery().getSubject().getNameIdentifier().getStringValue();
    }
}
