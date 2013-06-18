package com.l7tech.policy.assertion.ext.message;

import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNotSettableException;

import java.io.IOException;
import java.io.InputStream;
import org.w3c.dom.Document;

/**
 * Interface providing methods to extract different message formats from the service Request.
 * Supported formats are: DOM, JsonData, byte[], InputStream.
 */
public interface DataExtractor {
    /**
     * Depending on the format, get the message Document or JsonData or entire message body (either as bytes or input stream)
     *
     * @param targetable    The target message.
     *                      If null then target will default to Request or Response depending if the assertion is before or after routing.
     * @param format        Requested format {@link CustomMessageFormat}.
     * @return Depending on the format:
     *         <p>if {@link CustomMessageFormat#XML} then, a copy of the document that is associated with target message. NULL if the message doesn't contain XML document (e.g. is JSON message).</p>
     *         <p>if {@link CustomMessageFormat#JSON} then, a copy of the JSON data that is associated with target message. NULL if the message doesn't contain Json data (e.g. is XML message).</p>
     *         <p>if {@link CustomMessageFormat#BYTES} then, entire message body bytes, including attachments, if any. NULL if the message doesn't contain any bytes.</p>
     *         <p>if {@link CustomMessageFormat#INPUT_STREAM} then, entire message body as {@link java.io.InputStream}, including attachments, if any. NULL if the message doesn't contain any bytes.</p>
     * @throws NoSuchVariableException If a variable with the name exists but is not a message variable.
     * @throws IllegalArgumentException if format is null
     */
    CustomMessageData getMessageData(CustomMessageTargetable targetable, CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException;

    /**
     * Override of {@link #getMessageData(com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable, CustomMessageFormat)},
     * since targetable is not specified, target would be either default Request or Response (if it's before or after routing).
     *
     * @see #getMessageData(com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable, CustomMessageFormat)
     */
    CustomMessageData getMessageData(CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException;

    /**
     * Check if the message contains the requested format.
     *
     * @param targetable    The target message.
     *                      If null then target will default to Request or Response depending if the assertion is before or after routing.
     * @param format        Requested format {@link CustomMessageFormat}.
     * @return true if the message contains the requested format.
     * @throws NoSuchVariableException If a variable with the name exists but is not a message variable.
     * @throws IllegalArgumentException if format is null
     */
    boolean isMessageDataOfType(CustomMessageTargetable targetable, CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException;

    /**
     * Override of {@link #isMessageDataOfType(com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable, CustomMessageFormat)},
     * since targetable is not specified, target would be either default Request or Response (if it's before or after routing).
     *
     * @see #isMessageDataOfType(com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable, CustomMessageFormat)
     */
    boolean isMessageDataOfType(CustomMessageFormat format) throws NoSuchVariableException, IllegalArgumentException;

    /**
     * Set or replace the XML document that is associated with the target message, creates new message if target is not found.
     *
     * @param targetable    The target message.
     *                      If null then target will default to Request or Response depending if the assertion is before or after routing.
     * @param document      The target message XML document to be set.
     * @throws NoSuchVariableException      If a variable with the name exists but is not a message variable.
     * @throws VariableNotSettableException If the variable does not exist and cannot be created.
     */
    void setDOM(CustomMessageTargetable targetable, Document document) throws NoSuchVariableException, VariableNotSettableException;

    /**
     * Set or replace the XML document that is associated with default Request or Response (if it's before or after routing).
     *
     * @see #setDOM(CustomMessageTargetable, org.w3c.dom.Document)
     */
    void setDOM(Document document) throws NoSuchVariableException, VariableNotSettableException;

    /**
     * Set the Json data that is associated with the target message, creates new message if target is not found.
     *
     * @param targetable    The target message.
     *                      If null then target will default to Request or Response depending if the assertion is before or after routing.
     * @param jsonData      The target message Json data to be set.
     * @throws NoSuchVariableException      If a variable with the name exists but is not a message variable.
     * @throws VariableNotSettableException If the variable does not exist and cannot be created.
     * @throws IOException                  if there is a problem reading the data from the message body.
     */
    void setJson(CustomMessageTargetable targetable, String jsonData) throws NoSuchVariableException, VariableNotSettableException, IOException;

    /**
     * Set the Json data that is associated with default Request or Response (if it's before or after routing).
     *
     * @see #setJson(CustomMessageTargetable, String)
     */
    void setJson(String jsonData) throws NoSuchVariableException, VariableNotSettableException, IOException;

    /**
     * Set the entire target message bytes, creates new message if target is not found.
     *
     * @param targetable     The target message.
     *                       If null then target will default to Request or Response depending if the assertion is before or after routing.
     * @param contentType    Message body content type.
     * @param bytes          Entire message body bytes.
     * @throws NoSuchVariableException      If a variable with the name exists but is not a message variable.
     * @throws VariableNotSettableException If the variable does not exist and cannot be created.
     * @throws IOException                  if there is a problem reading the bytes.
     * @throws IllegalArgumentException     if contentType is null or invalid.
     */
    void setBytes(CustomMessageTargetable targetable, CustomContentHeader contentType, byte[] bytes) throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException;

    /**
     * Set the entire default Request or Response (if it's before or after routing) message bytes.
     *
     * @see #setBytes(CustomMessageTargetable, com.l7tech.policy.assertion.ext.message.CustomContentHeader, byte[])
     */
    void setBytes(CustomContentHeader contentType, byte[] bytes) throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException;

    /**
     * Set the entire message body with content attached to the specified InputStream.
     *
     * @param targetable    The target message.
     *                      If null then target will default to Request or Response depending if the assertion is before or after routing.
     * @param body          an InputStream positioned at the first byte of body content for this Message.
     * @throws NoSuchVariableException      If a variable with the name exists but is not a message variable.
     * @throws VariableNotSettableException If the variable does not exist and cannot be created.
     * @throws IOException                  if there is a problem reading the input stream.
     * @throws IllegalArgumentException     if contentType or body is null or invalid.
     */
    void setEntireMessageBodyFromInputStream(CustomMessageTargetable targetable, CustomContentHeader contentType, InputStream body) throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException;

    /**
     * Set the entire default Request or Response (if it's before or after routing) message body with content attached to the specified InputStream.
     *
     * @see #setEntireMessageBodyFromInputStream(com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable, com.l7tech.policy.assertion.ext.message.CustomContentHeader, java.io.InputStream)
     */
    void setEntireMessageBodyFromInputStream(CustomContentHeader contentType, InputStream body) throws NoSuchVariableException, VariableNotSettableException, IOException, IllegalArgumentException;

    /**
     * Parse a MIME Content-Type: header, not including the header name and colon.
     * Example: <code>parseValue("text/html; charset=\"UTF-8\"")</code>
     *
     * @param contentTypeHeaderValue the header value to parse
     * @return a CustomContentHeader instance.  Never null.
     * @throws java.io.IOException  if the specified header value was missing, empty, or syntactically invalid.
     */
    CustomContentHeader parseContentTypeValue(String contentTypeHeaderValue) throws IOException;

    /**
     * Create one of the predefined content-types
     * 
     * @param type the predefined {@link CustomContentHeader.Type} 
     * @return a CustomContentHeader instance.   Never null. 
     * @throws IllegalArgumentException if type is null or invalid.
     */
    CustomContentHeader createContentType(CustomContentHeader.Type type) throws IllegalArgumentException;
}
