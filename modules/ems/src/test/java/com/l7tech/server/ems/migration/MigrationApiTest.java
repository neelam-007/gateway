package com.l7tech.server.ems.migration;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.management.api.node.MigrationApi;
import com.l7tech.server.management.migration.bundle.MigrationBundle;
import com.l7tech.server.EntityHeaderUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ExternalEntityHeader;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.internal.InternalUser;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;

/**
 * @author jbufu
 */
public class MigrationApiTest extends TestCase {

    private static final Logger logger = Logger.getLogger(MigrationApiTest.class.getName());

    public MigrationApiTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(MigrationApiTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testExportApi() throws Exception {
        MigrationApi api = getMigrationApi();
        Collection<ExternalEntityHeader> headers = api.listEntities(PublishedService.class);
        logger.log(Level.FINE, "Retrieved " + headers.size() + " services.");


        MigrationBundle bundle = null;
        try {
            bundle = api.exportBundle(headers);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // print result
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(bundle, System.out);
    }

    public void testImportApi() throws Exception {
        MigrationApi api = getMigrationApi();
        Unmarshaller unmarshaller = getJaxbContext().createUnmarshaller();
        MigrationBundle bundle = (MigrationBundle) unmarshaller.unmarshal(new ByteArrayInputStream(JAXB_XML1.getBytes()));
//        api.importBundle(bundle, false);
        logger.log(Level.FINE, "Import done.");
    }

    public void testUser() throws Exception {
        User internal = new InternalUser("bla");
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(EntityHeaderUtils.fromEntity(internal), System.out);
    }

    public void testProviderConfig() throws Exception {
        IdentityProviderConfig config = new IdentityProviderConfig();
        Marshaller marshaller = getJaxbContext().createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(EntityHeaderUtils.fromEntity(config), System.out);
    }
    private JAXBContext getJaxbContext() throws JAXBException {
        Collection<Class> jaxbClasses = new HashSet<Class>() {{
            add(MigrationBundle.class);
            add(EntityType.POLICY.getEntityClass());
            add(EntityType.SERVICE.getEntityClass());
        }};
        return JAXBContext.newInstance(jaxbClasses.toArray(new Class[jaxbClasses.size()]));
    }

    private MigrationApi getMigrationApi() {
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        GatewayContext gwContext = new GatewayContext(null, "darmok" , 8443 ,"esmId", "userId");
        return gwContext.getMigrationApi();
    }

    private static final String JAXB_XML1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<migrationBundle>\n" +
        "    <metadata>\n" +
        "        <headers>\n" +
        "            <entityHeader oid=\"12812290\" type=\"FOLDER\" strId=\"12812290\">\n" +
        "                <description></description>\n" +
        "                <name>f3</name>\n" +
        "                <version>0</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"12812288\" type=\"FOLDER\" strId=\"12812288\">\n" +
        "                <description></description>\n" +
        "                <name>f1</name>\n" +
        "                <version>0</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"-5002\" type=\"FOLDER\" strId=\"-5002\">\n" +
        "                <description></description>\n" +
        "                <name>Root Node</name>\n" +
        "                <version>0</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"12812289\" type=\"FOLDER\" strId=\"12812289\">\n" +
        "                <description></description>\n" +
        "                <name>f2</name>\n" +
        "                <version>0</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"12845056\" type=\"SERVICE\" strId=\"12845056\">\n" +
        "                <description>s2</description>\n" +
        "                <name>s2</name>\n" +
        "                <version>8</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"12910593\" type=\"POLICY\" strId=\"12910593\">\n" +
        "                <description></description>\n" +
        "                <name>Policy for service #12845057, s1</name>\n" +
        "                <version>1</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"12910592\" type=\"POLICY\" strId=\"12910592\">\n" +
        "                <description></description>\n" +
        "                <name>Policy for service #12845056, s2</name>\n" +
        "                <version>0</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"12910594\" type=\"POLICY\" strId=\"12910594\">\n" +
        "                <description></description>\n" +
        "                <name>fragment1</name>\n" +
        "                <version>1</version>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"12845057\" type=\"SERVICE\" strId=\"12845057\">\n" +
        "                <description>s1</description>\n" +
        "                <name>s1</name>\n" +
        "                <version>8</version>\n" +
        "            </entityHeader>\n" +
        "        </headers>\n" +
        "        <mappings>\n" +
        "            <migrationMapping uploadedByParent=\"false\" mappedTarget=\"false\">\n" +
        "                <source type=\"FOLDER\" strId=\"12812290\"/>\n" +
        "                <propName>ParentFolder</propName>\n" +
        "                <type valueMapping=\"OPTIONAL\" nameMapping=\"OPTIONAL\"/>\n" +
        "                <target type=\"FOLDER\" strId=\"-5002\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"false\" mappedTarget=\"false\">\n" +
        "                <source type=\"FOLDER\" strId=\"12812289\"/>\n" +
        "                <propName>ParentFolder</propName>\n" +
        "                <type valueMapping=\"OPTIONAL\" nameMapping=\"OPTIONAL\"/>\n" +
        "                <target type=\"FOLDER\" strId=\"12812288\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"false\" mappedTarget=\"false\">\n" +
        "                <source type=\"FOLDER\" strId=\"12812288\"/>\n" +
        "                <propName>ParentFolder</propName>\n" +
        "                <type valueMapping=\"OPTIONAL\" nameMapping=\"OPTIONAL\"/>\n" +
        "                <target type=\"FOLDER\" strId=\"-5002\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"false\" mappedTarget=\"false\">\n" +
        "                <source type=\"POLICY\" strId=\"12910593\"/>\n" +
        "                <propName>Xml:2:EntitiesUsed</propName>\n" +
        "                <type valueMapping=\"OPTIONAL\" nameMapping=\"OPTIONAL\"/>\n" +
        "                <target type=\"POLICY\" strId=\"27b3deac-fd92-4f94-bb5d-a21f2f49cbf0\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"true\" mappedTarget=\"false\">\n" +
        "                <source type=\"SERVICE\" strId=\"12845056\"/>\n" +
        "                <propName>Policy</propName>\n" +
        "                <type valueMapping=\"NONE\" nameMapping=\"NONE\"/>\n" +
        "                <target type=\"POLICY\" strId=\"12910592\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"false\" mappedTarget=\"false\">\n" +
        "                <source type=\"SERVICE\" strId=\"12845057\"/>\n" +
        "                <propName>Folder</propName>\n" +
        "                <type valueMapping=\"NONE\" nameMapping=\"NONE\"/>\n" +
        "                <target type=\"FOLDER\" strId=\"12812288\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"false\" mappedTarget=\"false\">\n" +
        "                <source type=\"POLICY\" strId=\"12910594\"/>\n" +
        "                <propName>Folder</propName>\n" +
        "                <type valueMapping=\"NONE\" nameMapping=\"NONE\"/>\n" +
        "                <target type=\"FOLDER\" strId=\"12812290\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"false\" mappedTarget=\"false\">\n" +
        "                <source type=\"SERVICE\" strId=\"12845056\"/>\n" +
        "                <propName>Folder</propName>\n" +
        "                <type valueMapping=\"NONE\" nameMapping=\"NONE\"/>\n" +
        "                <target type=\"FOLDER\" strId=\"12812289\"/>\n" +
        "            </migrationMapping>\n" +
        "            <migrationMapping uploadedByParent=\"true\" mappedTarget=\"false\">\n" +
        "                <source type=\"SERVICE\" strId=\"12845057\"/>\n" +
        "                <propName>Policy</propName>\n" +
        "                <type valueMapping=\"NONE\" nameMapping=\"NONE\"/>\n" +
        "                <target type=\"POLICY\" strId=\"12910593\"/>\n" +
        "            </migrationMapping>\n" +
        "        </mappings>\n" +
        "    </metadata>\n" +
        "    <values>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"POLICY\" strId=\"12910593\"/>\n" +
        "            <policy>\n" +
        "                <oid>12910593</oid>\n" +
        "                <version>1</version>\n" +
        "                <name>Policy for service #12845057, s1</name>\n" +
        "                <guid>dd8dbe2c-5667-44c6-89d3-941d3e6ae61e</guid>\n" +
        "                <soap>false</soap>\n" +
        "                <type>PRIVATE_SERVICE</type>\n" +
        "                <versionActive>false</versionActive>\n" +
        "                <versionOrdinal>0</versionOrdinal>\n" +
        "                <xml>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:Include&gt;\n" +
        "            &lt;L7p:PolicyGuid stringValue=&quot;27b3deac-fd92-4f94-bb5d-a21f2f49cbf0&quot;/&gt;\n" +
        "        &lt;/L7p:Include&gt;\n" +
        "        &lt;L7p:HttpRoutingAssertion&gt;\n" +
        "            &lt;L7p:ProtectedServiceUrl stringValue=&quot;http://localhost/s1&quot;/&gt;\n" +
        "            &lt;L7p:RequestHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;SOAPAction&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:RequestHeaderRules&gt;\n" +
        "            &lt;L7p:RequestParamRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:ForwardAll booleanValue=&quot;true&quot;/&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;/&gt;\n" +
        "            &lt;/L7p:RequestParamRules&gt;\n" +
        "            &lt;L7p:ResponseHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Set-Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:ResponseHeaderRules&gt;\n" +
        "        &lt;/L7p:HttpRoutingAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</xml>\n" +
        "            </policy>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"FOLDER\" strId=\"12812288\"/>\n" +
        "            <folder>\n" +
        "                <oid>12812288</oid>\n" +
        "                <version>0</version>\n" +
        "                <name>f1</name>\n" +
        "                <parentFolder>\n" +
        "                    <oid>-5002</oid>\n" +
        "                    <version>0</version>\n" +
        "                    <name>Root Node</name>\n" +
        "                </parentFolder>\n" +
        "            </folder>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"SERVICE\" strId=\"12845056\"/>\n" +
        "            <publishedService>\n" +
        "                <oid>12845056</oid>\n" +
        "                <version>8</version>\n" +
        "                <name>s2</name>\n" +
        "                <disabled>false</disabled>\n" +
        "                <folder>\n" +
        "                    <oid>12812289</oid>\n" +
        "                    <version>0</version>\n" +
        "                    <name>f2</name>\n" +
        "                    <parentFolder>\n" +
        "                        <oid>12812288</oid>\n" +
        "                        <version>0</version>\n" +
        "                        <name>f1</name>\n" +
        "                        <parentFolder>\n" +
        "                            <oid>-5002</oid>\n" +
        "                            <version>0</version>\n" +
        "                            <name>Root Node</name>\n" +
        "                        </parentFolder>\n" +
        "                    </parentFolder>\n" +
        "                </folder>\n" +
        "                <httpMethods>GET</httpMethods>\n" +
        "                <httpMethods>POST</httpMethods>\n" +
        "                <httpMethods>PUT</httpMethods>\n" +
        "                <httpMethods>DELETE</httpMethods>\n" +
        "                <internal>false</internal>\n" +
        "                <laxResolution>false</laxResolution>\n" +
        "                <policy>\n" +
        "                    <oid>12910592</oid>\n" +
        "                    <version>0</version>\n" +
        "                    <name>Policy for service #12845056, s2</name>\n" +
        "                    <guid>d188d1c9-fe74-4d8c-a7f4-ef8029799a22</guid>\n" +
        "                    <soap>false</soap>\n" +
        "                    <type>PRIVATE_SERVICE</type>\n" +
        "                    <versionActive>false</versionActive>\n" +
        "                    <versionOrdinal>0</versionOrdinal>\n" +
        "                    <xml>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:HttpRoutingAssertion&gt;\n" +
        "            &lt;L7p:ProtectedServiceUrl stringValue=&quot;http://localhost/s2&quot;/&gt;\n" +
        "            &lt;L7p:RequestHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;SOAPAction&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:RequestHeaderRules&gt;\n" +
        "            &lt;L7p:RequestParamRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:ForwardAll booleanValue=&quot;true&quot;/&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;/&gt;\n" +
        "            &lt;/L7p:RequestParamRules&gt;\n" +
        "            &lt;L7p:ResponseHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Set-Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:ResponseHeaderRules&gt;\n" +
        "        &lt;/L7p:HttpRoutingAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</xml>\n" +
        "                </policy>\n" +
        "                <routingUri>/s2</routingUri>\n" +
        "                <soap>false</soap>\n" +
        "                <wsdlUrl></wsdlUrl>\n" +
        "            </publishedService>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"FOLDER\" strId=\"12812290\"/>\n" +
        "            <folder>\n" +
        "                <oid>12812290</oid>\n" +
        "                <version>0</version>\n" +
        "                <name>f3</name>\n" +
        "                <parentFolder>\n" +
        "                    <oid>-5002</oid>\n" +
        "                    <version>0</version>\n" +
        "                    <name>Root Node</name>\n" +
        "                </parentFolder>\n" +
        "            </folder>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"SERVICE\" strId=\"12845057\"/>\n" +
        "            <publishedService>\n" +
        "                <oid>12845057</oid>\n" +
        "                <version>8</version>\n" +
        "                <name>s1</name>\n" +
        "                <disabled>false</disabled>\n" +
        "                <folder>\n" +
        "                    <oid>12812288</oid>\n" +
        "                    <version>0</version>\n" +
        "                    <name>f1</name>\n" +
        "                    <parentFolder>\n" +
        "                        <oid>-5002</oid>\n" +
        "                        <version>0</version>\n" +
        "                        <name>Root Node</name>\n" +
        "                    </parentFolder>\n" +
        "                </folder>\n" +
        "                <httpMethods>GET</httpMethods>\n" +
        "                <httpMethods>POST</httpMethods>\n" +
        "                <httpMethods>PUT</httpMethods>\n" +
        "                <httpMethods>DELETE</httpMethods>\n" +
        "                <internal>false</internal>\n" +
        "                <laxResolution>false</laxResolution>\n" +
        "                <policy>\n" +
        "                    <oid>12910593</oid>\n" +
        "                    <version>1</version>\n" +
        "                    <name>Policy for service #12845057, s1</name>\n" +
        "                    <guid>dd8dbe2c-5667-44c6-89d3-941d3e6ae61e</guid>\n" +
        "                    <soap>false</soap>\n" +
        "                    <type>PRIVATE_SERVICE</type>\n" +
        "                    <versionActive>false</versionActive>\n" +
        "                    <versionOrdinal>0</versionOrdinal>\n" +
        "                    <xml>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:Include&gt;\n" +
        "            &lt;L7p:PolicyGuid stringValue=&quot;27b3deac-fd92-4f94-bb5d-a21f2f49cbf0&quot;/&gt;\n" +
        "        &lt;/L7p:Include&gt;\n" +
        "        &lt;L7p:HttpRoutingAssertion&gt;\n" +
        "            &lt;L7p:ProtectedServiceUrl stringValue=&quot;http://localhost/s1&quot;/&gt;\n" +
        "            &lt;L7p:RequestHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;SOAPAction&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:RequestHeaderRules&gt;\n" +
        "            &lt;L7p:RequestParamRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:ForwardAll booleanValue=&quot;true&quot;/&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;/&gt;\n" +
        "            &lt;/L7p:RequestParamRules&gt;\n" +
        "            &lt;L7p:ResponseHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Set-Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:ResponseHeaderRules&gt;\n" +
        "        &lt;/L7p:HttpRoutingAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</xml>\n" +
        "                </policy>\n" +
        "                <routingUri>/s1</routingUri>\n" +
        "                <soap>false</soap>\n" +
        "                <wsdlUrl></wsdlUrl>\n" +
        "            </publishedService>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"POLICY\" strId=\"12910594\"/>\n" +
        "            <policy>\n" +
        "                <oid>12910594</oid>\n" +
        "                <version>1</version>\n" +
        "                <name>fragment1</name>\n" +
        "                <folder>\n" +
        "                    <oid>12812290</oid>\n" +
        "                    <version>0</version>\n" +
        "                    <name>f3</name>\n" +
        "                    <parentFolder>\n" +
        "                        <oid>-5002</oid>\n" +
        "                        <version>0</version>\n" +
        "                        <name>Root Node</name>\n" +
        "                    </parentFolder>\n" +
        "                </folder>\n" +
        "                <guid>27b3deac-fd92-4f94-bb5d-a21f2f49cbf0</guid>\n" +
        "                <soap>false</soap>\n" +
        "                <type>INCLUDE_FRAGMENT</type>\n" +
        "                <versionActive>false</versionActive>\n" +
        "                <versionOrdinal>0</versionOrdinal>\n" +
        "                <xml>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:AuditDetailAssertion&gt;\n" +
        "            &lt;L7p:Detail stringValue=&quot;Policy Fragment: fragment1&quot;/&gt;\n" +
        "        &lt;/L7p:AuditDetailAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</xml>\n" +
        "            </policy>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"FOLDER\" strId=\"-5002\"/>\n" +
        "            <folder>\n" +
        "                <oid>-5002</oid>\n" +
        "                <version>0</version>\n" +
        "                <name>Root Node</name>\n" +
        "            </folder>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"POLICY\" strId=\"12910592\"/>\n" +
        "            <policy>\n" +
        "                <oid>12910592</oid>\n" +
        "                <version>0</version>\n" +
        "                <name>Policy for service #12845056, s2</name>\n" +
        "                <guid>d188d1c9-fe74-4d8c-a7f4-ef8029799a22</guid>\n" +
        "                <soap>false</soap>\n" +
        "                <type>PRIVATE_SERVICE</type>\n" +
        "                <versionActive>false</versionActive>\n" +
        "                <versionOrdinal>0</versionOrdinal>\n" +
        "                <xml>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:HttpRoutingAssertion&gt;\n" +
        "            &lt;L7p:ProtectedServiceUrl stringValue=&quot;http://localhost/s2&quot;/&gt;\n" +
        "            &lt;L7p:RequestHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;SOAPAction&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:RequestHeaderRules&gt;\n" +
        "            &lt;L7p:RequestParamRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:ForwardAll booleanValue=&quot;true&quot;/&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;/&gt;\n" +
        "            &lt;/L7p:RequestParamRules&gt;\n" +
        "            &lt;L7p:ResponseHeaderRules httpPassthroughRuleSet=&quot;included&quot;&gt;\n" +
        "                &lt;L7p:Rules httpPassthroughRules=&quot;included&quot;&gt;\n" +
        "                    &lt;L7p:item httpPassthroughRule=&quot;included&quot;&gt;\n" +
        "                        &lt;L7p:Name stringValue=&quot;Set-Cookie&quot;/&gt;\n" +
        "                    &lt;/L7p:item&gt;\n" +
        "                &lt;/L7p:Rules&gt;\n" +
        "            &lt;/L7p:ResponseHeaderRules&gt;\n" +
        "        &lt;/L7p:HttpRoutingAssertion&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</xml>\n" +
        "            </policy>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"FOLDER\" strId=\"12812289\"/>\n" +
        "            <folder>\n" +
        "                <oid>12812289</oid>\n" +
        "                <version>0</version>\n" +
        "                <name>f2</name>\n" +
        "                <parentFolder>\n" +
        "                    <oid>12812288</oid>\n" +
        "                    <version>0</version>\n" +
        "                    <name>f1</name>\n" +
        "                    <parentFolder>\n" +
        "                        <oid>-5002</oid>\n" +
        "                        <version>0</version>\n" +
        "                        <name>Root Node</name>\n" +
        "                    </parentFolder>\n" +
        "                </parentFolder>\n" +
        "            </folder>\n" +
        "        </exportedItem>\n" +
        "    </values>\n" +
        "</migrationBundle>";

}
