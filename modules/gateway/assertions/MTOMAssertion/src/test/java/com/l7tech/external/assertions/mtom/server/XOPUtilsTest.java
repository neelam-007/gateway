package com.l7tech.external.assertions.mtom.server;

import org.junit.Test;
import org.junit.Assert;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import com.l7tech.common.mime.StashManager;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.HexUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.message.Message;
import com.l7tech.server.StashManagerFactory;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 
 */
public class XOPUtilsTest {

    @Test
    public void testXOPRoundTrip() throws Exception {
        StashManagerFactory smf = buildStashManagerFactory();
        Message message = new Message();
        message.initialize(
                smf.createStashManager(),
                ContentTypeHeader.parseValue(mtomContentType),
                new ByteArrayInputStream(HexUtils.decodeBase64( mtom )));
        
        XOPUtils.reconstitute( message, true, smf );
        System.out.println( XmlUtil.nodeToFormattedString( message.getXmlKnob().getDocumentReadOnly() ));
        Assert.assertEquals( "Incorrect Content-Type", "text/xml", message.getMimeKnob().getOuterContentType().getMainValue() );
        Assert.assertFalse( "Contains XOP namespace", XmlUtil.nodeToFormattedString( message.getXmlKnob().getDocumentReadOnly() ).contains( XOPUtils.NS_XOP ) );

        NodeList nl = message.getXmlKnob().getDocumentReadOnly().getElementsByTagNameNS( "*", "delay" );
        XOPUtils.extract( message, Arrays.asList((Element)nl.item( 0 )), 0, true, smf );
        System.out.println( XmlUtil.nodeToFormattedString( message.getXmlKnob().getDocumentReadOnly() ));
        Assert.assertEquals( "Incorrect Content-Type", "multipart/related", message.getMimeKnob().getOuterContentType().getMainValue() );
        Assert.assertEquals( "Incorrect Content-Type type", "application/xop+xml", message.getMimeKnob().getOuterContentType().getParam("type") );
        Assert.assertEquals( "Incorrect Content-Type startinfo", "text/xml", message.getMimeKnob().getOuterContentType().getParam("startinfo") );

        XOPUtils.reconstitute( message, true, smf );
        System.out.println( XmlUtil.nodeToFormattedString( message.getXmlKnob().getDocumentReadOnly() ));
        Assert.assertEquals( "Incorrect Content-Type", "text/xml", message.getMimeKnob().getOuterContentType().getMainValue() );
        Assert.assertFalse( "Contains XOP namespace", XmlUtil.nodeToFormattedString( message.getXmlKnob().getDocumentReadOnly() ).contains( XOPUtils.NS_XOP ) );
    }

    @Test
    public void testXPODecode() throws Exception {
        StashManagerFactory smf = buildStashManagerFactory();
        Message message = new Message();
        message.initialize(
                smf.createStashManager(),
                ContentTypeHeader.parseValue(mtomContentType),
                new ByteArrayInputStream(HexUtils.decodeBase64( mtom )));

        XOPUtils.reconstitute( message, false, smf );

        System.out.println( message.getMimeKnob().getOuterContentType().getFullValue() );
        IOUtils.copyStream( message.getMimeKnob().getEntireMessageBodyAsInputStream(), System.out );

        Assert.assertEquals( "Incorrect Content-Type", "Multipart/Related", message.getMimeKnob().getOuterContentType().getMainValue() );
        Assert.assertEquals( "Incorrect Content-Type type", "application/xop+xml", message.getMimeKnob().getOuterContentType().getParam("type") );
        Assert.assertEquals( "Incorrect Content-Type startinfo", "text/xml", message.getMimeKnob().getOuterContentType().getParam("startinfo") );
        Assert.assertEquals( "Incorrect Part Content-Type", "application/xop+xml", message.getMimeKnob().getFirstPart().getContentType().getMainValue() );
        Assert.assertEquals( "Incorrect Part Content-Id", "mainpart", message.getMimeKnob().getFirstPart().getContentId(true) );
    }

