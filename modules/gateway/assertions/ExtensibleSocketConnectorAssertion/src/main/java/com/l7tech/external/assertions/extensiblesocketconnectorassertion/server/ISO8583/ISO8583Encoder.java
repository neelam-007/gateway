package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 18/02/13
 * Time: 4:31 PM
 * To change this template use File | Settings | File Templates.
 */
public interface ISO8583Encoder {

    /**
     * Marshal MTI data from a string to an encoded byte array representation to
     * be placed in an ISO8583 message
     *
     * @param version      as per ISO8583 spec
     * @param messageClass as per ISO8583 spec
     * @param function     as per ISO8583 spec
     * @param origin       as per ISO8583 spec
     * @return an array of bytes with the paramaters encoded
     */
    public byte[] marshalMTI(int version, int messageClass, int function, int origin);

    /**
     * Unmarshal the MTI data from an ISO8583 message into a ISO8583MessageTypeIndicator object.
     * Data from the ISO8583MessageTypeIndicator will be placed in XML
     *
     * @param data the full ISO8583 message.
     * @return the populated ISO8583MessageTypeIndicator object.
     */
    public ISO8583MessageTypeIndicator unmarshalMTI(byte[] data);

    /**
     * Marshal field data to an encoded byte array representation to be placed in an ISO8583 Message
     *
     * @param data        string represenation of the data
     * @param dataElement object that represents the attributes of the field based on the ISO8583 spec.
     * @return an ISO8583MarshaledFieldData containg the data as an array and any error messages
     */
    public ISO8583MarshaledFieldData marshalField(String data, ISO8583DataElement dataElement);

    /**
     * Unmarshal field data to an ISO8583UnmarshalledFieldData object.  Data from the object
     * to be placed into an XML document.
     * Should error check that the length of the field to read doesn't run past the end of the
     * ISO8583 message (@param data).  If it does then set errored to true in the ISO8583UnmarshaledFieldData object
     * and pass back an error message.
     *
     * @param data        the full ISO8583 message
     * @param startIndex  the index where the field begins in the ISO8583 message.
     * @param dataElement object that represents the attributes of the field based on the ISO8583 spec.
     * @return the number of bytes read from the ISO8583 message
     */
    public ISO8583UnmarshaledFieldData unmarshalField(byte[] data, int startIndex, ISO8583DataElement dataElement);

    /**
     * Marshal the bitmap from raw bits to another format.  eg: Raw bits -> ASCII
     *
     * @param rawData the bitmap in it's raw bit form.
     * @return the array containing the ASII representation of the bitmap
     */
    public byte[] marshalBitmap(byte[] rawData);

    /**
     * Unmarshal the bitmap from other format to raw bits. eg: ASCII -> Raw bits
     *
     * @param data       the full ISO8583 message
     * @param startIndex the start index of the bitmap
     * @return the array of bytes representing the bitmap
     */
    public byte[] unmarshalBitmap(byte[] data, int startIndex);

    public int getMtiLength();

    public int getBitmapLength();
}
