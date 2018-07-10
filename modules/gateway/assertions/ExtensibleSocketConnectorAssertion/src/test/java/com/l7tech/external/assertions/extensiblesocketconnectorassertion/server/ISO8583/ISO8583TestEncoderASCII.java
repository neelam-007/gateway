package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 19/02/13
 * Time: 2:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583TestEncoderASCII implements ISO8583TestEncoder {

    private ISO8583Encoder encoder = null;

    @Before
    public void setUp() {
        encoder = new ISO8583EncoderASCII();
    }

    @Test
    public void testEncodingMTI() throws Exception {

        //setup mti data
        int version = 2;
        int messageClass = 6;
        int function = 0;
        int origin = 3;
        byte[] exptectedMtiBytes = new byte[]{0x32, 0x36, 0x30, 0x33};
        ISO8583MessageTypeIndicator expectedMti = new ISO8583MessageTypeIndicator(2, 6, 0, 3);

        //execute tests
        //from int to ISO8583
        byte[] actualBytes = encoder.marshalMTI(version, messageClass, function, origin);
        Assert.assertArrayEquals(exptectedMtiBytes, actualBytes);

        //from ISO8583 to int
        ISO8583MessageTypeIndicator actualMti = encoder.unmarshalMTI(exptectedMtiBytes);
        Assert.assertEquals(expectedMti.getVersion(), actualMti.getVersion());
        Assert.assertEquals(expectedMti.getMessageClass(), actualMti.getMessageClass());
        Assert.assertEquals(expectedMti.getFunction(), actualMti.getFunction());
        Assert.assertEquals(expectedMti.getOrigin(), actualMti.getOrigin());
    }

    @Test
    public void testEncodingStaticFieldNonPadded() throws Exception {
        ISO8583DataElement dataElement = null;

        //setup static field data
        //numeric
        String staticNumericString = "123456";
        byte[] staticNumericMarshalByte = new byte[]{0x31, 0x32, 0x33, 0x34, 0x35, 0x36};
        byte[] staticNumericUnmarshalMessage = new byte[]{0x41, 0x42, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x43, 0x44};
        int staticNumericUnmarshalStartIndex = 2;
        int staticNumericBytesRead = 6;

        //alphanumeric
        String staticAlphaNumericString = "ABC123";
        byte[] staticAlphaNumericMarshalByte = new byte[]{0x41, 0x42, 0x43, 0x31, 0x32, 0x33};
        byte[] staticAlphaNumericUnmarshalMessage = new byte[]{0x46, 0x46, 0x41, 0x42, 0x43, 0x31, 0x32, 0x33, 0x46, 0x46};
        int staticAlphaNumericUnmarshalStartIndex = 2;
        int staticAlphaNumericBytesRead = 6;

        //actual variables
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;
        ISO8583MarshaledFieldData marshaledFieldData = null;

        //marshal numeric fields
        //data element represents field 3 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(6);
        dataElement.setVariable(false);
        dataElement.setVariableFieldLength(0);

        marshaledFieldData = encoder.marshalField(staticNumericString, dataElement);
        Assert.assertArrayEquals(staticNumericMarshalByte, marshaledFieldData.getData());

        //unmarshal numeric
        unmarshaledFieldData = encoder.unmarshalField(staticNumericUnmarshalMessage,
                staticNumericUnmarshalStartIndex, dataElement);
        Assert.assertEquals(staticNumericString, unmarshaledFieldData.getData());
        Assert.assertEquals(staticNumericBytesRead, unmarshaledFieldData.getBytesRead());

        //marshal alphanumeric
        //data element represents field 38 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.ALPHANUMERIC);
        dataElement.setLength(6);
        dataElement.setVariable(false);
        dataElement.setVariableFieldLength(0);

        marshaledFieldData = encoder.marshalField(staticAlphaNumericString, dataElement);
        Assert.assertArrayEquals(staticAlphaNumericMarshalByte, marshaledFieldData.getData());

        //unmarshal alphnumeric
        unmarshaledFieldData = encoder.unmarshalField(staticAlphaNumericUnmarshalMessage,
                staticAlphaNumericUnmarshalStartIndex, dataElement);
        Assert.assertEquals(staticAlphaNumericString, unmarshaledFieldData.getData());
        Assert.assertEquals(staticAlphaNumericBytesRead, unmarshaledFieldData.getBytesRead());
    }

    @Test
    public void testEncodingStaticFieldPadded() throws Exception {
        ISO8583DataElement dataElement = null;

        //setup padded static field data
        //numeric
        String staticPaddedNumericMarshalString = "123";
        byte[] staticPaddedNumericMarshalByte = new byte[]{0x30, 0x30, 0x30, 0x31, 0x32, 0x33};
        String staticPaddedNumericUnmarshalString = "000123";
        byte[] staticPaddedNumericUnmarshalMessage = new byte[]{0x46, 0x46, 0x30, 0x30, 0x30, 0x31, 0x32, 0x33, 0x46, 0x46};
        int staticPaddedNumericUnmarshalStartIndex = 2;
        int staticPaddedNumericBytesRead = 6;

        //alphanumeric
        String staticPaddedAlphaNumericMarshalString = "A2C";
        byte[] staticPaddedAlphaNumericMarshalByte = new byte[]{0x41, 0x32, 0x43, 0x20, 0x20, 0x20};
        String staticPaddedAlphaNumericUnmarshalString = "   A2C";
        byte[] staticPaddedAlphaNumericUnmarshalMessage = new byte[]{0x46, 0x46, 0x20, 0x20, 0x20, 0x41, 0x32, 0x43, 0x46, 0x46};
        int staticPaddedAlphaNumericUnmarshalStartIndex = 2;
        int staticPaddedAlphaNumericBytesRead = 6;

        //actual variables
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;
        ISO8583MarshaledFieldData marshaledFieldData = null;
        byte[] actualByte = null;

        //marshal numeric field needs to be padded
        //data element represents field 3 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(6);
        dataElement.setVariable(false);
        dataElement.setVariableFieldLength(0);

        marshaledFieldData = encoder.marshalField(staticPaddedNumericMarshalString, dataElement);
        Assert.assertArrayEquals(staticPaddedNumericMarshalByte, marshaledFieldData.getData());

        //unmarshal padded numeric field
        unmarshaledFieldData = encoder.unmarshalField(staticPaddedNumericUnmarshalMessage,
                staticPaddedNumericUnmarshalStartIndex, dataElement);
        Assert.assertEquals(staticPaddedNumericUnmarshalString, unmarshaledFieldData.getData());
        Assert.assertEquals(staticPaddedNumericBytesRead, unmarshaledFieldData.getBytesRead());

        //marshal alphanumeric field needs to be padded
        //data element represents field 38 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.ALPHANUMERIC);
        dataElement.setLength(6);
        dataElement.setVariable(false);
        dataElement.setVariableFieldLength(0);

        marshaledFieldData = encoder.marshalField(staticPaddedAlphaNumericMarshalString, dataElement);
        Assert.assertArrayEquals(staticPaddedAlphaNumericMarshalByte, marshaledFieldData.getData());

        //unmarshal padded alphanumeric field
        unmarshaledFieldData = encoder.unmarshalField(staticPaddedAlphaNumericUnmarshalMessage,
                staticPaddedAlphaNumericUnmarshalStartIndex, dataElement);
        Assert.assertEquals(staticPaddedAlphaNumericUnmarshalString, unmarshaledFieldData.getData());
        Assert.assertEquals(staticPaddedAlphaNumericBytesRead, unmarshaledFieldData.getBytesRead());
    }

    @Test
    public void testEncodingVariableField() throws Exception {
        ISO8583DataElement dataElement = null;

        //setup variable field data
        //numeric
        String variableNumericString = "1234";
        byte[] variableNumericMarshalByte = new byte[]{0x30, 0x34, 0x31, 0x32, 0x33, 0x34};
        byte[] variableNumericUnmarshalMessage = new byte[]{0x46, 0x46, 0x30, 0x34, 0x31, 0x32, 0x33, 0x34, 0x46, 0x46};
        int variableNumericUnmarshalStartIndex = 2;
        int variableNumericBytesRead = 6;

        //alphanumeric
        String variableAlphaNumericString = "A2C3";
        byte[] variableAlphaNumericMarshalByte = new byte[]{0x30, 0x34, 0x41, 0x32, 0x43, 0x33};
        byte[] variableAlphaNumericUnmarshalByte = new byte[]{0x46, 0x46, 0x30, 0x34, 0x41, 0x32, 0x43, 0x33, 0x46, 0x46};
        int variableAlphaNumericUnmarshalStartIndex = 2;
        int variableAlphaNumericBytesRead = 6;

        //actual variables
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;
        ISO8583MarshaledFieldData marshaledFieldData = null;
        byte[] actualByte = null;

        //marshal variable length numeric field
        //data element represents field 32 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.NUMERIC);
        dataElement.setLength(11);
        dataElement.setVariable(true);
        dataElement.setVariableFieldLength(2);

        marshaledFieldData = encoder.marshalField(variableNumericString, dataElement);
        Assert.assertArrayEquals(variableNumericMarshalByte, marshaledFieldData.getData());

        //unmarshal variable length numeric field
        unmarshaledFieldData = encoder.unmarshalField(variableNumericUnmarshalMessage,
                variableNumericUnmarshalStartIndex, dataElement);
        Assert.assertEquals(variableNumericString, unmarshaledFieldData.getData());
        Assert.assertEquals(variableNumericBytesRead, unmarshaledFieldData.getBytesRead());

        //marshal variable length alphanumeric field
        //data element represents field 44 from ISO8583 spec
        dataElement = new ISO8583DataElement();
        dataElement.setEncoderType(ISO8583EncoderType.ASCII);
        dataElement.setDataType(ISO8583DataType.ALPHANUMERIC);
        dataElement.setLength(25);
        dataElement.setVariable(true);
        dataElement.setVariableFieldLength(2);

        marshaledFieldData = encoder.marshalField(variableAlphaNumericString, dataElement);
        Assert.assertArrayEquals(variableAlphaNumericMarshalByte, marshaledFieldData.getData());

        //unmarshal variable length alphanumeric field
        unmarshaledFieldData = encoder.unmarshalField(variableAlphaNumericUnmarshalByte,
                variableAlphaNumericUnmarshalStartIndex, dataElement);
        Assert.assertEquals(variableAlphaNumericString, unmarshaledFieldData.getData());
        Assert.assertEquals(variableAlphaNumericBytesRead, unmarshaledFieldData.getBytesRead());
    }

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
        data = new byte[]{0x46, 0x46, 0x30, 0x32, 0x33, 0x34};
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

        //test variable header is not integer
        expectedErrorMessage = "Invalid data, leading digits are not numeric.";
        data = new byte[]{0x46, 0x46, 0x30, 0x41, 0x31, 0x32, 0x33, 0x34, 0x46, 0x46};
        startIndex = 2;

        unmarshaledFieldData = encoder.unmarshalField(data, startIndex, dataElement);

        Assert.assertEquals(true, unmarshaledFieldData.isErrored());
        Assert.assertEquals(expectedErrorMessage, unmarshaledFieldData.getErrorMessage());

        //test variable field length is longer than max field size
        expectedErrorMessage = "Invalid data, field data exceeds maximum field size.";
        data = new byte[]{0x46, 0x46, 0x31, 0x32, 0x31, 0x32, 0x33, 0x34, 0x46, 0x46};
        startIndex = 2;

        unmarshaledFieldData = encoder.unmarshalField(data, startIndex, dataElement);

        Assert.assertEquals(true, unmarshaledFieldData.isErrored());
        Assert.assertEquals(expectedErrorMessage, unmarshaledFieldData.getErrorMessage());

        //test variable field is longer than message
        expectedErrorMessage = "Missing data, field exceeds message length.";
        data = new byte[]{0x46, 0x46, 0x30, 0x34, 0x31, 0x32, 0x33};
        startIndex = 2;

        unmarshaledFieldData = encoder.unmarshalField(data, startIndex, dataElement);

        Assert.assertEquals(true, unmarshaledFieldData.isErrored());
        Assert.assertEquals(expectedErrorMessage, unmarshaledFieldData.getErrorMessage());
    }
}
