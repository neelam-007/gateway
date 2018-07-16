package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 19/02/13
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583EncoderASCII implements ISO8583Encoder {

    private int bitmapLength = 0;
    private int mtiLength = 4;
    private byte zero = 0x30;
    private byte space = 0x20;

    @Override
    public byte[] marshalMTI(int version, int messageClass, int function, int origin) {

        byte[] mti = new byte[mtiLength];

        mti[0] = Integer.toString(version).getBytes()[0];
        mti[1] = Integer.toString(messageClass).getBytes()[0];
        mti[2] = Integer.toString(function).getBytes()[0];
        mti[3] = Integer.toString(origin).getBytes()[0];

        return mti;
    }

    @Override
    public ISO8583MessageTypeIndicator unmarshalMTI(byte[] data) {

        ISO8583MessageTypeIndicator mti = new ISO8583MessageTypeIndicator();

        mti.setVersion(data[0] & 0x0F);
        mti.setMessageClass(data[1] & 0x0F);
        mti.setFunction(data[2] & 0x0F);
        mti.setOrigin(data[3] & 0x0F);

        return mti;
    }

    @Override
    public ISO8583MarshaledFieldData marshalField(String data, ISO8583DataElement dataElement) {
        ISO8583MarshaledFieldData marshaledFieldData = new ISO8583MarshaledFieldData();
        byte[] result = null;

        //process variable length field
        if (dataElement.isVariable())
            result = marshalVariableField(data, dataElement);
        else //process non variable length fields
            result = marshalStaticField(data, dataElement);

        marshaledFieldData.setData(result);

        return marshaledFieldData;
    }

    @Override
    public ISO8583UnmarshaledFieldData unmarshalField(byte[] data, int startIndex, ISO8583DataElement dataElement) {

        //if variable unmarshal variable field
        if (dataElement.isVariable()) {
            return unmarshalVariableField(data, startIndex, dataElement);
        } else { //unmarshal static field
            return unmarshalStaticField(data, startIndex, dataElement);
        }
    }

    @Override
    public byte[] marshalBitmap(byte[] rawData) {

        String result = "";

        for (byte b : rawData)
            result += marshalByte(b);

        return result.getBytes();
    }

    @Override
    public byte[] unmarshalBitmap(byte[] data, int startIndex) {

        bitmapLength = ISO8583Constants.BITMAP_LENGTH * 2;
        boolean secondaryBitmapPresent = false;

        //determine length of bitmap.
        //determine if we have a secondary bitmap
        byte tempByte = unmarshalByte(data, startIndex, 2);
        if (((tempByte >> 8) & 1) == 1) {
            bitmapLength += (ISO8583Constants.BITMAP_LENGTH * 2);
            secondaryBitmapPresent = true;
        }

        //determine if we have a tertiary bitmap
        if (secondaryBitmapPresent) {
            tempByte = unmarshalByte(data, startIndex + (ISO8583Constants.BITMAP_LENGTH * 2), 2);

            if (((tempByte >> 8) & 1) == 1)
                bitmapLength += (ISO8583Constants.BITMAP_LENGTH * 2);
        }

        //create bitmap...
        byte[] bitmap = new byte[bitmapLength / 2];
        int byteStartIndex = 0;

        for (int index = 0; index < bitmap.length; index++) {
            byteStartIndex = startIndex + (index * 2);

            bitmap[index] = unmarshalByte(data, byteStartIndex, 2);
        }

        return bitmap;
    }

    private String marshalByte(byte b) {

        //get the hex string
        String hexString = Integer.toHexString(b);

        if (hexString.length() < 2)
            hexString = "0" + hexString;
        else if (hexString.length() > 2)
            hexString = hexString.substring(hexString.length() - 2);

        hexString = hexString.toUpperCase();

        return hexString;
    }

    private byte unmarshalByte(byte[] data, int startIndex, int length) {

        byte[] byteData = Arrays.copyOfRange(data, startIndex, startIndex + length);

        int result = Integer.valueOf(new String(byteData), 16);

        return (byte) result;
    }

    private byte[] marshalStaticField(String data, ISO8583DataElement dataElement) {
        char paddingChar = ' ';
        int dataLength = data.length();
        int dataElementLength = dataElement.getLength();
        ISO8583DataType dataType = dataElement.getDataType();

        if (dataLength < dataElementLength) {
            int diff = dataElementLength - dataLength;

            if (dataType == ISO8583DataType.NUMERIC) {
                paddingChar = '0';

                for (int i = 0; i < diff; i++)
                    data = paddingChar + data;

            } else if (dataType != ISO8583DataType.BINARY) {
                for (int i = 0; i < diff; i++)
                    data = data + paddingChar;
            }
        }

        return data.getBytes();
    }

    private byte[] marshalVariableField(String data, ISO8583DataElement dataElement) {

        byte[] result = null;

        //create data header
        int headerLength = dataElement.getVariableFieldLength();
        String header = "0"; // default to 0 if data is missing.
        if (data != null) {
            header = Integer.toString(data.length());
        }

        if (header.length() < headerLength) {
            int lengthDif = headerLength - header.length();
            for (int i = 0; i < lengthDif; i++)
                header = "0" + header;
        }

        if (data != null) {
            result = (header + data).getBytes();
        } else {
            result = header.getBytes();
        }

        return result;
    }

    private ISO8583UnmarshaledFieldData unmarshalStaticField(byte[] data, int startIndex,
                                                             ISO8583DataElement dataElement) {
        ISO8583UnmarshaledFieldData unmarshaledFieldData = new ISO8583UnmarshaledFieldData();
        int fieldLength = dataElement.getLength();
        int endIndex = startIndex + fieldLength;

        //check that data doesn't run off the end of the message
        if (endIndex > data.length) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, field exceeds message length.");
        } else {
            unmarshaledFieldData.setBytesRead(fieldLength);
            unmarshaledFieldData.setData(new String(Arrays.copyOfRange(data, startIndex, endIndex)));
        }


        return unmarshaledFieldData;
    }

    private ISO8583UnmarshaledFieldData unmarshalVariableField(byte[] data, int startIndex,
                                                               ISO8583DataElement dataElement) {

        ISO8583UnmarshaledFieldData unmarshaledFieldData = new ISO8583UnmarshaledFieldData();
        int headerLength = dataElement.getVariableFieldLength();
        int fieldStartIndex = startIndex + headerLength;
        int fieldLength = 0;
        int fieldMaxLength = dataElement.getLength();

        //read the header and get the fieldLength
        if (startIndex + headerLength > data.length - 1) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, length of leading digits exceeds message length.");
            return unmarshaledFieldData;
        }

        String headerString = new String(Arrays.copyOfRange(data, startIndex, startIndex + headerLength));

        try {
            fieldLength = Integer.parseInt(headerString);
        } catch (NumberFormatException nfe) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Invalid data, leading digits are not numeric.");
            return unmarshaledFieldData;
        }

        //check that the length of the field data doesn't exceed it's maximum size
        if (fieldLength > fieldMaxLength) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Invalid data, field data exceeds maximum field size.");
            return unmarshaledFieldData;
        }

        //check that the field doesn't run past the end of ISO8583 Message
        if ((fieldStartIndex + fieldLength) > data.length) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, field exceeds message length.");
            return unmarshaledFieldData;
        }

        unmarshaledFieldData.setBytesRead(headerLength + fieldLength);
        unmarshaledFieldData.setData(
                new String(Arrays.copyOfRange(data, fieldStartIndex, fieldStartIndex + fieldLength)));

        return unmarshaledFieldData;
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
