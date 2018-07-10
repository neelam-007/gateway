package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 26/02/13
 * Time: 9:10 AM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583TestEncoderPackedBCD implements ISO8583TestEncoder {

    private ISO8583Encoder encoder = null;

    @Before
    public void setUp() {
        encoder = new ISO8583EncoderPackedBCD();
    }

    @Override
    //@Test
    public void testEncodingMTI() throws Exception {

        int version = 2;
        int messageClass = 6;
        int function = 0;
        int origin = 3;
        byte[] expectedMti = new byte[]{0x26, 0x03};

        ISO8583MessageTypeIndicator messageTypeIndicator = null;
        byte[] actualMti = null;

        //test marshaling from int to ISO8583
        actualMti = encoder.marshalMTI(version, messageClass, function, origin);
        Assert.assertArrayEquals(expectedMti, actualMti);

        //test marshaling from ISO8583 to int
        messageTypeIndicator = encoder.unmarshalMTI(expectedMti);
        Assert.assertEquals(version, messageTypeIndicator.getVersion());
        Assert.assertEquals(messageClass, messageTypeIndicator.getMessageClass());
        Assert.assertEquals(function, messageTypeIndicator.getFunction());
        Assert.assertEquals(origin, messageTypeIndicator.getOrigin());
    }

    @Override
    @Test
    public void testEncodingStaticFieldNonPadded() throws Exception {

        byte[] expectedBytes = null;
        byte[] iso8583Message = null;
        String data = "123456";
        ISO8583MarshaledFieldData marshaledFieldData = null;
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;
        ISO8583DataElement dataElement = null;

        //test marshaling numeric
        //marshal from string to ISO8583
        //data element represents field 3 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(6);
        dataElement.setVariable(false);
        dataElement.setVariableFieldLength(0);
        expectedBytes = new byte[]{0x12, 0x34, 0x56};

        marshaledFieldData = encoder.marshalField(data, dataElement);
        Assert.assertArrayEquals(expectedBytes, marshaledFieldData.getData());

        //unmarshal from ISO8583 to string
        iso8583Message = new byte[]{0x41, 0x42, 0x12, 0x34, 0x56, 0x43, 0x44};
        int startIndex = 2;
        int bytesRead = 3;

        unmarshaledFieldData = encoder.unmarshalField(iso8583Message, startIndex, dataElement);
        Assert.assertEquals(bytesRead, unmarshaledFieldData.getBytesRead());
        Assert.assertEquals(data, unmarshaledFieldData.getData());
    }

    @Override
    @Test
    public void testEncodingStaticFieldPadded() throws Exception {

        byte[] expectedBytes = null;
        byte[] iso8583Message = null;
        String data = "123";
        ISO8583MarshaledFieldData marshaledFieldData = null;
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;
        ISO8583DataElement dataElement = null;

        //test marshaling numeric
        //marshal from string to ISO8583
        //data element represents field 3 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(6);
        dataElement.setVariable(false);
        dataElement.setVariableFieldLength(0);
        expectedBytes = new byte[]{0x00, 0x01, 0x23};

        marshaledFieldData = encoder.marshalField(data, dataElement);
        Assert.assertArrayEquals(expectedBytes, marshaledFieldData.getData());

        //unmarshal from ISO8583 to string
        iso8583Message = new byte[]{0x41, 0x42, 0x00, 0x01, 0x23, 0x43, 0x44};
        String unmarshaledData = "000123";
        int startIndex = 2;
        int bytesRead = 3;

        unmarshaledFieldData = encoder.unmarshalField(iso8583Message, startIndex, dataElement);
        Assert.assertEquals(bytesRead, unmarshaledFieldData.getBytesRead());
        Assert.assertEquals(unmarshaledData, unmarshaledFieldData.getData());
    }

    @Override
    @Test
    public void testEncodingVariableField() throws Exception {

        String numericData = "";
        byte[] numericByte = null;
        String alphaNumericData = "";
        byte[] alphaNumericByte = null;
        byte[] isoMessage = null;
        int startIndex = 0;
        int bytesRead = 0;
        String unmarshalledString = "";
        ISO8583DataElement dataElement = null;
        ISO8583MarshaledFieldData marshaledFieldData = null;
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;

        //marshal variable length numeric field
        //data element represents field 32 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(11);
        dataElement.setVariable(true);
        dataElement.setVariableFieldLength(2);
        numericData = "9876543";
        numericByte = new byte[]{0x07, 0x09, (byte) 0x87, 0x65, 0x43};

        marshaledFieldData = encoder.marshalField(numericData, dataElement);
        Assert.assertArrayEquals(numericByte, marshaledFieldData.getData());

        //unmarshal variable length numeric field
        isoMessage = new byte[]{0x41, 0x42, 0x07, 0x09, (byte) 0x87, 0x65, 0x43, 0x43, 0x44};
        unmarshalledString = "09876543";
        startIndex = 2;
        bytesRead = 5;

        unmarshaledFieldData = encoder.unmarshalField(isoMessage, startIndex, dataElement);
        Assert.assertEquals(unmarshalledString, unmarshaledFieldData.getData());
        Assert.assertEquals(bytesRead, unmarshaledFieldData.getBytesRead());

        //marshal variable length alphanumeric field
        //data element represents field 44 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.ALPHANUMERIC);
        dataElement.setLength(25);
        dataElement.setVariable(true);
        dataElement.setVariableFieldLength(2);
        alphaNumericData = "ABCD123";
        alphaNumericByte = new byte[]{0x07, 0x41, 0x42, 0x43, 0x44, 0x31, 0x32, 0x33};

        marshaledFieldData = encoder.marshalField(alphaNumericData, dataElement);
        Assert.assertArrayEquals(alphaNumericByte, marshaledFieldData.getData());

        //unmarshal variable length alphanumeric field
        isoMessage = new byte[]{0x41, 0x42, 0x06, 0x41, 0x42, 0x43, 0x31, 0x32, 0x33, 0x43, 0x44};
        unmarshalledString = "ABC123";
        startIndex = 2;
        bytesRead = 7;

        unmarshaledFieldData = encoder.unmarshalField(isoMessage, startIndex, dataElement);
        Assert.assertEquals(unmarshalledString, unmarshaledFieldData.getData());
        Assert.assertEquals(bytesRead, unmarshaledFieldData.getBytesRead());
    }

    @Override
    @Test
    public void testEncodingErrors() throws Exception {

        //expected variables
        String expectedErrorMessage = "";

        //data variables
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;
        ISO8583DataElement dataElement = null;
        int startIndex = 0;
        byte[] data = null;

        //test static field is longer than message
        //data element represents field 3 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(6);
        dataElement.setVariable(false);
        dataElement.setVariableFieldLength(0);
        expectedErrorMessage = "Missing data, field exceeds message length.";
        data = new byte[]{0x46, 0x46, 0x30, 0x32};
        startIndex = 2;

        unmarshaledFieldData = encoder.unmarshalField(data, startIndex, dataElement);

        Assert.assertEquals(true, unmarshaledFieldData.isErrored());
        Assert.assertEquals(expectedErrorMessage, unmarshaledFieldData.getErrorMessage());

        //test variable field header is longer than message
        //data element represents field 32 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(11);
        dataElement.setVariable(true);
        dataElement.setVariableFieldLength(2);
        expectedErrorMessage = "Missing data, length of leading digits exceeds message length.";
        data = new byte[]{0x46, 0x46, 0x30};
        startIndex = 2;

        unmarshaledFieldData = encoder.unmarshalField(data, startIndex, dataElement);

        Assert.assertEquals(true, unmarshaledFieldData.isErrored());
        Assert.assertEquals(expectedErrorMessage, unmarshaledFieldData.getErrorMessage());

        //test variable field length is longer than max field size
        expectedErrorMessage = "Invalid data, field data exceeds maximum field size.";
        data = new byte[]{0x46, 0x46, (byte) 0xA1, 0x01, 0x23, 0x45, 0x67, (byte) 0x89, 0x01, 0x46, 0x46};
        startIndex = 2;

        unmarshaledFieldData = encoder.unmarshalField(data, startIndex, dataElement);

        Assert.assertEquals(true, unmarshaledFieldData.isErrored());
        Assert.assertEquals(expectedErrorMessage, unmarshaledFieldData.getErrorMessage());

        //test variable field is longer than message
        expectedErrorMessage = "Missing data, field exceeds message length.";
        data = new byte[]{0x46, 0x46, 0x06, 0x12, 0x34};
        startIndex = 2;

        unmarshaledFieldData = encoder.unmarshalField(data, startIndex, dataElement);

        Assert.assertEquals(true, unmarshaledFieldData.isErrored());
        Assert.assertEquals(expectedErrorMessage, unmarshaledFieldData.getErrorMessage());
    }
}
