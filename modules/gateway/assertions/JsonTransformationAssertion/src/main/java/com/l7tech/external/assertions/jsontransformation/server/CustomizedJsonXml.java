package com.l7tech.external.assertions.jsontransformation.server;

import org.json.*;

import java.util.Iterator;

/*
 *  This class contains customized versions of the XML-to-json methods from the org.json.XML class.
 *
 *  Included into Gateway assertion source with modifications by Mike.Lyons@ca.com.
 *  Based on original code from JSON.org.
 */

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

/**
 * This provides methods to convert an XML text into a JSONObject.
 * <p/>
 * This version has been modified by Mike.Lyons@ca.com:
 * <ul>
 *   <li>methods are no longer static, so behavior can be adjusted per-instance.
 *   <li>a switch is included for whether or not to always emit numeric-looking values as strings when converting XML to JSON
 *   <li>string values of "0.0" and "-0.0" are no longer emitted as the numeric value "0"
 *     (since this fails to conservatively retain the exact string representation of the input).
 *   <li>the string value "-Infinity" no longer triggers a JSONException from JSONObject.testValidity() "JSON does not allow non-finite numbers."
 * </ul>
 *
 * Original author: JSON.org
 * Original version: 2014-05-03
 */
public class CustomizedJsonXml {

    private boolean useNumbersWhenPossible;

    /**
     * Scan the content following the named tag, attaching it to the context.
     * @param x       The XMLTokener containing the source string.
     * @param context The JSONObject that will include the new material.
     * @param name    The tag name.
     * @return true if the close tag is processed.
     * @throws JSONException
     */
    private boolean parse(XMLTokener x, JSONObject context,
                                 String name) throws JSONException {
        char       c;
        int        i;
        JSONObject jsonobject;
        String     string;
        String     tagName;
        Object     token;

// Test for and skip past these forms:
//      <!-- ... -->
//      <!   ...   >
//      <![  ... ]]>
//      <?   ...  ?>
// Report errors for these forms:
//      <>
//      <=
//      <<

        token = x.nextToken();

// <!

        if (token == XML.BANG) {
            c = x.next();
            if (c == '-') {
                if (x.next() == '-') {
                    x.skipPast("-->");
                    return false;
                }
                x.back();
            } else if (c == '[') {
                token = x.nextToken();
                if ("CDATA".equals(token)) {
                    if (x.next() == '[') {
                        string = x.nextCDATA();
                        if (string.length() > 0) {
                            context.accumulate("content", string);
                        }
                        return false;
                    }
                }
                throw x.syntaxError("Expected 'CDATA['");
            }
            i = 1;
            do {
                token = x.nextMeta();
                if (token == null) {
                    throw x.syntaxError("Missing '>' after '<!'.");
                } else if (token == XML.LT) {
                    i += 1;
                } else if (token == XML.GT) {
                    i -= 1;
                }
            } while (i > 0);
            return false;
        } else if (token == XML.QUEST) {

// <?

            x.skipPast("?>");
            return false;
        } else if (token == XML.SLASH) {

// Close tag </

            token = x.nextToken();
            if (name == null) {
                throw x.syntaxError("Mismatched close tag " + token);
            }
            if (!token.equals(name)) {
                throw x.syntaxError("Mismatched " + name + " and " + token);
            }
            if (x.nextToken() != XML.GT) {
                throw x.syntaxError("Misshaped close tag");
            }
            return true;

        } else if (token instanceof Character) {
            throw x.syntaxError("Misshaped tag");

// Open tag <

        } else {
            tagName = (String)token;
            token = null;
            jsonobject = new JSONObject();
            for (;;) {
                if (token == null) {
                    token = x.nextToken();
                }

// attribute = value

                if (token instanceof String) {
                    string = (String)token;
                    token = x.nextToken();
                    if (token == XML.EQ) {
                        token = x.nextToken();
                        if (!(token instanceof String)) {
                            throw x.syntaxError("Missing value");
                        }
                        jsonobject.accumulate( string,
                                stringToValue( (String) token ) );
                        token = null;
                    } else {
                        jsonobject.accumulate(string, "");
                    }

// Empty tag <.../>

                } else if (token == XML.SLASH) {
                    if (x.nextToken() != XML.GT) {
                        throw x.syntaxError("Misshaped tag");
                    }
                    if (jsonobject.length() > 0) {
                        context.accumulate(tagName, jsonobject);
                    } else {
                        context.accumulate(tagName, "");
                    }
                    return false;

// Content, between <...> and </...>

                } else if (token == XML.GT) {
                    for (;;) {
                        token = x.nextContent();
                        if (token == null) {
                            if (tagName != null) {
                                throw x.syntaxError("Unclosed tag " + tagName);
                            }
                            return false;
                        } else if (token instanceof String) {
                            string = (String)token;
                            if (string.length() > 0) {
                                jsonobject.accumulate( "content",
                                        stringToValue( string ) );
                            }

// Nested element

                        } else if (token == XML.LT) {
                            if (parse(x, jsonobject, tagName)) {
                                if (jsonobject.length() == 0) {
                                    context.accumulate(tagName, "");
                                } else if (jsonobject.length() == 1 &&
                                        jsonobject.opt("content") != null) {
                                    context.accumulate(tagName,
                                            jsonobject.opt("content"));
                                } else {
                                    context.accumulate(tagName, jsonobject);
                                }
                                return false;
                            }
                        }
                    }
                } else {
                    throw x.syntaxError("Misshaped tag");
                }
            }
        }
    }



