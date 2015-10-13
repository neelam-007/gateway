package com.l7tech.server.solutionkit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Bundle;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitsConfig;
import com.l7tech.util.IOUtils;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.transform.dom.DOMSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static com.l7tech.gateway.common.solutionkit.SolutionKit.SK_PROP_ALLOW_ADDENDUM_KEY;
import static com.l7tech.gateway.common.solutionkit.SolutionKitsConfig.MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test the AddendumBundleHandler
 */
@RunWith(MockitoJUnitRunner.class)
public class AddendumBundleHandlerTest {
    private static final String FORM_FIELD_NAME_BUNDLE = "bundle";

    @Mock
    private FormDataMultiPart formDataMultiPart;
    @Mock
    private SolutionKitsConfig solutionKitsConfig;

    @Test
    public void applyAddendumBundlesErrors() throws Exception {
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(2);
        SolutionKit solutionKit = new SolutionKit();
        solutionKit.setProperty(SK_PROP_ALLOW_ADDENDUM_KEY, "true");
        selectedSolutionKits.add(solutionKit);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        {
            // setup addendum bundle
            final FormDataBodyPart addendumPart = mock(FormDataBodyPart.class);
            when(formDataMultiPart.getField(FORM_FIELD_NAME_BUNDLE)).thenReturn(addendumPart);
            String emptyAddendumBundleStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                    "</l7:Bundle>";
            when(addendumPart.getValueAs(InputStream.class)).thenReturn(new ByteArrayInputStream(emptyAddendumBundleStr.getBytes(StandardCharsets.UTF_8)));

            final AddendumBundleHandler addendumBundleHandler = new AddendumBundleHandler(formDataMultiPart, solutionKitsConfig, FORM_FIELD_NAME_BUNDLE);

            // test # selected solution kits > 1
            SolutionKit solutionKit2 = new SolutionKit();
            solutionKit.setName("SK2 name");
            selectedSolutionKits.add(solutionKit2);
            try {
                addendumBundleHandler.apply();
                fail("Expected: error. Can't have more than one Solution Kit in scope when using addendum bundle.");
            } catch (AddendumBundleHandler.AddendumBundleException e) {
                assertThat((String) e.getResponse().getEntity(), startsWith("Can't have more than one Solution Kit in scope when using form field named '"));
            }
            selectedSolutionKits.remove(solutionKit2);

            // test not allow addendum
            solutionKit.setProperty(SK_PROP_ALLOW_ADDENDUM_KEY, "false");
            try {
                addendumBundleHandler.apply();
                fail("Expected: error.  The selected .skar file does not allow addendum bundle.");
            } catch (AddendumBundleHandler.AddendumBundleException e) {
                assertThat((String) e.getResponse().getEntity(), startsWith("The selected .skar file does not allow addendum bundle.  Form field named '"));
            }
            solutionKit.setProperty(SK_PROP_ALLOW_ADDENDUM_KEY, "true");

            // test addendum bundle has no mapping
            try {
                addendumBundleHandler.apply();
                fail("Expected: error. The addendum bundle can't have null mappings.");
            } catch (AddendumBundleHandler.AddendumBundleException e) {
                assertThat((String) e.getResponse().getEntity(), startsWith("The addendum bundle specified using form field named '"));
            }
        }

        {
            // setup addendum bundle
            final FormDataBodyPart addendumPart = mock(FormDataBodyPart.class);
            when(formDataMultiPart.getField(FORM_FIELD_NAME_BUNDLE)).thenReturn(addendumPart);
            final String bundleStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                    "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                    "    <l7:References>\n" +
                    "        <l7:Item>\n" +
                    "            <l7:Name>SSG</l7:Name>\n" +
                    "            <l7:Id>0567c6a8f0c4cc2c9fb331cb03b4de6f</l7:Id>\n" +
                    "            <l7:Type>JDBC_CONNECTION</l7:Type>\n" +
                    "            <l7:TimeStamp>2015-03-13T11:44:51.513-07:00</l7:TimeStamp>\n" +
                    "            <l7:Resource>\n" +
                    "                <l7:JDBCConnection id=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" version=\"0\">\n" +
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
                    "                        <l7:JdbcUrl>jdbc:mysql://localhost:3306/ssg</l7:JdbcUrl>\n" +
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
                    "    </l7:References>\n" +
                    "    <l7:Mappings>\n" +
                    "        <l7:Mapping action=\"NewOrExisting\" srcId=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections/0567c6a8f0c4cc2c9fb331cb03b4de6f\" type=\"JDBC_CONNECTION\">\n" +
                    "            <l7:Properties>\n" +
                    "                <l7:Property key=\"FailOnNew\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                    "            </l7:Properties>\n" +
                    "        </l7:Mapping>\n" +
                    "    </l7:Mappings>\n" +
                    "</l7:Bundle>";
            InputStream inputStream = new ByteArrayInputStream(bundleStr.getBytes(StandardCharsets.UTF_8));
            when(addendumPart.getValueAs(InputStream.class)).thenReturn(inputStream);

            final AddendumBundleHandler addendumBundleHandler = new AddendumBundleHandler(formDataMultiPart, solutionKitsConfig, FORM_FIELD_NAME_BUNDLE);

            // test skar bundle can't be null
            try {
                addendumBundleHandler.apply();
                fail("Expected: error. The .skar file bundle can't be null when addendum bundle has been specified.");
            } catch (AddendumBundleHandler.AddendumBundleException e) {
                assertThat((String) e.getResponse().getEntity(), startsWith("The .skar file bundle can't be null (nor have null references, nor null mappings) "));
            }

            // test skar bundle mapping no override flag
            inputStream.reset();
            final DOMSource bundleSource = new DOMSource();
            final Document bundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(inputStream)));
            final Element addendumBundleEle = bundleDoc.getDocumentElement();
            bundleSource.setNode(addendumBundleEle);
            final Bundle bundle = MarshallingUtils.unmarshal(Bundle.class, bundleSource, true);   // Bundle, Item and Mapping constructors are private; use marshalling instead
            inputStream.reset();
            when(solutionKitsConfig.getBundle(solutionKit)).thenReturn(bundle);
            try {
                addendumBundleHandler.apply();
                fail("Expected: error. The .skar file bundle mapping property '" + MAPPING_PROPERTY_NAME_SK_ALLOW_MAPPING_OVERRIDE + "' is missing.");
            } catch (AddendumBundleHandler.AddendumBundleException e) {
                assertThat((String) e.getResponse().getEntity(), startsWith("Unable to process addendum bundle for mapping with scrId="));
            }
        }
    }

    @Test
    public void applyAddendumBundles() throws Exception {
        final Set<SolutionKit> selectedSolutionKits = new HashSet<>(1);
        SolutionKit solutionKit = new SolutionKit();
        solutionKit.setProperty(SK_PROP_ALLOW_ADDENDUM_KEY, "true");
        selectedSolutionKits.add(solutionKit);
        when(solutionKitsConfig.getSelectedSolutionKits()).thenReturn(selectedSolutionKits);

        // setup addendum bundle
        final FormDataBodyPart addendumPart = mock(FormDataBodyPart.class);
        when(formDataMultiPart.getField(FORM_FIELD_NAME_BUNDLE)).thenReturn(addendumPart);
        final String addendumBundleStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:References>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>CHANGED! SSG</l7:Name>\n" +
                "            <l7:Id>0567c6a8f0c4cc2c9fb331cb03b4de6f</l7:Id>\n" +
                "            <l7:Type>JDBC_CONNECTION</l7:Type>\n" +
                "            <l7:TimeStamp>2015-03-13T11:44:51.513-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:JDBCConnection id=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" version=\"0\">\n" +
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
                "                        <l7:JdbcUrl>jdbc:mysql://localhost:3306/ssg</l7:JdbcUrl>\n" +
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
                "    </l7:References>\n" +
                "    <l7:Mappings>\n" +
                "        <l7:Mapping action=\"NewOrExisting\" srcId=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" srcUri=\"https://CHANGED:8443/restman/1.0/jdbcConnections/0567c6a8f0c4cc2c9fb331cb03b4de6f\" type=\"JDBC_CONNECTION\">\n" +
                "            <l7:Properties>\n" +
                "                <l7:Property key=\"FailOnNew\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                "                <l7:Property key=\"SK_AllowMappingOverride\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                "            </l7:Properties>\n" +
                "        </l7:Mapping>\n" +
                "    </l7:Mappings>\n" +
                "</l7:Bundle>";
        final InputStream addendumInputStream = new ByteArrayInputStream(addendumBundleStr.getBytes(StandardCharsets.UTF_8));
        when(addendumPart.getValueAs(InputStream.class)).thenReturn(addendumInputStream);

        // setup skar bundle (REPLACE reference item)
        String bundleStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:References>\n" +
                "        <l7:Item>\n" +
                "            <l7:Name>SSG</l7:Name>\n" +
                "            <l7:Id>0567c6a8f0c4cc2c9fb331cb03b4de6f</l7:Id>\n" +
                "            <l7:Type>JDBC_CONNECTION</l7:Type>\n" +
                "            <l7:TimeStamp>2015-03-13T11:44:51.513-07:00</l7:TimeStamp>\n" +
                "            <l7:Resource>\n" +
                "                <l7:JDBCConnection id=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" version=\"0\">\n" +
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
                "                        <l7:JdbcUrl>jdbc:mysql://localhost:3306/ssg</l7:JdbcUrl>\n" +
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
                "    </l7:References>\n" +
                "    <l7:Mappings>\n" +
                "        <l7:Mapping action=\"NewOrExisting\" srcId=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections/0567c6a8f0c4cc2c9fb331cb03b4de6f\" type=\"JDBC_CONNECTION\">\n" +
                "            <l7:Properties>\n" +
                "                <l7:Property key=\"FailOnNew\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                "                <l7:Property key=\"SK_AllowMappingOverride\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                "            </l7:Properties>\n" +
                "        </l7:Mapping>\n" +
                "    </l7:Mappings>\n" +
                "</l7:Bundle>";
        InputStream inputStream = new ByteArrayInputStream(bundleStr.getBytes(StandardCharsets.UTF_8));
        DOMSource bundleSource = new DOMSource();
        Document bundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(inputStream)));
        Element addendumBundleEle = bundleDoc.getDocumentElement();
        bundleSource.setNode(addendumBundleEle);
        Bundle bundle = MarshallingUtils.unmarshal(Bundle.class, bundleSource, true);   // Bundle, Item and Mapping constructors are private; use marshalling instead
        when(solutionKitsConfig.getBundle(solutionKit)).thenReturn(bundle);

        // make sure we have 1 of each before we start
        assertEquals(1, bundle.getReferences().size());
        assertEquals(1, bundle.getMappings().size());

        final AddendumBundleHandler addendumBundleHandler = new AddendumBundleHandler(formDataMultiPart, solutionKitsConfig, FORM_FIELD_NAME_BUNDLE);

        // apply addendum bundle
        addendumBundleHandler.apply();

        // make sure we still have 1 of each after
        assertEquals(1, bundle.getMappings().size());
        assertEquals(1, bundle.getReferences().size());

        // test bundle mapping replaced with addendum bundle mapping
        assertThat(bundle.getMappings().get(0).getSrcUri(), startsWith("https://CHANGED"));

        // test skar bundle reference item replaced with addendum bundle reference item
        assertThat(bundle.getReferences().get(0).getName(), startsWith("CHANGED! "));

        // setup skar bundle (ADD reference item)
        addendumInputStream.reset();
        bundleStr = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:References>\n" +
                "    </l7:References>\n" +
                "    <l7:Mappings>\n" +
                "        <l7:Mapping action=\"NewOrExisting\" srcId=\"0567c6a8f0c4cc2c9fb331cb03b4de6f\" srcUri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/jdbcConnections/0567c6a8f0c4cc2c9fb331cb03b4de6f\" type=\"JDBC_CONNECTION\">\n" +
                "            <l7:Properties>\n" +
                "                <l7:Property key=\"FailOnNew\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                "                <l7:Property key=\"SK_AllowMappingOverride\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>\n" +
                "            </l7:Properties>\n" +
                "        </l7:Mapping>\n" +
                "    </l7:Mappings>\n" +
                "</l7:Bundle>";
        inputStream = new ByteArrayInputStream(bundleStr.getBytes(StandardCharsets.UTF_8));
        bundleSource = new DOMSource();
        bundleDoc = XmlUtil.parse(new ByteArrayInputStream(IOUtils.slurpStream(inputStream)));
        addendumBundleEle = bundleDoc.getDocumentElement();
        bundleSource.setNode(addendumBundleEle);
        bundle = MarshallingUtils.unmarshal(Bundle.class, bundleSource, true);   // Bundle, Item and Mapping constructors are private; use marshalling instead
        when(solutionKitsConfig.getBundle(solutionKit)).thenReturn(bundle);

        // make sure we have 0 references items and 1 mapping before we start
        assertEquals(0, bundle.getReferences().size());
        assertEquals(1, bundle.getMappings().size());

        // apply addendum bundle
        addendumBundleHandler.apply();

        // test that addendum bundle reference item was added to skar bundle
        assertEquals(1, bundle.getReferences().size());
        assertEquals(1, bundle.getMappings().size());
    }
}
