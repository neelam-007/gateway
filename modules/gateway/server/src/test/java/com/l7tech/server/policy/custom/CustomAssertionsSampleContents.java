package com.l7tech.server.policy.custom;

/**
 * Class holder for sample contents used for unit-testing. 
 * If needed add new contents here. 
 */
@SuppressWarnings({"UnusedDeclaration", "MismatchedReadAndWriteOfArray"})
public class CustomAssertionsSampleContents {
    static public final String XML_CONTENT = "<?xml version=\"1.0\" encoding=\"utf-8\"?><xmlSource>input</xmlSource>";
    static public final String XML_CONTENT_INVALID = "<test>invalid doc. tag not closed";
    static public final String JSON_CONTENT = "{\n" +
            "\"input\": [\n" +
            "{ \"firstName\":\"John\" , \"lastName\":\"Doe\" }, \n" +
            "{ \"firstName\":\"Anna\" , \"lastName\":\"Smith\" }, \n" +
            "{ \"firstName\":\"Peter\" , \"lastName\":\"Jones\" }\n" +
            "]\n" +
            "}";
    static public final String JSON_CONTENT_INVALID = "{\n" +
            "\"input\": [\n" +
            "{ \"firstName\":\"John\" , \"lastName\":\"Doe\" }, \n" +
            "{ \"firstName\":\"Anna\" , \"lastName\":\"Smith\" , \n" +     // array not closed ("}" missing)
            "{ \"firstName\":\"Peter\" , \"lastName\":\"Jones\" }\n" +
            "]\n" +
            "}";
    static public final String JSON_SECOND_CONTENT = "{\n" +
            "\"second\": [\n" +
            "{ \"firstName\":\"John\" , \"lastName\":\"Doe\" }, \n" +
            "{ \"firstName\":\"Peter\" , \"lastName\":\"Jones\" }\n" +
            "]\n" +
            "}";
    static public final String SOAP_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
            "   <soap:Body>\n" +
            "       <mns:SoapSourceInput " +
            "xmlns:mns=\"http://warehouse.acme.com/ws\" " +
            "soap:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "           <bstrParam1 xsi:type=\"xsd:string\">param1</bstrParam1>\n" +
            "           <bstrParam2 xsi:type=\"xsd:string\">param2</bstrParam2>\n" +
            "       </mns:SoapSourceInput>\n" +
            "   </soap:Body>\n" +
            "</soap:Envelope>";
    static public final String TEXT_CONTENT = "text source test";
    static public final String BINARY_CONTENT_STRING = "binary source test";
    static public final byte[] BINARY_CONTENT = BINARY_CONTENT_STRING.getBytes();
    static public final String INPUT_STREAM_CONTENT_STRING = "input stream source test";
    static public final byte[] INPUT_STREAM_CONTENT_BYTES = INPUT_STREAM_CONTENT_STRING.getBytes();

    static public final String CRLF = "\r\n";

    // app/octet
    static public final String MULTIPART_APP_OCTET_PART_CONTENT_ID = "102";
    static public final String MULTIPART_APP_OCTET_PART_HEADER = "Content-Transfer-Encoding: 8bit" + CRLF +
            "Content-Type: application/octet-stream" + CRLF +
            "Content-ID: " + MULTIPART_APP_OCTET_PART_CONTENT_ID + CRLF;
    static public final String MULTIPART_APP_OCTET_PART_CONTENT = "require 'soap/rpc/driver'\n" +
            "require 'soap/attachment'\n" +
            "\n" +
            "attachment = ARGV.shift || __FILE__\n" +
            "\n" +
            "#server = 'http://localhost:7700/'\n" +
            "server = 'http://data.l7tech.com:80/'\n" +
            "\n" +
            "driver = SOAP::RPC::Driver.new(server, 'urn:EchoAttachmentsService')\n" +
            "driver.wiredump_dev = STDERR\n" +
            "driver.add_method('echoOne', 'file')\n" +
            "\n" +
            "File.open(attachment)  do |fin|\n" +
            "  File.open('attachment.out', 'w') do |fout|\n" +
            ".fout << driver.echoOne(SOAP::Attachment.new(fin))\n" +
            "  end      \n" +
            "end\n" +
            "\n" +
            "\n";

