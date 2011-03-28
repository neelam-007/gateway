package com.l7tech.server.ems.util;

import org.mortbay.util.ajax.JSON;

/**
 * JSON utility methods.
 */
public class JsonUtil {

    public static String toJson( final Object data, final int size ) {
        final StringBuffer buffer = new StringBuffer( size );

        writeJson(data, buffer);

        return buffer.toString();
    }

    public static void writeJson( final Object data, final StringBuffer buffer ) {
        JSON json = new JSON();
        json.append(buffer, data);
    }

}