    /**
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string. This is much less ambitious than
     * JSONObject.stringToValue, especially because it does not attempt to
     * convert plus forms, octal forms, hex forms, or E forms lacking decimal
     * points.
     * @param string A String.
     * @return A simple JSON value.
     */
    public Object stringToValue(String string) {
        if ("true".equalsIgnoreCase(string)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(string)) {
            return Boolean.FALSE;
        }
        if ("null".equalsIgnoreCase(string)) {
            return JSONObject.NULL;
        }

        if ( !useNumbersWhenPossible ) {
            return string;
        }

// If it might be a number, try converting it, first as a Long, and then as a
// Double. If that doesn't work, return the string.
        if ( "0".equals( string ) ) {
            return 0L;
        }

        char initial = string.charAt(0);
        if ( initial == '-' && string.length() > 1 ) {
            initial = string.charAt(1);
        }

        if ( initial >= '0' && initial <= '9' ) {
            try {
                Long value = Long.parseLong( string );
                if (value.toString().equals(string)) {
                    return value;
                }
            }  catch (NumberFormatException ignore) {
                try {
                    Double value = Double.parseDouble( string );
                    // values of "0.0" and "-0.0", if returned as a Double, would get changed to "0" or "-0" respectively by the JSON layer
                    if (value.toString().equals(string) && !value.equals( 0.0 ) && !value.equals( -0.0 ) ) {
                        return value;
                    }
                }  catch (Exception ignoreAlso) {
                    /* IGNORE */
                }
            }
        }
        return string;
    }


    /**
     * Convert a well-formed (but not necessarily valid) XML string into a
     * JSONObject. Some information may be lost in this transformation
     * because JSON is a data format and XML is a document format. XML uses
     * elements, attributes, and content text, while JSON uses unordered
     * collections of name/value pairs and arrays of values. JSON does not
     * does not like to distinguish between elements and attributes.
     * Sequences of similar elements are represented as JSONArrays. Content
     * text may be placed in a "content" member. Comments, prologs, DTDs, and
     * <code>&lt;[ [ ]]></code> are ignored.
     * @param string The source string.
     * @return A JSONObject containing the structured data from the XML string.
     * @throws JSONException
     */
    public JSONObject toJSONObject(String string) throws JSONException {
        JSONObject jo = new JSONObject();
        XMLTokener x = new XMLTokener(string);
        while (x.more() && x.skipPast("<")) {
            parse(x, jo, null);
        }
        return jo;
    }


