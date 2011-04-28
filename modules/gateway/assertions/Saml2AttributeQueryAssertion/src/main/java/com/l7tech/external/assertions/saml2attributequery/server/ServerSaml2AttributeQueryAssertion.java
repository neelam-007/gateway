package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion;
import com.l7tech.external.assertions.saml2attributequery.SamlToLdapMap;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.message.XmlKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterPropertyCache;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.ldap.LdapIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.xml.soap.SoapUtil;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.SOAPConstants;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the Saml2AttributeQueryAssertion.
 *
 * @see com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion
 */
public class ServerSaml2AttributeQueryAssertion extends AbstractServerAssertion<Saml2AttributeQueryAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSaml2AttributeQueryAssertion.class.getName());

    private final IdentityProviderFactory identityProviderFactory;
    private final ClusterPropertyCache clusterPropertyCache;
    private final Auditor auditor;

    private Saml2AttributeQueryResponseGenerator responseGenerator;

    private static HashMap<String, String> NS_PREFIXES;
    static {
        NS_PREFIXES = new HashMap<String, String>(7);
        NS_PREFIXES.put(SamlConstants.NS_SAML,  SamlConstants.NS_SAML_PREFIX);
        NS_PREFIXES.put(SamlConstants.NS_SAML2,  SamlConstants.NS_SAML2_PREFIX);
        NS_PREFIXES.put(SamlConstants.NS_SAMLP,  SamlConstants.NS_SAMLP_PREFIX);
        NS_PREFIXES.put(SamlConstants.NS_SAMLP2,  SamlConstants.NS_SAMLP2_PREFIX);
        NS_PREFIXES.put(SoapUtil.DIGSIG_URI, "ds");
        NS_PREFIXES.put(SoapUtil.XMLENC_NS, "xenc");
        NS_PREFIXES.put("http://www.w3.org/2001/XMLSchema", "xs");
    }

    public static final String SOAP_ENV_PREFIX = "soapenv";

    NamespacePrefixMapper NAMESPACE_PREFIX_MAPPER = new NamespacePrefixMapper() {
        public String getPreferredPrefix(String namespaceUri, String suggestion, boolean requirePrefix) {
            if (NS_PREFIXES.containsKey(namespaceUri))
                return NS_PREFIXES.get(namespaceUri);
            return suggestion;
        }
    };

    //- PUBLIC

    @SuppressWarnings({"UnusedDeclaration"})
    public ServerSaml2AttributeQueryAssertion( final Saml2AttributeQueryAssertion assertion,
                                               final ApplicationContext context )
        throws PolicyAssertionException
    {
        super(assertion);
        identityProviderFactory = (IdentityProviderFactory) context.getBean("identityProviderFactory", IdentityProviderFactory.class);
        clusterPropertyCache = (ClusterPropertyCache) context.getBean("clusterPropertyCache", ClusterPropertyCache.class);
        X509Certificate rootCert = (X509Certificate)context.getBean("sslKeystoreCertificate");
        X509Certificate[] serverCertChain = new X509Certificate[]{rootCert};
        auditor = new Auditor(this, context, logger);

        try {
            responseGenerator = new Saml2AttributeQueryResponseGenerator();
        } catch(SamlpAssertionException e) {
            logger.log(Level.WARNING, "Failed to initialize the SAML v2.0 attribute query response generator.", e);
        }
    }

    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        AssertionStatus status = AssertionStatus.FALSIFIED;

        try {
            if(!context.getRequest().isSoap()) {
                return status;
            }

            Object obj = context.getVariable(assertion.getIdContextVariable());
            if(obj == null || !(obj instanceof String)) {
                return status;
            }
            String idValue = (String)obj;

            String audience = null;
            if(assertion.getAudienceRestriction() != null && assertion.getAudienceRestriction().length() > 0) {
                Map<String, Object> vars = context.getVariableMap(Syntax.getReferencedNames(assertion.getAudienceRestriction()), auditor);
                audience = ExpandVariables.process(assertion.getAudienceRestriction(), vars, auditor, true);
            }

            ClusterProperty property = clusterPropertyCache.getCachedEntityByName(assertion.getMapClusterProperty(), 30000);
            if(property == null) {
                return status;
            }
            SamlToLdapMap map = new SamlToLdapMap(property.getValue());

            XmlKnob xmlKnob = context.getRequest().getXmlKnob();
            Document doc = xmlKnob.getDocumentReadOnly();

            Element root = doc.getDocumentElement();
            if(!SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(root.getNamespaceURI()) || !"Envelope".equals(root.getLocalName())) {
                return status;
            }
            NodeList children = root.getElementsByTagNameNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, "Body");
            if(children.getLength() == 0) {
                return status;
            }
            root = (Element)children.item(0);

            children = root.getElementsByTagNameNS(SamlConstants.NS_SAMLP2, "AttributeQuery");
            if(children.getLength() == 0) {
                return status;
            }
            root = (Element)children.item(0);

            Saml2AttributeQuery attributeQuery = null;
            try {
                attributeQuery = new Saml2AttributeQuery(root, map);
            } catch(SamlAttributeNotMappedException sanme) {
                context.getResponse().initialize(createErrorResponse(Saml2AttributeQuery.getQueryId(root), Saml2AttributeQuery.getQueriedAttributes(root)));
                context.setRoutingStatus(RoutingStatus.ROUTED);
                auditor.logAndAudit(AssertionMessages.SAML2_AQ_REQUEST_SAML_ATTR_UNKNOWN );

                return status;
            }

            if(!verifyAttributePermissions(attributeQuery)) {
                context.getResponse().initialize(createErrorResponse(Saml2AttributeQuery.getQueryId(root), Saml2AttributeQuery.getQueriedAttributes(root)));
                context.setRoutingStatus(RoutingStatus.ROUTED);
                auditor.logAndAudit(AssertionMessages.SAML2_AQ_REQUEST_SAML_ATTR_FORBIDDEN );
            } else {
                HashMap<String, Object> values = retrieveAttributeValues(attributeQuery, idValue);
                attributeQuery.filterValues(values);

                Saml2AttributeQueryResponseGenerator.Conditions conditions = new Saml2AttributeQueryResponseGenerator.Conditions();
                if(assertion.getConditionsNotBeforeSecondsInPast() > -1) {
                    conditions.notBeforeTime = assertion.getConditionsNotBeforeSecondsInPast();
                }
                if(assertion.getConditionsNotOnOrAfterExpirySeconds() > -1) {
                    conditions.notOnOrAfterTime = assertion.getConditionsNotOnOrAfterExpirySeconds();
                }
                if(audience != null && audience.length() > 0) {
                    conditions.audience = audience;
                }

                context.getResponse().initialize(createSuccessResponse(attributeQuery, map, values, conditions));
                context.setRoutingStatus(RoutingStatus.ROUTED);
                status = AssertionStatus.NONE;
            }
        } catch(SAXException e) {
            status = AssertionStatus.SERVER_ERROR;
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO);
        } catch(FindException e) {
            auditor.logAndAudit(AssertionMessages.IDENTITY_PROVIDER_NOT_FOUND);
            status = AssertionStatus.SERVER_ERROR;
        } catch(NamingException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, e.toString());
            status = AssertionStatus.SERVER_ERROR;
        } catch(Exception e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, e.toString());
            status = AssertionStatus.SERVER_ERROR;
        }

        return status;
    }

    //- PRIVATE

    private boolean verifyAttributePermissions(Saml2AttributeQuery attributeQuery) {
        for(String attributeName : attributeQuery.getLdapAttributeNames()) {
            if((assertion.isWhiteList() && !assertion.getRestrictedAttributeList().contains(attributeName)) ||
                    !assertion.isWhiteList() && assertion.getRestrictedAttributeList().contains(attributeName))
            {
                return false;
            }
        }

        return true;
    }

    private HashMap<String, Object> retrieveAttributeValues(Saml2AttributeQuery attributeQuery, String idValue)
    throws FindException, NamingException
    {
        HashMap<String, Object> values;

        LdapIdentityProvider ldapProvider = (LdapIdentityProvider)identityProviderFactory.getProvider(assertion.getLdapProviderOid());
        LdapIdentityProviderConfig providerConfig = (LdapIdentityProviderConfig)ldapProvider.getConfig();

        UserMappingConfig[] mappings = providerConfig.getUserMappings();
        String queryString;
        if(mappings.length == 0) {
            throw new NameNotFoundException();
        } else if(mappings.length == 1) {
            queryString = "(&(objectclass=" + mappings[0].getObjClass() + ")(" + assertion.getIdFieldName() + "=" + idValue + "))";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("(&(");
            sb.append(assertion.getIdFieldName());
            sb.append("=");
            sb.append(idValue);
            sb.append(")(|");
            for(UserMappingConfig mapping : mappings) {
                sb.append("(objectclass=");
                sb.append(mapping.getObjClass());
                sb.append(")");
            }
            sb.append("))");

            queryString = sb.toString();
        }

        DirContext ldapcontext = ldapProvider.getBrowseContext();
        SearchControls sc = new SearchControls();
        sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
        String[] ldapAttributeNames = attributeQuery.getLdapAttributeNames();
        if(ldapAttributeNames.length > 0) {
            sc.setReturningAttributes(attributeQuery.getLdapAttributeNames());
        }
        NamingEnumeration answer = ldapcontext.search(providerConfig.getSearchBase(), queryString, sc);

        if(answer.hasMore()) {
            SearchResult searchResult = (SearchResult)answer.next();
            Attributes attributes = searchResult.getAttributes();

            values = new HashMap<String, Object>();
            for(NamingEnumeration attrs = attributes.getAll();attrs.hasMore();) {
                Attribute attribute = (Attribute)attrs.next();
                List<String> valueList = new ArrayList<String>();

                for(NamingEnumeration ne = attribute.getAll();ne.hasMore();) {
                    valueList.add(ne.next().toString());
                }

                if(valueList.size() == 1) {
                    values.put(attribute.getID(), valueList.get(0));
                } else {
                    values.put(attribute.getID(), valueList);
                }
            }
        } else {
            answer.close();
            ldapcontext.close();

            throw new NameNotFoundException();
        }

        answer.close();
        ldapcontext.close();
        return values;
    }

    private Document createSuccessResponse(Saml2AttributeQuery attributeQuery,
                                           SamlToLdapMap map,
                                           HashMap<String, Object> values,
                                           Saml2AttributeQueryResponseGenerator.Conditions conditions)
            throws JAXBException, ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element envelope = doc.createElementNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAP_ENV_PREFIX + ":Envelope");
        envelope.setAttribute(SoapUtil.XMLNS + ":" + SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
        doc.appendChild(envelope);

        Element header = doc.createElementNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAP_ENV_PREFIX + ":Header");
        envelope.appendChild(header);

        Element body = doc.createElementNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAP_ENV_PREFIX + ":Body");
        envelope.appendChild(body);

        JAXBContext ctx = JAXBContext.newInstance("saml.v2.protocol:saml.v2.assertion:saml.support.ds", Saml2AttributeQuery.class.getClassLoader());
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", NAMESPACE_PREFIX_MAPPER);
        m.marshal(responseGenerator.createSuccessMesage(attributeQuery, assertion.getIssuer(), map, values, conditions), body);

        return doc;
    }

    private Document createErrorResponse(String queryId, List<Saml2AttributeQuery.QueriedAttribute> queriedAttributes)
            throws JAXBException, ParserConfigurationException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.newDocument();

        Element envelope = doc.createElementNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAP_ENV_PREFIX + ":Envelope");
        envelope.setAttribute(SoapUtil.XMLNS + ":" + SOAP_ENV_PREFIX, SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
        doc.appendChild(envelope);

        Element header = doc.createElementNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAP_ENV_PREFIX + ":Header");
        envelope.appendChild(header);

        Element body = doc.createElementNS(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, SOAP_ENV_PREFIX + ":Body");
        envelope.appendChild(body);

        JAXBContext ctx = JAXBContext.newInstance("saml.v2.protocol:saml.v2.assertion:saml.support.ds", Saml2AttributeQuery.class.getClassLoader());
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty("com.sun.xml.bind.namespacePrefixMapper", NAMESPACE_PREFIX_MAPPER);
        m.marshal(responseGenerator.createErrorMessage("urn:oasis:names:tc:SAML:2.0:status:Requester",
                "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue",
                "One or more of the requested attributes are not available",
                queryId, queriedAttributes), body);

        return doc;
    }
}