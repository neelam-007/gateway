/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.message;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.json.InvalidJsonException;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;

import java.io.IOException;
import java.nio.charset.Charset;
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

    private static class JsonKnobImpl implements JsonKnob{

        public JsonKnobImpl(MimeKnob mimeKnob) {
            if (mimeKnob == null) throw new NullPointerException();
            this.mimeKnob = mimeKnob;
        }

        @Override
        public JSONData getJsonData() throws IOException, InvalidJsonException{
            if(jsonData == null){
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

                jsonData =  JSONFactory.getInstance().newJsonData(jsonDataStr);
            }else{
                logger.log(Level.WARNING, "Did not need to create json data");   
            }
            return jsonData;
        }

        //- PRIVATE
        private final MimeKnob mimeKnob;
        private JSONData jsonData;
        private final static Logger logger = Logger.getLogger(JsonFacet.class.getName());
    }
}
