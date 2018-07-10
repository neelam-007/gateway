package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 07/01/13
 * Time: 3:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583Marshaler {

    private class Bitmap {

        private static final int BYTE_LENGTH = 8;
        private static final int SECONDARY_BITMAP_THRESHOLD = 64;
        private static final int TERTIARY_BITMAP_THRESHOLD = 128;

        private int bitmapStartIndex = 0;
        private boolean secondaryBitmapMandatory = false;
        private boolean tertiaryBitmapMandatory = false;
        private ISO8583Encoder bitmapEncoder = null;

        private int bitmapLength = 0;
        private int currentBitIndex = 0;
        private int currentByteIndex = 0;
        private byte[] bitmap = null;

        public Bitmap(boolean _secondaryBitmapMandatory, boolean _tertiaryBitmapMandatory, ISO8583Encoder _bitmapEncoder) {
            secondaryBitmapMandatory = _secondaryBitmapMandatory;
            tertiaryBitmapMandatory = _tertiaryBitmapMandatory;
            bitmapEncoder = _bitmapEncoder;
        }

        public void extractBitmap(byte[] data) {

            if (bitmapEncoder == null) {
                extractBitmapDefault(data);
            } else {
                bitmap = bitmapEncoder.unmarshalBitmap(data, bitmapStartIndex);
                bitmapLength = bitmap.length;
            }
        }

        public void extractBitmapDefault(byte[] data) {

            int bmLength = ISO8583Constants.BITMAP_LENGTH; //will always have primary bitmap, initialize for primary bitmap

            //check for secondary bitmap
            if (((data[bitmapStartIndex] >> 8) & 1) == 1) //check that the first bit of the primary bitmap is on
                bmLength += ISO8583Constants.BITMAP_LENGTH;

            //check for tertiary bitmap
            int tertiaryBitmapIndex = bitmapStartIndex + ISO8583Constants.BITMAP_LENGTH;
            if (data.length > tertiaryBitmapIndex &&
                    bmLength == 16 &&  //we have a secondary bitmap
                    ((data[tertiaryBitmapIndex] >> 8) & 1) == 1) //check that the first bit of the secondary bitmap is on
                bmLength += ISO8583Constants.BITMAP_LENGTH;

            bitmap = Arrays.copyOfRange(data, bitmapStartIndex, bitmapStartIndex + bmLength);
            bitmapLength = bmLength;
        }

        public void addField(int position) {

            //initialize bitmap
            if (bitmap == null) {

                //create primary bitmap...
                bitmap = new byte[]{0, 0, 0, 0, 0, 0, 0, 0};
                bitmapLength = bitmap.length;//adjust bitmap length because we have added bitmaps

                //create secondary bitmap if required
                if (secondaryBitmapMandatory) {
                    bitmap = concatByteArray(bitmap, new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
                    setBitmapBit(1);
                    bitmapLength = bitmap.length;//adjust bitmap length because we have added bitmaps
                }

                //create tertiary bitmap if required
                //GUI ensures that if the tertary bitmap is required then the
                //secondary bitmap will be present... thus we don't need
                //that logic here.
                if (tertiaryBitmapMandatory) {
                    bitmap = concatByteArray(bitmap, new byte[]{0, 0, 0, 0, 0, 0, 0, 0});
                    setBitmapBit(65);
                    bitmapLength = bitmap.length;//adjust bitmap length because we have added bitmaps
                }
            }

            //set field position in bitmaps
            if (position > TERTIARY_BITMAP_THRESHOLD &&
                    bitmapLength < 32) {

                //add secondary bitmap if it doesn't exit
                if (bitmapLength < 16)
                    bitmap = concatByteArray(bitmap, new byte[]{0, 0, 0, 0, 0, 0, 0, 0});

                //add tertiary bitmap
                bitmap = concatByteArray(bitmap, new byte[]{0, 0, 0, 0, 0, 0, 0, 0});

                //set the bit in the secondary bitmap to denote that a
                //tertiary bitmap exists
                setBitmapBit(65);
                bitmapLength = bitmap.length;//adjust bitmap length because we have added bitmaps
            } else if (position > SECONDARY_BITMAP_THRESHOLD &&
                    position <= TERTIARY_BITMAP_THRESHOLD &&
                    bitmapLength < 16) {
                bitmap = concatByteArray(bitmap, new byte[]{0, 0, 0, 0, 0, 0, 0, 0});

                //set the bit in the primary bitmap to denote that a
                //tertiary bitmap exists
                setBitmapBit(1);
                bitmapLength = bitmap.length;//adjust bitmap length because we have added bitmaps
            }

            setBitmapBit(position);
        }

        public int getNextField() {

            int result = 0;
            boolean positionFound = false;

            while (currentByteIndex < bitmapLength &&
                    !positionFound) {

                result = getNextBytePosition(currentByteIndex * BYTE_LENGTH);

                if (result != 0)
                    positionFound = true;
                else
                    currentByteIndex++;
            }

            return result;
        }

        public byte[] getBytes() {

            if (bitmap != null) {

                if (bitmapEncoder != null) {
                    return bitmapEncoder.marshalBitmap(bitmap);
                } else
                    return bitmap;
            }

            return new byte[]{};
        }

        public int getBitmapLength() {

            if (bitmapEncoder != null)
                return bitmapEncoder.getBitmapLength();
            else
                return bitmapLength;
        }

        private int getNextBytePosition(int offset) {

            byte b = bitmap[currentByteIndex];
            boolean positionFound = false;
            int result = 0;
            int mask = 0;

            //if this byte contains no set bits...
            //return 0;
            if (b == 0) {
                currentBitIndex = 0;
                return 0;
            }

            //search for the next set bit in the byte
            while (currentBitIndex < BYTE_LENGTH &&
                    !positionFound) {

                mask = (int) Math.pow(2, (BYTE_LENGTH - (currentBitIndex + 1)));
                result = currentBitIndex + 1 + offset;

                //the bit is flagged and it isn't one of the bits
                //used to flag the existence of a bitmap (1 = secondary bitmap present,
                //65 tertiary bitmap present).
                if (((b & mask) != 0) && result != 1 && result != 65)
                    positionFound = true;

                currentBitIndex++;
            }

            if (!positionFound) {
                currentBitIndex = 0;
                result = 0;
            }

            return result;
        }

        private void setBitmapBit(int position) {

            //setup bitmap
            int index = 0;

            if (position > BYTE_LENGTH) {

                //determine byte index
                index = position / BYTE_LENGTH;
                if ((position % BYTE_LENGTH) == 0)
                    index -= 1; //to compensate for 0 based arrays

                position = position - (BYTE_LENGTH * index);
            }

            bitmap[index] = (byte) (bitmap[index] | 0x80 >> (position - 1));
        }

        private byte[] concatByteArray(byte[] firstArray, byte[] secondArray) {

            if (firstArray == null && secondArray != null)
                return secondArray;
            else if (firstArray != null && secondArray == null)
                return firstArray;
            else if (firstArray == null && secondArray == null)
                return new byte[]{};

            byte[] result = new byte[firstArray.length + secondArray.length];

            System.arraycopy(firstArray, 0, result, 0, firstArray.length);
            System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);

            return result;
        }

        public int getBitmapStartIndex() {
            return bitmapStartIndex;
        }

        public void setBitmapStartIndex(int bitmapStartIndex) {
            this.bitmapStartIndex = bitmapStartIndex;
        }

        public boolean isSecondaryBitmapMandatory() {
            return secondaryBitmapMandatory;
        }

        public void setSecondaryBitmapMandatory(boolean secondaryBitmapMandatory) {
            this.secondaryBitmapMandatory = secondaryBitmapMandatory;
        }

        public boolean isTertiaryBitmapMandatory() {
            return tertiaryBitmapMandatory;
        }

        public void setTertiaryBitmapMandatory(boolean tertiaryBitmapMandatory) {
            this.tertiaryBitmapMandatory = tertiaryBitmapMandatory;
        }

        public ISO8583Encoder getBitmapEncoder() {
            return bitmapEncoder;
        }

        public void setBitmapEncoder(ISO8583Encoder bitmapEncoder) {
            this.bitmapEncoder = bitmapEncoder;
        }
    }

    private static final String MESSAGE_TAG = "Message";
    private static final String MIT_TAG = "MessageTypeIndicator";
    private static final String FIELDS_TAG = "Fields";
    private static final String FIELD_TAG = "Field";

    private static final int MESSAGE_TYPE_INDICATOR_LENGTH = 2;

    private static final String CLASS_ATTRIBUTE = "class";
    private static final String FUNCTION_ATTRIBUTE = "function";
    private static final String ORIGIN_ATTRIBUTE = "origin";
    private static final String VERSION_ATTRIBUTE = "version";
    private static final String POSITION_ATTRIBUTE = "position";

    private static final String TRIM_NULL_REGEX = "^([\u0000]+)|([\u0000]+)$";
    private static final Pattern TRIM_NULL_PATTERN = Pattern.compile(TRIM_NULL_REGEX);
    private static final String TRIM_LEADING_ZERO_REGEX = "^0*";
    private static final Pattern TRIM_LEADING_ZERO_PATTERN = Pattern.compile(TRIM_LEADING_ZERO_REGEX);
    private static final String ONLY_ZERO_REGEX = "^0*$";
    private static final Pattern ONLY_ZERO_PATTERN = Pattern.compile(ONLY_ZERO_REGEX);

    private DocumentBuilder xmlBuilder = null;
    private Bitmap bitmap = null;
    private ISO8583EncoderType mtiEncodingType = null;
    private boolean secondaryBitmapMandatory = false;
    private boolean tertiaryBitmapMandatory = false;
    private ISO8583DataElement[] iso8583Schema = null;
    private Map<ISO8583EncoderType, ISO8583Encoder> encoderMap = null;

    public ISO8583Marshaler() throws ISO8583MarshalingException {

        //initialize xml document factory
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setIgnoringElementContentWhitespace(true);
            xmlBuilder = dbFactory.newDocumentBuilder();
        } catch (ParserConfigurationException pCF) {
            throw new ISO8583MarshalingException("Unable to create marshaller instance: " + pCF.getMessage());
        }
    }

    public byte[] marshal(byte[] xmlData) throws ISO8583MarshalingException {

        Document doc = null;
        ByteArrayInputStream byteStreamIn = new ByteArrayInputStream(xmlData);

        //setup bitmap
        //get bitmapEncoder
        ISO8583Encoder bitmapEncoder = encoderMap.get(iso8583Schema[0].getEncoderType());
        bitmap = new Bitmap(secondaryBitmapMandatory, tertiaryBitmapMandatory, bitmapEncoder);

        //parse the array of bytes that represent our xml into a DOM object.
        try {
            doc = xmlBuilder.parse(byteStreamIn);
        } catch (SAXException sEx) {
            throw new ISO8583MarshalingException(sEx.getMessage());
        } catch (IOException ioEx) {
            throw new ISO8583MarshalingException(ioEx.getMessage());
        }

        if (iso8583Schema == null)
            throw new ISO8583MarshalingException("No ISO8583 Schema initialized.");

        //marshall the DOM object into the ISO8583 object
        return marshalXmlDocument(doc);
    }

    public byte[] unmarshal(byte[] iso8583Data) throws ISO8583MarshalingException {

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        //setup bitmap
        //get bitmapEncoder
        ISO8583Encoder bitmapEncoder = encoderMap.get(iso8583Schema[0].getEncoderType());
        bitmap = new Bitmap(secondaryBitmapMandatory, tertiaryBitmapMandatory, bitmapEncoder);

        //ensure that we have at least a message type indicator and a bitmap
        if (iso8583Data.length < (MESSAGE_TYPE_INDICATOR_LENGTH + 8))
            throw new ISO8583MarshalingException("Invalid Data: ISO8583 message missing Message Type Indicator and/or Bitmap.");

        //initialize document
        Document doc = xmlBuilder.newDocument();
        Element messageElement = doc.createElement(MESSAGE_TAG);
        doc.appendChild(messageElement);

        if (iso8583Schema == null)
            throw new ISO8583MarshalingException("No ISO8583 Schema initialized.");

        //marshal MIT data
        umarshalISO8583MessageTypeIndicator(iso8583Data, doc);

        //marshal field data
        unmarshalISO8583Fields(iso8583Data, doc);

        //format document for output
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(output);

            transformer.transform(source, result);
        } catch (TransformerConfigurationException tCE) {
            throw new ISO8583MarshalingException(tCE.getMessage());
        } catch (TransformerException tE) {
            throw new ISO8583MarshalingException(tE.getMessage());
        }

        return output.toByteArray();
    }

    private byte[] marshalXmlDocument(Document doc) throws ISO8583MarshalingException {

        byte[] mit = null;
        byte[] data = null;

        //marshall the message type indicator
        mit = marshalXmlMessageTypeIndicator(doc);

        //marshall the fields
        data = marshalXmlFields(doc);

        return Utils.concatByteArray(mit, data);
    }

    private byte[] marshalXmlMessageTypeIndicator(Document doc) throws ISO8583MarshalingException {

        int version = 0;
        int messageClass = 0;
        int function = 0;
        int origin = 0;

        byte[] result = null;
        NodeList nodeList = doc.getElementsByTagName(MIT_TAG);
        Node mitNode = null;

        if (nodeList == null ||
                nodeList.getLength() == 0 ||
                nodeList.getLength() > 1)
            throw new ISO8583MarshalingException("Invalid number of MessageTypedIndicator elements: only 1 must be present.");

        mitNode = nodeList.item(0);
        NamedNodeMap attributeMap = mitNode.getAttributes();
        Node tempNode = null;

        if (attributeMap == null || attributeMap.getLength() == 0)
            throw new ISO8583MarshalingException("No attributes present for MessageTypeIndicator element.");

        //marshal version
        tempNode = attributeMap.getNamedItem(VERSION_ATTRIBUTE);
        if (tempNode == null)
            throw new ISO8583MarshalingException("No version attribute present for MessageTypeIndicator element.");

        try {
            version = Integer.parseInt(tempNode.getNodeValue());

            //version, messageClass, function, origin must be a number from 0 - 9
            if (version < 0 || version > 10)
                throw new ISO8583MarshalingException("Invalid data: Version must be an integer between 0 - 9");
        } catch (NumberFormatException nFE) {
            throw new ISO8583MarshalingException("Invalid data: Version attribute needs to be an integer");
        }

        //marshal class
        tempNode = attributeMap.getNamedItem(CLASS_ATTRIBUTE);
        if (tempNode == null)
            throw new ISO8583MarshalingException("No class attribute present for MessageTypeIndicator element.");

        try {
            messageClass = Integer.parseInt(tempNode.getNodeValue());

            if (messageClass < 0 || messageClass > 10)
                throw new ISO8583MarshalingException("Invalid data: Class must be an integer between 0 - 9");
        } catch (NumberFormatException nFE) {
            throw new ISO8583MarshalingException("Invalid data: Class attribute needs to be an integer");
        }

        //marshal function
        tempNode = attributeMap.getNamedItem(FUNCTION_ATTRIBUTE);
        if (tempNode == null)
            throw new ISO8583MarshalingException("No function attribute present for MessageTypeIndicator element.");

        try {
            function = Integer.parseInt(tempNode.getNodeValue());

            if (function < 0 || function > 10)
                throw new ISO8583MarshalingException("Invalid data: Function must be an integer between 0 - 9");
        } catch (NumberFormatException nFE) {
            throw new ISO8583MarshalingException("Invalid data: Function attribute needs to be an integer");
        }

        //marshal origin
        tempNode = attributeMap.getNamedItem(ORIGIN_ATTRIBUTE);
        if (tempNode == null)
            throw new ISO8583MarshalingException("No origin attribute present for MessageTypeIndicator element.");

        try {
            origin = Integer.parseInt(tempNode.getNodeValue());

            if (origin < 0 || origin > 10)
                throw new ISO8583MarshalingException("Invalid data: Origin must be an integer between 0 - 9");
        } catch (NumberFormatException nFE) {
            throw new ISO8583MarshalingException("Invalid data: Origin attribute needs to be an integer");
        }

        //encode the mti
        result = encoderMap.get(mtiEncodingType).marshalMTI(version, messageClass, function, origin);

        return result;
    }

    private byte[] marshalXmlFields(Document doc) throws ISO8583MarshalingException {

        Node field = null;
        int fieldPosition = 0;
        byte[] result = new byte[]{};

        NodeList fieldList = doc.getElementsByTagName(FIELD_TAG);

        if (fieldList == null || fieldList.getLength() == 0)
            return result; //no fields return no data

        int fieldListLength = fieldList.getLength();

        for (int i = 0; i < fieldListLength; i++) {
            field = fieldList.item(i);

            fieldPosition = getFieldPosition(field); //throws exception if no valid position is found

            bitmap.addField(fieldPosition);

            result = Utils.concatByteArray(result, marshalXmlField(field, fieldPosition));
        }

        //add bitmaps to data
        result = Utils.concatByteArray(bitmap.getBytes(), result);

        return result;
    }

    private byte[] marshalXmlField(Node field, int position) throws ISO8583MarshalingException {

        ISO8583DataType dataType = null;
        int dataElementMaxLength = 0;
        String dataString = null;
        byte[] result = null;

        //process data
        NodeList dataList = field.getChildNodes();

        if (dataList.item(0) != null && dataList.item(0).getNodeType() == Node.TEXT_NODE)
            dataString = dataList.item(0).getNodeValue();

        //get schema information for field
        ISO8583DataElement dataElement = iso8583Schema[position - 1]; //-1 because of 0 based arrays
        dataType = dataElement.getDataType();
        dataElementMaxLength = dataElement.getLength();

        //error checking
        errorCheckFieldData(dataString, position, dataType, dataElementMaxLength);

        //encode the field if there is data for the field or if the field is variable length, dataString maybe null
        //always need to add length for variable length field even if dataString is null.
        if (dataString != null || dataElement.isVariable()) {

            ISO8583MarshaledFieldData marshaledFieldData =
                    encoderMap.get(dataElement.getEncoderType()).marshalField(dataString, dataElement);

            if (marshaledFieldData.isErrored())
                throw new ISO8583MarshalingException("Error at field "
                        + position + ": " + marshaledFieldData.getErrorMessage());

            result = marshaledFieldData.getData();
        }

        return result;
    }

    private int getFieldPosition(Node field) throws ISO8583MarshalingException {

        int result = 0;
        Node tempNode = null;

        //process attributes
        NamedNodeMap attributeMap = field.getAttributes();

        if (attributeMap == null || attributeMap.getLength() == 0)
            throw new ISO8583MarshalingException("Invalid Field attribute: missing Position attribute.");

        //get position
        tempNode = attributeMap.getNamedItem(POSITION_ATTRIBUTE);

        if (tempNode == null)
            throw new ISO8583MarshalingException("Invalid Field attribute: missing Position attribute.");

        try {
            result = Integer.parseInt(tempNode.getNodeValue());
        } catch (NumberFormatException nFE) {
            throw new ISO8583MarshalingException("Invalid data: Position attribute needs to be an integer from 1 to 192.");
        }

        return result;
    }

    private void errorCheckFieldData(String fieldData, int fieldPosition, ISO8583DataType dataType, int dataMaxLength) throws ISO8583MarshalingException {

        if (fieldData == null || fieldData.isEmpty())
            return;

        //if the datatype is not text or binary then
        //error check the data
        if (dataType != ISO8583DataType.TEXT &&
                dataType != ISO8583DataType.BINARY) {

            //error check data...
            Pattern p = dataType.getDataCheckPattern();
            Matcher m = p.matcher(fieldData);

            //check valid data
            if (!m.find())
                throw new ISO8583MarshalingException("Invalid data at field " + fieldPosition + ": [" + fieldData + "]");

            //check valid data length
            if (fieldData.length() > dataMaxLength)
                throw new ISO8583MarshalingException("Invalid data at field " + fieldPosition + ": data larger than field max size " + dataMaxLength);
        }

        if (dataType == ISO8583DataType.BINARY) {

            if (fieldData.length() * ISO8583Constants.BITS_IN_HEX_VALUE > dataMaxLength)
                throw new ISO8583MarshalingException("Invalid data at field " + fieldPosition + ": data larger than field max size " + dataMaxLength);

            if (fieldData.length() % ISO8583Constants.HEX_VALUE_LENGTH > 0)
                throw new ISO8583MarshalingException("Invalid data at field " + fieldPosition + ": data length must be a multiple of 2.");
        }
    }

    private void umarshalISO8583MessageTypeIndicator(byte[] data, Document doc) {

        ISO8583MessageTypeIndicator mti = encoderMap.get(mtiEncodingType).unmarshalMTI(data);

        Element mitElement = doc.createElement(MIT_TAG);
        mitElement.setAttribute(VERSION_ATTRIBUTE, Integer.toString(mti.getVersion())); // unmarshal version
        mitElement.setAttribute(CLASS_ATTRIBUTE, Integer.toString(mti.getMessageClass())); // unmarshal class
        mitElement.setAttribute(FUNCTION_ATTRIBUTE, Integer.toString(mti.getFunction())); // unmarshal function
        mitElement.setAttribute(ORIGIN_ATTRIBUTE, Integer.toString(mti.getOrigin())); // unmarshal origin

        Element root = doc.getDocumentElement();
        root.appendChild(mitElement);
    }

    private void unmarshalISO8583Fields(byte[] data, Document doc) throws ISO8583MarshalingException {

        int bitmapLength = 0;
        ISO8583Encoder bitmapEncoder = null;

        int position = -1;
        int offset = 0;
        int bitmapStartIndex = encoderMap.get(mtiEncodingType).getMtiLength();

        //create fields element
        Element fieldsElement = doc.createElement(FIELDS_TAG);
        doc.getDocumentElement().appendChild(fieldsElement);

        //retrieve bitmaps
        bitmap.setBitmapStartIndex(bitmapStartIndex);
        bitmap.extractBitmap(data);
        bitmapLength = bitmap.getBitmapLength();

        //setup index where the data in the ISO8583 message starts
        offset = bitmapStartIndex + bitmapLength;

        //cycle through each field and unmarshal it
        position = bitmap.getNextField();
        while (position != 0) {
            //marshal the data into the XML document... track
            //number of bytes read so we know where the next field will start
            offset += unmarshalISO8583Field(offset, position, data, fieldsElement, doc);

            position = bitmap.getNextField();
        }
    }

    private int unmarshalISO8583Field(int offset,
                                      int position,
                                      byte[] data,
                                      Element fieldsElement,
                                      Document doc) throws ISO8583MarshalingException {

        int startIndex = offset;
        int bytesRead = 0;
        String dataString = "";
        ISO8583UnmarshaledFieldData unmarshaledFieldData = null;

        //get schema information for the field
        ISO8583DataElement dataElement = iso8583Schema[position - 1];

        if (startIndex > data.length - 1)
            throw new ISO8583MarshalingException("Invalid Data: Missing field data for field " + position);

        //unmarshal the field
        unmarshaledFieldData = encoderMap.get(dataElement.getEncoderType()).unmarshalField(data, startIndex, dataElement);

        //check for any errors...
        if (unmarshaledFieldData.isErrored())
            throw new ISO8583MarshalingException("Error at field "
                    + position + ": " + unmarshaledFieldData.getErrorMessage());

        dataString = unmarshaledFieldData.getData();
        bytesRead = unmarshaledFieldData.getBytesRead();

        //remove any null characters
        Matcher nullMatcher = TRIM_NULL_PATTERN.matcher(dataString);
        dataString = nullMatcher.replaceAll("");

        //trim any leading zeros only for static numeric fields that are not all zero
        Matcher onlyZeroMatcher = ONLY_ZERO_PATTERN.matcher(dataString);
        if (!dataElement.isVariable() &&
                dataElement.getDataType() == ISO8583DataType.NUMERIC &&
                !onlyZeroMatcher.find()) {
            Matcher leadingZeroMatcher = TRIM_LEADING_ZERO_PATTERN.matcher(dataString);
            dataString = leadingZeroMatcher.replaceAll("");
        }

        //create a new field element
        Element fieldElement = doc.createElement(FIELD_TAG);
        fieldElement.setAttribute(POSITION_ATTRIBUTE, Integer.toString(position));
        fieldElement.appendChild(doc.createTextNode(dataString));

        //assign new field to fields collection
        fieldsElement.appendChild(fieldElement);

        return bytesRead; //return number of bytes read
    }

    /*
    private byte[] concatByteArray(byte[] firstArray, byte[] secondArray) {

        if (firstArray == null && secondArray != null)
            return secondArray;
        else if (firstArray != null && secondArray == null)
            return firstArray;
        else if (firstArray == null && secondArray == null)
            return new byte[]{};

        byte[] result = new byte[firstArray.length + secondArray.length];

        System.arraycopy(firstArray, 0, result, 0, firstArray.length);
        System.arraycopy(secondArray, 0, result, firstArray.length, secondArray.length);

        return result;
    }
    */

    public Map<ISO8583EncoderType, ISO8583Encoder> getEncoderMap() {
        return encoderMap;
    }

    public void setEncoderMap(Map<ISO8583EncoderType, ISO8583Encoder> encoderMap) {
        this.encoderMap = encoderMap;
    }

    public ISO8583DataElement[] getIso8583Schema() {
        return iso8583Schema;
    }

    public void setIso8583Schema(ISO8583DataElement[] iso8583Schema) {
        this.iso8583Schema = iso8583Schema;
    }

    public ISO8583EncoderType getMtiEncodingType() {
        return mtiEncodingType;
    }

    public void setMtiEncodingType(ISO8583EncoderType mtiEncodingType) {
        this.mtiEncodingType = mtiEncodingType;
    }

    public boolean isSecondaryBitmapMandatory() {
        return secondaryBitmapMandatory;
    }

    public void setSecondaryBitmapMandatory(boolean secondaryBitmapMandatory) {
        this.secondaryBitmapMandatory = secondaryBitmapMandatory;
    }

    public boolean isTertiaryBitmapMandatory() {
        return tertiaryBitmapMandatory;
    }

    public void setTertiaryBitmapMandatory(boolean tertiaryBitmapMandatory) {
        this.tertiaryBitmapMandatory = tertiaryBitmapMandatory;
    }
}
