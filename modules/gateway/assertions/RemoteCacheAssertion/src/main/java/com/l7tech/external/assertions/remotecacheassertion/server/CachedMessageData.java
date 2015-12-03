package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import com.l7tech.message.JsonKnob;
import com.l7tech.message.Message;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.util.HexUtils;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 15/11/11
 * Time: 10:13 AM
 * To change this template use File | Settings | File Templates.
 */
public class CachedMessageData implements Serializable {

    private String contentType;
    private byte[] bodyBytes;
    private Map<ValueType, Object> cacheMessageData;
    private ValueType valueType;

    public static enum ValueType {JSON, BYTE_ARRAY}

    public CachedMessageData(Message msg, StashManagerFactory stashManagerFactory, String strValueType) throws Exception {

        cacheMessageData = new HashMap<>();
        ValueType type = ValueType.valueOf(strValueType);

        try {
            InputStream messageBody = null;
            try {
                ContentTypeHeader contentTypeHeader = msg.getMimeKnob().getOuterContentType();
                contentType = contentTypeHeader.getFullValue();

                messageBody = msg.getMimeKnob().getEntireMessageBodyAsInputStream();

                if (type.equals(ValueType.JSON)) {
                    JSONData jsonData = null;
                    if (msg.getJsonKnob() != null && msg.getMimeKnob().getFirstPart().getContentType().isJson()) {
                        JsonKnob jsonKnob = msg.getJsonKnob();
                        jsonData = jsonKnob.getJsonData();
                    } else {
                        //set message body
                        setBodyBytes(messageBody, stashManagerFactory);
                        String msgEncoded = HexUtils.encodeBase64(bodyBytes);
                        //create json object with mimeType and message body
                        String jsonString = "{\"mimeType\":\"" + contentType + "\", \"body\":\"" + msgEncoded + "\"}";
                        final JSONFactory instance = JSONFactory.getInstance();
                        jsonData = instance.newJsonData(jsonString);
                    }
                    cacheMessageData.put(type, jsonData.getJsonData());
                    valueType = ValueType.JSON;
                } else {
                    setBodyBytes(messageBody, stashManagerFactory);
                    cacheMessageData.put(type, bodyBytes);
                    valueType = ValueType.BYTE_ARRAY;
                }
            } finally {
                if (messageBody != null) messageBody.close();
            }
        } catch (Exception e) {
            throw new Exception();
        }
    }

    public CachedMessageData(byte[] bytes) throws Exception {
        if (bytes.length < 4) {
            throw new Exception();
        }

        int contentTypeLength = (bytes[0] << 24) + ((bytes[1] & 0xFF) << 16) + ((bytes[2] & 0xFF) << 8) + (bytes[3] & 0xFF);
        if (bytes.length < 4 + contentTypeLength) {
            throw new Exception();
        }

        contentType = new String(bytes, 4, contentTypeLength);

        bodyBytes = new byte[bytes.length - 4 - contentTypeLength];
        System.arraycopy(bytes, 4 + contentTypeLength, bodyBytes, 0, bytes.length - 4 - contentTypeLength);
    }

    public CachedMessageData(String jsonString) throws Exception {
        contentType = "application/json; charset=utf-8";
        bodyBytes = jsonString.getBytes();
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public byte[] toByteArray() {
        byte[] contentTypeBytes = contentType.getBytes();
        byte[] retVal = new byte[4 + contentTypeBytes.length + bodyBytes.length];

        retVal[0] = (byte) (contentTypeBytes.length >>> 24);
        retVal[1] = (byte) (contentTypeBytes.length >>> 16);
        retVal[2] = (byte) (contentTypeBytes.length >>> 8);
        retVal[3] = (byte) contentTypeBytes.length;

        System.arraycopy(contentTypeBytes, 0, retVal, 4, contentTypeBytes.length);
        System.arraycopy(bodyBytes, 0, retVal, 4 + contentTypeBytes.length, bodyBytes.length);

        return retVal;
    }

    public int sizeInBytes(ValueType type) {

        if (type.equals(ValueType.JSON)) {
            return 4 + contentType.getBytes().length + ((String) cacheMessageData.get(type)).getBytes().length;
        } else {
            return 4 + contentType.getBytes().length + ((byte[]) cacheMessageData.get(type)).length;
        }
    }

    public Object getCacheMessageData(ValueType valueType) {

        if (valueType.equals(ValueType.JSON)) {
            return (String) cacheMessageData.get(valueType);
        } else {
            return (byte[]) cacheMessageData.get(valueType);
        }
    }

    public ValueType getValueType() {
        return valueType;
    }

    private void setBodyBytes(InputStream messageBody, StashManagerFactory stashManagerFactory) throws Exception {

        StashManager sm = stashManagerFactory.createStashManager();
        try {
            sm.stash(0, messageBody);

            int index = 0;
            bodyBytes = new byte[(int) sm.getSize(0)];
            byte[] buffer = new byte[4096];
            InputStream is = sm.recall(0);
            while (true) {
                int bytesRead = is.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                System.arraycopy(buffer, 0, bodyBytes, index, bytesRead);
                index += bytesRead;
            }
        } finally {
            sm.close();
        }
    }
}
