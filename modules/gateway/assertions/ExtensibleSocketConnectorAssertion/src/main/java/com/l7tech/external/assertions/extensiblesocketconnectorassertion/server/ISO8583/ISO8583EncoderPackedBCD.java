package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import java.util.Arrays;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 19/02/13
 * Time: 10:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583EncoderPackedBCD implements ISO8583Encoder {

    private int mtiLength = 2;

    @Override
    public byte[] marshalMTI(int version, int messageClass, int function, int origin) {

        byte[] result = new byte[mtiLength];

        //store version
        result[0] = (byte) (version << 4);
        //store message class
        result[0] = (byte) (result[0] | messageClass);
        //store function
        result[1] = (byte) (function << 4);
        //store origin
        result[1] = (byte) (result[1] | origin);

        return result;
    }

    @Override
    public ISO8583MessageTypeIndicator unmarshalMTI(byte[] data) {
        ISO8583MessageTypeIndicator mti = new ISO8583MessageTypeIndicator();

        //unmarshal version
        mti.setVersion(data[0] >> 4);
        //unmarshal messageClass
        mti.setMessageClass(data[0] & 0x0F);
        //unmarshal function
        mti.setFunction(data[1] >> 4);
        //unmarshal origin
        mti.setOrigin(data[1] & 0x0F);

        return mti;
    }

    @Override
    public ISO8583MarshaledFieldData marshalField(String data, ISO8583DataElement dataElement) {
        ISO8583MarshaledFieldData marshaledFieldData = new ISO8583MarshaledFieldData();

        //marshal variable field
        if (dataElement.isVariable())
            marshaledFieldData.setData(marshalVariableField(data, dataElement));
            //marshal static field
        else {
            //only marshal numeric fields
            if (dataElement.getDataType() != ISO8583DataType.NUMERIC) {
                marshaledFieldData.setErrored(true);
                marshaledFieldData.setErrorMessage("Cannot marshal non numeric static fields to packed BCD.");
            }

            marshaledFieldData.setData(marshalStaticField(data, dataElement));
        }

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
        return new byte[0]; //Packed bcd not used for encoding bitmap
    }

    @Override
    public byte[] unmarshalBitmap(byte[] data, int startIndex) {
        return new byte[0]; //Packed bcd not used for encoding bitmap
    }

    private byte[] marshalStaticField(String data, ISO8583DataElement dataElement) {
        return stringToPackedBCD(data, dataElement.getLength());
    }

    private byte[] marshalVariableField(String data, ISO8583DataElement dataElement) {

        //create a header
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

        byte[] headerByte = stringToPackedBCD(header, header.length());
        int headerByteArrayLength = headerByte.length;

        //create the data...
        byte[] dataByte = null;
        int dataByteArrayLength = 0;
        if (data != null) {
            if (dataElement.getDataType() == ISO8583DataType.NUMERIC) {
                dataByte = stringToPackedBCD(data, data.length());
            } else {
                dataByte = data.getBytes();
            }
            dataByteArrayLength = dataByte.length;
        }

        //create byte array
        byte[] result = new byte[headerByteArrayLength + dataByteArrayLength];

        System.arraycopy(headerByte, 0, result, 0, headerByteArrayLength);
        if (dataByte != null) {
            System.arraycopy(dataByte, 0, result, headerByteArrayLength, dataByteArrayLength);
        }

        return result;
    }

    private ISO8583UnmarshaledFieldData unmarshalStaticField(byte[] data, int startIndex, ISO8583DataElement dataElement) {
        ISO8583UnmarshaledFieldData unmarshaledFieldData = new ISO8583UnmarshaledFieldData();

        int fieldLength = dataElement.getLength();
        int endIndex = 0;
        int numBytes = 0;

        //determine number of bytes
        numBytes = getNumberBytes(fieldLength);

        endIndex = startIndex + numBytes;

        //check that data doesn't run off the end of the message
        if (endIndex > data.length) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, field exceeds message length.");
        } else {
            unmarshaledFieldData.setData(packedBCDToString(data, startIndex, endIndex));
            unmarshaledFieldData.setBytesRead(numBytes);
        }

        return unmarshaledFieldData;
    }

    private ISO8583UnmarshaledFieldData unmarshalVariableField(byte[] data, int startIndex, ISO8583DataElement dataElement) {

        ISO8583UnmarshaledFieldData unmarshaledFieldData = new ISO8583UnmarshaledFieldData();

        int headerLength = dataElement.getVariableFieldLength();
        int fieldMaxLength = dataElement.getLength();
        ISO8583DataType dataType = dataElement.getDataType();

        //get header
        int headerBytes = getNumberBytes(headerLength);
        int headerEndIndex = startIndex + headerBytes;

        if (headerEndIndex > data.length - 1) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, length of leading digits exceeds message length.");
            return unmarshaledFieldData;
        }

        String headerStr = packedBCDToString(data, startIndex, headerEndIndex);
        int dataLength = 0;
        try {
            dataLength = Integer.parseInt(headerStr);
        } catch (NumberFormatException nfe) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Invalid data, leading digits are not numeric.");
            return unmarshaledFieldData;
        }

        //do error checking that the length of the data doesn't exceed the maximum field length
        if (dataLength > fieldMaxLength) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Invalid data, field data exceeds maximum field size.");
            return unmarshaledFieldData;
        }

        //get the field data
        int dataBytes = 0;
        if (dataType == ISO8583DataType.NUMERIC)
            dataBytes = getNumberBytes(dataLength);
        else
            dataBytes = dataLength;

        int dataStartIndex = startIndex + headerBytes;
        int dataEndIndex = dataStartIndex + dataBytes;

        //check that the field data doesn't run off the end of the message.
        if (dataEndIndex > data.length) {
            unmarshaledFieldData.setErrored(true);
            unmarshaledFieldData.setErrorMessage("Missing data, field exceeds message length.");
            return unmarshaledFieldData;
        }

        //if datatype is numeric unmarshal numeric data
        if (dataElement.getDataType() == ISO8583DataType.NUMERIC)
            unmarshaledFieldData.setData(packedBCDToString(data, dataStartIndex, dataEndIndex));
            //else unmarshal as text data
        else
            unmarshaledFieldData.setData(new String(Arrays.copyOfRange(data, dataStartIndex, dataEndIndex)));

        unmarshaledFieldData.setBytesRead(headerBytes + dataBytes);
        return unmarshaledFieldData;
    }

    ;

    private String packedBCDToString(byte[] data, int startIndex, int endIndex) {

        String result = "";

        for (int i = startIndex; i < endIndex; i++) {
            result += Integer.toString((data[i] >> 4) & 0x0F);
            result += Integer.toString(data[i] & 0x0F);
        }

        return result;
    }

    private byte[] stringToPackedBCD(String data, int fieldLength) {

        int dataLength = data.length();
        byte[] result = null;
        int numBytes = 0;

        //determine the length of the array
        numBytes = getNumberBytes(fieldLength);

        //create new array
        result = new byte[numBytes];

        //populate the number into the array as packed bcd
        int byteIndex = numBytes - 1;
        int digit = 0;
        boolean byteDirty = false;
        for (int i = dataLength - 1; i >= 0; i--) {
            digit = Integer.parseInt("" + data.charAt(i));

            //place the digit into the specific part of the byte
            if (!byteDirty) {
                result[byteIndex] = (byte) digit;
                byteDirty = true;
            } else {
                result[byteIndex] = (byte) (result[byteIndex] | (digit << 4));
                byteDirty = false;
                byteIndex--;
            }
        }

        return result;
    }

    private int getNumberBytes(int fieldLength) {
        if (fieldLength % 2 > 0)
            return (fieldLength / 2) + 1;
        else
            return fieldLength / 2;
    }

    @Override
    public int getMtiLength() {
        return mtiLength;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public int getBitmapLength() {
        return 0;  //Packed bcd not used for encoding bitmap
    }
}
