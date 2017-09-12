/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.message;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.json.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JsonFacet extends MessageFacet{
    /**
     * @param message  the Message that owns this aspect
     * @param delegate the delegate to chain to or null if there isn't one.  Can't be changed after creation.
     */
    JsonFacet(Message message, MessageFacet delegate) {
        super(message, delegate);
    }

    @Override
    MessageKnob getKnob(Class c) {
        if(c == JsonKnob.class){
            final MimeKnob mk = (MimeKnob)super.getKnob(MimeKnob.class);
            return new JsonKnobImpl(mk);
        }

        return super.getKnob(c);
    }

    private static class JsonKnobImpl implements JsonKnob {

        public JsonKnobImpl(MimeKnob mimeKnob) {
            if (mimeKnob == null) throw new NullPointerException();
            this.mimeKnob = mimeKnob;
            jsonDataHolder = new ConcurrentHashMap<>(JsonSchemaVersion.values().length);
        }

        @Override
        public JSONData getJsonData() throws IOException, InvalidJsonException {
            return getJsonData(JsonSchemaVersion.DRAFT_V2);
        }

        @Override
        public JSONData getJsonData(JsonSchemaVersion version) throws IOException, InvalidJsonException{
            try {
                return jsonDataHolder.computeIfAbsent(version, this::createJsonDataUnchecked);
            } catch (UncheckedInvalidJsonException e) {
                throw e.getCause();
            } catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }

        /**
         * Create json data with unchecked exceptions
         * @param version the version of json schema to create
         * @return JSONData
         * @throws UncheckedInvalidJsonException if an unsupported json schema version is provided
         * @throws UncheckedIOException if an io issues occurred
         */
        private JSONData createJsonDataUnchecked(final JsonSchemaVersion version) {
            try {
                return createJsonData(version);
            } catch (InvalidJsonException e) {
                throw new UncheckedInvalidJsonException(e);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        /**
         * Create json data with unchecked exceptions
         * @param version the version of json schema to create
         * @return JSONData
         * @throws InvalidJsonException if an unsupported json schema version is provided
         * @throws IOException if any io issues occurred
         */
        private JSONData createJsonData(final JsonSchemaVersion version) throws IOException, InvalidJsonException {
            logger.log(Level.FINE, () -> "Creating json data for version " + version);
            final PartInfo firstPart = mimeKnob.getFirstPart();
            if (!firstPart.getContentType().isJson()) {
                //this should never happen as it should already have been called by the caller
                throw new IOException("Content type of first part of Message is not JSON (application/json)");
            }

            final String jsonDataStr;
            try {
                final Charset encoding = firstPart.getContentType().getEncoding();
                jsonDataStr = new String(IOUtils.slurpStream(firstPart.getInputStream(false)), encoding);
            } catch (NoSuchPartException e) {
                throw new IOException("Unable to parse JSON: " + ExceptionUtils.getMessage(e));
            }

            return JSONFactory.INSTANCE.newJsonData(jsonDataStr, version);
        }

        //- PRIVATE
        private final MimeKnob mimeKnob;
        private final ConcurrentMap<JsonSchemaVersion, JSONData> jsonDataHolder;
        private final static Logger logger = Logger.getLogger(JsonFacet.class.getName());
    }
}
