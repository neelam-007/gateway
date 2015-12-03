package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 03/06/13
 * Time: 2:30 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583EncoderBinary implements ISO8583Encoder {

    int bitmapLength = 0;
    private int mtiLength = 4;

    @Override
    public byte[] marshalMTI(int version, int messageClass, int function, int origin) {

        byte[] mti = new byte[mtiLength];

        mti[0] = (byte) version;
        mti[1] = (byte) messageClass;
        mti[2] = (byte) function;
        mti[3] = (byte) origin;

        return mti;
    }

    @Override
    public ISO8583MessageTypeIndicator unmarshalMTI(byte[] data) {

        ISO8583MessageTypeIndicator mti = new ISO8583MessageTypeIndicator();

        mti.setVersion(data[0]);
        mti.setMessageClass(data[1]);
        mti.setFunction(data[2]);
        mti.setOrigin(data[3]);

        return mti;
    }

    @Override
    public ISO8583MarshaledFieldData marshalField(String data, ISO8583DataElement dataElement) {
        //process variable length field
        if (dataElement.isVariable())
            return marshalVariableField(data, dataElement);
        else //process non variable length fields
            return marshalStaticField(data, dataElement);
    }

    @Override
    public ISO8583UnmarshaledFieldData unmarshalField(byte[] data, int startIndex, ISO8583DataElement dataElement) {
        //process variable length field
        if (dataElement.isVariable())
            return unmarshalVariableField(data, startIndex, dataElement);
        else //process non variable length fields
            return unmarshalStaticField(data, startIndex, dataElement);
    }

    @Override
    public byte[] marshalBitmap(byte[] rawData) {
        return rawData;
    }

    @Override
    public byte[] unmarshalBitmap(byte[] data, int startIndex) {
        int bmLength = ISO8583Constants.BITMAP_LENGTH; //will always have primary bitmap, initialize for primary bitmap

        //check for secondary bitmap
        if (((data[startIndex] >> 8) & 1) == 1) //check that the first bit of the primary bitmap is on
            bmLength += ISO8583Constants.BITMAP_LENGTH;

        //check for tertiary bitmap
        int tertiaryBitmapIndex = startIndex + ISO8583Constants.BITMAP_LENGTH;
        if (data.length > tertiaryBitmapIndex &&
                bmLength == 16 &&  //we have a secondary bitmap
                ((data[tertiaryBitmapIndex] >> 8) & 1) == 1) //check that the first bit of the secondary bitmap is on
            bmLength += ISO8583Constants.BITMAP_LENGTH;

        bitmapLength = bmLength;
        return Arrays.copyOfRange(data, startIndex, startIndex + bmLength);
    }

    private ISO8583MarshaledFieldData marshalStaticField(String data, ISO8583DataElement dataElement) {

        ISO8583MarshaledFieldData marshaledFieldData = new ISO8583MarshaledFieldData();

        //error check data
        int dataElementLength = dataElement.getLength();
        int dataLength = data.length();

        //ensure the data matches the length of the field... we are not padding data.
        if (dataLength * ISO8583Constants.BITS_IN_HEX_VALUE != dataElementLength) {
            marshaledFieldData.setErrored(true);
            marshaledFieldData.setErrorMessage("Bad Data Size, Data in binary field does not match field size.");
            return marshaledFieldData;
        }

        //marshal the data
        byte[] dataBytes = marshalFieldData(data, dataLength);

        marshaledFieldData.setData(dataBytes);

        return marshaledFieldData;
    }

    private ISO8583UnmarshaledFieldData unmarshalStaticField(byte[] data, int startIndex, ISO8583DataElement dataElement) {

        ISO8583UnmarshaledFieldData unmarshaledFieldData = new ISO8583UnmarshaledFieldData();

        int fieldLength = dataElement.getLength() / ISO8583Constants.BYTE_LENGTH;
        int endIndex = startIndex + fieldLength;

        //check that data doesn't run off the end of the message
        if (endIndex > data.length) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, field exceeds message length.");
        } else {
            unmarshaledFieldData.setBytesRead(fieldLength);
            unmarshaledFieldData.setData(unmarshalFieldData(data, startIndex, endIndex));
        }

        return unmarshaledFieldData;
    }

    private ISO8583MarshaledFieldData marshalVariableField(String data, ISO8583DataElement dataElement) {

        ISO8583MarshaledFieldData marshaledFieldData = new ISO8583MarshaledFieldData();
        byte[] dataHeader = null;
        byte[] dataBytes = null;

        int dataLength = 0; // default to 0 if data is missing.
        if (data != null) {
            dataLength = data.length();
        }

        //marshal the data header
        dataHeader = marshalDataHeader(dataLength, dataElement.getVariableFieldLength());

        //marshal the field data
        dataBytes = marshalFieldData(data, dataLength);

        marshaledFieldData.setData(Utils.concatByteArray(dataHeader, dataBytes));

        return marshaledFieldData;
    }

    private ISO8583UnmarshaledFieldData unmarshalVariableField(byte[] data, int startIndex, ISO8583DataElement dataElement) {

        ISO8583UnmarshaledFieldData unmarshaledFieldData = new ISO8583UnmarshaledFieldData();
        int headerLength = dataElement.getVariableFieldLength();
        int fieldStartIndex = startIndex + headerLength;
        int fieldLength = 0;
        int numBytesInField = 0;
        int fieldMaxLength = dataElement.getLength();

        //read the header and get the fieldLength
        if (startIndex + headerLength > data.length) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, length of leading digits exceeds message length.");
            return unmarshaledFieldData;
        }

        try {
            fieldLength = unmarshalDataheader(Arrays.copyOfRange(data, startIndex, startIndex + headerLength));
            numBytesInField = fieldLength / ISO8583Constants.BYTE_LENGTH;
        } catch (NumberFormatException nfe) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage(nfe.getMessage());
            return unmarshaledFieldData;
        }

        //check that the length of the field data doesn't exceed it's maximum size
        if (fieldLength > fieldMaxLength) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Invalid data, field data exceeds maximum field size.");
            return unmarshaledFieldData;
        }

        //check that the field doesn't run past the end of ISO8583 Message
        if ((fieldStartIndex + numBytesInField) > data.length) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, field exceeds message length.");
            return unmarshaledFieldData;
        }

        unmarshaledFieldData.setBytesRead(headerLength + numBytesInField);
        unmarshaledFieldData.setData(unmarshalFieldData(data, fieldStartIndex, fieldStartIndex + numBytesInField));

        return unmarshaledFieldData;
    }

    private byte[] marshalDataHeader(int dataLength, int variableFieldLength) {

        byte[] headerBytes = null;

        //create header string
        String dataLengthString = Integer.toString(dataLength * ISO8583Constants.BITS_IN_HEX_VALUE);
        if (dataLengthString.length() < variableFieldLength) {
            int lengthDiff = variableFieldLength - dataLengthString.length();

            for (int i = 0; i < lengthDiff; i++)
                dataLengthString = "0" + dataLengthString;
        }

        //create binary encoded array of bytes
        headerBytes = dataLengthString.getBytes();
        for (int i = 0; i < headerBytes.length; i++) {
            headerBytes[i] = (byte) (headerBytes[i] & 0x0F); //change ascii character to int
        }

        return headerBytes;
    }

    private int unmarshalDataheader(byte[] headerBytes) throws NumberFormatException {

        int headerDataLength = headerBytes.length;
        int dataLength = 0;

        if (headerBytes[0] > 9)
            throw new NumberFormatException("Invalid data, field header contains non numeric data.");

        if (headerDataLength > 1 && headerBytes[1] > 9)
            throw new NumberFormatException("Invalid data, field header contains non numeric data.");

        if (headerDataLength > 2 && headerBytes[2] > 9)
            throw new NumberFormatException("Invalid data, field header contains non numeric data.");

        dataLength = headerBytes[0];

        if (headerDataLength > 1) {
            dataLength *= 10;
            dataLength += headerBytes[1];
        }

        if (headerDataLength > 2) {
            dataLength *= 10;
            dataLength += headerBytes[2];
        }

        return dataLength;
    }


    private byte[] marshalFieldData(String data, int dataLength) {

        byte[] byteData = null;
        int temp = 0;
        int startIndex = 0;
        int endIndex = 0;

        //marshal the data
        if (data != null) {
            byteData = new byte[data.length() / ISO8583Constants.HEX_VALUE_LENGTH];
        } else {
            byteData = new byte[]{};
        }

        for (int i = 0; i < byteData.length; i++) {
            startIndex = i * ISO8583Constants.HEX_VALUE_LENGTH;
            endIndex = startIndex + ISO8583Constants.HEX_VALUE_LENGTH;
            temp = Integer.valueOf(data.substring(startIndex, endIndex), 16);
            byteData[i] = (byte) temp;
        }

        return byteData;
    }

    private String unmarshalFieldData(byte[] data, int startIndex, int endIndex) {

        byte[] byteData = Arrays.copyOfRange(data, startIndex, endIndex);
        String hexString = "";
        String dataString = "";
        int hexStringLength = 0;

        for (byte b : byteData) {
            hexString = Integer.toHexString(b);
            hexStringLength = hexString.length();

            if (hexStringLength < ISO8583Constants.HEX_VALUE_LENGTH)
                dataString += "0" + hexString;
            else if (hexStringLength > ISO8583Constants.HEX_VALUE_LENGTH) {
                dataString += hexString.substring(hexStringLength - ISO8583Constants.HEX_VALUE_LENGTH, hexStringLength);
            } else {
                dataString += hexString;
            }
        }

        return dataString;
    }

    @Override
    public int getMtiLength() {
        return mtiLength;
    }

    @Override
    public int getBitmapLength() {
        return bitmapLength;
    }
}
