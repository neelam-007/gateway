package com.l7tech.server.ems.gateway;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.saml.Attribute;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.util.Config;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.w3c.dom.Document;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 */
public class GatewayTrustTokenFactoryImpl implements GatewayTrustTokenFactory {

    //- PUBLIC

    public GatewayTrustTokenFactoryImpl( final Config config,
                                         final EsmSecurityManager emsSecurityManager,
                                         final UserPropertyManager userPropertyManager,
                                         final DefaultKey defaultKey ) {
        this.config = config;
        this.emsSecurityManager = emsSecurityManager;
        this.userPropertyManager = userPropertyManager;            
        this.defaultKey = defaultKey;            
    }

    @Override
    public String getTrustToken() throws GatewayException {
        HttpSession session = ((WebRequest)RequestCycle.get().getRequest()).getHttpServletRequest().getSession(true);
        EsmSecurityManager.LoginInfo info = emsSecurityManager.getLoginInfo(session);
        User user = info == null ?  null : info.getUser();
        if ( user == null ) {
            throw new GatewayException("Not authenticated.");            
        }
        return getTrustToken( user );
    }

    /**
     * Get a trust token for use by the given user.
     *
     * @param user The user establishing trust.
     * @return The trust token
     */
    @Override
    public String getTrustToken( final User user ) throws GatewayException {
        try {
            String userUuid;
            Map<String,String> properties = userPropertyManager.getUserProperties(user);
            if ( !properties.containsKey( GatewayConsts.PROP_USER_UUID ) ) {
                userUuid = UUID.randomUUID().toString();
                properties.put( GatewayConsts.PROP_USER_UUID, userUuid );
                userPropertyManager.saveUserProperties( user, properties );                
            } else {
                userUuid = properties.get( GatewayConsts.PROP_USER_UUID );
            }

            SamlAssertionGenerator generator = new SamlAssertionGenerator( defaultKey.getSslInfo().getSignerInfo() );
            SamlAssertionGenerator.Options options = new SamlAssertionGenerator.Options();
            options.setVersion(SamlAssertionGenerator.Options.VERSION_2);
            options.setSignAssertion(true);
            options.setExpiryMinutes(24*60); // Good for one day (including some skew)
            options.setBeforeOffsetMinutes(60); // Allow up to one hour of clock skew
            options.setProofOfPosessionRequired(false);
            options.setIssuerKeyInfoType(KeyInfoInclusionType.CERT);

            Attribute[] attributes = new Attribute[] {
                    new Attribute("EM-UUID", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic", config.getProperty("em.server.id", "")),
                    new Attribute("EM-USER-UUID", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic", userUuid),
                    new Attribute("EM-USER-DESC", "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified", user.getLogin()),
            };

            SubjectStatement statement = SubjectStatement.createAttributeStatement(
                    LoginCredentials.makePasswordCredentials( user.getLogin(), new char[0], Object.class ),  // credentials are required but not used
                    SubjectStatement.BEARER,
                    attributes,
                    KeyInfoInclusionType.CERT,
                    NameIdentifierInclusionType.SPECIFIED,
                    null, null, null);
            Document assertionDocument = generator.createAssertion(statement, options);
            return XmlUtil.nodeToString(assertionDocument);
        } catch ( ObjectModelException ome ) {
            logger.log( Level.WARNING, "Error processing user properties.", ome );
            throw new GatewayException( "Error generating token." );
        } catch ( IOException ioe ) {
            logger.log( Level.WARNING, "Error accessing signer.", ioe );
            throw new GatewayException( "Error generating token." );
        } catch (SignatureException se) {
            logger.log( Level.WARNING, "Error signing token.", se );
            throw new GatewayException( "Error generating token." );
        } catch (CertificateException ce) {
            logger.log( Level.WARNING, "Error with signing certificate.", ce );
            throw new GatewayException( "Error generating token." );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( GatewayTrustTokenFactoryImpl.class.getName() );

    private final Config config;
    private final EsmSecurityManager emsSecurityManager;
    private final UserPropertyManager userPropertyManager;
    private final DefaultKey defaultKey;
}
