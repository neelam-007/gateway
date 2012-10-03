package com.l7tech.external.assertions.apiportalintegration.server.resource;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class ApiIdPlanIdMapAdapterTest {
    private ApiIdPlanIdMapAdapter adapter;
    private List<ApiIdPlanIdPair> pairs;
    private Map<String, String> stringMap;

    @Before
    public void setup() {
        adapter = new ApiIdPlanIdMapAdapter();
        pairs = new ArrayList<ApiIdPlanIdPair>();
        stringMap = new HashMap<String, String>();
    }

    @Test
    public void unmarshal() throws Exception {
        pairs.add(new ApiIdPlanIdPair("s1", "p1"));
        pairs.add(new ApiIdPlanIdPair("s2", "p2"));

        final Map<String, String> result = adapter.unmarshal(new ApiIdPlanIdPairs(pairs));

        assertEquals(2, result.size());
        assertEquals("p1", result.get("s1"));
        assertEquals("p2", result.get("s2"));
    }

    @Test
    public void unmarshalEmpty() throws Exception {
        assertTrue(adapter.unmarshal(new ApiIdPlanIdPairs(Collections.<ApiIdPlanIdPair>emptyList())).isEmpty());
    }

    @Test
    public void unmarshalNull() throws Exception {
        assertTrue(adapter.unmarshal(null).isEmpty());
    }

    @Test
    public void unmarshalNullPairs() throws Exception {
        assertTrue(adapter.unmarshal(new ApiIdPlanIdPairs(null)).isEmpty());
    }

    @Test
    public void marshal() throws Exception {
        stringMap.put("s1", "p1");
        stringMap.put("s2", "p2");

        final ApiIdPlanIdPairs result = adapter.marshal(stringMap);

        assertEquals(2, result.getPairs().size());
        assertTrue(result.getPairs().contains(new ApiIdPlanIdPair("s1", "p1")));
        assertTrue(result.getPairs().contains(new ApiIdPlanIdPair("s2", "p2")));
    }

    @Test
    public void marshalEmpty() throws Exception {
        final ApiIdPlanIdPairs result = adapter.marshal(Collections.<String, String>emptyMap());

        assertTrue(result.getPairs().isEmpty());
    }

    @Test
    public void marshalNull() throws Exception {
        final ApiIdPlanIdPairs result = adapter.marshal(null);

        assertTrue(result.getPairs().isEmpty());
    }
}