    /**
     * Convert a JSONObject into a well-formed, element-normal XML string.
     * @param object A JSONObject.
     * @return  A string.
     * @throws  JSONException
     */
    public String toString(Object object) throws JSONException {
        return toString(object, null);
    }


    /**
     * Convert a JSONObject into a well-formed, element-normal XML string.
     * @param object A JSONObject.
     * @param tagName The optional name of the enclosing tag.
     * @return A string.
     * @throws JSONException
     */
    public String toString(Object object, String tagName)
            throws JSONException {
        StringBuilder       sb = new StringBuilder();
        int                 i;
        JSONArray ja;
        JSONObject          jo;
        String              key;
        Iterator<String> keys;
        int                 length;
        String              string;
        Object              value;
        if (object instanceof JSONObject) {

// Emit <tagName>

            if (tagName != null) {
                sb.append('<');
                sb.append(tagName);
                sb.append('>');
            }

// Loop thru the keys.

            jo = (JSONObject)object;
            keys = jo.keys();
            while (keys.hasNext()) {
                key = keys.next();
                value = jo.opt(key);
                if (value == null) {
                    value = "";
                }
                string = value instanceof String ? (String)value : null;

// Emit content in body

                if ("content".equals(key)) {
                    if (value instanceof JSONArray) {
                        ja = (JSONArray)value;
                        length = ja.length();
                        for (i = 0; i < length; i += 1) {
                            if (i > 0) {
                                sb.append('\n');
                            }
                            sb.append(XML.escape(ja.get(i).toString()));
                        }
                    } else {
                        sb.append(XML.escape(value.toString()));
                    }

// Emit an array of similar keys

                } else if (value instanceof JSONArray) {
                    ja = (JSONArray)value;
                    length = ja.length();
                    for (i = 0; i < length; i += 1) {
                        value = ja.get(i);
                        if (value instanceof JSONArray) {
                            sb.append('<');
                            sb.append(key);
                            sb.append('>');
                            sb.append(toString(value));
                            sb.append("</");
                            sb.append(key);
                            sb.append('>');
                        } else {
                            sb.append(toString(value, key));
                        }
                    }
                } else if ("".equals(value)) {
                    sb.append('<');
                    sb.append(key);
                    sb.append("/>");

// Emit a new tag <k>

                } else {
                    sb.append(toString(value, key));
                }
            }
            if (tagName != null) {

// Emit the </tagname> close tag

                sb.append("</");
                sb.append(tagName);
                sb.append('>');
            }
            return sb.toString();

// XML does not have good support for arrays. If an array appears in a place
// where XML is lacking, synthesize an <array> element.

        } else {
            if (object.getClass().isArray()) {
                object = new JSONArray(object);
            }
            if (object instanceof JSONArray) {
                ja = (JSONArray)object;
                length = ja.length();
                String tag = tagName == null ? "array" : tagName;
                if(length == 0){
                    sb.append("<").append(tag).append("/>");
                    return sb.toString();
                }
                sb.append("<").append(tag).append(">");
                for (i = 0; i < length; i += 1) {
                    sb.append(toString(ja.opt(i), "value"));
                }
                sb.append("</").append(tag).append(">");
                return sb.toString();
            } else {
                string = (object == null) ? "null" : XML.escape(object.toString());
                return (tagName == null) ? "\"" + string + "\"" :
                        (string.length() == 0) ? "<" + tagName + "/>" :
                                "<" + tagName + ">" + string + "</" + tagName + ">";
            }
        }
    }

    public void setUseNumbersWhenPossible( boolean useNumbersWhenPossible ) {
        this.useNumbersWhenPossible = useNumbersWhenPossible;
    }

    public boolean isUseNumbersWhenPossible() {
        return useNumbersWhenPossible;
    }
}