    @Test
    public void testContentTypeHeader() throws Exception {
        System.out.println( ContentTypeHeader.parseValue( "text/xml;    charset=utf-8" ).getFullValue() );
    }

    @Test
    public void testXOPEncode() throws Exception {
        StashManagerFactory smf = buildStashManagerFactory();
        final String soap =
                "<soapenv:Envelope\n" +
                "    xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xmlmime=\"http://www.w3.org/2004/11/xmlmime\">\n" +
                "    <soapenv:Body>\n" +
                "        <tns:Content xmlmime:contentType=\"text/xml\" xmlns:tns=\"http://tempuri.org/tns\">VGV4dCBlbmNvZGVkIGFzIGJhc2U2NCBjb250ZW50</tns:Content>\n" +
                "    </soapenv:Body>\n" +
                "</soapenv:Envelope>";
        final Document document = XmlUtil.parse( soap );

        Message message = new Message(document);
        XOPUtils.extract( message, iter(document.getElementsByTagNameNS( "http://tempuri.org/tns", "Content" )), 0, true, smf );

        System.out.println( message.getMimeKnob().getOuterContentType().getFullValue() );
        IOUtils.copyStream( message.getMimeKnob().getEntireMessageBodyAsInputStream(), System.out );

        XOPUtils.validate( message );
    }

    private Iterable<Element> iter( final NodeList elementNodeList ) {
        Collection<Element> elements = new ArrayList<Element>();

        for ( int i=0; i<elementNodeList.getLength(); i++ ) {
            elements.add( (Element) elementNodeList.item( i ) );
        }

        return elements;
    }

    private StashManagerFactory buildStashManagerFactory() {
        return new StashManagerFactory() {
            @Override
            public StashManager createStashManager() {
                return new ByteArrayStashManager();
            }
        };
    }