    // SOAP/XML
    static public final String MULTIPART_SOAP_PART_CONTENT_ID = "101";
    static public final String MULTIPART_SOAP_PART_HEADER = "Content-Transfer-Encoding: 8bit" + CRLF +
            "Content-Type: application/soap+xml; charset=utf-8" + CRLF +
            "Content-ID: " + MULTIPART_SOAP_PART_CONTENT_ID + CRLF;
    static public final String MULTIPART_SOAP_PART_CONTENT = SOAP_CONTENT + "\n";

    // json
    static public final String MULTIPART_JSON_PART_CONTENT_ID = "103";
    static public final String MULTIPART_JSON_PART_HEADER = "Content-Transfer-Encoding: 8bit" + CRLF +
            "Content-Type: application/json; charset=utf-8" + CRLF +
            "Content-ID: " + MULTIPART_JSON_PART_CONTENT_ID + CRLF;
    static public final String MULTIPART_JSON_PART_CONTENT = JSON_CONTENT + "\n";

    // xml
    static public final String MULTIPART_XML_PART_CONTENT_ID = "104";
    static public final String MULTIPART_XML_PART_HEADER = "Content-Transfer-Encoding: 8bit" + CRLF +
            "Content-Type: text/xml; charset=utf-8" + CRLF +
            "Content-ID: " + MULTIPART_XML_PART_CONTENT_ID + CRLF;
    static public final String MULTIPART_XML_PART_CONTENT = XML_CONTENT + "\n";
    

    static public final String MULTIPART_BOUNDARY = "----=Part_100000000001";

    // multipart: first part soap
    static public final String MULTIPART_FIRST_PART_SOAP_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MULTIPART_BOUNDARY + "\"; start=\"" + MULTIPART_SOAP_PART_CONTENT_ID + "\"";
    static public final String MULTIPART_FIRST_PART_SOAP_CONTENT = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;
    static public final String MULTIPART_FIRST_PART_SOAP_CONTENT_EMPTY = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;

    // multipart: first part json
    static public final String MULTIPART_FIRST_PART_JSON_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MULTIPART_BOUNDARY + "\"; start=\"" + MULTIPART_JSON_PART_CONTENT_ID + "\"";
    static public final String MULTIPART_FIRST_PART_JSON_CONTENT = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;
    static public final String MULTIPART_FIRST_PART_JSON_CONTENT_EMPTY = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;
    static public final String MULTIPART_FIRST_PART_JSON_CONTENT_INVALID = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;

    // multipart: first part xml
    static public final String MULTIPART_FIRST_PART_XML_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MULTIPART_BOUNDARY + "\"; start=\"" + MULTIPART_XML_PART_CONTENT_ID + "\"";
    static public final String MULTIPART_FIRST_PART_XML_CONTENT = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;
    static public final String MULTIPART_FIRST_PART_XML_CONTENT_EMPTY = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;
    static public final String MULTIPART_FIRST_PART_XML_CONTENT_INVALID = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;

    // multipart: first part app-octet
    static public final String MULTIPART_FIRST_PART_APP_OCTET_CONTENT_TYPE = "multipart/related; type=\"text/xml\"; boundary=\"" +
            MULTIPART_BOUNDARY + "\"; start=\"" + MULTIPART_APP_OCTET_PART_CONTENT_ID + "\"";
    static public final String MULTIPART_FIRST_PART_APP_OCTET_CONTENT = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            MULTIPART_APP_OCTET_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;
    static public final String MULTIPART_FIRST_PART_APP_OCTET_CONTENT_EMPTY = "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_APP_OCTET_PART_HEADER + CRLF +
            CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_XML_PART_HEADER + CRLF +
            MULTIPART_XML_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_JSON_PART_HEADER + CRLF +
            MULTIPART_JSON_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + CRLF +
            MULTIPART_SOAP_PART_HEADER + CRLF +
            MULTIPART_SOAP_PART_CONTENT + CRLF +
            "--" + MULTIPART_BOUNDARY + "--" + CRLF;
}
