package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server;

import com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583.*;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 17/12/12
 * Time: 2:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583Codec implements ProtocolCodecFactory, ExtensibleSocketConnectorCodec {

    private static final Logger logger = Logger.getLogger(ISO8583Codec.class.getName());

    private Map<ISO8583EncoderType, ISO8583Encoder> encoderMap = null;
    private ISO8583EncoderType mtiEncodingType = null;
    private String fieldPropertiesFileLocation = "";
    private ISO8583DataElement[] iso8583Schema = null;
    private boolean secondaryBitmapMandatory = false;
    private boolean tertiaryBitmapMandatory = false;
    private byte[] messageDelimiter = null;

    private byte[] messagePortion = null;

    public ISO8583Codec() {

        ISO8583Encoder encoder = null;
        String className = "";

        //setup a HashMap with available encoders
        //instantiate new HashMap
        encoderMap = new HashMap<ISO8583EncoderType, ISO8583Encoder>();

        //get list of available encoders, cycle through,
        //instantiate the encoder and place it in the HashMap
        EnumSet<ISO8583EncoderType> encoderTypes = EnumSet.allOf(ISO8583EncoderType.class);

        for (ISO8583EncoderType encoderType : encoderTypes) {

            //instantiate the encoder
            try {
                className = encoderType.getClassName();

                if (className != null) {
                    Class encoderClass =
                            ISO8583Codec.class.getClassLoader().loadClass(className);
                    encoder = (ISO8583Encoder) encoderClass.newInstance();
                } else
                    encoder = null;

            } catch (Exception e) {
            }

            encoderMap.put(encoderType, encoder);
        }
    }

    public void initializeISO8583Schema() {
        ISO8583Schema schema = new ISO8583Schema();

        try {
            schema.initialize(fieldPropertiesFileLocation);
            iso8583Schema = schema.getSchema();
        } catch (ISO8583SchemaException sE) {
            logger.log(Level.WARNING, sE.getMessage());
        }
    }

    @Override
    public void configureCodec(Object codecConfig) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        //get mtiEncodingType
        Method getMtiEncodingMethod = codecConfig.getClass().getMethod("getMtiEncoding");
        Object encoderType = getMtiEncodingMethod.invoke(codecConfig);
        mtiEncodingType = ISO8583EncoderType.fromString(encoderType.toString());

        //get if secondaryBitmapMandatory
        Method isSecondaryBitmapMandatoryMethod = codecConfig.getClass().getMethod("isSecondaryBitmapMandatory");
        secondaryBitmapMandatory = (Boolean) isSecondaryBitmapMandatoryMethod.invoke(codecConfig);

        //get if tertiaryBitmapMandatory
        Method isTertiaryBitmapMandatoryMethod = codecConfig.getClass().getMethod("isTertiaryBitmapMandatory");
        tertiaryBitmapMandatory = (Boolean) isTertiaryBitmapMandatoryMethod.invoke(codecConfig);

        //get location of ISO8583 properties file
        Method getFieldPropertiesFileLocationMethod = codecConfig.getClass().getMethod("getFieldPropertiesFileLocation");
        fieldPropertiesFileLocation = (String) getFieldPropertiesFileLocationMethod.invoke(codecConfig);

        //get the message delimiter
        Method getMessageDelimiterArrayMethod = codecConfig.getClass().getMethod("getMessageDelimiterArray");
        messageDelimiter = (byte[]) getMessageDelimiterArrayMethod.invoke(codecConfig); //returns byte[]

        initializeISO8583Schema();
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession session) throws Exception {
        return new ProtocolEncoder() {
            @Override
            public void encode(IoSession ioSession, Object message, ProtocolEncoderOutput out) throws Exception {

                ISO8583Marshaler marshaler = null;
                byte[] data = null;
                byte[] result = null;

                if (message == null)
                    throw new IllegalArgumentException("Message to encode is null.");
                else if (message instanceof Exception)
                    throw (Exception) message;

                //get data
                if (message instanceof String)
                    data = ((String) message).getBytes();
                else if (message instanceof byte[])
                    data = (byte[]) message;
                else
                    throw new IllegalArgumentException("The message to encode is not a supported type: " +
                            message.getClass().getCanonicalName());

                try {
                    marshaler = new ISO8583Marshaler();
                    marshaler.setEncoderMap(encoderMap); //set the encoder map
                    marshaler.setMtiEncodingType(mtiEncodingType); //set the encoding type for the mti
                    marshaler.setSecondaryBitmapMandatory(secondaryBitmapMandatory); //set if secondary bitmap is mandatory to include in the ISO8583 message
                    marshaler.setTertiaryBitmapMandatory(tertiaryBitmapMandatory); //set if tertiary bitmap is mandatory to include in the ISO8583 message
                    marshaler.setIso8583Schema(iso8583Schema); //set the schema
                    result = marshaler.marshal(data);
                } catch (ISO8583MarshalingException mE) {
                    // Error has occurred. Log and throw exception.
                    logger.log(Level.WARNING, mE.getMessage());
                    throw mE;
                }

                //send data out
                IoBuffer byteBuffer = createIoBuffer(result);
                out.write(byteBuffer);
            }

            @Override
            public void dispose(IoSession ioSession) throws Exception {
            }
        };
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession session) throws Exception {
        return new ProtocolDecoder() {
            @Override
            public void decode(IoSession ioSession, IoBuffer in, ProtocolDecoderOutput out) throws Exception {

                byte[] result = null;

                while (in.hasRemaining()) {
                    //get our data
                    byte[] data = getMessage(in, ioSession);

                    //we have received an entire message
                    if (data != null) {
                        //unmarshal the message
                        result = unmarshalMessage(data);

                        //write message
                        out.write(result);
                    }
                }
            }

            @Override
            public void finishDecode(IoSession ioSession, ProtocolDecoderOutput protocolDecoderOutput) throws Exception {
            }

            @Override
            public void dispose(IoSession ioSession) throws Exception {
            }
        };
    }

    private byte[] getMessage(IoBuffer in, IoSession ioSession) {
        if (messageDelimiter.length > 0)
            return getDelimitedMessage(in, ioSession);
        else
            return getNonDelimitedMessage(in);
    }

    private byte[] getNonDelimitedMessage(IoBuffer in) {

        int length = in.remaining();
        byte[] data = new byte[length];
        in.get(data);

        return data;
    }

    private byte[] getDelimitedMessage(IoBuffer in, IoSession ioSession) {

        String sessionAttributeKey = "sAttributeKey";
        boolean delimiterFound = false;
        int messageDelimiterCounter = 0;
        byte b = 0;
        int bytesRead = 0;
        byte[] temp = null;
        byte[] result = null;

        in.mark();

        //look for a message
        while (in.hasRemaining() && !delimiterFound) {

            b = in.get();

            if (b == messageDelimiter[messageDelimiterCounter])
                messageDelimiterCounter++;
            else {
                //gaurd against delimiter bytes being in the middle of a message.
                if (messageDelimiterCounter > 0)
                    messageDelimiterCounter = 0;
            }

            bytesRead++;

            if (messageDelimiterCounter == messageDelimiter.length)
                delimiterFound = true;
        }

        in.reset(); //move to beginning of message

        if (delimiterFound) {
            //read bytes
            temp = new byte[bytesRead - messageDelimiter.length];
            in.get(temp);

            //if we have portions of a message then add them
            //to the remainder of the message
            if (ioSession.getAttribute(sessionAttributeKey) != null) {
                result = concateByteArray((byte[]) ioSession.getAttribute(sessionAttributeKey), temp);
                ioSession.setAttribute(sessionAttributeKey, null); //clear out an message portions, they have been used
            } else { //we have received a complete message
                result = temp;
            }

            in.skip(messageDelimiter.length);
        } else {
            //read remaining bytes
            temp = new byte[bytesRead];
            in.get(temp);

            //store the bytes
            ioSession.setAttribute(sessionAttributeKey,
                    concateByteArray((byte[]) ioSession.getAttribute(sessionAttributeKey), temp));
            //messagePortion = concateByteArray(messagePortion, temp);
        }

        return result;
    }

    private IoBuffer createIoBuffer(byte[] data) {

        if (messageDelimiter.length > 0)
            return createDelimitedIoBuffer(data);
        else
            return createNonDelimitedIoBuffer(data);
    }

    private IoBuffer createNonDelimitedIoBuffer(byte[] data) {

        IoBuffer byteBuffer = IoBuffer.allocate(data.length);
        byteBuffer.put(data);
        byteBuffer.flip();

        return byteBuffer;
    }

    private IoBuffer createDelimitedIoBuffer(byte[] data) {

        IoBuffer byteBuffer = IoBuffer.allocate(data.length + messageDelimiter.length);
        byteBuffer.put(data);
        byteBuffer.put(messageDelimiter);
        byteBuffer.flip();

        return byteBuffer;
    }

    private byte[] unmarshalMessage(byte[] data) throws ISO8583MarshalingException {

        ISO8583Marshaler marshaler = null;
        byte[] result = null;

        //unmarshal data to xml
        try {
            marshaler = new ISO8583Marshaler();
            marshaler.setEncoderMap(encoderMap); //set the encoder map
            marshaler.setMtiEncodingType(mtiEncodingType); //set the encoding type for the mti
            marshaler.setSecondaryBitmapMandatory(secondaryBitmapMandatory); //set if secondary bitmap is mandatory to include in the ISO8583 message
            marshaler.setTertiaryBitmapMandatory(tertiaryBitmapMandatory); //set if tertiary bitmap is mandatory to include in the ISO8583 message
            marshaler.setIso8583Schema(iso8583Schema); //set the schema
            result = marshaler.unmarshal(data);
        } catch (ISO8583MarshalingException mE) {
            // Error has occurred. Log and throw exception.
            logger.log(Level.WARNING, mE.getMessage());
            throw mE;
        }

        return result;
    }

    private byte[] concateByteArray(byte[] arr1, byte[] arr2) {

        int arraySize = 0;

        if (arr1 == null) {
            if (arr2 != null)
                arraySize += arr2.length;
            else
                return null;
        } else {
            arraySize += arr1.length;
            if (arr2 != null)
                arraySize += arr2.length;
        }

        byte[] result = new byte[arraySize];

        if (arr1 == null)
            System.arraycopy(arr2, 0, result, 0, arr2.length);
        else if (arr2 == null)
            System.arraycopy(arr1, 0, result, 0, arr1.length);
        else {
            System.arraycopy(arr1, 0, result, 0, arr1.length);
            System.arraycopy(arr2, 0, result, arr1.length, arr2.length);
        }

        return result;
    }

    public ISO8583EncoderType getMtiEncodingType() {
        return mtiEncodingType;
    }

    public void setMtiEncodingType(ISO8583EncoderType mtiEncodingType) {
        this.mtiEncodingType = mtiEncodingType;
    }

    public String getFieldPropertiesFileLocation() {
        return fieldPropertiesFileLocation;
    }

    public void setFieldPropertiesFileLocation(String fieldPropertiesFileLocation) {
        this.fieldPropertiesFileLocation = fieldPropertiesFileLocation;
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

    public byte[] getMessageDelimiter() {
        return messageDelimiter;
    }

    public void setMessageDelimiter(byte[] messageDelimiter) {
        this.messageDelimiter = messageDelimiter;
    }
}