    private static final String mtomContentType = "Multipart/Related;boundary=MIME_boundary; type=\"application/xop+xml\"; start=\"<mainpart>\"; startinfo=\"text/xml\"";
    private static final String mtom =
            "LS1NSU1FX2JvdW5kYXJ5DQpDb250ZW50LVR5cGU6IGFwcGxpY2F0aW9uL3hvcCt4bWw7DQogICBj\n" +
            "aGFyc2V0PVVURi04Ow0KICAgdHlwZT0idGV4dC94bWwiDQpDb250ZW50LVRyYW5zZmVyLUVuY29k\n" +
            "aW5nOiA4Yml0DQpDb250ZW50LUxlbmd0aDozODczDQpDb250ZW50LUlEOiA8bWFpbnBhcnQ+DQoN\n" +
            "Cjxzb2FwZW52OkVudmVsb3BlIHhtbG5zOnNvYXBlbnY9Imh0dHA6Ly9zY2hlbWFzLnhtbHNvYXAu\n" +
            "b3JnL3NvYXAvZW52ZWxvcGUvIiB4bWxuczp4c2Q9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1M\n" +
            "U2NoZW1hIiB4bWxuczp4c2k9Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvWE1MU2NoZW1hLWluc3Rh\n" +
            "bmNlIj4KICAgIDxzb2FwZW52OkhlYWRlcj48d3NzZTpTZWN1cml0eSBzb2FwZW52Om11c3RVbmRl\n" +
            "cnN0YW5kPSIxIiB4bWxuczp3c3NlPSJodHRwOi8vZG9jcy5vYXNpcy1vcGVuLm9yZy93c3MvMjAw\n" +
            "NC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktc2VjZXh0LTEuMC54c2QiIHhtbG5zOndz\n" +
            "dT0iaHR0cDovL2RvY3Mub2FzaXMtb3Blbi5vcmcvd3NzLzIwMDQvMDEvb2FzaXMtMjAwNDAxLXdz\n" +
            "cy13c3NlY3VyaXR5LXV0aWxpdHktMS4wLnhzZCI+PHdzdTpUaW1lc3RhbXAgd3N1OklkPSJUaW1l\n" +
            "c3RhbXAtMS1jMWQ1MDk0NDMxMDQ5MjFmMmFhYjhlODYzOThhMDQ1MyI+PHdzdTpDcmVhdGVkPjIw\n" +
            "MDktMDgtMjhUMTc6MTU6MDEuMzQyMjI5ODgzWjwvd3N1OkNyZWF0ZWQ+PHdzdTpFeHBpcmVzPjIw\n" +
            "MDktMDgtMjhUMTg6MTU6MDEuMzQyWjwvd3N1OkV4cGlyZXM+PC93c3U6VGltZXN0YW1wPjx3c3Nl\n" +
            "OkJpbmFyeVNlY3VyaXR5VG9rZW4gRW5jb2RpbmdUeXBlPSJodHRwOi8vZG9jcy5vYXNpcy1vcGVu\n" +
            "Lm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXNvYXAtbWVzc2FnZS1zZWN1cml0eS0x\n" +
            "LjAjQmFzZTY0QmluYXJ5IiBWYWx1ZVR5cGU9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dz\n" +
            "cy8yMDA0LzAxL29hc2lzLTIwMDQwMS13c3MteDUwOS10b2tlbi1wcm9maWxlLTEuMCNYNTA5djMi\n" +
            "IHdzdTpJZD0iQmluYXJ5U2VjdXJpdHlUb2tlbi0wLTYyM2M4YjY4NjEyMTVkMTBmZGQ3Y2I4ZmU5\n" +
            "MmNhYzdlIj5NSUlERERDQ0FmU2dBd0lCQWdJUU02WUVmN0ZWWXgvdFp5RVhnVkNvbVRBTkJna3Fo\n" +
            "a2lHOXcwQkFRVUZBREF3TVE0d0RBWURWUVFLREFWUFFWTkpVekVlTUJ3R0ExVUVBd3dWVDBGVFNW\n" +
            "TWdTVzUwWlhKdmNDQlVaWE4wSUVOQk1CNFhEVEExTURNeE9UQXdNREF3TUZvWERURTRNRE14T1RJ\n" +
            "ek5UazFPVm93UWpFT01Bd0dBMVVFQ2d3RlQwRlRTVk14SURBZUJnTlZCQXNNRjA5QlUwbFRJRWx1\n" +
            "ZEdWeWIzQWdWR1Z6ZENCRFpYSjBNUTR3REFZRFZRUUREQVZCYkdsalpUQ0JuekFOQmdrcWhraUc5\n" +
            "dzBCQVFFRkFBT0JqUUF3Z1lrQ2dZRUFvcWk5OUJ5MVZZbzBhSHJrS0NOVDREa0lnUEwvU2dhaGJl\n" +
            "S2RHaHJidTNLMlhHN2FyZkQ5dHFJQklLTWZyWDRHcDkwTkphODVBVjF5aU5zRXl2cSttVW5NcE5j\n" +
            "S25MWExPamtUbU1DcURZYmJrZWhKbFhQbmFXTHp2ZSttVzBwSmRQeHRmM3JiRDRQUy9jQlFJdnRw\n" +
            "am1yREFVOFZzWktUOERONUt5eitFWnNDQXdFQUFhT0JrekNCa0RBSkJnTlZIUk1FQWpBQU1ETUdB\n" +
            "MVVkSHdRc01Db3dLS0ltaGlSb2RIUndPaTh2YVc1MFpYSnZjQzVpWW5SbGMzUXVibVYwTDJOeWJD\n" +
            "OWpZUzVqY213d0RnWURWUjBQQVFIL0JBUURBZ1N3TUIwR0ExVWREZ1FXQkJRSzRsMFRVSFoxUVYz\n" +
            "VjJRdGxMTkRtK1BveGlEQWZCZ05WSFNNRUdEQVdnQlRBblNqOHdlczFvUjNXcXFxZ0hCcE53a2tQ\n" +
            "RHpBTkJna3Foa2lHOXcwQkFRVUZBQU9DQVFFQUJUcXBPcHZXKzZ5ckxYeVVsUDJ4SmJFa29oWEhJ\n" +
            "NU9Xd0tXbGVPYjlobGtoV250VWFsZmNGT0pBZ1V5SDMwVFRwSGxkengxK3ZLMkxQemhvVUZLWUhF\n" +
            "MUl5UXZva0JOMkpqRk82NEJRdWtDS25aaGxkTFJQeEdoZmtUZHhRZ2RmNXJDSy93aDN4VnNaQ05U\n" +
            "ZnVNTm1sQU02bE9BZzhRZHVEYWgzV0ZacEVBMHMybndRYUNOUVROTWpKQzh0YXYxQ0JyNitFNUZB\n" +
            "bXdQWFA3cEp4bjlGdzlPWFJ5cWJSQTR2Mnk3WXBiR2tHMkdJOVV2T0h3NlNHdmY0RlJTdGhNTU8z\n" +
            "NVlicGlrR3NMaXgzdkFzWFdXaTRyd2ZWT1l6UUswT0ZQTmk5Uk1DVWRTSDA2bTl1TFdja2lDeGpv\n" +
            "czBGUU9EWkU5bDRBVEd5OXM5aE5Wd3J5T0pUdz09PC93c3NlOkJpbmFyeVNlY3VyaXR5VG9rZW4+\n" +
            "PGRzOlNpZ25hdHVyZSB4bWxuczpkcz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2ln\n" +
            "IyI+PGRzOlNpZ25lZEluZm8+PGRzOkNhbm9uaWNhbGl6YXRpb25NZXRob2QgQWxnb3JpdGhtPSJo\n" +
            "dHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48ZHM6U2lnbmF0dXJlTWV0\n" +
            "aG9kIEFsZ29yaXRobT0iaHR0cDovL3d3dy53My5vcmcvMjAwMC8wOS94bWxkc2lnI3JzYS1zaGEx\n" +
            "Ii8+PGRzOlJlZmVyZW5jZSBVUkk9IiNCb2R5LTEtYmY5ODZkOGZlYzdiZmRmNDc1NmRjMzQ5NzVj\n" +
            "Y2VhNTYiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3\n" +
            "LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48L2RzOlRyYW5zZm9ybXM+PGRzOkRpZ2Vz\n" +
            "dE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNzaGEx\n" +
            "Ii8+PGRzOkRpZ2VzdFZhbHVlPldZNjhZQkczSHJPSlBYY3ZvWUZnV09TUmFWST08L2RzOkRpZ2Vz\n" +
            "dFZhbHVlPjwvZHM6UmVmZXJlbmNlPjxkczpSZWZlcmVuY2UgVVJJPSIjVGltZXN0YW1wLTEtYzFk\n" +
            "NTA5NDQzMTA0OTIxZjJhYWI4ZTg2Mzk4YTA0NTMiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zv\n" +
            "cm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48\n" +
            "L2RzOlRyYW5zZm9ybXM+PGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMu\n" +
            "b3JnLzIwMDAvMDkveG1sZHNpZyNzaGExIi8+PGRzOkRpZ2VzdFZhbHVlPm5Ec09EdFoxTEtBZVNP\n" +
            "RFhUQWlZaFBCYS90UT08L2RzOkRpZ2VzdFZhbHVlPjwvZHM6UmVmZXJlbmNlPjwvZHM6U2lnbmVk\n" +
            "SW5mbz48ZHM6U2lnbmF0dXJlVmFsdWU+VXR6NVVtVUp1NVBUYjNvZ3Bzb0N6ci9mQmpJZDR6VXlC\n" +
            "NTNTemttVWFMMmo4K29nZmErQ1JZME9kQzJrMnhyUWdNOEs3eVlEdXh5cHVLbDROcHU0cU12OFdE\n" +
            "S3gwZDZtN0NkR1ZzekNhdU9EVzFxZTliZXU2ZndqRVY3eWtQVXU2OXV4Z0p2UDJHTk1GUXorM0xu\n" +
            "ZUhIYW9SdktlejNYbHFyQ1VuNUZIUEhRPTwvZHM6U2lnbmF0dXJlVmFsdWU+PGRzOktleUluZm8+\n" +
            "PHdzc2U6U2VjdXJpdHlUb2tlblJlZmVyZW5jZSB4bWxuczp3c3NlPSJodHRwOi8vZG9jcy5vYXNp\n" +
            "cy1vcGVuLm9yZy93c3MvMjAwNC8wMS9vYXNpcy0yMDA0MDEtd3NzLXdzc2VjdXJpdHktc2VjZXh0\n" +
            "LTEuMC54c2QiPjx3c3NlOlJlZmVyZW5jZSBVUkk9IiNCaW5hcnlTZWN1cml0eVRva2VuLTAtNjIz\n" +
            "YzhiNjg2MTIxNWQxMGZkZDdjYjhmZTkyY2FjN2UiIFZhbHVlVHlwZT0iaHR0cDovL2RvY3Mub2Fz\n" +
            "aXMtb3Blbi5vcmcvd3NzLzIwMDQvMDEvb2FzaXMtMjAwNDAxLXdzcy14NTA5LXRva2VuLXByb2Zp\n" +
            "bGUtMS4wI1g1MDl2MyIvPjwvd3NzZTpTZWN1cml0eVRva2VuUmVmZXJlbmNlPjwvZHM6S2V5SW5m\n" +
            "bz48L2RzOlNpZ25hdHVyZT48L3dzc2U6U2VjdXJpdHk+PC9zb2FwZW52OkhlYWRlcj48c29hcGVu\n" +
            "djpCb2R5IHdzdTpJZD0iQm9keS0xLWJmOTg2ZDhmZWM3YmZkZjQ3NTZkYzM0OTc1Y2NlYTU2IiB4\n" +
            "bWxuczp3c3U9Imh0dHA6Ly9kb2NzLm9hc2lzLW9wZW4ub3JnL3dzcy8yMDA0LzAxL29hc2lzLTIw\n" +
            "MDQwMS13c3Mtd3NzZWN1cml0eS11dGlsaXR5LTEuMC54c2QiPgogICAgICAgIDx0bnM6bGlzdFBy\n" +
            "b2R1Y3RzIHhtbG5zOnRucz0iaHR0cDovL3dhcmVob3VzZS5hY21lLmNvbS93cyI+CiAgICAgICAg\n" +
            "ICAgIDx0bnM6ZGVsYXk+PEluY2x1ZGUgaHJlZj0iY2lkOnBhcnQtMSIgeG1sbnM9Imh0dHA6Ly93\n" +
            "d3cudzMub3JnLzIwMDQvMDgveG9wL2luY2x1ZGUiLz48L3RuczpkZWxheT4KICAgICAgICA8L3Ru\n" +
            "czpsaXN0UHJvZHVjdHM+CiAgICA8L3NvYXBlbnY6Qm9keT4KPC9zb2FwZW52OkVudmVsb3BlPg0K\n" +
            "LS1NSU1FX2JvdW5kYXJ5DQpDb250ZW50LVR5cGU6IGFwcGxpY2F0aW9uL29jdGV0LXN0cmVhbQ0K\n" +
            "Q29udGVudC1UcmFuc2Zlci1FbmNvZGluZzogYmluYXJ5DQpDb250ZW50LUxlbmd0aDoxMjgNCkNv\n" +
            "bnRlbnQtSUQ6IDxwYXJ0LTE+DQoNCjUB0s1oJEorgyjwEc3X/syzbqa1Lkfow2cBPfLjcgmLedVX\n" +
            "6iV3PO0WyJi8yy5xqaiqZzh5qqyl0xfx4RI6E6Lp2hugh3veu0rkNRf0eE+egpgsyo8Sz44DGtcq\n" +
            "j0Xvtxw7R8DzKKw/MF7pTYS2+hDwEBZFMhYM+Xyk9YXK1f5FDQotLU1JTUVfYm91bmRhcnktLQ0K";
}
