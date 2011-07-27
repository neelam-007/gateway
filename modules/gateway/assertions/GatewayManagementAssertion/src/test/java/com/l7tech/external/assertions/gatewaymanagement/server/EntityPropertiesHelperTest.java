package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.IdentityProviderMO.IdentityProviderType;
import com.l7tech.gateway.api.JMSConnection.JMSProviderType;
import com.l7tech.gateway.api.ListenPortMO.TlsSettings.ClientAuthentication;
import com.l7tech.gateway.api.PolicyDetail.PolicyType;
import com.l7tech.gateway.api.PolicyImportResult.ImportedPolicyReferenceType;
import com.l7tech.gateway.api.PolicyReferenceInstruction.PolicyReferenceInstructionType;
import com.l7tech.gateway.api.PolicyValidationResult.ValidationStatus;
import com.l7tech.gateway.common.security.SpecialKeyType;
import com.l7tech.gateway.common.transport.SsgConnector.Endpoint;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig.UserCertificateUseType;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.util.BeanUtils;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import org.junit.Test;
import static org.junit.Assert.*;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 */
public class EntityPropertiesHelperTest {

    @Test
    public void testBeanProperties() throws Exception {
        final EntityPropertiesHelper helper = new EntityPropertiesHelper();

        for ( final EntityType baseType : EntityType.values() ) {
            for ( final Class<? extends Entity> entityClass : explode(baseType) ) {
                if ( entityClass != null && !helper.getPropertiesMap( entityClass ).isEmpty() ) {
                    System.out.println("Testing properties for : " + entityClass.getName());
                    final Collection<String> ignoredProperties = helper.getIgnoredProperties(entityClass);
                    final Collection<String> writeOnlyProperties = helper.getWriteOnlyProperties(entityClass);
                    final Collection<String> defaultProperties = helper.getPropertyDefaultsMap(entityClass).keySet();
                    final Map<String,String> propertyMapping = helper.getPropertiesMap(entityClass);

                    assertTrue( "Invalid write only property for " + entityClass.getName(), propertyMapping.keySet().containsAll( writeOnlyProperties ));
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
        testXmlEnumValueAnnotations( Endpoint.class,
                "Published service message input",
                "Policy Manager access",
                "Enterprise Manager access",
                "Administrative access",
                "Browser-based administration",
                "Policy download service",
                "WS-Trust security token service",
                "Certificate signing service",
                "Password changing service",
                "WSDL download service",
                "SNMP Query service",
                "HP SOA Manager agent service",
                "Built-in services",
                "Node Control",
                "Inter-Node Communication" );

        testXmlEnumValueAnnotations( IdentityProviderType.class,
                "Internal",
                "LDAP",
                "Federated" );

        testXmlEnumValueAnnotations( JMSProviderType.class,
                "TIBCO EMS",
                "WebSphere MQ over LDAP",
                "FioranoMQ" );

        testXmlEnumValueAnnotations( ClientAuthentication.class,
                "None",
                "Optional",
                "Required" );

        testXmlEnumValueAnnotations( PolicyType.class,
                "Include",
                "Internal",
                "Global" );

        testXmlEnumValueAnnotations( ImportedPolicyReferenceType.class,
                "Created",
                "Mapped" );

        testXmlEnumValueAnnotations( PolicyReferenceInstructionType.class,
                "Delete",
                "Ignore",
                "Map",
                "Rename" );

        testXmlEnumValueAnnotations( ValidationStatus.class,
                "OK",
                "Warning",
                "Error" );

        testXmlEnumValueAnnotations( SpecialKeyType.class,
                "Default SSL Key",
                "Default CA Key",
                "Audit Viewer Key",
                "Audit Signing Key" );

        testXmlEnumValueAnnotations( UserCertificateUseType.class,
                "None",
                "Index",
                "Custom Index",
                "Search" );

        testXmlEnumValueAnnotations( CertificateValidationType.class,
                "Validate",
                "Validate Certificate Path",
                "Revocation Checking" );
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
                    LdapIdentityProviderConfig.class
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
