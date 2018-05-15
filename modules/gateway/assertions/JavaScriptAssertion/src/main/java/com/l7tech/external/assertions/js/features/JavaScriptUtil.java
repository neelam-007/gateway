package com.l7tech.external.assertions.js.features;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.message.Message;
import com.l7tech.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * JavaScriptUtil to provide utility methods
 */
public class JavaScriptUtil {

    public static String getStringFromMsg(Message msgObj) throws IOException, NoSuchPartException {
        final InputStream bodyAsInputStream = msgObj.getMimeKnob().getEntireMessageBodyAsInputStream();
        return new String(IOUtils.slurpStream(bodyAsInputStream));
    }
}
