package com.l7tech.external.assertions.js.features.bindings;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.message.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import jdk.nashorn.api.scripting.ScriptObjectMirror;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;

/**
 * JavaScriptMessage implementation to support messages.
 */
public class JavaScriptMessageImpl implements JavaScriptMessage {

    private static final Logger LOGGER = Logger.getLogger(JavaScriptMessageImpl.class.getName());

    protected final Message message;
    protected final ScriptObjectMirror scriptObjectMirror;

    public JavaScriptMessageImpl(final Message message, final ScriptObjectMirror scriptObjectMirror) {
        this.message = message;
        this.scriptObjectMirror = scriptObjectMirror;
    }

    @Override
    public Object getHeaders() {
        return toJavaScriptObject(message.getHeadersKnob().getHeaders().stream().filter(header -> header.getType() == HEADER_TYPE_HTTP));
    }

    @Override
    public void setHeaders(final Object headers) {
        final HeadersKnob headersKnob = message.getHeadersKnob();

        // Remove the existing headers
        final Collection<Header> headersCollection = headersKnob.getHeaders();
        for (Header h : headersCollection) {
            headersKnob.removeHeader(h.getKey(), h.getType());
        }

        // Add new headers
        final Map<String, Object> newHeaders = fromJavaScriptObject(headers);
        for (Map.Entry<String, Object> entry : newHeaders.entrySet()) {
            setHeader(headersKnob, entry.getKey(), entry.getValue());
        }
    }

    @Override
    public Object getHeader(final String name) {
        return message.getHeadersKnob().getHeaders(name, HEADER_TYPE_HTTP);
    }

    @Override
    public void setHeader(final String name, final Object value) {
        setHeader(message.getHeadersKnob(), name, value);
    }

    @Override
    public void addHeader(final String name, final String value) {
        message.getHeadersKnob().addHeader(name, value, HEADER_TYPE_HTTP);
    }

    @Override
    public void removeHeader(final String name) {
        message.getHeadersKnob().removeHeader(name, HEADER_TYPE_HTTP);
    }

    @Override
    public boolean hasHeader(final String name) {
        return message.getHeadersKnob().containsHeader(name, HEADER_TYPE_HTTP);
    }

    @Override
    public String getContentType() {
        try {
            return message.getMimeKnob().getFirstPart().getContentType().getFullValue();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Exception in getting Content-Type from message.", ExceptionUtils.getDebugException(e));
        }
        return null;
    }

    @Override
    public Object getContent() {
        try {
            final String contentType = getContentType();

            if (StringUtils.isNotBlank(contentType)) {
                final ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue(contentType);
                if (contentTypeHeader.isJson()) {
                    return scriptObjectMirror.callMember("parse", message.getJsonKnob().getJsonData().getJsonData());
                } else if (contentTypeHeader.isTextualContentType()) {
                    final MimeKnob mimeKnob = message.getMimeKnob();
                    return new String(IOUtils.slurpStream(mimeKnob.getEntireMessageBodyAsInputStream()), mimeKnob.getFirstPart().getContentType().getEncoding());
                }
            }

            return IOUtils.slurpStream(message.getMimeKnob().getEntireMessageBodyAsInputStream());
        } catch (IOException | NoSuchPartException | InvalidJsonException e) {
            LOGGER.log(Level.WARNING, "Exception in getting content from message.", ExceptionUtils.getDebugException(e));
            return null;
        }
    }

    @Override
    public void setContent(final Object content, final String contentType) {
        try {
            byte[] contentBytes;
            final ContentTypeHeader contentTypeHeader = ContentTypeHeader.parseValue(contentType);

            if (content instanceof ScriptObjectMirror) {
                contentBytes = ((String) scriptObjectMirror.callMember("stringify", content)).getBytes();
            } else if (content instanceof String) {
                contentBytes = ((String) content).getBytes();
            } else if (content instanceof byte[]) {
                contentBytes = (byte[]) content;
            } else {
                contentBytes = content.toString().getBytes();
            }
            message.initialize(contentTypeHeader, contentBytes);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Exception in setting content for message.", ExceptionUtils.getDebugException(e));
        }
    }

    /**
     * This method is intentionally empty, to adhere to the views and to flush if there are any attributes in future
     */
    @Override
    public void end() {
        // does nothing
    }

    protected void setHeader(final HeadersKnob headersKnob, final String name, final Object value) {
        if (value instanceof String) {
            headersKnob.setHeader(name, value, HEADER_TYPE_HTTP);
        } else if (value instanceof String[]) {
            headersKnob.removeHeader(name, HEADER_TYPE_HTTP);
            for (String val : (String[]) value) {
                headersKnob.addHeader(name, val, HEADER_TYPE_HTTP);
            }
        }
    }

    protected Object toJavaScriptObject(final Stream<Header> headerStream) {
        final Map<String, Object> headersMap = new HashMap<>();
        headerStream.forEach(header -> headersMap.put(header.getKey(), header.getValue()));
        return toJavaScriptObject(headersMap);
    }

    protected Object toJavaScriptObject(final Map<String, Object> map) {
        return scriptObjectMirror.callMember("parse", new JSONObject(map));
    }

    /**
     * Returns map out of one-level javascript object.
     * Currently, it is used to translate headers json object to equivalent MAP object.
     * @param jsonObject JavaScript JSON Object
     * @return Map representation.
     */
    protected Map<String, Object> fromJavaScriptObject(final Object jsonObject) {
        final String jsonString = (String) scriptObjectMirror.callMember("stringify", jsonObject);
        final JSONObject json = (JSONObject) JSONValue.parse(jsonString);
        return jsonObjectToMap(json);
    }

    /**
     * Transforms one-level JSONObject to its equivalent MAP representation
     * @param jsonObject JSONObject instance
     * @return Map representation
     */
    private  Map<String, Object> jsonObjectToMap(JSONObject jsonObject) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object key : jsonObject.keySet()) {
            String stringKey = String.valueOf(key);
            Object value = convertJsonObject(jsonObject.get(stringKey));
            result.put(stringKey, value);
        }
        return  result;
    }

    /**
     * JSONArray will be flattened in simple array.
     * @param value String or JSONArray.
     * @return String or String[]
     */
    private  Object convertJsonObject(Object value) {
        if (value instanceof JSONArray) {
            final JSONArray jsonArray = (JSONArray) value;
            final String[] array = new String[jsonArray.size()];
            return jsonArray.toArray(array);
        }
        return value;
    }
}
