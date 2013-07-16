package com.l7tech.external.assertions.ldapupdate.server.resource;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test the JAXBResourceUnmarshaller
 *
 * @author rraquepo
 */
public class DefaultJAXBResourceUnmarshallerTest {

    @Test
    public void testSingleOperation() throws Exception {
        try {
            JAXBResourceUnmarshaller unmarshaller = DefaultJAXBResourceUnmarshaller.getInstance();
            final LDAPOperation operation = (LDAPOperation) unmarshaller.unmarshal(CREATE_OPERATION_XML, Resource.class);
            assertEquals(operation.getOperation(), "CREATE");
            assertEquals(operation.getDn(), "uid=chard2,ou=orgunit1,dc=l7tech,dc=com");
            assertEquals(operation.getAttributes().getAttributes().size(), 4);
            OperationAttribute attribute = operation.getAttributes().getAttributes().get(0);
            assertEquals(attribute.getValues().getValues().size(), 4);
            assertEquals(attribute.getValues().getValues().get(0), "top");
            assertEquals(attribute.getValues().getValues().get(1), "person");
            assertEquals(attribute.getValues().getValues().get(2), "inetOrgPerson");
            assertEquals(attribute.getValues().getValues().get(3), "organizationalPerson");
        } catch (Exception e) {
            fail("Should not fail");
        }
    }

    @Test
    public void testMultipleOperation() throws Exception {
        try {
            JAXBResourceUnmarshaller unmarshaller = DefaultJAXBResourceUnmarshaller.getInstance();
            final LDAPOperations operations = (LDAPOperations) unmarshaller.unmarshal(MULTPLE_OPERATION_XML, Resource.class);
            assertEquals(operations.getOperations().size(), 3);
            assertEquals(operations.getOperations().get(0).getOperation(), "DELETE");
            assertEquals(operations.getOperations().get(0).getDn(), "uid=chard1,ou=orgunit1,dc=l7tech,dc=com");
            assertEquals(operations.getOperations().get(1).getOperation(), "UPDATE");
            assertEquals(operations.getOperations().get(1).getDn(), "uid=chard2,ou=orgunit1,dc=l7tech,dc=com");
            assertEquals(operations.getOperations().get(2).getOperation(), "CREATE");
            assertEquals(operations.getOperations().get(2).getDn(), "uid=chard3,ou=orgunit1,dc=l7tech,dc=com");
        } catch (Exception e) {
            fail("Should not fail");
        }

    }

    private final static String CREATE_OPERATION_XML = "<l7:LDAPOperation xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:operation>CREATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t<l7:attributes>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>objectClass</l7:name>\n" +
            "\t\t\t<l7:values>\n" +
            "\t\t\t<l7:value>top</l7:value>\n" +
            "\t\t\t<l7:value>person</l7:value>\n" +
            "\t\t\t<l7:value>inetOrgPerson</l7:value>\n" +
            "\t\t\t<l7:value>organizationalPerson</l7:value>\n" +
            "\t\t\t</l7:values>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>sn</l7:name>\n" +
            "\t\t\t<l7:value>Richard</l7:value>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\n" +
            "\t\t\t<l7:name>cn</l7:name>\n" +
            "\t\t\t<l7:value>chard2</l7:value>\n" +
            "\t\t</l7:attribute>\n" +
            "\t\t<l7:attribute>\t\t\t\n" +
            "\t\t\t<l7:name>userPassword</l7:name>\n" +
            "\t\t\t<l7:value>password</l7:value>\n" +
            "\t\t</l7:attribute>\t\n" +
            "\t</l7:attributes>\n" +
            "</l7:LDAPOperation>";

    private final static String MULTPLE_OPERATION_XML = "<l7:LDAPOperations xmlns:l7=\"http://ns.l7tech.com/2013/01/ldap-manage\">\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>DELETE</l7:operation>\n" +
            "\t<l7:dn>uid=chard1,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t</l7:LDAPOperation>\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>UPDATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard2,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t</l7:LDAPOperation>\n" +
            "\t<l7:LDAPOperation>\n" +
            "\t<l7:operation>CREATE</l7:operation>\n" +
            "\t<l7:dn>uid=chard3,ou=orgunit1,dc=l7tech,dc=com</l7:dn>\n" +
            "\t</l7:LDAPOperation>\n" +
            "</l7:LDAPOperations>";
}
