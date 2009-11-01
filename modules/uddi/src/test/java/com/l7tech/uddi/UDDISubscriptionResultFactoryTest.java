package com.l7tech.uddi;

import org.junit.Test;
import static org.junit.Assert.*;
import com.l7tech.common.io.XmlUtil;

/**
 *
 */
public class UDDISubscriptionResultFactoryTest {

    @Test
    public void testResultsFactory() throws Exception {
        String message =
                "<soapenv:Envelope\n" +
                "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <soapenv:Body>\n" +
                "        <notify_subscriptionListener xmlns=\"urn:uddi-org:subr_v3\">\n" +
                "            <subscriptionResultsList xmlns=\"urn:uddi-org:sub_v3\">\n" +
                "                <coveragePeriod>\n" +
                "                    <startPoint>2009-10-29T22:49:36.411-07:00</startPoint>\n" +
                "                    <endPoint>2009-10-29T22:50:46.426-07:00</endPoint>\n" +
                "                </coveragePeriod>\n" +
                "                <subscription brief=\"true\">\n" +
                "                    <subscriptionKey>uddi:65d8b570-c4fb-11de-a82a-e81aaebbee33</subscriptionKey>\n" +
                "                    <subscriptionFilter>\n" +
                "                        <get_serviceDetail xmlns=\"urn:uddi-org:api_v3\">\n" +
                "                            <serviceKey>uddi:f3ccc1ed-a94d-11de-ac2d-ad92660cd6ed</serviceKey>\n" +
                "                        </get_serviceDetail>\n" +
                "                    </subscriptionFilter>\n" +
                "                    <bindingKey xmlns=\"urn:uddi-org:api_v3\">uddi:74f44ce0-c4f6-11de-a82a-fd6aa40b6b62</bindingKey>\n" +
                "                    <notificationInterval>PT1M0.000S</notificationInterval>\n" +
                "                    <expiresAfter>2009-10-30T18:24:49.283-08:00</expiresAfter>\n" +
                "                </subscription>\n" +
                "                <keyBag>\n" +
                "                    <deleted>false</deleted>\n" +
                "                    <serviceKey xmlns=\"urn:uddi-org:api_v3\">uddi:f3ccc1ed-a94d-11de-ac2d-ad92660cd6ed</serviceKey>\n" +
                "                </keyBag>\n" +
                "            </subscriptionResultsList>\n" +
                "        </notify_subscriptionListener>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";

        UDDISubscriptionResults results = UDDISubscriptionResultFactory.buildResults( XmlUtil.parse(message) );

        assertNotNull( "Result not null", results );
        assertEquals( "Subscription key", "uddi:65d8b570-c4fb-11de-a82a-e81aaebbee33", results.getSubscriptionKey() );
        assertEquals( "Coverage start", 1256881776411L, results.getStartTime() );
        assertEquals( "Coverage end", 1256881846426L, results.getEndTime() );
        assertNotNull( "Results not null", results.getResults() );
        assertEquals( "Results size", 1, results.getResults().size() );
        assertEquals( "entity key 1", "uddi:f3ccc1ed-a94d-11de-ac2d-ad92660cd6ed", results.getResults().iterator().next().getEntityKey() );
        assertEquals( "entity deleted 1", false, results.getResults().iterator().next().isDeleted() );
    }
}
