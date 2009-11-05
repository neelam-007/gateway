package com.l7tech.server.uddi;

import org.junit.Test;
import static org.junit.Assert.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

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
        assertEquals( "Metrics keyed references size", 11, template.getMetricsKeyedReferences().size() );
    }

    @Test
    public void testTemplateKeyedReferencePropertyValues() throws Exception {
        JAXBContext context = JAXBContext.newInstance("com.l7tech.server.uddi");
        Unmarshaller unmarshaller = context.createUnmarshaller();
        UDDITemplate template = (UDDITemplate) unmarshaller.unmarshal( UDDITemplate.class.getResourceAsStream("uddiTemplates/CentraSite_ActiveSOA.xml") );

        assertNotNull( "Metrics keyed references", template.getMetricsKeyedReferences() );
        for ( UDDITemplate.KeyedReferenceTemplate krt : template.getMetricsKeyedReferences() ) {
            if ( krt.getValueProperty() != null ) {
                MetricsUDDITaskFactory.Metric.valueOf( krt.getValueProperty() );
            }
        }
    }
}
