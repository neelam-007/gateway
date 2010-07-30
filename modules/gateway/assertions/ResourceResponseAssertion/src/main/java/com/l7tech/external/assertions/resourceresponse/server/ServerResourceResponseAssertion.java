package com.l7tech.external.assertions.resourceresponse.server;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.external.assertions.resourceresponse.ResourceResponseAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;
import com.l7tech.xml.DocumentReferenceProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ResourceResponseAssertion.
 *
 * @see com.l7tech.external.assertions.resourceresponse.ResourceResponseAssertion
 */
public class ServerResourceResponseAssertion extends AbstractServerAssertion<ResourceResponseAssertion> {

    //- PUBLIC

    public ServerResourceResponseAssertion( final ResourceResponseAssertion assertion ) throws PolicyAssertionException {
        super(assertion);
        long lastModified = System.currentTimeMillis();
        this.lastModifiedText = GenericHttpHeader.makeDateHeader( "dummy", new Date(lastModified) ).getFullValue();
    }

    @Override
    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        final Message request = context.getRequest();
        final HttpServletRequestKnob httpRequestKnob = request.getKnob( HttpServletRequestKnob.class );
        if ( httpRequestKnob == null ) {
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }

        boolean responded = false;
        final String resourceId = httpRequestKnob.getParameter( PARAM_RESOURCE_ID );
        final ResourceResponseAssertion.ResourceData[] resources = assertion.getResources();
        if ( resourceId != null ) {
            final ResourceResponseAssertion.ResourceData resourceData = getResourceData( resources, resourceId );
            if ( resourceData != null ) {
                responded = true;
                sendResponseIfNeeded( httpRequestKnob, context, resourceData );
            }
        } else if ( resources != null && resources.length > 0 && resources[0] != null ) {
            responded = true;
            sendResponseIfNeeded( httpRequestKnob, context, resources[0] );
        }

        if ( !responded && !assertion.isFailOnMissing() ) {
            responded = true;
            send404Response( context );             
        }

