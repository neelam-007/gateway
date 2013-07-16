package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyValidationResult;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

public class PolicyValidationMarshallerTest {
    private PolicyValidationMarshaller marshaller;
    @Mock
    private PolicyValidationResult result;
    // this is not the EXACT format that jaxb uses by default - jaxb actually adds a colon to the time zone
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private Date lastUpdate;

    @Before
    public void setup() throws Exception {
        marshaller = PolicyValidationMarshaller.getInstance();
        final Calendar calendar = new GregorianCalendar(2012, Calendar.JANUARY, 1, 1, 1, 1);
        calendar.set(Calendar.MILLISECOND, 1);
        calendar.setTimeZone(TimeZone.getTimeZone("GMT-7:00"));
        lastUpdate = calendar.getTime();
    }

    @Test
    public void marshall() throws Exception {
        final PolicyValidationResult pvr = ManagedObjectFactory.createPolicyValidationResult();
        pvr.setStatus(PolicyValidationResult.ValidationStatus.OK);
        final String xml = marshaller.marshal(pvr);
        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(pvr)), StringUtils.deleteWhitespace(xml));

        pvr.setStatus(PolicyValidationResult.ValidationStatus.ERROR);
        final String xmlError = marshaller.marshal(pvr);
        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(pvr)), StringUtils.deleteWhitespace(xmlError));

        pvr.setStatus(PolicyValidationResult.ValidationStatus.WARNING);
        final String xmlWarning = marshaller.marshal(pvr);
        assertEquals(StringUtils.deleteWhitespace(buildExpectedXml(pvr)), StringUtils.deleteWhitespace(xmlWarning));
    }

    private String buildExpectedXml(final PolicyValidationResult result) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<ns1:PolicyValidationResult xmlns:ns1=\"http://ns.l7tech.com/2010/04/gateway-management\">");
        stringBuilder.append("<ns1:ValidationStatus>");
        if (result.getStatus() == PolicyValidationResult.ValidationStatus.ERROR) {
            stringBuilder.append("Error");
        } else if (result.getStatus() == PolicyValidationResult.ValidationStatus.WARNING) {
            stringBuilder.append("Warning");
        } else {
            stringBuilder.append(result.getStatus().toString());
        }
        stringBuilder.append("</ns1:ValidationStatus>");
        stringBuilder.append("</ns1:PolicyValidationResult>");
        return stringBuilder.toString();
    }

}
