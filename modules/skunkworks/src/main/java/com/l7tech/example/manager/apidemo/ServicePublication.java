/**
 * Copyright (C) 2006-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.example.manager.apidemo;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gateway.common.service.ServiceHeader;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is a sample demonstrating operations on Published Service
 * on a SecureSpan Gateway.
 */
public class ServicePublication {
    private SsgAdminSession session;
    private static final Logger logger = Logger.getLogger(ServicePublication.class.getName());

    public static void main(String[] args) throws Exception {
        final String[] fargs = args;
        Subject.doAsPrivileged(new Subject(), new PrivilegedAction() {
            public Object run() {
                try {
                    ServicePublication.run(fargs);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "exception running Identity Provider", e);
                }
                return null;
            }
        }, null);
    }

    public static void run(String[] args) throws Exception {
        try {
            ServicePublication me = new ServicePublication(Main.SSGHOST, Main.ADMINACCOUNT_NAME, Main.ADMINACCOUNT_PASSWD);
            String[] res = me.listPublishedServices();
            logger.info("Published services found (" + res.length + ")");
            for (String line : res) {
                logger.info(line);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "ERROR while listing published service", e);
        }
    }

    /**
     * Use this demo functionality using its own internal admin session.
     * @param ssghost SecureSpan Host name to connect to
     * @param login the admin username to use
     * @param passwd the admin password to use
     */
    public ServicePublication(String ssghost, String login, String passwd) throws MalformedURLException, LoginException, RemoteException {
        session = new SsgAdminSession(ssghost, login, passwd);
    }

    /**
     * Use this demo functionality using a pre-established admin session.
     * @param session pre-established session
     */
    public ServicePublication(SsgAdminSession session) {
        this.session = session;
    }

    /**
     * Publish a Sample Web Service on a SecureSpan Gateway. Note that a PublishedService cannot be
     * published twice on the same cluster with exactly the same parameters.
     * @return the object id of the newly published service
     */
    public Goid publishSampleService() throws SaveException, PolicyAssertionException, RemoteException, VersionException, UpdateException, FindException {
        return publishService("sendSms", SAMPLE_POLICY_XML, SAMPLE_SERVICE_WSDL);
    }

    /**
     * Publish a Web Service on a SecureSpan Gateway. Note that a PublishedService cannot be
     * published twice on the same cluster with exactly the same parameters.
     * @param name name for the PS
     * @param policyXML a policy xml to apply to this service
     * @param wsdlXML the wsdl for this web service
     * @return the object id of the newly published service
     */
    public Goid publishService(final String name, final String policyXML, final String wsdlXML) throws SaveException, PolicyAssertionException, RemoteException, VersionException, UpdateException, FindException {
        ServiceAdmin serviceAdmin = session.getServiceAdmin();
        PublishedService newService = new PublishedService();
        newService.setName(name);
        newService.setPolicy(new Policy(PolicyType.PRIVATE_SERVICE, null, policyXML, true));
        newService.setWsdlXml(wsdlXML);
        newService.setFolder(getRootFolder());
        return serviceAdmin.savePublishedService(newService);
    }    

    private Folder getRootFolder() throws FindException {
        return session.getFolderAdmin().findByPrimaryKey(new Goid(0,-5002L));
    }

    /**
     * Un publish (delete) a PublishedService previously published on a SecureSpan Gateway.
     * @param serviceOID the object id of the previously published service
     * @return true if deleted succesfully
     */
    public boolean unPublishService(final long serviceOID) throws DeleteException, RemoteException {
        ServiceAdmin serviceAdmin = session.getServiceAdmin();
        serviceAdmin.deletePublishedService(Long.toString(serviceOID));
        return true;
    }

    /**
     * rename a PublishedService previously published on a SecureSpan Gateway.
     * @param serviceOID the object id of the previously published service
     * @param newName the new desired name
     * @return true if renamed ok
     */
    public boolean renamePublishedService(final long serviceOID, final String newName) throws RemoteException, FindException, SaveException, PolicyAssertionException, VersionException, UpdateException {
        ServiceAdmin serviceAdmin = session.getServiceAdmin();
        PublishedService existingService = serviceAdmin.findServiceByID(Long.toString(serviceOID));
        if (existingService != null) {
            existingService.setName(newName);
            serviceAdmin.savePublishedService(existingService);
            return true;
        } else {
            logger.info("the service " + serviceOID + " does not seem to exist");
            return false;
        }

    }

    /**
     * Enable or disabled a PublishedService previously published on a SecureSpan Gateway.
     * @param serviceOID the object id of the previously published service
     * @param enabled whether it should be enabled or disabled
     * @return true if change ok
     */
    public boolean enablePublishedService(final long serviceOID, final boolean enabled) throws RemoteException, FindException, SaveException, PolicyAssertionException, VersionException, UpdateException {
        ServiceAdmin serviceAdmin = session.getServiceAdmin();
        PublishedService existingService = serviceAdmin.findServiceByID(Long.toString(serviceOID));
        if (existingService != null) {
            existingService.setDisabled(!enabled);
            serviceAdmin.savePublishedService(existingService);
            return Boolean.TRUE;
        } else {
            logger.info("the service " + serviceOID + " does not seem to exist");
            return Boolean.FALSE;
        }

    }

    /**
     * Get a list of already published services on a SecureSpan Gateway
     * @return an array of string (one for each service). each string describes one published service
     */
    public String[] listPublishedServices() throws RemoteException, FindException {
        ServiceAdmin serviceAdmin = session.getServiceAdmin();
        com.l7tech.gateway.common.service.ServiceHeader[] res = serviceAdmin.findAllPublishedServices();
        String[] output = new String[res.length];
        for (int i = 0; i < res.length; i++) {
            ServiceHeader header = res[i];
            PublishedService ps = serviceAdmin.findServiceByID(Long.toString(header.getOid()));
            output[i] = "Service Name: " + header.getName() + " Service ID: " + header.getOid() + "\n\n" + ps.getPolicy().getXml() + "\n\n";
            
        }
        return output;
    }

    private static final String SAMPLE_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <wsse:SecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:TokenType>http://schemas.xmlsoap.org/ws/2004/04/security/sc/sct</wsse:TokenType>\n" +
            "        </wsse:SecurityToken>\n" +
            "        <wsse:Integrity wsp:Usage=\"wsp:Required\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n" +
            "            <wsse:MessageParts\n" +
            "                Dialect=\"http://www.w3.org/TR/1999/REC-xpath-19991116\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">/soapenv:Envelope/soapenv:Body</wsse:MessageParts>\n" +
            "        </wsse:Integrity>\n" +
            "        <L7p:SpecificUser>\n" +
            "            <L7p:UserLogin stringValue=\"quincy\"/>\n" +
            "            <L7p:IdentityProviderOid longValue=\"-2\"/>\n" +
            "            <L7p:UserUid stringValue=\"2457601\"/>\n" +
            "            <L7p:UserName stringValue=\"quincy\"/>\n" +
            "        </L7p:SpecificUser>\n" +
            "        <L7p:ThroughputQuota>\n" +
            "            <L7p:Quota longValue=\"50\"/>\n" +
            "            <L7p:CounterName stringValue=\"sms\"/>\n" +
            "            <L7p:TimeUnit intValue=\"2\"/>\n" +
            "        </L7p:ThroughputQuota>\n" +
            "        <L7p:HttpRoutingAssertion>\n" +
            "            <L7p:ProtectedServiceUrl stringValue=\"http://smsgateway/svc/sendsms\"/>\n" +
            "        </L7p:HttpRoutingAssertion>\n" +
            "        <L7p:ResponseWssIntegrity/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n" +
            "";
    private static final String SAMPLE_SERVICE_WSDL = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsdl:definitions targetNamespace=\"http://www.csapi.org/schema/parlayx/sms/send/v1_0/local\" xmlns:apachesoap=\"http://xml.apache.org/xml-soap\" xmlns:impl=\"http://www.csapi.org/schema/parlayx/sms/send/v1_0/local\" xmlns:intf=\"http://www.csapi.org/schema/parlayx/sms/send/v1_0/local\" xmlns:tns1=\"http://www.csapi.org/schema/parlayx/common/v1_0\" xmlns:tns2=\"http://www.csapi.org/schema/parlayx/sms/v1_0\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "<!--WSDL created by Apache Axis version: 1.2RC3\n" +
            "Built on Feb 28, 2005 (10:15:14 EST)-->\n" +
            " <wsdl:types>\n" +
            "  <schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.csapi.org/schema/parlayx/sms/send/v1_0/local\" xmlns=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "   <import namespace=\"http://www.csapi.org/schema/parlayx/sms/v1_0\"/>\n" +
            "   <import namespace=\"http://www.csapi.org/schema/parlayx/common/v1_0\"/>\n" +
            "   <element name=\"sendSms\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"sendSms&gt;destAddressSet\" type=\"tns1:ArrayOfEndUserIdentifier\"/>\n" +
            "      <element name=\"sendSms&gt;senderName\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSms&gt;charging\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSms&gt;message\" type=\"xsd:string\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "   <element name=\"sendSmsResponse\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"result\" type=\"xsd:string\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "   <element name=\"sendSmsLogo\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"sendSmsLogo&gt;destAddressSet\" type=\"tns1:ArrayOfEndUserIdentifier\"/>\n" +
            "      <element name=\"sendSmsLogo&gt;senderName\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSmsLogo&gt;charging\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSmsLogo&gt;image\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSmsLogo&gt;smsFormat\" type=\"tns2:SmsFormat\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "   <element name=\"sendSmsLogoResponse\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"result\" type=\"xsd:string\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "   <element name=\"sendSmsRingTone\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"sendSmsRingTone&gt;destAddressSet\" type=\"tns1:ArrayOfEndUserIdentifier\"/>\n" +
            "      <element name=\"sendSmsRingTone&gt;senderName\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSmsRingTone&gt;charging\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSmsRingTone&gt;ringtone\" type=\"xsd:string\"/>\n" +
            "      <element name=\"sendSmsRingTone&gt;smsFormat\" type=\"tns2:SmsFormat\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "   <element name=\"sendSmsRingToneResponse\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"result\" type=\"xsd:string\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "   <element name=\"getSmsDeliveryStatus\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"getSmsDeliveryStatus&gt;requestIdentifier\" type=\"xsd:string\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "   <element name=\"getSmsDeliveryStatusResponse\">\n" +
            "    <complexType>\n" +
            "     <sequence>\n" +
            "      <element name=\"result\" type=\"tns2:ArrayOfDeliveryStatusType\"/>\n" +
            "     </sequence>\n" +
            "    </complexType>\n" +
            "   </element>\n" +
            "  </schema>\n" +
            "  <schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.csapi.org/schema/parlayx/common/v1_0\" xmlns=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "   <import namespace=\"http://www.csapi.org/schema/parlayx/sms/v1_0\"/>\n" +
            "   <complexType name=\"EndUserIdentifier\">\n" +
            "    <sequence>\n" +
            "     <element name=\"value\" type=\"xsd:anyURI\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <complexType name=\"ArrayOfEndUserIdentifier\">\n" +
            "    <sequence>\n" +
            "     <element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"ArrayOfEndUserIdentifier\" nillable=\"true\" type=\"tns1:EndUserIdentifier\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <complexType name=\"InvalidArgumentException\">\n" +
            "    <sequence>\n" +
            "     <element name=\"InvalidArgumentException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <element name=\"InvalidArgumentException\" type=\"tns1:InvalidArgumentException\"/>\n" +
            "   <complexType name=\"UnknownEndUserException\">\n" +
            "    <sequence>\n" +
            "     <element name=\"UnknownEndUserException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <element name=\"UnknownEndUserException\" type=\"tns1:UnknownEndUserException\"/>\n" +
            "   <complexType name=\"MessageTooLongException\">\n" +
            "    <sequence>\n" +
            "     <element name=\"MessageTooLongException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <element name=\"MessageTooLongException\" type=\"tns1:MessageTooLongException\"/>\n" +
            "   <complexType name=\"PolicyException\">\n" +
            "    <sequence>\n" +
            "     <element name=\"PolicyException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <element name=\"PolicyException\" type=\"tns1:PolicyException\"/>\n" +
            "   <complexType name=\"ServiceException\">\n" +
            "    <sequence>\n" +
            "     <element name=\"ServiceException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <element name=\"ServiceException\" type=\"tns1:ServiceException\"/>\n" +
            "  </schema>\n" +
            "  <schema elementFormDefault=\"qualified\" targetNamespace=\"http://www.csapi.org/schema/parlayx/sms/v1_0\" xmlns=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "   <import namespace=\"http://www.csapi.org/schema/parlayx/common/v1_0\"/>\n" +
            "   <simpleType name=\"SmsFormat\">\n" +
            "    <restriction base=\"xsd:string\">\n" +
            "     <enumeration value=\"Ems\"/>\n" +
            "     <enumeration value=\"SmartMessaging\"/>\n" +
            "    </restriction>\n" +
            "   </simpleType>\n" +
            "   <complexType name=\"UnsupportedFormatException\">\n" +
            "    <sequence>\n" +
            "     <element name=\"UnsupportedFormatException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <element name=\"UnsupportedFormatException\" type=\"tns2:UnsupportedFormatException\"/>\n" +
            "   <simpleType name=\"DeliveryStatus\">\n" +
            "    <restriction base=\"xsd:string\">\n" +
            "     <enumeration value=\"Delivered\"/>\n" +
            "     <enumeration value=\"DeliveryUncertain\"/>\n" +
            "     <enumeration value=\"DeliveryImpossible\"/>\n" +
            "     <enumeration value=\"MessageWaiting\"/>\n" +
            "    </restriction>\n" +
            "   </simpleType>\n" +
            "   <complexType name=\"DeliveryStatusType\">\n" +
            "    <sequence>\n" +
            "     <element name=\"destinationAddress\" type=\"tns1:EndUserIdentifier\"/>\n" +
            "     <element name=\"deliveryStatus\" type=\"tns2:DeliveryStatus\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <complexType name=\"ArrayOfDeliveryStatusType\">\n" +
            "    <sequence>\n" +
            "     <element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"ArrayOfDeliveryStatusType\" nillable=\"true\" type=\"tns2:DeliveryStatusType\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <complexType name=\"UnknownRequestIdentifierException\">\n" +
            "    <sequence>\n" +
            "     <element name=\"UnknownRequestIdentifierException\" nillable=\"true\" type=\"xsd:string\"/>\n" +
            "    </sequence>\n" +
            "   </complexType>\n" +
            "   <element name=\"UnknownRequestIdentifierException\" type=\"tns2:UnknownRequestIdentifierException\"/>\n" +
            "  </schema>\n" +
            " </wsdl:types>\n" +
            "   <wsdl:message name=\"sendSmsRequest\">\n" +
            "      <wsdl:part element=\"impl:sendSms\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"sendSmsLogoResponse\">\n" +
            "      <wsdl:part element=\"impl:sendSmsLogoResponse\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"ServiceException\">\n" +
            "      <wsdl:part element=\"tns1:ServiceException\" name=\"ServiceException\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"MessageTooLongException\">\n" +
            "      <wsdl:part element=\"tns1:MessageTooLongException\" name=\"MessageTooLongException\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"UnknownEndUserException\">\n" +
            "      <wsdl:part element=\"tns1:UnknownEndUserException\" name=\"UnknownEndUserException\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"InvalidArgumentException\">\n" +
            "      <wsdl:part element=\"tns1:InvalidArgumentException\" name=\"InvalidArgumentException\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"getSmsDeliveryStatusResponse\">\n" +
            "      <wsdl:part element=\"impl:getSmsDeliveryStatusResponse\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"sendSmsResponse\">\n" +
            "      <wsdl:part element=\"impl:sendSmsResponse\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"sendSmsLogoRequest\">\n" +
            "      <wsdl:part element=\"impl:sendSmsLogo\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"sendSmsRingToneResponse\">\n" +
            "      <wsdl:part element=\"impl:sendSmsRingToneResponse\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"sendSmsRingToneRequest\">\n" +
            "      <wsdl:part element=\"impl:sendSmsRingTone\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"getSmsDeliveryStatusRequest\">\n" +
            "      <wsdl:part element=\"impl:getSmsDeliveryStatus\" name=\"parameters\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"UnknownRequestIdentifierException\">\n" +
            "      <wsdl:part element=\"tns2:UnknownRequestIdentifierException\" name=\"UnknownRequestIdentifierException\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"PolicyException\">\n" +
            "      <wsdl:part element=\"tns1:PolicyException\" name=\"PolicyException\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:message name=\"UnsupportedFormatException\">\n" +
            "      <wsdl:part element=\"tns2:UnsupportedFormatException\" name=\"UnsupportedFormatException\"/>\n" +
            "   </wsdl:message>\n" +
            "   <wsdl:portType name=\"SendSms\">\n" +
            "      <wsdl:operation name=\"sendSms\">\n" +
            "         <wsdl:input message=\"impl:sendSmsRequest\" name=\"sendSmsRequest\"/>\n" +
            "         <wsdl:output message=\"impl:sendSmsResponse\" name=\"sendSmsResponse\"/>\n" +
            "         <wsdl:fault message=\"impl:InvalidArgumentException\" name=\"InvalidArgumentException\"/>\n" +
            "         <wsdl:fault message=\"impl:UnknownEndUserException\" name=\"UnknownEndUserException\"/>\n" +
            "         <wsdl:fault message=\"impl:MessageTooLongException\" name=\"MessageTooLongException\"/>\n" +
            "         <wsdl:fault message=\"impl:PolicyException\" name=\"PolicyException\"/>\n" +
            "         <wsdl:fault message=\"impl:ServiceException\" name=\"ServiceException\"/>\n" +
            "      </wsdl:operation>\n" +
            "      <wsdl:operation name=\"sendSmsLogo\">\n" +
            "         <wsdl:input message=\"impl:sendSmsLogoRequest\" name=\"sendSmsLogoRequest\"/>\n" +
            "         <wsdl:output message=\"impl:sendSmsLogoResponse\" name=\"sendSmsLogoResponse\"/>\n" +
            "         <wsdl:fault message=\"impl:InvalidArgumentException\" name=\"InvalidArgumentException\"/>\n" +
            "         <wsdl:fault message=\"impl:UnknownEndUserException\" name=\"UnknownEndUserException\"/>\n" +
            "         <wsdl:fault message=\"impl:MessageTooLongException\" name=\"MessageTooLongException\"/>\n" +
            "         <wsdl:fault message=\"impl:UnsupportedFormatException\" name=\"UnsupportedFormatException\"/>\n" +
            "         <wsdl:fault message=\"impl:PolicyException\" name=\"PolicyException\"/>\n" +
            "         <wsdl:fault message=\"impl:ServiceException\" name=\"ServiceException\"/>\n" +
            "      </wsdl:operation>\n" +
            "      <wsdl:operation name=\"sendSmsRingTone\">\n" +
            "         <wsdl:input message=\"impl:sendSmsRingToneRequest\" name=\"sendSmsRingToneRequest\"/>\n" +
            "         <wsdl:output message=\"impl:sendSmsRingToneResponse\" name=\"sendSmsRingToneResponse\"/>\n" +
            "         <wsdl:fault message=\"impl:InvalidArgumentException\" name=\"InvalidArgumentException\"/>\n" +
            "         <wsdl:fault message=\"impl:UnknownEndUserException\" name=\"UnknownEndUserException\"/>\n" +
            "         <wsdl:fault message=\"impl:MessageTooLongException\" name=\"MessageTooLongException\"/>\n" +
            "         <wsdl:fault message=\"impl:UnsupportedFormatException\" name=\"UnsupportedFormatException\"/>\n" +
            "         <wsdl:fault message=\"impl:PolicyException\" name=\"PolicyException\"/>\n" +
            "         <wsdl:fault message=\"impl:ServiceException\" name=\"ServiceException\"/>\n" +
            "      </wsdl:operation>\n" +
            "      <wsdl:operation name=\"getSmsDeliveryStatus\">\n" +
            "         <wsdl:input message=\"impl:getSmsDeliveryStatusRequest\" name=\"getSmsDeliveryStatusRequest\"/>\n" +
            "         <wsdl:output message=\"impl:getSmsDeliveryStatusResponse\" name=\"getSmsDeliveryStatusResponse\"/>\n" +
            "         <wsdl:fault message=\"impl:UnknownRequestIdentifierException\" name=\"UnknownRequestIdentifierException\"/>\n" +
            "         <wsdl:fault message=\"impl:ServiceException\" name=\"ServiceException\"/>\n" +
            "      </wsdl:operation>\n" +
            "   </wsdl:portType>\n" +
            "   <wsdl:binding name=\"SendSmsSoapBinding\" type=\"impl:SendSms\">\n" +
            "      <wsdlsoap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "      <wsdl:operation name=\"sendSms\">\n" +
            "         <wsdlsoap:operation soapAction=\"\"/>\n" +
            "         <wsdl:input name=\"sendSmsRequest\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:input>\n" +
            "         <wsdl:output name=\"sendSmsResponse\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:output>\n" +
            "         <wsdl:fault name=\"InvalidArgumentException\">\n" +
            "            <wsdlsoap:fault name=\"InvalidArgumentException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"UnknownEndUserException\">\n" +
            "            <wsdlsoap:fault name=\"UnknownEndUserException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"MessageTooLongException\">\n" +
            "            <wsdlsoap:fault name=\"MessageTooLongException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"PolicyException\">\n" +
            "            <wsdlsoap:fault name=\"PolicyException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"ServiceException\">\n" +
            "            <wsdlsoap:fault name=\"ServiceException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "      </wsdl:operation>\n" +
            "      <wsdl:operation name=\"sendSmsLogo\">\n" +
            "         <wsdlsoap:operation soapAction=\"\"/>\n" +
            "         <wsdl:input name=\"sendSmsLogoRequest\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:input>\n" +
            "         <wsdl:output name=\"sendSmsLogoResponse\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:output>\n" +
            "         <wsdl:fault name=\"InvalidArgumentException\">\n" +
            "            <wsdlsoap:fault name=\"InvalidArgumentException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"UnknownEndUserException\">\n" +
            "            <wsdlsoap:fault name=\"UnknownEndUserException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"MessageTooLongException\">\n" +
            "            <wsdlsoap:fault name=\"MessageTooLongException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"UnsupportedFormatException\">\n" +
            "            <wsdlsoap:fault name=\"UnsupportedFormatException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"PolicyException\">\n" +
            "            <wsdlsoap:fault name=\"PolicyException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"ServiceException\">\n" +
            "            <wsdlsoap:fault name=\"ServiceException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "      </wsdl:operation>\n" +
            "      <wsdl:operation name=\"sendSmsRingTone\">\n" +
            "         <wsdlsoap:operation soapAction=\"\"/>\n" +
            "         <wsdl:input name=\"sendSmsRingToneRequest\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:input>\n" +
            "         <wsdl:output name=\"sendSmsRingToneResponse\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:output>\n" +
            "         <wsdl:fault name=\"InvalidArgumentException\">\n" +
            "            <wsdlsoap:fault name=\"InvalidArgumentException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"UnknownEndUserException\">\n" +
            "            <wsdlsoap:fault name=\"UnknownEndUserException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"MessageTooLongException\">\n" +
            "            <wsdlsoap:fault name=\"MessageTooLongException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"UnsupportedFormatException\">\n" +
            "            <wsdlsoap:fault name=\"UnsupportedFormatException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"PolicyException\">\n" +
            "            <wsdlsoap:fault name=\"PolicyException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"ServiceException\">\n" +
            "            <wsdlsoap:fault name=\"ServiceException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "      </wsdl:operation>\n" +
            "      <wsdl:operation name=\"getSmsDeliveryStatus\">\n" +
            "         <wsdlsoap:operation soapAction=\"\"/>\n" +
            "         <wsdl:input name=\"getSmsDeliveryStatusRequest\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:input>\n" +
            "         <wsdl:output name=\"getSmsDeliveryStatusResponse\">\n" +
            "            <wsdlsoap:body use=\"literal\"/>\n" +
            "         </wsdl:output>\n" +
            "         <wsdl:fault name=\"UnknownRequestIdentifierException\">\n" +
            "            <wsdlsoap:fault name=\"UnknownRequestIdentifierException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "         <wsdl:fault name=\"ServiceException\">\n" +
            "            <wsdlsoap:fault name=\"ServiceException\" use=\"literal\"/>\n" +
            "         </wsdl:fault>\n" +
            "      </wsdl:operation>\n" +
            "   </wsdl:binding>\n" +
            "   <wsdl:service name=\"SendSmsService\">\n" +
            "      <wsdl:port binding=\"impl:SendSmsSoapBinding\" name=\"SendSms\">\n" +
            "         <wsdlsoap:address location=\"http://localhost:8080/parlayx/services/SendSms\"/>\n" +
            "      </wsdl:port>\n" +
            "   </wsdl:service>\n" +
            "</wsdl:definitions>";
}