        return responded ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerResourceResponseAssertion.class.getName());

    private static final String PARAM_RESOURCE_ID = "rid";

    private final String lastModifiedText;

    private ResourceResponseAssertion.ResourceData getResourceData( final ResourceResponseAssertion.ResourceData[] resources,
                                                                    final String resourceId ) {
        ResourceResponseAssertion.ResourceData resourceData = null;

        if ( resources != null ) {
            for ( final ResourceResponseAssertion.ResourceData resource : resources ) {
                if ( resource != null && resourceId.equals( resource.getId() ) ) {
                    resourceData = resource;
                    break;
                }
            }
        }

        return resourceData;
    }

    private String eTag( final ResourceResponseAssertion.ResourceData resource ) {
        return "\"" + resource.contentHash() + "\"";
    }

    private void sendResponseIfNeeded( final HttpServletRequestKnob httpRequestKnob,
                                       final PolicyEnforcementContext context,
                                       final ResourceResponseAssertion.ResourceData resource ) throws IOException {
        final String[] ifModifiedSince = httpRequestKnob.getHeaderValues( HttpConstants.HEADER_IF_MODIFIED_SINCE );
        final String[] ifNoneMatch = httpRequestKnob.getHeaderValues( HttpConstants.HEADER_IF_NONE_MATCH );

        boolean notModified = false;
        if ( ifNoneMatch != null && ifNoneMatch.length==1 && ifNoneMatch[0] != null ) {
            final String eTag = ifNoneMatch[0]; // we should really be splitting and checking each tag, this assumes one only
            if ( eTag.equals( eTag(resource)) ) {
                notModified = true;
            }
        }

        if ( !notModified ) {
            if ( ifModifiedSince != null && ifModifiedSince.length==1 && ifModifiedSince[0] != null ) {
                final String lastModified = ifModifiedSince[0];
                if ( lastModified.equals(lastModifiedText) ) {  // we should attempt to parse the request date and check that too
                    notModified = true;
                }
            }
        }

        if ( notModified ) {
            sendNotModified( context, resource );
        } else {
            sendResponse( httpRequestKnob, context, resource );
        }
    }

    private void sendNotModified( final PolicyEnforcementContext context,
                               final ResourceResponseAssertion.ResourceData resource ) throws IOException {
        final Message response = context.getResponse();
        response.initialize( ContentTypeHeader.TEXT_DEFAULT, new byte[0] );

        final HttpResponseKnob httpResponseKnob = response.getKnob( HttpResponseKnob.class );
        if ( httpResponseKnob != null ) {
            httpResponseKnob.setStatus( HttpConstants.STATUS_NOT_MODIFIED );
            httpResponseKnob.addHeader( HttpConstants.HEADER_LAST_MODIFIED, lastModifiedText );
            httpResponseKnob.addHeader( HttpConstants.HEADER_ETAG, eTag(resource) );
        }
    }

    private void sendResponse( final HttpServletRequestKnob httpRequestKnob,
                               final PolicyEnforcementContext context,
                               final ResourceResponseAssertion.ResourceData resource ) throws IOException {

        HttpServletRequest req = httpRequestKnob.getHttpServletRequest();
        String serviceUrl = context.getService().getRoutingUri();  // use routing uri to skip any import name
        if ( serviceUrl==null || serviceUrl.isEmpty() ) {
            serviceUrl = "/service/" + context.getService().getOid() ;
        } else if ( serviceUrl.endsWith( "*" )) {
            serviceUrl = serviceUrl.substring( 0, serviceUrl.length()-1 );   
        }
        try {
            serviceUrl = new URI(req.getRequestURL().toString()).resolve(serviceUrl).toString();
        } catch ( Exception e ) {
            logger.warning("Unable to determine absolute URL for resource response '"+ ExceptionUtils.getMessage(e)+"'.");
        }

        final Document wsdlDoc;
        try {
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream( new StringReader(resource.getContent()) );
            inputSource.setSystemId( resource.getUri() );
            wsdlDoc = XmlUtil.parse( inputSource, false );
        } catch ( SAXException e ) {
            logger.log( Level.WARNING, "Error parsing resource '"+ExceptionUtils.getMessage( e )+"'", ExceptionUtils.getDebugException(e ));
            throw new AssertionStatusException( AssertionStatus.FAILED );
        }
        rewriteReferences(resource.getId(), wsdlDoc, assertion.getResources(), serviceUrl);

        // output the wsdl with appropriate headers
        final Message response = context.getResponse();
        response.initialize( ContentTypeHeader.XML_DEFAULT, XmlUtil.nodeToString( wsdlDoc ).getBytes( ContentTypeHeader.XML_DEFAULT.getEncoding() ) );

        final HttpResponseKnob httpResponseKnob = response.getKnob( HttpResponseKnob.class );
        if ( httpResponseKnob != null ) {
            httpResponseKnob.setStatus( 200 );
            httpResponseKnob.addHeader( HttpConstants.HEADER_LAST_MODIFIED, lastModifiedText );
            httpResponseKnob.addHeader( HttpConstants.HEADER_ETAG, eTag(resource) );
        }
    }

    private void send404Response( final PolicyEnforcementContext context ) throws IOException {
        final Message response = context.getResponse();
        response.initialize( ContentTypeHeader.TEXT_DEFAULT, "Not Found".getBytes( ContentTypeHeader.TEXT_DEFAULT.getEncoding() ) );

        final HttpResponseKnob httpResponseKnob = response.getKnob( HttpResponseKnob.class );
        if ( httpResponseKnob != null ) {
            httpResponseKnob.setStatus( 404 );   
        }
    }

    /**
     * Rewrite any dependency references (schema/wsdl) in the given doc to request from the gateway.
     */
    private void rewriteReferences( final String resourceId,
                                    final Document resourceDoc,
                                    final ResourceResponseAssertion.ResourceData[] resources,
                                    final String requestUri ) {
        if ( resources != null && resources.length > 0 ) {
            final DocumentReferenceProcessor documentReferenceProcessor = new DocumentReferenceProcessor();
            documentReferenceProcessor.processDocumentReferences( resourceDoc, new DocumentReferenceProcessor.ReferenceCustomizer() {
                @Override
                public String customize( final Document document,
                                         final Node node,
                                         final String documentUrl,
                                         final DocumentReferenceProcessor.ReferenceInfo referenceInfo ) {
                    String uri = null;

                    if ( documentUrl != null && referenceInfo.getReferenceUrl() != null ) {
                        try {
                            URI base = new URI(documentUrl);
                            String docUrl = base.resolve(new URI(referenceInfo.getReferenceUrl())).toString();
                            for ( ResourceResponseAssertion.ResourceData serviceDocument : resources ) {
                                if ( docUrl.equals(serviceDocument.getUri()) ) {
                                    uri = requestUri + getSuffix(serviceDocument) + "?" +
                                            PARAM_RESOURCE_ID + "=" + serviceDocument.getId();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            logger.log( Level.WARNING, "Error rewriting url for resource '"+resourceId+"'..", e );
                        }
                    }

                    return uri;
                }
            } );
        }
    }

    private String getSuffix( final ResourceResponseAssertion.ResourceData resource  ) {
        String suffix = "";
        if ( assertion.isUsePath() ) {
            suffix = "/" + getName(resource);
        }
        return suffix;
    }

    /**
     * Create a "user-friendly" display name for the document.
     */
    private String getName( final ResourceResponseAssertion.ResourceData resource ) {
        String name = resource.getUri();

        int index = name.lastIndexOf('/');
        if ( index >= 0 ) {
            name = name.substring( index+1 );
        }

        index = name.indexOf('?');
        if ( index >= 0 ) {
            name = name.substring( 0, index );
        }

        index = name.indexOf('#');
        if ( index >= 0 ) {
            name = name.substring( 0, index );
        }

        String permittedCharacters = ValidationUtils.ALPHA_NUMERIC  + "_-.";
        StringBuilder nameBuilder = new StringBuilder();
        for ( char nameChar : name.toCharArray() ) {
            if ( permittedCharacters.indexOf(nameChar) >= 0 ) {
                nameBuilder.append( nameChar );
            }
        }

        return nameBuilder.toString();
    }

}
