-- MySQL dump 8.22
--
-- Host: localhost    Database: ssg
---------------------------------------------------------
-- Server version	3.23.56-log

--
-- Table structure for table 'address'
--

DROP TABLE IF EXISTS address;
CREATE TABLE address (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  address varchar(128) NOT NULL default '',
  address2 varchar(128) default NULL,
  city varchar(64) default NULL,
  state bigint(20) default NULL,
  country bigint(20) default NULL,
  postal_code varchar(64) default NULL,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'address'
--



--
-- Table structure for table 'country'
--

DROP TABLE IF EXISTS country;
CREATE TABLE country (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  code char(2) NOT NULL default '',
  name varchar(64) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'country'
--



--
-- Table structure for table 'hibernate_unique_key'
--

DROP TABLE IF EXISTS hibernate_unique_key;
CREATE TABLE hibernate_unique_key (
  next_hi int(11) default NULL
) TYPE=InnoDB;

--
-- Dumping data for table 'hibernate_unique_key'
--


INSERT INTO hibernate_unique_key VALUES (70);

--
-- Table structure for table 'identity_provider'
--

DROP TABLE IF EXISTS identity_provider;
CREATE TABLE identity_provider (
  oid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext NOT NULL,
  type bigint(20) NOT NULL default '0',
  properties text,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'identity_provider'
--


INSERT INTO identity_provider VALUES (3735552,NULL,'Spock LDAP','Spock LDAPd',2,'<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n<java version=\"1.4.1\" class=\"java.beans.XMLDecoder\"> \n <object class=\"java.util.HashMap\"> \n  <void method=\"put\"> \n   <string>ldapSearchBase</string> \n   <string>dc=layer7-tech,dc=com</string> \n  </void> \n  <void method=\"put\"> \n   <string>ldapHostURL</string> \n   <string>ldap://spock:389</string> \n  </void> \n </object> \n</java> \n');

--
-- Table structure for table 'identity_provider_type'
--

DROP TABLE IF EXISTS identity_provider_type;
CREATE TABLE identity_provider_type (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  class_name varchar(255) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'identity_provider_type'
--


INSERT INTO identity_provider_type VALUES (1,0,'Internal Identity Provider',NULL,'com.l7tech.identity.internal.imp.InternalIdentityProviderImp');
INSERT INTO identity_provider_type VALUES (1925666,0,'LDAP Identity Provider','LDAP Identity Provider','com.l7tech.identity.ldap.LdapIdentityProviderServer');

--
-- Table structure for table 'internal_group'
--

DROP TABLE IF EXISTS internal_group;
CREATE TABLE internal_group (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  provider bigint(20) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  description mediumtext,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_group'
--


INSERT INTO internal_group VALUES (6666,0,0,'ssgadmin','ssgadmin');

--
-- Table structure for table 'internal_organization'
--

DROP TABLE IF EXISTS internal_organization;
CREATE TABLE internal_organization (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(128) NOT NULL default '',
  address bigint(20) default NULL,
  billing_address bigint(20) default NULL,
  mailing_address bigint(20) default NULL,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_organization'
--



--
-- Table structure for table 'internal_user'
--

DROP TABLE IF EXISTS internal_user;
CREATE TABLE internal_user (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  provider bigint(20) NOT NULL default '0',
  login varchar(32) NOT NULL default '',
  password varchar(32) NOT NULL default '',
  first_name varchar(32) default NULL,
  last_name varchar(32) default NULL,
  email varchar(128) default NULL,
  title varchar(64) default NULL,
  organization bigint(20) default NULL,
  department varchar(128) default NULL,
  address bigint(20) default NULL,
  mailing_address bigint(20) default NULL,
  billing_address bigint(20) default NULL,
  name varchar(128) default NULL,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_user'
--


INSERT INTO internal_user VALUES (666,0,0,'ssgadmin','309b9c7ab4c3ee2144fce9b071acd440',NULL,NULL,NULL,NULL,0,NULL,0,0,0,'ssgadmin');
INSERT INTO internal_user VALUES (3997696,0,0,'fred','c01efa3de3cfe0a281c4839fa1cfe04e',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL);

--
-- Table structure for table 'internal_user_group'
--

DROP TABLE IF EXISTS internal_user_group;
CREATE TABLE internal_user_group (
  internal_user bigint(20) NOT NULL default '0',
  internal_group bigint(20) NOT NULL default '0',
  PRIMARY KEY  (internal_user,internal_group)
) TYPE=InnoDB;

--
-- Dumping data for table 'internal_user_group'
--


INSERT INTO internal_user_group VALUES (666,6666);
INSERT INTO internal_user_group VALUES (666,6666666);

--
-- Table structure for table 'ldap_identity_provider'
--

DROP TABLE IF EXISTS ldap_identity_provider;
CREATE TABLE ldap_identity_provider (
  oid bigint(20) NOT NULL default '0',
  version int(11) default NULL,
  name varchar(128) NOT NULL default '',
  description mediumtext NOT NULL,
  type bigint(20) NOT NULL default '0',
  ldap_host_url varchar(128) NOT NULL default '',
  search_base varchar(128) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'ldap_identity_provider'
--


INSERT INTO ldap_identity_provider VALUES (421456,0,'Spock Directory','Spock Ldap directory as seen from k office',1925666,'ldap://localhost:3899','dc=layer7-tech,dc=com');

--
-- Table structure for table 'object_identity'
--

DROP TABLE IF EXISTS object_identity;
CREATE TABLE object_identity (
  class_name varchar(255) NOT NULL default '',
  table_name varchar(255) default NULL,
  class_seed smallint(6) NOT NULL auto_increment,
  server_seed smallint(6) default '0',
  key_seed bigint(20) NOT NULL default '0',
  key_batch_size int(11) NOT NULL default '0',
  PRIMARY KEY  (class_seed)
) TYPE=InnoDB;

--
-- Dumping data for table 'object_identity'
--



--
-- Table structure for table 'published_service'
--

DROP TABLE IF EXISTS published_service;
CREATE TABLE published_service (
  oid bigint(20) NOT NULL default '0',
  version int(11) NOT NULL default '0',
  name varchar(64) NOT NULL default '',
  policy_xml text,
  wsdl_url varchar(255) NOT NULL default '',
  wsdl_xml text,
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'published_service'
--


INSERT INTO published_service VALUES (4521984,0,'http://www.xmethods.net/sd/BabelFishService.wsdl','<?xml version=\"1.0\" encoding=\"UTF-8\"?> \n<java version=\"1.4.1_02\" class=\"java.beans.XMLDecoder\"> \n <object id=\"AllAssertion0\" class=\"com.l7tech.policy.assertion.composite.AllAssertion\"> \n  <void property=\"children\"> \n   <object class=\"java.util.LinkedList\"> \n    <void method=\"add\"> \n     <object class=\"com.l7tech.policy.assertion.credential.http.HttpBasic\"> \n      <void property=\"parent\"> \n       <object idref=\"AllAssertion0\"/> \n      </void> \n     </object> \n    </void> \n    <void method=\"add\"> \n     <object id=\"OneOrMoreAssertion0\" class=\"com.l7tech.policy.assertion.composite.OneOrMoreAssertion\"> \n      <void property=\"children\"> \n       <object class=\"java.util.LinkedList\"> \n        <void method=\"add\"> \n         <object class=\"com.l7tech.policy.assertion.identity.MemberOfGroup\"> \n          <void property=\"group\"> \n           <object class=\"com.l7tech.identity.Group\"> \n            <void property=\"name\"> \n             <string>kenny rogers fan club</string> \n            </void> \n           </object> \n          </void> \n          <void property=\"identityProvider\"> \n           <object class=\"com.l7tech.adminws.identity.IdentityProviderClient\"/> \n          </void> \n          <void property=\"parent\"> \n           <object idref=\"OneOrMoreAssertion0\"/> \n          </void> \n         </object> \n        </void> \n       </object> \n      </void> \n      <void property=\"parent\"> \n       <object idref=\"AllAssertion0\"/> \n      </void> \n     </object> \n    </void> \n   </object> \n  </void> \n </object> \n</java> \n','http://www.xmethods.net/sd/2001/BabelFishService.wsdl','<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n<definitions name=\"BabelFishService\" targetNamespace=\"http://www.xmethods.net/sd/BabelFishService.wsdl\" xmlns:tns=\"http://www.xmethods.net/sd/BabelFishService.wsdl\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns=\"http://schemas.xmlsoap.org/wsdl/\">\r\n  <message name=\"BabelFishResponse\">\r\n    <part name=\"return\" type=\"xsd:string\"/>\r\n  </message>\r\n  <message name=\"BabelFishRequest\">\r\n    <part name=\"translationmode\" type=\"xsd:string\"/>\r\n    <part name=\"sourcedata\" type=\"xsd:string\"/>\r\n  </message>\r\n  <portType name=\"BabelFishPortType\">\r\n    <operation name=\"BabelFish\">\r\n      <input message=\"tns:BabelFishRequest\"/>\r\n      <output message=\"tns:BabelFishResponse\"/>\r\n    </operation>\r\n  </portType>\r\n  <binding name=\"BabelFishBinding\" type=\"tns:BabelFishPortType\">\r\n    <soap:binding style=\"rpc\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\r\n    <operation name=\"BabelFish\">\r\n      <soap:operation soapAction=\"urn:xmethodsBabelFish#BabelFish\"/>\r\n      <input>\r\n        <soap:body use=\"encoded\" encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" namespace=\"urn:xmethodsBabelFish\"/>\r\n      </input>\r\n      <output>\r\n        <soap:body use=\"encoded\" encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\" namespace=\"urn:xmethodsBabelFish\"/>\r\n      </output>\r\n    </operation>\r\n  </binding>\r\n  <service name=\"BabelFishService\">\r\n<documentation>Translates text of up to 5k in length, between a variety of languages.</documentation>\r\n    <port name=\"BabelFishPort\" binding=\"tns:BabelFishBinding\">\r\n      <soap:address location=\"http://services.xmethods.net:80/perl/soaplite.cgi\"/>\r\n    </port>\r\n  </service>\r\n</definitions>\r\n');

--
-- Table structure for table 'state'
--

DROP TABLE IF EXISTS state;
CREATE TABLE state (
  oid bigint(20) NOT NULL default '0',
  country bigint(20) NOT NULL default '0',
  code varchar(16) NOT NULL default '',
  name varchar(64) NOT NULL default '',
  PRIMARY KEY  (oid)
) TYPE=InnoDB;

--
-- Dumping data for table 'state'
--



