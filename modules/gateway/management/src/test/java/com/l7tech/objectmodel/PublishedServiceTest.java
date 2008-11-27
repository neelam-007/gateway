package com.l7tech.objectmodel;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collections;
import java.util.Collection;
import java.util.HashSet;
import java.util.EnumSet;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import static com.l7tech.objectmodel.EntityType.*;

import com.l7tech.objectmodel.migration.EntityHeaderWithDependencies;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.server.management.migration.bundle.MigrationBundle;

/**
 * @author jbufu
 */
public class PublishedServiceTest extends TestCase {
    private static final Logger logger = Logger.getLogger(EntityHeaderMarshallingTest.class.getName());

    public PublishedServiceTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(EntityHeaderMarshallingTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testSomething() throws Exception {

        try {
        Collection<Class> jaxbClasses = new HashSet<Class>() {{
            add(MigrationBundle.class);
            EnumSet<EntityType> nonEntities = EnumSet.of(EntityType.ANY, EntityType.USER, EntityType.GROUP, EntityType.SERVICE_TEMPLATE, EntityType.AUDIT_MESSAGE, EntityType.LOG_RECORD, EntityType.SSG_KEY_ENTRY, EntityType.RBAC_ROLE, EntityType.MAP_IDENTITY, EntityType.MAP_TOKEN, EntityType.AUDIT_SYSTEM, EntityType.AUDIT_RECORD, EntityType.AUDIT_ADMIN, EntityType.MAP_ATTRIBUTE);
            for(EntityType type : EntityType.values()) {
                if (nonEntities.contains(type)) continue;
                add(type.getEntityClass());
            }
//            add(EntityType.POLICY.getEntityClass());
//            add(EntityType.SERVICE.getEntityClass());
        }};

        JAXBContext jaxbc = JAXBContext.newInstance(jaxbClasses.toArray(new Class[jaxbClasses.size()]));
        Unmarshaller unmarshaller = jaxbc.createUnmarshaller();
        MigrationBundle bundle2 = (MigrationBundle) unmarshaller.unmarshal(new ByteArrayInputStream(xml.getBytes()));
        logger.log(Level.FINE, "Unmarshalling done: " + bundle2);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error unmarshalling", e);
        }
    }


    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
        "<migrationBundle>\n" +
        "    <metadata>\n" +
        "        <headers>\n" +
        "            <entityHeader oid=\"360448\" type=\"Published Service\" strId=\"360448\">\n" +
        "                <name>smth</name>\n" +
        "            </entityHeader>\n" +
        "            <entityHeader oid=\"425984\" type=\"Policy\" strId=\"425984\">\n" +
        "                <description></description>\n" +
        "                <name>Policy for service #360448, smth</name>\n" +
        "            </entityHeader>\n" +
        "        </headers>\n" +
        "        <mappings>\n" +
        "            <migrationMapping>\n" +
        "                <source type=\"Published Service\" strId=\"360448\"/>\n" +
        "                <propName>getPolicy</propName>\n" +
        "                <type valueMapping=\"OPTIONAL\" nameMapping=\"OPTIONAL\"/>\n" +
        "                <target type=\"Policy\" strId=\"425984\"/>\n" +
        "            </migrationMapping>\n" +
        "        </mappings>\n" +
        "    </metadata>\n" +
        "    <values>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"Published Service\" strId=\"360448\"/>\n" +
        "            <publishedService>\n" +
        "                <oid>360448</oid>\n" +
        "                <version>11</version>\n" +
        "                <name>smth</name>\n" +
        "                <disabled>false</disabled>\n" +
        "                <folderOid>-5002</folderOid>\n" +
        "                <internal>false</internal>\n" +
        "                <laxResolution>false</laxResolution>\n" +
        "                <policy>\n" +
        "                    <oid>425984</oid>\n" +
        "                    <version>1</version>\n" +
        "                    <name>Policy for service #360448, smth</name>\n" +
        "                    <guid>2f2715b5-8f7c-4447-9f7c-df933f8de8d6</guid>\n" +
        "                    <soap>false</soap>\n" +
        "                    <type>PRIVATE_SERVICE</type>\n" +
        "                    <versionActive>false</versionActive>\n" +
        "                    <versionOrdinal>0</versionOrdinal>\n" +
        "                    <xml>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:AuditAssertion/&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</xml>\n" +
        "                </policy>\n" +
        "                <routingUri>/smth</routingUri>\n" +
        "                <soap>false</soap>\n" +
        "                <wsdlUrl></wsdlUrl>\n" +
        "            </publishedService>\n" +
        "        </exportedItem>\n" +
        "        <exportedItem mappedValue=\"false\">\n" +
        "            <headerRef type=\"Policy\" strId=\"425984\"/>\n" +
        "            <policy>\n" +
        "                <oid>425984</oid>\n" +
        "                <version>1</version>\n" +
        "                <name>Policy for service #360448, smth</name>\n" +
        "                <guid>2f2715b5-8f7c-4447-9f7c-df933f8de8d6</guid>\n" +
        "                <soap>false</soap>\n" +
        "                <type>PRIVATE_SERVICE</type>\n" +
        "                <versionActive>false</versionActive>\n" +
        "                <versionOrdinal>0</versionOrdinal>\n" +
        "                <xml>&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
        "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
        "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
        "        &lt;L7p:AuditAssertion/&gt;\n" +
        "    &lt;/wsp:All&gt;\n" +
        "&lt;/wsp:Policy&gt;\n" +
        "</xml>\n" +
        "            </policy>\n" +
        "        </exportedItem>\n" +
        "    </values>\n" +
        "</migrationBundle>";
}
