package com.l7tech.external.assertions.xacmlpdp.server;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.external.assertions.xacmlpdp.XacmlPdpAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.MessageUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.url.AbstractUrlObjectCache;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.server.util.res.ResourceGetter;
import com.l7tech.server.util.res.ResourceObjectFactory;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.Charsets;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Resolver;
import com.l7tech.util.ValidatedConfig;
import com.sun.xacml.Indenter;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ParsingException;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.ctx.Result;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.soap.SOAPConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.server.util.res.ResourceGetter.*;

/**
 * User: njordan
 * Date: 13-Mar-2009
 * Time: 11:33:42 PM
 */
public class ServerXacmlPdpAssertion extends AbstractServerAssertion<XacmlPdpAssertion> {
    public ServerXacmlPdpAssertion(XacmlPdpAssertion ea, ApplicationContext applicationContext) throws ServerPolicyException {
        super(ea);
        stashManagerFactory = applicationContext.getBean("stashManagerFactory", StashManagerFactory.class);
        envModule = new CurrentEnvModule();

        this.variablesUsed = ea.getVariablesUsed();

        ResourceObjectFactory<PolicyFinder> resourceObjectfactory =
            new ResourceObjectFactory<PolicyFinder>()
            {
                @Override
                public PolicyFinder createResourceObject(final String resourceString) throws ParseException {
                    try {
                        return cacheObjectFactory.createUserObject("", new AbstractUrlObjectCache.UserObjectSource(){
                            @Override
                            public byte[] getBytes() throws IOException {
                                throw new IOException("Not supported");
                            }

                            @Override
                            public ContentTypeHeader getContentType() {
                                return null;
                            }

                            @Override
                            public String getString(boolean isXml) {
                                return resourceString;
                            }
                        });
                    } catch (IOException e) {
                        throw (ParseException)new ParseException("Unable to parse: " +
                                ExceptionUtils.getMessage(e), 0).initCause(e);
                    }
                }

                @Override
                public void closeResourceObject( final PolicyFinder resourceObject ) {
                }
            };

        //If assertion.getResourceInfo() returns a SingleUrlResourceGetter then the above ResourceObjectFactory
        //will NEVER be used by the ResourceGetter
        boolean hasPolicyVariable = false;
        AssertionResourceInfo resourceInfo = assertion.getResourceInfo();
        if ( resourceInfo instanceof StaticResourceInfo ) {
            StaticResourceInfo sri = (StaticResourceInfo) resourceInfo;
            String doc = sri.getDocument();
            if (doc != null)
                hasPolicyVariable = Syntax.getReferencedNames(doc).length > 0;
        }

        if (resourceInfo instanceof MessageUrlResourceInfo)
            throw new ServerPolicyException(assertion, "MessageUrlResourceInfo is not yet supported.");
        
        ResourceGetter<PolicyFinder, Void> resourceGetter;//Void as no UrlFinder is provided. Change if support for MessageUrlResourceInfo is added
        try {
            resourceGetter = hasPolicyVariable ? null : ResourceGetter.<PolicyFinder, Void>createResourceGetter(
                    assertion,
                    assertion.getResourceInfo(),
                    resourceObjectfactory,
                    null,
                    getCache(applicationContext),
                    getAudit());
        } catch ( ServerPolicyException spe ) {
            ParsingException pe = ExceptionUtils.getCauseIfCausedBy( spe, ParsingException.class );
            if ( pe == null ) {
                throw spe;
            } else {
                logger.warning( "Error parsing XACML policy '"+ExceptionUtils.getMessage( pe )+"'." );
                resourceGetter = ResourceGetter.createErrorResourceGetter( new ResourceParseException(ExceptionUtils.getMessage( pe ), pe, "static") );
            }
        }
        this.resourceGetter = resourceGetter;
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            AttributeFinder attrFinder = new AttributeFinder();
            List<AttributeFinderModule> attrModules = new ArrayList<AttributeFinderModule>();
            attrModules.add(envModule);
            attrModules.add(new ContextVariableAttributeFinderModule(context));
            attrFinder.setModules(attrModules);

            PolicyFinder policyFinder = getPolicyFinder(context);
            PDP pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));

            Element rootElement;
            try {
                if(assertion.getInputMessageSource() == XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE) {
                    rootElement = context.getResponse().getXmlKnob().getDocumentReadOnly().getDocumentElement();
                } else if(assertion.getInputMessageSource() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
                    try {
                        rootElement = ((Message)context.getVariable(assertion.getInputMessageVariableName())).getXmlKnob().getDocumentReadOnly().getDocumentElement();
                    } catch(NoSuchVariableException nsve) {
                        return AssertionStatus.FAILED;
                    }
                } else {
                    rootElement = context.getRequest().getXmlKnob().getDocumentReadOnly().getDocumentElement();
                }
            } catch(SAXException se) {
                return AssertionStatus.FAILED;
            }

            if(assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.REQUEST) ||
                    assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.REQUEST_AND_RESPONSE))
            {
                rootElement = XmlUtil.findFirstChildElementByName(rootElement, rootElement.getNamespaceURI(), "Body");
                if(rootElement == null) {
                    logAndAudit(AssertionMessages.XACML_PDP_REQUEST_NOT_ENCAPSULATED);
                    return AssertionStatus.FALSIFIED;
                }
                //the namespace varies with the version, do not check namespace here, it is checked below
                rootElement = XmlUtil.findFirstChildElementByName(rootElement, (String) null, "Request");
                if(rootElement == null) {
                    logAndAudit(AssertionMessages.XACML_PDP_REQUEST_NOT_ENCAPSULATED);
                    return AssertionStatus.FALSIFIED;
                }
            }

            //check the namespace on rootElement is valid. The namespace represents the xacml request version
            String nameSpace = rootElement.getNamespaceURI();
            if(!XacmlAssertionEnums.XacmlVersionType.isValidXacmlVersionType(nameSpace)){
                logAndAudit(AssertionMessages.XACML_PDP_REQUEST_NAMESPACE_UNKNOWN, nameSpace);
                return AssertionStatus.FALSIFIED;
            }
            
            RequestCtx requestCtx;
            try {
                requestCtx = RequestCtx.getInstance(rootElement);
            } catch (ParsingException e) {
                throw new InvalidRequestException( ExceptionUtils.getMessage(e), e );
            }
            ResponseCtx responseCtx = pdp.evaluate(requestCtx);

            AssertionStatus status = AssertionStatus.NONE;

            if(assertion.getFailIfNotPermit()) {
                Set results = responseCtx.getResults();
                if(results.isEmpty() || results.size() > 1) {
                    status = AssertionStatus.FAILED;
                } else {
                    Result result = (Result)results.iterator().next();
                    if(result.getDecision() != Result.DECISION_PERMIT) {
                        status = AssertionStatus.FALSIFIED;
                    }
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] messageBytes;
            if(assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.RESPONSE) ||
                    assertion.getSoapEncapsulation().equals(XacmlPdpAssertion.SoapEncapsulationType.REQUEST_AND_RESPONSE))
            {
                responseCtx.encode(baos, new Indenter() {
                    private String spaces = "    ";

                    @Override
                    public void in() {
                        spaces += "  ";
                    }

                    @Override
                    public String makeString() {
                        return spaces;
                    }

                    @Override
                    public void out() {
                        spaces = spaces.substring(2);
                    }
                });

                byte[] prefix = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<soapenv:Envelope xmlns:soapenv=\"" +
                        SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE + "\">\n  <soapenv:Header/>\n  <soapenv:Body>\n").getBytes(Charsets.UTF8);
                byte[] suffix = "  </soapenv:Body>\n</soapenv:Envelope>\n".getBytes(Charsets.UTF8);
                byte[] content = baos.toByteArray();

                messageBytes = new byte[prefix.length + content.length + suffix.length];

                System.arraycopy(prefix, 0, messageBytes, 0, prefix.length);
                System.arraycopy(content, 0, messageBytes, prefix.length, content.length);
                System.arraycopy(suffix, 0, messageBytes, prefix.length + content.length, suffix.length);
            } else {
                responseCtx.encode(baos, new Indenter(2));
                messageBytes = baos.toByteArray();
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(messageBytes);

            ContentTypeHeader cth = ContentTypeHeader.XML_DEFAULT;
            Message message;
            if(assertion.getOutputMessageTarget() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
                message = context.getOrCreateTargetMessage( new MessageTargetableSupport(assertion.getOutputMessageVariableName()), false );
            } else if (assertion.getOutputMessageTarget() == XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST) {
                message = context.getRequest();
            } else {
                message = context.getResponse();
            }
            message.initialize( stashManagerFactory.createStashManager(), cth, bais );

            return status;
        } catch(InvalidRequestException e) {
            logAndAudit(AssertionMessages.XACML_PDP_INVALID_REQUEST, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        } catch (InvalidPolicyException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.SERVER_ERROR;
        } catch (NoSuchVariableException e) {
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {ExceptionUtils.getMessage(e)}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }
    }

    /**
     * Access the PolicyFinder
     *
     * @throws IOException If there is an IO issuen for the request message
     * @throws InvalidMessageException If the request is not valid
     * @throws InvalidPolicyException If there is a problem with the XACML policy
     */
    private PolicyFinder getPolicyFinder( final PolicyEnforcementContext context ) throws IOException, InvalidPolicyException {
        final Map<String,Object> variables = context.getVariableMap(variablesUsed, getAudit());
        if (resourceGetter != null) {
            try {
                return resourceGetter.getResource(null, variables);
            } catch (ResourceIOException e) {
                throw new InvalidPolicyException("Error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (GeneralSecurityException e) {
                throw new InvalidPolicyException("Error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (ResourceParseException e) {
                throw new InvalidPolicyException("Invalid XACML policy '" + ExceptionUtils.getMessage(e) + "'.", e );
            } catch (UrlResourceException e) { // should not happen since we are not examining the message
                throw new InvalidPolicyException("URL error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (UrlNotFoundException e) { // should not happen since we are not examining the message
                throw new InvalidPolicyException("URL error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            } catch (InvalidMessageException e) { // should not happen since we are not examining the message
                throw new InvalidPolicyException("URL error accessing XACML policy resource '" + ExceptionUtils.getMessage(e) + "'.", e);
            }
        }
        PolicyFinder policyFinder = new PolicyFinder();
        try {
            String policy = ((StaticResourceInfo)assertion.getResourceInfo()).getDocument();
            String expandedPolicy = ExpandVariables.process(policy, variables, getAudit(), true);
            ConstantPolicyModule policyModule = new ConstantPolicyModule(expandedPolicy);
            Set<ConstantPolicyModule> policyModules = new HashSet<ConstantPolicyModule>();
            policyModules.add(policyModule);
            policyFinder.setModules(policyModules);
        } catch (SAXException e) {
            throw new InvalidPolicyException("Invalid XACML policy after variable expansion '" + ExceptionUtils.getMessage(e) + "'", e);
        } catch (ParsingException e) {
            throw new InvalidPolicyException("Invalid XACML policy after variable expansion '" + ExceptionUtils.getMessage(e) + "'", e);
        } catch (VariableNameSyntaxException e) {
            throw new InvalidPolicyException("Invalid XACML policy during variable expansion", e); // message from exception is already audited
        }

        return policyFinder;
    }

    private static synchronized HttpObjectCache<PolicyFinder> getCache( BeanFactory spring ) {
        if (httpObjectCache != null)
            return httpObjectCache;

        GenericHttpClientFactory clientFactory = (GenericHttpClientFactory)spring.getBean("httpClientFactory");
        if (clientFactory == null) throw new IllegalStateException("No httpClientFactory bean");

        Config config = validated( spring.getBean( "serverConfig", Config.class ) );
        httpObjectCache = new HttpObjectCache<PolicyFinder>(
                "XACML Policy",
                config.getIntProperty(XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_ENTRIES, 100),
                config.getIntProperty(XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_AGE, 300000),
                config.getIntProperty(XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_STALE_AGE, -1),
                clientFactory,
                cacheObjectFactory,
                HttpObjectCache.WAIT_INITIAL,
                ClusterProperty.asServerConfigPropertyName(XacmlPdpAssertion.XACML_PDP_MAX_DOWNLOAD_SIZE)
        );

        return httpObjectCache;
    }

    private static Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {

                if ( XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_ENTRIES.equals( key ) ) {
                    return XacmlPdpAssertion.CPROP_XACML_POLICY_CACHE_MAX_ENTRIES;
                }

                if ( XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_STALE_AGE.equals( key ) ) {
                    return XacmlPdpAssertion.CPROP_XACML_POLICY_CACHE_MAX_STALE_AGE;
                }

                return key;
            }
        } );

        vc.setMinimumValue( XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_ENTRIES, 0 );
        vc.setMaximumValue( XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_ENTRIES, 1000000 );

        vc.setMinimumValue(XacmlPdpAssertion.PARAM_XACML_POLICY_CACHE_MAX_STALE_AGE, -1);

        return vc;
    }

    private CurrentEnvModule envModule;
    private final ResourceGetter<PolicyFinder, Void> resourceGetter;
    private final String[] variablesUsed;

    private static final Logger logger = Logger.getLogger(ServerXacmlPdpAssertion.class.getName());


    private final StashManagerFactory stashManagerFactory;
    private static HttpObjectCache<PolicyFinder> httpObjectCache = null;

    private static final class InvalidPolicyException extends Exception {
        public InvalidPolicyException( String message, Throwable cause ) {
            super( message, cause );
        }
    }

    private static final class InvalidRequestException extends Exception {
        public InvalidRequestException( String message, Throwable cause ) {
            super( message, cause );
        }
    }

    private static final HttpObjectCache.UserObjectFactory<PolicyFinder> cacheObjectFactory =
                new HttpObjectCache.UserObjectFactory<PolicyFinder>() {
                    @Override
                    public PolicyFinder createUserObject(String url, AbstractUrlObjectCache.UserObjectSource responseSource) throws IOException {
                        String response = responseSource.getString(true);
                        try {
                            ConstantPolicyModule policyModule = new ConstantPolicyModule(response);
                            PolicyFinder policyFinder = new PolicyFinder();
                            Set<ConstantPolicyModule> policyModules = new HashSet<ConstantPolicyModule>();
                            policyModules.add(policyModule);
                            policyFinder.setModules(policyModules);

                            return policyFinder;
                        } catch (SAXException saxe) {
                            throw new CausedIOException(ExceptionUtils.getMessage(saxe), saxe);
                        } catch (ParsingException pe) {
                            throw new CausedIOException(ExceptionUtils.getMessage(pe), pe);
                        }
                    }
                };
}
