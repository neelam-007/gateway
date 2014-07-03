package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.IdentityProviderMO.IdentityProviderType;
import com.l7tech.gateway.api.JMSConnection.JMSProviderType;
import com.l7tech.gateway.api.ListenPortMO.TlsSettings.ClientAuthentication;
import com.l7tech.gateway.api.PolicyDetail.PolicyType;
import com.l7tech.gateway.api.PolicyImportResult.ImportedPolicyReferenceType;
import com.l7tech.gateway.api.PolicyReferenceInstruction.PolicyReferenceInstructionType;
import com.l7tech.gateway.api.PolicyValidationResult.ValidationStatus;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.security.password.SecurePassword.SecurePasswordType;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.BindOnlyLdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig.UserCertificateUseType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.Test;

import java.beans.PropertyDescriptor;
import java.util.*;

import static org.junit.Assert.*;

/**
 * 
 */
public class EntityPropertiesHelperTest {

    @Test
    public void testBeanProperties() throws Exception {
        final EntityPropertiesHelper helper = new EntityPropertiesHelper();

        for ( final EntityType baseType : EntityType.values() ) {
            for ( final Class<? extends Entity> entityClass : explode(baseType) ) {
                if ( entityClass != null && (!helper.getPropertiesMap( entityClass ).isEmpty() || !helper.getIgnoredProperties( entityClass ).isEmpty()) ) {
                    System.out.println("Testing properties for : " + entityClass.getName());
                    final Collection<String> ignoredProperties = helper.getIgnoredProperties(entityClass);
                    final Collection<String> passwordProperties = helper.getPasswordProperties(entityClass);
                    final Collection<String> defaultProperties = helper.getPropertyDefaultsMap(entityClass).keySet();
                    final Map<String,String> propertyMapping = helper.getPropertiesMap(entityClass);

                    assertTrue( "Invalid write only property for " + entityClass.getName(), propertyMapping.keySet().containsAll( passwordProperties ));
                    assertTrue( "Invalid default property for " + entityClass.getName(), propertyMapping.keySet().containsAll( defaultProperties ));

                    final Set<PropertyDescriptor> properties = BeanUtils.omitProperties(
                            BeanUtils.getProperties( entityClass ),
                            ignoredProperties.toArray(new String[ignoredProperties.size()]));

                    for ( PropertyDescriptor prop : properties ) {
                        if ( !propertyMapping.containsKey(prop.getName()) ) {
                            dumpProperties( properties );
                            fail( "Unknown entity property '"+prop.getName()+"' for entity '" + entityClass.getName() + "'."  );
                        }
                        final Class<?> returnType = prop.getReadMethod().getReturnType();
                        if ( Boolean.TYPE.isAssignableFrom( returnType ) ||
                             Boolean.class.isAssignableFrom( returnType ) ||
                             Integer.TYPE.isAssignableFrom( returnType ) ||
                             Integer.class.isAssignableFrom( returnType ) ||
                             Long.TYPE.isAssignableFrom( returnType ) ||
                             Long.class.isAssignableFrom( returnType ) ||
                             String.class.isAssignableFrom( returnType ) ||
                             Date.class.isAssignableFrom( returnType ) ||
                             Enum.class.isAssignableFrom( returnType ) ) {
                            // OK
                        } else {
                            fail("Unsupported property type '" + returnType +"' for entity '" + entityClass.getName() + "', property '"+prop.getName()+"'."  );
                        }
                    }

                }
            }
        }
    }

    /**
     * Verify that enum annotations do not change, these are part of the API.
     */
    @Test
    public void xmlEnumValueTest() throws Exception {
        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( Endpoint.class,
                "Published service message input",
                "Policy Manager access",
                "Enterprise Manager access",
                "Administrative access",
                "Browser-based administration",
                "Policy download service",
                "Ping service",
                "WS-Trust security token service",
                "Certificate signing service",
                "Password changing service",
                "WSDL download service",
                "SNMP Query service",
                "Built-in services",
                "Node Control",
                "Inter-Node Communication" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( IdentityProviderType.class,
                "Internal",
                "LDAP",
                "Federated",
                "Simple LDAP",
                // Serialization should not break if a new enum type is added.
                // Old clients that do not expect to see Policy-Backed will likely throw an error saying unknown type.
                // for example The GMC will output: 'cvc-enumeration-valid: Value 'New Type Unknown' is not facet-valid with respect to enumeration '[Internal, LDAP, Federated, Simple LDAP, Policy-Backed]'. It must be a value from the enumeration.'
                "Policy-Backed");

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( JMSProviderType.class,
                "TIBCO EMS",
                "WebSphere MQ over LDAP",
                "FioranoMQ",
                "WebLogic JMS");

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( ClientAuthentication.class,
                "None",
                "Optional",
                "Required" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( PolicyType.class,
                "Include",
                "Internal",
                "Global",
                // Serialization should not break if a new enum type is added.
                // Old clients that do not expect to see "Identity Provider" will likely throw an error saying unknown type.
                // for example The GMC will output: 'cvc-enumeration-valid: Value 'New Type Unknown' is not facet-valid with respect to enumeration '[Include, Internal, Global, Identity Provider]'. It must be a value from the enumeration.'
                "Identity Provider");

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( ImportedPolicyReferenceType.class,
                "Created",
                "Mapped" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( PolicyReferenceInstructionType.class,
                "Delete",
                "Ignore",
                "Map",
                "Rename" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( ValidationStatus.class,
                "OK",
                "Warning",
                "Error" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( SpecialKeyType.class,
                "Default SSL Key",
                "Default CA Key",
                "Audit Viewer Key",
                "Audit Signing Key" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( UserCertificateUseType.class,
                "None",
                "Index",
                "Custom Index",
                "Search" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( CertificateValidationType.class,
                "Validate",
                "Validate Certificate Path",
                "Revocation Checking" );

        //NOTE : Do not change values, read javadoc above
        testXmlEnumValueAnnotations( SecurePasswordType.class,
                "Password",
                "PEM Private Key" );
    }

    private <E extends Enum<E>> void testXmlEnumValueAnnotations( final Class<E> enumType,
                                                                  final String... values ) throws Exception {
        assertEquals( "Enum value count for " + enumType, EnumSet.allOf( enumType ).size(), values.length );

        for ( final String value : values ) {
            assertNotNull( "Null value for: " + value, EntityPropertiesHelper.getEnumValue( enumType, value ) );
        }
    }

    private Collection<Class<? extends Entity>> explode( final EntityType type ) {
        if ( type == EntityType.ID_PROVIDER_CONFIG ) {
            return CollectionUtils.<Class<? extends Entity>>list(
                    FederatedIdentityProviderConfig.class,
                    IdentityProviderConfig.class,
                    LdapIdentityProviderConfig.class,
                    BindOnlyLdapIdentityProviderConfig.class
                    // TODO PolicyBackedIdentityProvider.class
            );
        } else {
            return Collections.<Class<? extends Entity>>singleton( type.getEntityClass() );
        }
    }

    private void dumpProperties( final Set<PropertyDescriptor> properties ) {
        System.out.println( "Properties are:" );
        final Set<String> names = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        names.addAll( Functions.map( properties, new Functions.Unary<String,PropertyDescriptor>(){
            @Override
            public String call( final PropertyDescriptor propertyDescriptor ) {
                return propertyDescriptor.getName();
            }
        } ) );
        for ( final String name : names ) {
            System.out.println( name );
        }
    }
}
