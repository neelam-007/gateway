/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 17, 2009
 * Time: 2:09:45 PM
 */
package com.l7tech.util;

import org.junit.Test;
import org.junit.Assert;

import java.io.IOException;
import java.io.StringReader;

import static org.junit.Assert.assertEquals;

public class SqlUtilsTest {

    @Test
    public void testNullChar() {
        String s = "\u0000";
        String expectedVal = "";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testBackSlashRChar() {
        String s = "\r";
        String expectedVal = "\\\\r";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testBackSlashNChar() {
        String s = "\n";
        String expectedVal = "\\\\n";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testSubChar() {
        String s = "\u001a";
        String expectedVal = "";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testDoubleQuoteChar() {
        String s = "\"";
        String expectedVal = "\\\"";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testSingleQuoteChar() {
        String s = "'";
        String expectedVal = "\\'";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

    @Test
    public void testBackSlashChar() {
        String s = "\\";
        String expectedVal = "\\\\";
        String actualValue = SqlUtils.mySqlEscapeIllegalSqlChars(s);
        Assert.assertEquals("Expected: " + expectedVal + " actual: " + actualValue, actualValue, expectedVal);
    }

        @Test
    public void testParseSqlStatement() throws Exception {
        testSingleStatement( "Simple", "SELECT * from test;", "SELECT * from test" );
        testSingleStatement( "Insert",
                "INSERT INTO rbac_role VALUES (-1050,0,'Manage Secure Passwords', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete any stored password.');",
                "INSERT INTO rbac_role VALUES (-1050,0,'Manage Secure Passwords', null,null,null, 'Users assigned to the {0} role have the ability to read, create, update and delete any stored password.')");
        testSingleStatement( "Complex insert",
                "INSERT INTO `rbac_permission` VALUES\n" +
                "    (-1101,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1102,0,-1100,'DELETE',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1103,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1104,0,-1100,'READ',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1105,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1106,0,-1100,'UPDATE',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1107,0,-1100,'CREATE',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1108,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1109,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1110,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1111,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1112,0,-1100,'UPDATE',NULL,'SSG_KEYSTORE'),\n" +
                "    (-1113,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1114,0,-1100,'READ',NULL,'SSG_KEYSTORE');",
                "INSERT INTO `rbac_permission` VALUES\n" +
                "    (-1101,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1102,0,-1100,'DELETE',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1103,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1104,0,-1100,'READ',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1105,0,-1100,'READ',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1106,0,-1100,'UPDATE',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1107,0,-1100,'CREATE',NULL,'SSG_KEY_ENTRY'),\n" +
                "    (-1108,0,-1100,'UPDATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1109,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1110,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1111,0,-1100,'CREATE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1112,0,-1100,'UPDATE',NULL,'SSG_KEYSTORE'),\n" +
                "    (-1113,0,-1100,'DELETE',NULL,'CLUSTER_PROPERTY'),\n" +
                "    (-1114,0,-1100,'READ',NULL,'SSG_KEYSTORE')");
        testSingleStatement( "Table create",
                "CREATE TABLE trusted_cert (\n" +
                "  objectid bigint NOT NULL,\n" +
                "  version integer NOT NULL,\n" +
                "  name varchar(128) NOT NULL,\n" +
                "  subject_dn varchar(500),\n" +
                "  cert_base64 mediumtext NOT NULL,\n" +
                "  trusted_for_ssl tinyint(1) default '0',\n" +
                "  trusted_for_client tinyint(1) default '0',\n" +
                "  trusted_for_server tinyint(1) default '0',\n" +
                "  trusted_for_saml tinyint(1) default '0',\n" +
                "  trusted_as_saml_attesting_entity tinyint(1) default '0',\n" +
                "  verify_hostname tinyint(1) default '0',\n" +
                "  thumbprint_sha1 varchar(64),\n" +
                "  ski varchar(64) default '',\n" + // test default ''
                "  trust_anchor tinyint default 1,\n" +
                "  revocation_type varchar(128) NOT NULL DEFAULT 'USE_DEFAULT',\n" +
                "  revocation_policy_oid bigint(20),\n" +
                "  issuer_dn varchar(500),\n" +
                "  serial varchar(64),\n" +
                "  PRIMARY KEY (objectid),\n" +
                "  UNIQUE i_thumb (thumbprint_sha1),\n" +
                "  INDEX i_ski (ski),\n" +
                "  INDEX i_subject_dn (subject_dn(255)),\n" +
                "  INDEX i_issuer_dn (issuer_dn(255)),\n" +
                "  FOREIGN KEY (revocation_policy_oid) REFERENCES revocation_check_policy (objectid)\n" +
                ") ENGINE=InnoDB DEFAULT CHARACTER SET utf8;",
                "CREATE TABLE trusted_cert (\n" +
                "  objectid bigint NOT NULL,\n" +
                "  version integer NOT NULL,\n" +
                "  name varchar(128) NOT NULL,\n" +
                "  subject_dn varchar(500),\n" +
                "  cert_base64 mediumtext NOT NULL,\n" +
                "  trusted_for_ssl tinyint(1) default '0',\n" +
                "  trusted_for_client tinyint(1) default '0',\n" +
                "  trusted_for_server tinyint(1) default '0',\n" +
                "  trusted_for_saml tinyint(1) default '0',\n" +
                "  trusted_as_saml_attesting_entity tinyint(1) default '0',\n" +
                "  verify_hostname tinyint(1) default '0',\n" +
                "  thumbprint_sha1 varchar(64),\n" +
                "  ski varchar(64) default '',\n" +
                "  trust_anchor tinyint default 1,\n" +
                "  revocation_type varchar(128) NOT NULL DEFAULT 'USE_DEFAULT',\n" +
                "  revocation_policy_oid bigint(20),\n" +
                "  issuer_dn varchar(500),\n" +
                "  serial varchar(64),\n" +
                "  PRIMARY KEY (objectid),\n" +
                "  UNIQUE i_thumb (thumbprint_sha1),\n" +
                "  INDEX i_ski (ski),\n" +
                "  INDEX i_subject_dn (subject_dn(255)),\n" +
                "  INDEX i_issuer_dn (issuer_dn(255)),\n" +
                "  FOREIGN KEY (revocation_policy_oid) REFERENCES revocation_check_policy (objectid)\n" +
                ") ENGINE=InnoDB DEFAULT CHARACTER SET utf8");
        testSingleStatement( "Complex escapes",
                "INSERT INTO table VALUES 'quoted\\'st\\nring with a ; in it\\\\\\\\';",
                "INSERT INTO table VALUES 'quoted\\'st\\nring with a ; in it\\\\\\\\'" );
        testSingleStatement( "Complex content",
                "INSERT INTO table VALUES 'quoted\\'st\\nring with a ; in it';",
                "INSERT INTO table VALUES 'quoted\\'st\\nring with a ; in it'" );
        testSingleStatement( "XML content small",
                "INSERT INTO identity_provider (objectid,name,description,type,properties,version) VALUES (-2,'Internal Identity Provider','Internal Identity Provider',1,'<java version=\"1.6.0_01\" class=\"java.beans.XMLDecoder\"><object class=\"java.util.HashMap\"><void method=\"put\"><string>adminEnabled</string><boolean>true</boolean></void></object></java>',0);",
                "INSERT INTO identity_provider (objectid,name,description,type,properties,version) VALUES (-2,'Internal Identity Provider','Internal Identity Provider',1,'<java version=\"1.6.0_01\" class=\"java.beans.XMLDecoder\"><object class=\"java.util.HashMap\"><void method=\"put\"><string>adminEnabled</string><boolean>true</boolean></void></object></java>',0)");
        testSingleStatement( "XML content",
                "INSERT INTO table (objectid, version, content) VALUES (-5,0,'<?xml version=\\'1.0\\'?>\\n<xs:schema targetNamespace=\\\"http://www.w3.org/XML/1998/namespace\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" xml:lang=\\\"en\\\">\\n\\n <xs:annotation>\\n  <xs:documentation>In keeping with the XML Schema WG\\'s standard versioning\\n   policy, this schema document will persist at\\n   http://www.w3.org/2007/08/xml.xsd.\\n   At the date of issue it can also be found at\\n   http://www.w3.org/2001/xml.xsd.\\n   The schema document at that URI may however change in the future,\\n   in order to remain compatible with the latest version of XML Schema\\n   itself, or with the XML namespace itself.  In other words, if the XML\\n   Schema or XML namespaces change, the version of this document at\\n   http://www.w3.org/2001/xml.xsd will change\\n   accordingly; the version at\\n   http://www.w3.org/2007/08/xml.xsd will not change.\\n  </xs:documentation>\\n </xs:annotation>\\n\\n <xs:attribute name=\\\"lang\\\">\\n  <xs:annotation>\\n   <xs:documentation>Attempting to install the relevant ISO 2- and 3-letter\\n         codes as the enumerated possible values is probably never\\n         going to be a realistic possibility.  See\\n         RFC 3066 at http://www.ietf.org/rfc/rfc3066.txt and the IANA registry\\n         at http://www.iana.org/assignments/lang-tag-apps.htm for\\n         further information.\\n\\n         The union allows for the \\'un-declaration\\' of xml:lang with\\n         the empty string.</xs:documentation>\\n  </xs:annotation>\\n  <xs:simpleType>\\n   <xs:union memberTypes=\\\"xs:language\\\">\\n    <xs:simpleType>    \\n     <xs:restriction base=\\\"xs:string\\\">\\n      <xs:enumeration value=\\\"\\\"/>\\n     </xs:restriction>\\n    </xs:simpleType>\\n   </xs:union>\\n  </xs:simpleType>\\n </xs:attribute>\\n\\n <xs:attribute name=\\\"space\\\">\\n  <xs:simpleType>\\n   <xs:restriction base=\\\"xs:NCName\\\">\\n    <xs:enumeration value=\\\"default\\\"/>\\n    <xs:enumeration value=\\\"preserve\\\"/>\\n   </xs:restriction>\\n  </xs:simpleType>\\n </xs:attribute>\\n\\n <xs:attribute name=\\\"base\\\" type=\\\"xs:anyURI\\\">\\n  <xs:annotation>\\n   <xs:documentation>See http://www.w3.org/TR/xmlbase/ for\\n                     information about this attribute.</xs:documentation>\\n  </xs:annotation>\\n </xs:attribute>\\n \\n <xs:attribute name=\\\"id\\\" type=\\\"xs:ID\\\">\\n  <xs:annotation>\\n   <xs:documentation>See http://www.w3.org/TR/xml-id/ for\\n                     information about this attribute.</xs:documentation>\\n  </xs:annotation>\\n </xs:attribute>\\n\\n <xs:attributeGroup name=\\\"specialAttrs\\\">\\n  <xs:attribute ref=\\\"xml:base\\\"/>\\n  <xs:attribute ref=\\\"xml:lang\\\"/>\\n  <xs:attribute ref=\\\"xml:space\\\"/>\\n  <xs:attribute ref=\\\"xml:id\\\"/>\\n </xs:attributeGroup>\\n\\n</xs:schema>');",
                "INSERT INTO table (objectid, version, content) VALUES (-5,0,'<?xml version=\\'1.0\\'?>\\n<xs:schema targetNamespace=\\\"http://www.w3.org/XML/1998/namespace\\\" xmlns:xs=\\\"http://www.w3.org/2001/XMLSchema\\\" xml:lang=\\\"en\\\">\\n\\n <xs:annotation>\\n  <xs:documentation>In keeping with the XML Schema WG\\'s standard versioning\\n   policy, this schema document will persist at\\n   http://www.w3.org/2007/08/xml.xsd.\\n   At the date of issue it can also be found at\\n   http://www.w3.org/2001/xml.xsd.\\n   The schema document at that URI may however change in the future,\\n   in order to remain compatible with the latest version of XML Schema\\n   itself, or with the XML namespace itself.  In other words, if the XML\\n   Schema or XML namespaces change, the version of this document at\\n   http://www.w3.org/2001/xml.xsd will change\\n   accordingly; the version at\\n   http://www.w3.org/2007/08/xml.xsd will not change.\\n  </xs:documentation>\\n </xs:annotation>\\n\\n <xs:attribute name=\\\"lang\\\">\\n  <xs:annotation>\\n   <xs:documentation>Attempting to install the relevant ISO 2- and 3-letter\\n         codes as the enumerated possible values is probably never\\n         going to be a realistic possibility.  See\\n         RFC 3066 at http://www.ietf.org/rfc/rfc3066.txt and the IANA registry\\n         at http://www.iana.org/assignments/lang-tag-apps.htm for\\n         further information.\\n\\n         The union allows for the \\'un-declaration\\' of xml:lang with\\n         the empty string.</xs:documentation>\\n  </xs:annotation>\\n  <xs:simpleType>\\n   <xs:union memberTypes=\\\"xs:language\\\">\\n    <xs:simpleType>    \\n     <xs:restriction base=\\\"xs:string\\\">\\n      <xs:enumeration value=\\\"\\\"/>\\n     </xs:restriction>\\n    </xs:simpleType>\\n   </xs:union>\\n  </xs:simpleType>\\n </xs:attribute>\\n\\n <xs:attribute name=\\\"space\\\">\\n  <xs:simpleType>\\n   <xs:restriction base=\\\"xs:NCName\\\">\\n    <xs:enumeration value=\\\"default\\\"/>\\n    <xs:enumeration value=\\\"preserve\\\"/>\\n   </xs:restriction>\\n  </xs:simpleType>\\n </xs:attribute>\\n\\n <xs:attribute name=\\\"base\\\" type=\\\"xs:anyURI\\\">\\n  <xs:annotation>\\n   <xs:documentation>See http://www.w3.org/TR/xmlbase/ for\\n                     information about this attribute.</xs:documentation>\\n  </xs:annotation>\\n </xs:attribute>\\n \\n <xs:attribute name=\\\"id\\\" type=\\\"xs:ID\\\">\\n  <xs:annotation>\\n   <xs:documentation>See http://www.w3.org/TR/xml-id/ for\\n                     information about this attribute.</xs:documentation>\\n  </xs:annotation>\\n </xs:attribute>\\n\\n <xs:attributeGroup name=\\\"specialAttrs\\\">\\n  <xs:attribute ref=\\\"xml:base\\\"/>\\n  <xs:attribute ref=\\\"xml:lang\\\"/>\\n  <xs:attribute ref=\\\"xml:space\\\"/>\\n  <xs:attribute ref=\\\"xml:id\\\"/>\\n </xs:attributeGroup>\\n\\n</xs:schema>')");
        testSingleStatement( "Complex whitespace",
                "SELECT\t   *  \n \n from test;",
                "SELECT\t   *  \n \n from test" );
        testSingleStatement( "Multiple lines",
                "SELECT * \n" +
                "from\n" +
                 "test;",
                "SELECT * \nfrom\ntest" );
        testSingleStatement( "Simple comment",
                "-- Comment \n" +
                "SELECT * from test;",
                "SELECT * from test" );
        testSingleStatement( "Quote in comment",
                "-- Comment with quote's and escapes \\ in\n" +
                "SELECT * from test;",
                "SELECT * from test" );
    }

    @Test
    public void testParseSqlStatements() throws Exception {
        String sql =
                "-- Comment \n" +
                "SELECT * FROM a;\n" +
                "\n" +
                "--\n" +
                "-- Comment \n" +
                "--\n" +
                "SELECT * FROM b;;\n" + // test duplicate ;
                "\n" +
                "\n" +
                "SELECT * \n" +
                "FROM\n" +
                "c;";
        String[] statements = SqlUtils.getStatementsFromReader( new StringReader( sql ) );
        assertEquals( "statement count", 3, statements.length);
        assertEquals( "statement 1", "SELECT * FROM a", statements[0]);
        assertEquals( "statement 2", "SELECT * FROM b", statements[1]);
        assertEquals( "statement 3", "SELECT * \nFROM\nc", statements[2]);
    }

    private void testSingleStatement( final String description,
                                      final String sql,
                                      final String expectedStatement ) throws IOException {
        String[] statements = SqlUtils.getStatementsFromReader( new StringReader( sql ) );
        assertEquals( description + " statements", 1, statements.length);
        assertEquals( description + " statement", expectedStatement, statements[0]);
    }    
}
