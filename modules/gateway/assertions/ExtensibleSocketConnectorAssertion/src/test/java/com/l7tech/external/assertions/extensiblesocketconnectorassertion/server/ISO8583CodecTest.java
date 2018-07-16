package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583.ISO8583EncoderType;
import org.apache.mina.core.buffer.IoBuffer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 04/02/13
 * Time: 10:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583CodecTest {

    private String dataString = "<Message>" +
            "<MessageTypeIndicator version=\"1\" class=\"5\" function=\"2\" origin=\"5\"/>" +
            "<Fields>" +
            "<Field position=\"2\">" +
            "222222" +
            "</Field>" +
            "<Field position=\"3\">" +
            "333333" +
            "</Field>" +
            "<Field position=\"4\">" +
            "444444" +
            "</Field>" +
            "<Field position=\"41\">" +
            "Field Forty One." +
            "</Field>" +
            "<Field position=\"42\">" +
            "     Forty Two." +
            "</Field>" +
            "<Field position=\"72\">" +
            "Field 72 Variable Text." +
            "</Field>" +
            "</Fields>" +
            "</Message>";

    /* Pretty formatted.  Byte equivalent is the dataByte variable below.
    <Message>
        <MessageTypeIndicator version="1" class="5" function="2" origin="5"/>
        <Fields>
            <Field position="2">222222</Field>  //field2 6x2
            <Field position="3">333333</Field>  //field3 6x3
            <Field position="4">444444</Field>  //field4 6x4
            <Field position="41">Field Forty One.</Field>
            <Field position="42">     Forty Two.</Field>
            <Field position="72">Field 72 Variable Text.</Field>
        </Fields>
    </Message>
     */

    // ISO 8583 message structure: Message header, Message Type Identifier, One or more bitmap indicating which data elements are present in the message, Data elements or fields
    // Assumption: messageDelimiterString = "03" as defined from ISO8583CodecConfiguration
    // See https://sites.google.com/site/paymentsystemsblog/iso8583-financial-transaction-message-format for description of message format.
    private byte[] dataByte = new byte[]{
            // MTI (Message Type Identifier)
            // 1     5     2     5
            0x31, 0x35, 0x32, 0x35,

            // BITMAP(S)
            //primary bitmap  First bit of each bitmap signifies presence of next bitmap.
            //     1     2     3     4     5            6     7     8
            (byte) 0xf0, 0x00, 0x00, 0x00, 0x00, (byte) 0xc0, 0x00, 0x00,
            //1 1111 0000   field 2,3,4
            //2 0000 0000
            //3 0000 0000
            //4 0000 0000
            //5 0000 0000
            //6 1100 0000  field41, 42
            //7 0000 0000
            //8 0000 0000

            //secondary bitmap
            //     9     10    11    12    13    14    15    16
            (byte) 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            //9  0000 0001   field 72

            //tertiary bitmap would go here if secondary bitmap first bit was 1.
            //0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,

            // DATA ELEMENTS
            // Field2 PAN n..19 LLVAR
            //Len   Len  Value
            //  0     6     2     2     2     2     2     2
             0x30, 0x36, 0x32, 0x32, 0x32, 0x32, 0x32, 0x32,

            // Field3 processing code n6
            //  3     3     3     3     3     3
             0x33, 0x33, 0x33, 0x33, 0x33, 0x33,

            // Field4  amount transaction n12
            //                                      4     4     4     4     4     4
             0x30, 0x30, 0x30, 0x30, 0x30, 0x30, 0x34, 0x34, 0x34, 0x34, 0x34, 0x34,//field4  padded with 0's + 6x4's. size=12 elements.

            // Field41 card acceptor terminal identification ans 16 (Alpha, Numeric,Special chars) (length=16)
            // F  i     e     l     d           F     o     r     t     y           O     n     e     .
            0x46, 0x69, 0x65, 0x6c, 0x64, 0x20, 0x46, 0x6f, 0x72, 0x74, 0x79, 0x20, 0x4f, 0x6e, 0x65, 0x2e,//field 41 card acceptor terminal id 16 elements

            // Field42 card acceptor identification code ans 15 (length=15)
            //                            F     o     r     t     y           T     w     o     .
            0x20, 0x20, 0x20, 0x20, 0x20, 0x46, 0x6f, 0x72, 0x74, 0x79, 0x20, 0x54, 0x77, 0x6f, 0x2e,//field 42 card acceptor id code 15 elements

            // Field 72  data record ans..999 LLLVAR
            // 0     2     3   F     i     e     l     d           7     2           V     a     r     i     a     b     l     e           T     e     x     t     .
             0x30, 0x32, 0x33, 0x46, 0x69, 0x65, 0x6c, 0x64, 0x20, 0x37, 0x32, 0x20, 0x56, 0x61, 0x72, 0x69, 0x61, 0x62, 0x6c, 0x65, 0x20, 0x54, 0x65, 0x78, 0x74, 0x2e,//field 72 LLLVAR
            //len  len   len   value

    };


    private ISO8583Codec iso8583Codec = new ISO8583Codec();

    @Before
    public void setupCodec() {
        iso8583Codec.setFieldPropertiesFileLocation("DEFAULT");
        iso8583Codec.setMtiEncodingType(ISO8583EncoderType.ASCII);
        iso8583Codec.setSecondaryBitmapMandatory(false);
        iso8583Codec.setTertiaryBitmapMandatory(false);
        iso8583Codec.initializeISO8583Schema();
        iso8583Codec.setMessageDelimiter(new byte[]{});
    }

    @Test
    public void testEncodeDefault() throws Exception {

        EncoderOut eOut = new EncoderOut();
        byte[] actual;
        byte[] expected = dataByte;

        System.out.println("The XML to encode is:"+dataString);

        //execute test, Encode our XML to ISO8583 format
        iso8583Codec.getEncoder(null).encode(null, dataString, eOut);
        actual = eOut.getOutput();

        //for visual test
        //print expected
        System.out.print("Expected: [");
        printRawByteArray(expected);
        System.out.println("]");

        //print actual
        System.out.print("Actual  : [");
        printRawByteArray(actual);
        System.out.println("]");

        //Test that the ISO8583 message is formatted correctly
        Assert.assertArrayEquals(expected, actual);
    }

    @Test
    public void testDecodeDefault() throws Exception {

        DecoderOut dOut = new DecoderOut();
        IoBuffer ioBuffer = null;
        String expected = dataString;
        String actual = "";
        Document expectedDom = null;
        Document actualDom = null;

        //create a byte buffer
        ioBuffer = IoBuffer.allocate(dataByte.length);
        ioBuffer.put(dataByte);
        ioBuffer.flip();

        //execute the test
        iso8583Codec.getDecoder(null).decode(null, ioBuffer, dOut);
        actual = dOut.getOutput();

        //Create two dom objects and check that they are equal.
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        expectedDom = db.parse(new ByteArrayInputStream(expected.getBytes()));
        expectedDom.normalizeDocument();

        actualDom = db.parse(new ByteArrayInputStream(actual.getBytes()));
        actualDom.normalizeDocument();

        System.out.println("The decoded XML document is "+actual);

        //Compare both dom objects
        Assert.assertEquals("XML Documents are equal: ", true, expectedDom.isEqualNode(actualDom));
    }

    private void printRawByteArray(byte[] data) {

        for (byte b : data)
            System.out.print("|" + b);
    }
}
