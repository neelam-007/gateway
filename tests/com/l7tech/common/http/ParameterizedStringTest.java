package com.l7tech.common.http;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Hashtable;
import java.net.URLDecoder;

import junit.framework.TestCase;

/**
 * Test cases for parameterized string parser.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ParameterizedStringTest  extends TestCase {

    //- PUBLIC

    public void testBasic() {
        String queryString = "a=b";
        Map paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    public void testNoName() {
        String queryString = "=b";
        Map paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    public void testNoValue() {
        String queryString = "a=";
        Map paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    public void testEqualsInValue() {
        String queryString = "a=b&c=d=e";
        Map paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    public void testPercentInValue() {
        String queryString = "a=b&c=d%e";
        Map paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    public void testEncodedName() throws Exception {
        String queryString = "a%3b=b";
        Map paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    public void testEncodedValue() {
        String queryString = "a=b%3b";
        Map paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    public void testStrictEncoding() {
        String queryString = "a=b%c";

        ParameterizedString.parseQueryString(queryString); // not strict, should work

        try {
            ParameterizedString.parseQueryString(queryString, true);
            fail("Expected exception during parsing of '"+queryString+"'");
        }
        catch(IllegalArgumentException iae) {
        }
    }

    public void testStrictCharacters() {
        String queryString = "a=b c";

        ParameterizedString.parseQueryString(queryString); // not strict should work

        try {
            ParameterizedString.parseQueryString(queryString, true);
            fail("Expected exception during parsing of '"+queryString+"'");
        }
        catch(IllegalArgumentException iae) {
        }
    }

    public void testIllegalValue() {
        String queryString = "a";
        boolean httpUtilThrew = false;
        boolean paramStrThrew = false;

        try {
            ParameterizedString.parseQueryString(queryString);
        }
        catch(IllegalArgumentException iae) {
            paramStrThrew = true;
        }
        try {
            HttpUtils_parseQueryString(queryString);
        }
        catch(IllegalArgumentException iae) {
            httpUtilThrew = true;
        }

        assertEquals("Test for exception during parsing of query string '"+queryString+"'", httpUtilThrew, paramStrThrew);
    }


    //- PRIVATE

    private Map enlist(Map params) {
        Map enlisted = new LinkedHashMap();
        for(Iterator iterator = params.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            enlisted.put(entry.getKey(), Arrays.asList((String[])entry.getValue()));
        }
        return enlisted;
    }

    // just so we don't get as many deprecation warnings
    private static Hashtable HttpUtils_parseQueryString(String query) {
        return javax.servlet.http.HttpUtils.parseQueryString(query);
    }
}
