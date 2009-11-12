package com.l7tech.server.uddi;

import org.junit.Test;
import static org.junit.Assert.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.l7tech.util.MockConfig;
import com.l7tech.uddi.UDDIRegistryInfo;

import java.util.Properties;
import java.util.Collection;

/**
 *
 */
public class UDDITemplateTest {

    @Test
    public void testReadTemplate() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.l7tech.server.uddi");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        UDDITemplate template = (UDDITemplate) unmarshaller.unmarshal( UDDITemplate.class.getResourceAsStream("uddiTemplates/CentraSite_ActiveSOA.xml") );

        assertEquals( "Inquiry URL Suffix", "inquiry", template.getInquiryUrl() );
        assertEquals( "Publish URL Suffix", "publish", template.getPublicationUrl() );
        assertEquals( "Security URL Suffix", "publish", template.getSecurityPolicyUrl() );
        assertEquals( "Subscription URL Suffix", "publish", template.getSubscriptionUrl() );

        assertNotNull( "Service metrics keyed reference", template.getServiceMetricsKeyedReference() );
        assertEquals( "Service metrics keyed reference key", "uddi:centrasite.com:management:metrics:reference", template.getServiceMetricsKeyedReference().getKey() );
        assertEquals( "Service metrics keyed reference name", "Metrics", template.getServiceMetricsKeyedReference().getName() );
        
        assertNotNull( "Metrics keyed references", template.getMetricsKeyedReferences() );
        assertEquals( "Metrics keyed references size", 12, template.getMetricsKeyedReferences().size() );
    }

    @Test
    public void testTemplates() throws Exception {
        UDDITemplateManager manager = new UDDITemplateManager( new MockConfig( new Properties() ) );
        Collection<UDDIRegistryInfo> registryTypes = manager.getTemplatesAsUDDIRegistryInfo();

        assertNotNull( "Found registry config", registryTypes );
        assertTrue( "Found registry config", !registryTypes.isEmpty() );

        for ( UDDIRegistryInfo info : registryTypes ) {
            System.out.println("Checking UDDI : " + info.getName());
            assertNotNull( "Name", info.getName() );
            assertNotNull( "Inquiry URL", info.getInquiry() );
            assertNotNull( "Publication URL", info.getPublication() );
            assertNotNull( "Security URL", info.getSecurityPolicy() );
            assertNotNull( "Security URL", info.isSupportsMetrics() );
        }
    }

    @Test
    public void testTemplateKeyedReferencePropertyValues() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.l7tech.server.uddi");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        UDDITemplate template = (UDDITemplate) unmarshaller.unmarshal( UDDITemplate.class.getResourceAsStream("uddiTemplates/CentraSite_ActiveSOA.xml") );

        assertNotNull( "Metrics keyed references", template.getMetricsKeyedReferences() );
        for ( UDDITemplate.KeyedReferenceTemplate krt : template.getMetricsKeyedReferences() ) {
            if ( krt.getValueProperty() != null ) {
                String key = krt.getValueProperty();
                int index = key.indexOf(':');
                if ( index > 0 ) {
                    key = key.substring( 0, index );
                }
                MetricsUDDITaskFactory.Metric.valueOf( key );
            }
        }
    }
}
