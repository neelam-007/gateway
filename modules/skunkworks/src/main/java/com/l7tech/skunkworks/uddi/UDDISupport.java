package com.l7tech.skunkworks.uddi;

import com.l7tech.common.uddi.guddiv3.UDDISecurityPortType;
import com.l7tech.common.uddi.guddiv3.GetAuthToken;
import com.l7tech.common.uddi.guddiv3.DispositionReportFaultMessage;
import com.l7tech.common.uddi.guddiv3.UDDISecurity;
import com.l7tech.common.uddi.guddiv3.UDDIInquiryPortType;
import com.l7tech.common.uddi.guddiv3.UDDIInquiry;
import com.l7tech.common.uddi.guddiv3.UDDIPublicationPortType;
import com.l7tech.common.uddi.guddiv3.UDDIPublication;
import com.l7tech.common.uddi.guddiv3.UDDISubscriptionPortType;
import com.l7tech.common.uddi.guddiv3.UDDISubscription;
import com.l7tech.common.uddi.guddiv3.DispositionReport;
import com.l7tech.common.uddi.guddiv3.Result;
import com.l7tech.common.uddi.guddiv3.ErrInfo;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Binding;
import javax.xml.ws.handler.Handler;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.soap.SOAPException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import java.net.URL;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.InputStream;
import java.io.IOException;

/**
 *
 */
public abstract class UDDISupport {

    //- PROTECTED

    UDDISupport( final Logger logger ) {
        this.logger = logger;
    }

    abstract protected String getUsername();
    abstract protected String getPassword();
    abstract protected String getInquiryUrl();
    abstract protected String getPublishingUrl();
    abstract protected String getSubscriptionUrl();
    abstract protected String getSecurityUrl();

    protected String authToken() throws Exception {
        login = getUsername();
        String password = getPassword();

        if (authToken == null && (login!=null && login.trim().length()>0)) {
            authToken = getAuthToken(login.trim(), password);
        }

        return authToken;
    }

    private UDDISecurityPortType getSecurityPort() {
        UDDISecurity security = new UDDISecurity(buildUrl("resources/uddi_v3_service_s.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDISecurity"));
        UDDISecurityPortType securityPort = security.getUDDISecurityPort();
        stubConfig(securityPort, getSecurityUrl());
        return securityPort;
    }

    protected UDDIInquiryPortType getInquirePort() {
        UDDIInquiry inquiry = new UDDIInquiry(buildUrl("resources/uddi_v3_service_i.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDIInquiry"));
        UDDIInquiryPortType inquiryPort = inquiry.getUDDIInquiryPort();
        stubConfig(inquiryPort, getInquiryUrl());
        return inquiryPort;
    }

    protected UDDIPublicationPortType getPublishPort() {
        UDDIPublication publication = new UDDIPublication(buildUrl("resources/uddi_v3_service_p.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDIPublication"));
        UDDIPublicationPortType publicationPort = publication.getUDDIPublicationPort();
        stubConfig(publicationPort, getPublishingUrl());
        return publicationPort;
    }

