package com.l7tech.external.assertions.policybundleexporter;

import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.util.FileUtils;
import org.junit.Ignore;
import org.junit.Test;

import java.util.*;

/**
 * Test Policy Bundle Installer .aar file generation.
 */
public class AarFileGeneratorTest {
    final static String SIMPLE_MIGRATION_BUNDLE = "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:References>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>Simple Policy Bundle</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b0c</l7:Id>\n" +
            "            <l7:Type>FOLDER</l7:Type>\n" +
            "            <l7:TimeStamp>2014-04-24T14:31:17.247-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/f1649a0664f1ebb6235ac238a6f71b0c\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/folders\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:Folder id=\"f1649a0664f1ebb6235ac238a6f71b0c\" folderId=\"0000000000000000ffffffffffffec76\">\n" +
            "                    <l7:Name>Simple Policy Bundle</l7:Name>\n" +
            "                </l7:Folder>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>simpleIncludedPolicyFragment</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b61</l7:Id>\n" +
            "            <l7:Type>POLICY</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.536-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b61\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:Policy guid=\"e9aaee50-21cf-4b3f-bc11-bbbb1711e265\" id=\"f1649a0664f1ebb6235ac238a6f71b61\" version=\"0\">\n" +
            "                    <l7:PolicyDetail folderId=\"f1649a0664f1ebb6235ac238a6f71b0c\" guid=\"e9aaee50-21cf-4b3f-bc11-bbbb1711e265\" id=\"f1649a0664f1ebb6235ac238a6f71b61\" version=\"0\">\n" +
            "                        <l7:Name>simpleIncludedPolicyFragment</l7:Name>\n" +
            "                        <l7:PolicyType>Include</l7:PolicyType>\n" +
            "                        <l7:Properties>\n" +
            "                            <l7:Property key=\"revision\">\n" +
            "                                <l7:LongValue>1</l7:LongValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"soap\">\n" +
            "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                        </l7:Properties>\n" +
            "                    </l7:PolicyDetail>\n" +
            "                    <l7:Resources>\n" +
            "                        <l7:ResourceSet tag=\"policy\">\n" +
            "                            <l7:Resource type=\"policy\"><![CDATA[<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PHNpbXBsZUluY2x1ZGVkUG9saWN5RnJhZ21lbnQ+JHtyZXF1ZXN0Lm1haW5wYXJ0fTwvc2ltcGxlSW5jbHVkZWRQb2xpY3lGcmFnbWVudD4=\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"simpleIncludedPolicyFragmentVariable\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:Include>\n" +
            "            <L7p:PolicyGuid stringValue=\"cfa49381-54aa-4231-b72e-6d483a032bf7\"/>\n" +
            "        </L7p:Include>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>]]></l7:Resource>\n" +
            "                        </l7:ResourceSet>\n" +
            "                    </l7:Resources>\n" +
            "                </l7:Policy>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>simpleEncapsulatedAssertionFragment</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b75</l7:Id>\n" +
            "            <l7:Type>POLICY</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.536-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b75\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:Policy guid=\"4c8e03fa-3554-40ed-b67a-de5d86f36d7e\" id=\"f1649a0664f1ebb6235ac238a6f71b75\" version=\"0\">\n" +
            "                    <l7:PolicyDetail folderId=\"f1649a0664f1ebb6235ac238a6f71b0c\" guid=\"4c8e03fa-3554-40ed-b67a-de5d86f36d7e\" id=\"f1649a0664f1ebb6235ac238a6f71b75\" version=\"0\">\n" +
            "                        <l7:Name>simpleEncapsulatedAssertionFragment</l7:Name>\n" +
            "                        <l7:PolicyType>Include</l7:PolicyType>\n" +
            "                        <l7:Properties>\n" +
            "                            <l7:Property key=\"revision\">\n" +
            "                                <l7:LongValue>1</l7:LongValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"soap\">\n" +
            "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                        </l7:Properties>\n" +
            "                    </l7:PolicyDetail>\n" +
            "                    <l7:Resources>\n" +
            "                        <l7:ResourceSet tag=\"policy\">\n" +
            "                            <l7:Resource type=\"policy\"><![CDATA[<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PGVuY2Fwc3VsYXRlZEFzc2VydGlvbj4ke2luU2ltcGxlRW5jYXBzdWxhdGVkQXNzZXJ0aW9uRnJhZ21lbnR9PC9lbmNhcHN1bGF0ZWRBc3NlcnRpb24+\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"outSimpleEncapsulatedAssertionFragment\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:Include>\n" +
            "            <L7p:PolicyGuid stringValue=\"e9aaee50-21cf-4b3f-bc11-bbbb1711e265\"/>\n" +
            "        </L7p:Include>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>]]></l7:Resource>\n" +
            "                        </l7:ResourceSet>\n" +
            "                    </l7:Resources>\n" +
            "                </l7:Policy>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>Simple Encapsulated Assertion</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b89</l7:Id>\n" +
            "            <l7:Type>ENCAPSULATED_ASSERTION</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.536-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/f1649a0664f1ebb6235ac238a6f71b89\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:EncapsulatedAssertion id=\"f1649a0664f1ebb6235ac238a6f71b89\" version=\"0\">\n" +
            "                    <l7:Name>Simple Encapsulated Assertion</l7:Name>\n" +
            "                    <l7:Guid>506589b0-eba5-4b3f-81b5-be7809817623</l7:Guid>\n" +
            "                    <l7:PolicyReference id=\"f1649a0664f1ebb6235ac238a6f71b75\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
            "                    <l7:EncapsulatedArguments>\n" +
            "                        <l7:EncapsulatedAssertionArgument>\n" +
            "                            <l7:Ordinal>1</l7:Ordinal>\n" +
            "                            <l7:ArgumentName>inSimpleEncapsulatedAssertionFragment</l7:ArgumentName>\n" +
            "                            <l7:ArgumentType>string</l7:ArgumentType>\n" +
            "                            <l7:GuiPrompt>false</l7:GuiPrompt>\n" +
            "                        </l7:EncapsulatedAssertionArgument>\n" +
            "                    </l7:EncapsulatedArguments>\n" +
            "                    <l7:EncapsulatedResults>\n" +
            "                        <l7:EncapsulatedAssertionResult>\n" +
            "                            <l7:ResultName>outSimpleEncapsulatedAssertionFragment</l7:ResultName>\n" +
            "                            <l7:ResultType>string</l7:ResultType>\n" +
            "                        </l7:EncapsulatedAssertionResult>\n" +
            "                    </l7:EncapsulatedResults>\n" +
            "                    <l7:Properties>\n" +
            "                        <l7:Property key=\"paletteFolder\">\n" +
            "                            <l7:StringValue>routing</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"policyGuid\">\n" +
            "                            <l7:StringValue>4c8e03fa-3554-40ed-b67a-de5d86f36d7e</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"description\">\n" +
            "                            <l7:StringValue>A simple encapsulated assertion</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                    </l7:Properties>\n" +
            "                </l7:EncapsulatedAssertion>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>mysql_root</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71a6d</l7:Id>\n" +
            "            <l7:Type>SECURE_PASSWORD</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.541-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/passwords/f1649a0664f1ebb6235ac238a6f71a6d\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/passwords/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/passwords\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:StoredPassword id=\"f1649a0664f1ebb6235ac238a6f71a6d\" version=\"1\">\n" +
            "                    <l7:Name>mysql_root</l7:Name>\n" +
            "                    <l7:Properties>\n" +
            "                        <l7:Property key=\"usageFromVariable\">\n" +
            "                            <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"description\">\n" +
            "                            <l7:StringValue/>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"lastUpdated\">\n" +
            "                            <l7:DateValue>2014-06-04T14:08:27.076-07:00</l7:DateValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"type\">\n" +
            "                            <l7:StringValue>Password</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                    </l7:Properties>\n" +
            "                </l7:StoredPassword>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>SSG</l7:Name>\n" +
            "            <l7:Id>0567c6a8f0c4cc2c9fb331cb03b4de6f</l7:Id>\n" +
            "            <l7:Type>JDBC_CONNECTION</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.541-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections/0567c6a8f0c4cc2c9fb331cb03b4de6f\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:JDBCConnection id=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" version=\"2\">\n" +
            "                    <l7:Name>SSG</l7:Name>\n" +
            "                    <l7:Enabled>true</l7:Enabled>\n" +
            "                    <l7:Properties>\n" +
            "                        <l7:Property key=\"maximumPoolSize\">\n" +
            "                            <l7:IntegerValue>15</l7:IntegerValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"minimumPoolSize\">\n" +
            "                            <l7:IntegerValue>3</l7:IntegerValue>\n" +
            "                        </l7:Property>\n" +
            "                    </l7:Properties>\n" +
            "                    <l7:Extension>\n" +
            "                        <l7:DriverClass>com.l7tech.jdbc.mysql.MySQLDriver</l7:DriverClass>\n" +
            "                        <l7:JdbcUrl>jdbc:mysql://localhost:3306/ssg82ske</l7:JdbcUrl>\n" +
            "                        <l7:ConnectionProperties>\n" +
            "                            <l7:Property key=\"EnableCancelTimeout\">\n" +
            "                                <l7:StringValue>true</l7:StringValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"password\">\n" +
            "                                <l7:StringValue>${secpass.mysql_root.plaintext}</l7:StringValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"user\">\n" +
            "                                <l7:StringValue>root</l7:StringValue>\n" +
            "                            </l7:Property>\n" +
            "                        </l7:ConnectionProperties>\n" +
            "                    </l7:Extension>\n" +
            "                </l7:JDBCConnection>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>simpleCompositeEncapsulatedAssertionFragment</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b8f</l7:Id>\n" +
            "            <l7:Type>POLICY</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.541-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b8f\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:Policy guid=\"e9f40c8f-6f76-4a74-803d-187afc91e28d\" id=\"f1649a0664f1ebb6235ac238a6f71b8f\" version=\"0\">\n" +
            "                    <l7:PolicyDetail folderId=\"f1649a0664f1ebb6235ac238a6f71b0c\" guid=\"e9f40c8f-6f76-4a74-803d-187afc91e28d\" id=\"f1649a0664f1ebb6235ac238a6f71b8f\" version=\"0\">\n" +
            "                        <l7:Name>simpleCompositeEncapsulatedAssertionFragment</l7:Name>\n" +
            "                        <l7:PolicyType>Include</l7:PolicyType>\n" +
            "                        <l7:Properties>\n" +
            "                            <l7:Property key=\"revision\">\n" +
            "                                <l7:LongValue>1</l7:LongValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"soap\">\n" +
            "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                        </l7:Properties>\n" +
            "                    </l7:PolicyDetail>\n" +
            "                    <l7:Resources>\n" +
            "                        <l7:ResourceSet tag=\"policy\">\n" +
            "                            <l7:Resource type=\"policy\"><![CDATA[<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"JHtpblNpbXBsZUNvbXBvc2l0ZUVuY2Fwc3VsYXRlZEFzc2VydGlvbkZyYWdtZW50fQ==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"inSimpleEncapsulatedAssertionFragment\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:Encapsulated>\n" +
            "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\"506589b0-eba5-4b3f-81b5-be7809817623\"/>\n" +
            "            <L7p:EncapsulatedAssertionConfigName stringValue=\"Simple Encapsulated Assertion\"/>\n" +
            "        </L7p:Encapsulated>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PGNvbXBvc2l0ZUVuY2Fwc3VsYXRlZEFzc2VydGlvbj4ke291dFNpbXBsZUVuY2Fwc3VsYXRlZEFzc2VydGlvbkZyYWdtZW50fTwvY29tcG9zaXRlRW5jYXBzdWxhdGVkQXNzZXJ0aW9uPg==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"outSimpleCompositeEncapsulatedAssertionFragment\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>]]></l7:Resource>\n" +
            "                        </l7:ResourceSet>\n" +
            "                    </l7:Resources>\n" +
            "                </l7:Policy>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>Simple Composite Encapsulated Assertion</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71ba3</l7:Id>\n" +
            "            <l7:Type>ENCAPSULATED_ASSERTION</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.541-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/f1649a0664f1ebb6235ac238a6f71ba3\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:EncapsulatedAssertion id=\"f1649a0664f1ebb6235ac238a6f71ba3\" version=\"0\">\n" +
            "                    <l7:Name>Simple Composite Encapsulated Assertion</l7:Name>\n" +
            "                    <l7:Guid>75062052-9f23-4be2-b7fc-c3caad51620d</l7:Guid>\n" +
            "                    <l7:PolicyReference id=\"f1649a0664f1ebb6235ac238a6f71b8f\" resourceUri=\"http://ns.l7tech.com/2010/04/gateway-management/policies\"/>\n" +
            "                    <l7:EncapsulatedArguments>\n" +
            "                        <l7:EncapsulatedAssertionArgument>\n" +
            "                            <l7:Ordinal>1</l7:Ordinal>\n" +
            "                            <l7:ArgumentName>inSimpleCompositeEncapsulatedAssertionFragment</l7:ArgumentName>\n" +
            "                            <l7:ArgumentType>string</l7:ArgumentType>\n" +
            "                            <l7:GuiLabel>aaa</l7:GuiLabel>\n" +
            "                            <l7:GuiPrompt>false</l7:GuiPrompt>\n" +
            "                        </l7:EncapsulatedAssertionArgument>\n" +
            "                    </l7:EncapsulatedArguments>\n" +
            "                    <l7:EncapsulatedResults>\n" +
            "                        <l7:EncapsulatedAssertionResult>\n" +
            "                            <l7:ResultName>outSimpleCompositeEncapsulatedAssertionFragment</l7:ResultName>\n" +
            "                            <l7:ResultType>string</l7:ResultType>\n" +
            "                        </l7:EncapsulatedAssertionResult>\n" +
            "                    </l7:EncapsulatedResults>\n" +
            "                    <l7:Properties>\n" +
            "                        <l7:Property key=\"paletteFolder\">\n" +
            "                            <l7:StringValue>routing</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"description\">\n" +
            "                            <l7:StringValue>A simple composite encapsulated assertion</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"policyGuid\">\n" +
            "                            <l7:StringValue>e9f40c8f-6f76-4a74-803d-187afc91e28d</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                        <l7:Property key=\"allowTracing\">\n" +
            "                            <l7:StringValue>false</l7:StringValue>\n" +
            "                        </l7:Property>\n" +
            "                    </l7:Properties>\n" +
            "                </l7:EncapsulatedAssertion>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>simpleRestService</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71ba9</l7:Id>\n" +
            "            <l7:Type>SERVICE</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.543-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/services/f1649a0664f1ebb6235ac238a6f71ba9\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/services/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/services\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:Service id=\"f1649a0664f1ebb6235ac238a6f71ba9\" version=\"2\">\n" +
            "                    <l7:ServiceDetail folderId=\"f1649a0664f1ebb6235ac238a6f71b0c\" id=\"f1649a0664f1ebb6235ac238a6f71ba9\" version=\"2\">\n" +
            "                        <l7:Name>simpleRestService</l7:Name>\n" +
            "                        <l7:Enabled>true</l7:Enabled>\n" +
            "                        <l7:ServiceMappings>\n" +
            "                            <l7:HttpMapping>\n" +
            "                                <l7:UrlPattern>/simpleRestService</l7:UrlPattern>\n" +
            "                                <l7:Verbs>\n" +
            "                                    <l7:Verb>GET</l7:Verb>\n" +
            "                                    <l7:Verb>POST</l7:Verb>\n" +
            "                                    <l7:Verb>PUT</l7:Verb>\n" +
            "                                    <l7:Verb>DELETE</l7:Verb>\n" +
            "                                </l7:Verbs>\n" +
            "                            </l7:HttpMapping>\n" +
            "                        </l7:ServiceMappings>\n" +
            "                        <l7:Properties>\n" +
            "                            <l7:Property key=\"policyRevision\">\n" +
            "                                <l7:LongValue>1</l7:LongValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"wssProcessingEnabled\">\n" +
            "                                <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"soap\">\n" +
            "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"internal\">\n" +
            "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"tracingEnabled\">\n" +
            "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                        </l7:Properties>\n" +
            "                    </l7:ServiceDetail>\n" +
            "                    <l7:Resources>\n" +
            "                        <l7:ResourceSet tag=\"policy\">\n" +
            "                            <l7:Resource type=\"policy\" version=\"0\"><![CDATA[<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:Simple/>\n" +
            "        <L7p:Include>\n" +
            "            <L7p:PolicyGuid stringValue=\"e9aaee50-21cf-4b3f-bc11-bbbb1711e265\"/>\n" +
            "        </L7p:Include>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"JHtyZXF1ZXN0Lm1haW5wYXJ0fQ==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"inSimpleEncapsulatedAssertionFragment\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:Encapsulated>\n" +
            "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\"506589b0-eba5-4b3f-81b5-be7809817623\"/>\n" +
            "            <L7p:EncapsulatedAssertionConfigName stringValue=\"Simple Encapsulated Assertion\"/>\n" +
            "        </L7p:Encapsulated>\n" +
            "        <L7p:JdbcQuery>\n" +
            "            <L7p:ConnectionName stringValue=\"SSG\"/>\n" +
            "            <L7p:ConvertVariablesToStrings booleanValue=\"false\"/>\n" +
            "            <L7p:NamingMap mapValue=\"included\">\n" +
            "                <L7p:entry>\n" +
            "                    <L7p:key stringValue=\"current_version\"/>\n" +
            "                    <L7p:value stringValue=\"ssgVersion\"/>\n" +
            "                </L7p:entry>\n" +
            "            </L7p:NamingMap>\n" +
            "            <L7p:SqlQuery stringValue=\"select current_version from ssg_version;\"/>\n" +
            "            <L7p:VariablePrefix stringValue=\"simpleJdbcQuery\"/>\n" +
            "        </L7p:JdbcQuery>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"JHtyZXF1ZXN0Lm1haW5wYXJ0fQ==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"inSimpleCompositeEncapsulatedAssertionFragment\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:Encapsulated>\n" +
            "            <L7p:EncapsulatedAssertionConfigGuid stringValue=\"75062052-9f23-4be2-b7fc-c3caad51620d\"/>\n" +
            "            <L7p:EncapsulatedAssertionConfigName stringValue=\"Simple Composite Encapsulated Assertion\"/>\n" +
            "        </L7p:Encapsulated>\n" +
            "        <L7p:HardcodedResponse>\n" +
            "            <L7p:Base64ResponseBody stringValue=\"PHNpbXBsZVJlc3BvbnNlPgogICA8cmVxdWVzdC51cmw+JHtyZXF1ZXN0LnVybH08L3JlcXVlc3QudXJsPgogICA8cmVxdWVzdC5odHRwLm1ldGhvZD4ke3JlcXVlc3QuaHR0cC5tZXRob2R9PC9yZXF1ZXN0Lmh0dHAubWV0aG9kPgogICA8cmVxdWVzdC5tYWlucGFydD4ke3JlcXVlc3QubWFpbnBhcnR9PC9yZXF1ZXN0Lm1haW5wYXJ0PgogICA8c2ltcGxlSW5jbHVkZWRQb2xpY3lGcmFnbWVudD4KICAgICAgJHtzaW1wbGVJbmNsdWRlZFBvbGljeUZyYWdtZW50VmFyaWFibGV9CiAgICAgICAgICR7c2ltcGxlTmVzdGVkSW5jbHVkZWRQb2xpY3lGcmFnbWVudFZhcmlhYmxlfQogICA8L3NpbXBsZUluY2x1ZGVkUG9saWN5RnJhZ21lbnQ+CiAgIDxvdXRTaW1wbGVFbmNhcHN1bGF0ZWRBc3NlcnRpb25GcmFnbWVudD4KICAgICAgJHtvdXRTaW1wbGVFbmNhcHN1bGF0ZWRBc3NlcnRpb25GcmFnbWVudH0KICAgPC9vdXRTaW1wbGVFbmNhcHN1bGF0ZWRBc3NlcnRpb25GcmFnbWVudD4KICAgPHNpbXBsZUpkYmNRdWVyeT4KICAgICAgPHNzZ1ZlcnNpb24+JHtzaW1wbGVKZGJjUXVlcnkuc3NnVmVyc2lvbn08L3NzZ1ZlcnNpb24+CiAgIDwvc2ltcGxlSmRiY1F1ZXJ5PgogICA8b3V0U2ltcGxlQ29tcG9zaXRlRW5jYXBzdWxhdGVkQXNzZXJ0aW9uRnJhZ21lbnQ+CiAgICAgICR7b3V0U2ltcGxlQ29tcG9zaXRlRW5jYXBzdWxhdGVkQXNzZXJ0aW9uRnJhZ21lbnR9CiAgIDwvb3V0U2ltcGxlQ29tcG9zaXRlRW5jYXBzdWxhdGVkQXNzZXJ0aW9uRnJhZ21lbnQ+Cjwvc2ltcGxlUmVzcG9uc2U+\"/>\n" +
            "        </L7p:HardcodedResponse>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>]]></l7:Resource>\n" +
            "                        </l7:ResourceSet>\n" +
            "                    </l7:Resources>\n" +
            "                </l7:Service>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "        <l7:Item>\n" +
            "            <l7:Name>simpleNestedIncludedPolicyFragment</l7:Name>\n" +
            "            <l7:Id>f1649a0664f1ebb6235ac238a6f71b4c</l7:Id>\n" +
            "            <l7:Type>POLICY</l7:Type>\n" +
            "            <l7:TimeStamp>2014-06-04T14:11:20.543-07:00</l7:TimeStamp>\n" +
            "            <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b4c\"/>\n" +
            "            <l7:Link rel=\"template\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/template\"/>\n" +
            "            <l7:Link rel=\"list\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies\"/>\n" +
            "            <l7:Resource>\n" +
            "                <l7:Policy guid=\"cfa49381-54aa-4231-b72e-6d483a032bf7\" id=\"f1649a0664f1ebb6235ac238a6f71b4c\" version=\"0\">\n" +
            "                    <l7:PolicyDetail folderId=\"f1649a0664f1ebb6235ac238a6f71b0c\" guid=\"cfa49381-54aa-4231-b72e-6d483a032bf7\" id=\"f1649a0664f1ebb6235ac238a6f71b4c\" version=\"0\">\n" +
            "                        <l7:Name>simpleNestedIncludedPolicyFragment</l7:Name>\n" +
            "                        <l7:PolicyType>Include</l7:PolicyType>\n" +
            "                        <l7:Properties>\n" +
            "                            <l7:Property key=\"revision\">\n" +
            "                                <l7:LongValue>1</l7:LongValue>\n" +
            "                            </l7:Property>\n" +
            "                            <l7:Property key=\"soap\">\n" +
            "                                <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                            </l7:Property>\n" +
            "                        </l7:Properties>\n" +
            "                    </l7:PolicyDetail>\n" +
            "                    <l7:Resources>\n" +
            "                        <l7:ResourceSet tag=\"policy\">\n" +
            "                            <l7:Resource type=\"policy\"><![CDATA[<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PHNpbXBsZU5lc3RlZEluY2x1ZGVkUG9saWN5RnJhZ21lbnQ+JHtyZXF1ZXN0Lm1haW5wYXJ0fTwvc2ltcGxlTmVzdGVkSW5jbHVkZWRQb2xpY3lGcmFnbWVudD4=\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"simpleNestedIncludedPolicyFragmentVariable\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>]]></l7:Resource>\n" +
            "                        </l7:ResourceSet>\n" +
            "                    </l7:Resources>\n" +
            "                </l7:Policy>\n" +
            "            </l7:Resource>\n" +
            "        </l7:Item>\n" +
            "    </l7:References>\n" +
            "    <l7:Mappings>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b0c\" type=\"FOLDER\"/>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b61\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b61\" type=\"POLICY\"/>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b75\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b75\" type=\"POLICY\"/>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b89\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/f1649a0664f1ebb6235ac238a6f71b89\" type=\"ENCAPSULATED_ASSERTION\"/>\n" +
            "        <l7:Mapping action=\"NewOrExisting\" srcId=\"f1649a0664f1ebb6235ac238a6f71a6d\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/passwords/f1649a0664f1ebb6235ac238a6f71a6d\" type=\"SECURE_PASSWORD\">\n" +
            "            <l7:Properties>\n" +
            "                <l7:Property key=\"FailOnNew\">\n" +
            "                    <l7:BooleanValue>true</l7:BooleanValue>\n" +
            "                </l7:Property>\n" +
            "            </l7:Properties>\n" +
            "        </l7:Mapping>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections/0567c6a8f0c4cc2c9fb331cb03b4de6f\" type=\"JDBC_CONNECTION\"/>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b8f\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b8f\" type=\"POLICY\"/>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71ba3\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/encapsulatedAssertions/f1649a0664f1ebb6235ac238a6f71ba3\" type=\"ENCAPSULATED_ASSERTION\"/>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71ba9\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/services/f1649a0664f1ebb6235ac238a6f71ba9\" type=\"SERVICE\"/>\n" +
            "        <l7:Mapping action=\"NewOrUpdate\" srcId=\"f1649a0664f1ebb6235ac238a6f71b4c\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/policies/f1649a0664f1ebb6235ac238a6f71b4c\" type=\"POLICY\"/>\n" +
            "    </l7:Mappings>\n" +
            "</l7:Bundle>\n";

    @Test
    @Ignore("Developer test")
    public void generateAar() throws Exception {
        final String assCamelName = "FooBar"; // "SimplePolicyBundle";
        final String bundleVersion = "1.0";
        final FolderHeader bundleFolder = new FolderHeader(Folder.ROOT_FOLDER_ID, "/", null, 0, "/", null);
        final String componentName = "FooBarComponent";
        final String componentDescription = "FooBar Component Description";
        final String componentVersion = "1.0";
        final String componentId = UUID.randomUUID().toString();
        final FolderHeader componentFolder = new FolderHeader(new Goid(0, 123345678), "componentFolder", Folder.ROOT_FOLDER_ID, 0, "/componentFolder", null);
        final ComponentInfo componentInfo = new ComponentInfo(componentName, componentDescription, componentVersion, false, componentId, componentFolder);
        List<ComponentInfo> componentInfoList = new LinkedList<>();
        componentInfoList.add(componentInfo);

        final Map<Goid, String> componentRestmanBundleXmls = new HashMap<>();
        componentRestmanBundleXmls.put(new Goid(0, 123345678), SIMPLE_MIGRATION_BUNDLE);

        //new Goid(0, 123345678)
        final PolicyBundleExporterProperties exportProperties = new PolicyBundleExporterProperties(assCamelName, bundleVersion, bundleFolder, componentInfoList);
        exportProperties.setComponentRestmanBundleXmls(componentRestmanBundleXmls);
        final byte[] aarBytes = new AarFileGenerator().generateInstallerAarFile(exportProperties);

        // write test aar to file
        FileUtils.saveFileSafely("D:\\Temp\\ExtractedAars\\" + assCamelName + "InstallerAssertion.aar", true, new FileUtils.ByteSaver(aarBytes));
        // FileUtils.saveFileSafely("C:\\Temp\\FooBarInstallerAssertion.aar", true, new FileUtils.ByteSaver(aarBytes));
    }

    // TODO unit test coverage with assert checks (without needing to actually writing the aar to file).
}
