package com.l7tech.common.http;

import java.util.*;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Test cases for parameterized string parser.
 */
public class ParameterizedStringTest  {

    //- PUBLIC
    @BeforeClass
    public static void setUp() throws Exception {
        System.setProperty("com.l7tech.http.useSemicolonAsSeparator", "true");
    }

    @Test
    public void testBasic() {
        String queryString = "a=b";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testNoName() {
        String queryString = "=b";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testNoValue() {
        String queryString = "a=";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testNoEquals() {
        String queryString = "a";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        assertTrue("Test for query string '"+queryString+"'", paramStrMap.isEmpty() );
    }

    @Test
    public void testEqualsInValue() {
        String queryString = "a=b&c=d=e";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testPercentInValue() {
        String queryString = "a=b&c=d%e";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testPermittedUnwise() {
        String queryString = "a=|//^`";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString, true);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testPermittedBraces() {
        String queryString = "a=()&b={}&c=[]&d=<>";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString, true);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testPermittedOthers() {
        String queryString = "a=\\/:@|=?#\"`^<>~";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString, true);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testEncodedName() throws Exception {
        String queryString = "a%3b=b";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
    public void testEncodedValue() {
        String queryString = "a=b%3b";
        Map<String,String[]> paramStrMap = ParameterizedString.parseQueryString(queryString);
        Map httpUtilMap = HttpUtils_parseQueryString(queryString);

        assertEquals("Test for query string '"+queryString+"'", enlist(httpUtilMap), enlist(paramStrMap));
    }

    @Test
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

    @Test
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

    @Test
    public void testIllegalValue() {
        String queryString = "&";
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

    //Bug SSG-6907
    @Test
    public void testParseParameterString_semicolonIncluded() throws Exception {
        String query = "fred=1&freda=2;joe=6;josephine=8&george=8&georgina=9";
        Map<String,String[]> parametersMap = new HashMap<>();
        ParameterizedString.parseParameterString(parametersMap, query, false);
        assertEquals(6, parametersMap.size());
        assertArrayEquals(new String[]{"1"}, parametersMap.get("fred"));
        assertArrayEquals(new String[]{"2"}, parametersMap.get("freda"));
        assertArrayEquals(new String[]{"6"}, parametersMap.get("joe"));
        assertArrayEquals(new String[]{"8"}, parametersMap.get("josephine"));
        assertArrayEquals(new String[]{"8"}, parametersMap.get("george"));
        assertArrayEquals(new String[]{"9"}, parametersMap.get("georgina"));
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
