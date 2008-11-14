package com.l7tech.server.ems.gateway;

import com.l7tech.identity.User;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.security.saml.SamlAssertionGenerator;
import com.l7tech.security.saml.SubjectStatement;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.Attribute;
import com.l7tech.security.xml.KeyInfoInclusionType;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.util.Config;
import com.l7tech.common.io.XmlUtil;

import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.security.SignatureException;
import java.security.cert.CertificateException;

import org.w3c.dom.Document;

/**
 * 
 */
public class GatewayTrustTokenFactoryImpl implements GatewayTrustTokenFactory {

    //- PUBLIC

    public GatewayTrustTokenFactoryImpl( final Config config,
                                     final UserPropertyManager userPropertyManager,
                                     final DefaultKey defaultKey ) {
        this.config = config;
        this.userPropertyManager = userPropertyManager;            
        this.defaultKey = defaultKey;            
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
            options.setExpiryMinutes(5);
            options.setBeforeOffsetMinutes(1);
            options.setId(UUID.randomUUID().toString());
            options.setProofOfPosessionRequired(false);
            options.setIssuerKeyInfoType(KeyInfoInclusionType.CERT);

            Attribute[] attributes = new Attribute[] {
                    new Attribute("EM-UUID", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic", config.getProperty("em.server.id", "")),
                    new Attribute("EM-USER-UUID", "urn:oasis:names:tc:SAML:2.0:attrname-format:basic", userUuid),
                    new Attribute("EM-USER-DESC", "urn:oasis:names:tc:SAML:2.0:attrname-format:unspecified", user.getEmail()!=null ? user.getEmail() : user.getLogin()),
            };

            SubjectStatement statement = SubjectStatement.createAttributeStatement(
                    LoginCredentials.makePasswordCredentials( user.getLogin(), new char[0], Object.class ),  // credentials are required but not used
                    SubjectStatement.BEARER,
                    attributes,
                    KeyInfoInclusionType.CERT,
                    NameIdentifierInclusionType.NONE,
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

    private Config config;
    private UserPropertyManager userPropertyManager;
    private DefaultKey defaultKey;
}