    protected UDDISubscriptionPortType getSubscriptionPort() {
        UDDISubscription subscription = new UDDISubscription(buildUrl("resources/uddi_v3_service_sub.wsdl"), new QName(UDDIV3_NAMESPACE, "UDDISubscription"));
        UDDISubscriptionPortType subscriptionPort = subscription.getUDDISubscriptionPort();
        stubConfig(subscriptionPort, getSubscriptionUrl());
        return subscriptionPort;
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    protected Exception buildFaultException(final String contextMessage,
                                          final DispositionReportFaultMessage faultMessage) {
        Exception exception;

        if ( hasResult(faultMessage, 10150) ) {
            exception = new Exception("Authentication failed for '" + login + "'.");
        } else if ( hasResult(faultMessage, 10140) ||
                    hasResult(faultMessage, 10120)) {
                exception = new Exception("Authorization failed for '" + login + "'.");
        } else if ( hasResult(faultMessage, 10110)) {
                exception = new Exception("Session expired or invalid.");
        } else if ( hasResult(faultMessage, 10400)) {
                exception = new Exception("UDDI registry is too busy.");
        } else if ( hasResult(faultMessage, 10040)) {
                exception = new Exception("UDDI registry version mismatch.");
        } else if ( hasResult(faultMessage, 10050)) {
                exception = new Exception("UDDI registry does not support a required feature.");
        } else {
            // handle general exception
            exception = new Exception(contextMessage + toString(faultMessage));
        }

        return exception;
    }

    //- PRIVATE

    private static final String UDDIV3_NAMESPACE = "urn:uddi-org:api_v3_service";

    final Logger logger;
    private String login;
    private String authToken;

    private String getAuthToken(final String login,
                                final String password) throws Exception {
        String authToken = null;

        if ( login != null && login.length() > 0 ) {
            try {
                UDDISecurityPortType securityPort = getSecurityPort();
                GetAuthToken getAuthToken = new GetAuthToken();
                getAuthToken.setUserID(login);
                getAuthToken.setCred(password);
                authToken = securityPort.getAuthToken(getAuthToken).getAuthInfo();
            } catch (DispositionReportFaultMessage drfm) {
                throw buildFaultException("Error getting authentication token: ", drfm);
            } catch (RuntimeException e) {
                throw new Exception("Error getting authentication token.", e);
            }
        }

        return authToken;
    }

    private String toString(DispositionReportFaultMessage dispositionReport) {
        StringBuffer buffer = new StringBuffer(512);

        DispositionReport report = dispositionReport.getFaultInfo();
        if ( report != null ) {
            for ( Result result : report.getResult()) {
                buffer.append("errno:");
                buffer.append(result.getErrno());
                ErrInfo info = result.getErrInfo();
                buffer.append("/errcode:");
                buffer.append(info.getErrCode());
                buffer.append("/description:");
                buffer.append(info.getValue());
            }
        }

        return buffer.toString();
    }

    private boolean hasResult(DispositionReportFaultMessage faultMessage, int errorCode) {
        boolean foundResult = false;

        DispositionReport report = faultMessage.getFaultInfo();
        if ( report != null ) {
            for (Result result : report.getResult()) {
                if ( result.getErrno() == errorCode ) {
                    foundResult = true;
                    break;
                }
            }
        }

        return foundResult;
    }

    private URL buildUrl(String relativeUrl) {
        return UDDIInquiry.class.getResource(relativeUrl);
    }

    private void stubConfig(Object proxy, String url) {
        BindingProvider bindingProvider = (BindingProvider) proxy;
        Binding binding = bindingProvider.getBinding();
        Map<String,Object> context = bindingProvider.getRequestContext();
        List<Handler> handlerChain = new ArrayList<Handler>();

        // Add handler to fix any issues with invalid faults
        handlerChain.add(new SOAPHandler<SOAPMessageContext>(){
            @Override
            public Set<QName> getHeaders() {
                return null;
            }

            @Override
            public boolean handleMessage(SOAPMessageContext context) {
                SOAPMessage soapMessage = context.getMessage();

                if ( soapMessage != null ) {
                    try {
                        SOAPPart soapPart = soapMessage.getSOAPPart();

                        Source source = soapPart.getContent();

                        if (source instanceof StreamSource ) {
                            StreamSource streamSource = (StreamSource) source;

                            InputStream in = null;
                            try {
                                in = streamSource.getInputStream();
                                IOUtils.copyStream(in, System.out);
                            } finally {
                                ResourceUtils.closeQuietly(in);
                            }
                        } else if (source instanceof DOMSource ) {
                            XmlUtil.nodeToFormattedOutputStream(((DOMSource)source).getNode(), System.out);
                        }
                    } catch (SOAPException se) {
                        logger.log( Level.INFO,
                                "Error processing SOAP message when checking namespaces: " + ExceptionUtils.getMessage(se),
                                ExceptionUtils.getDebugException(se));
                    } catch (IOException ioe) {
                        logger.log(Level.INFO,
                                "Error processing SOAP message when checking namespaces: " + ExceptionUtils.getMessage(ioe),
                                ExceptionUtils.getDebugException(ioe));
                    }
                }


                return true;
            }

            @Override
            public boolean handleFault(SOAPMessageContext context) {
                return true;
            }

            @Override
            public void close( MessageContext context) {
            }
        });

        // Set handlers
        binding.setHandlerChain(handlerChain);

        // Set endpoint
        context.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, url);
    }

}
