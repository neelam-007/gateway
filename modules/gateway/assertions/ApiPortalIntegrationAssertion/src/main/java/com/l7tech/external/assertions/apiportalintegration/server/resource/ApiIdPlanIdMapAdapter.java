package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom map adaptor to handle marshalling/unmarshalling of api id - plan id pairs.
 */
public class ApiIdPlanIdMapAdapter extends XmlAdapter<ApiIdPlanIdPairs, Map<String, String>> {

    @Override
    public Map<String, String> unmarshal(final ApiIdPlanIdPairs serviceIdPlanPairs) throws Exception {
        final Map<String, String> pairMap = new HashMap<String, String>();
        if (serviceIdPlanPairs != null && serviceIdPlanPairs.getPairs() != null) {
            for (final ApiIdPlanIdPair pair : serviceIdPlanPairs.getPairs()) {
                pairMap.put(pair.getApiId(), pair.getPlanId());
            }
        }
        return pairMap;
    }

    @Override
    public ApiIdPlanIdPairs marshal(final Map<String, String> map) throws Exception {
        final List<ApiIdPlanIdPair> pairs = new ArrayList<ApiIdPlanIdPair>();
        if (map != null) {
            for (final Map.Entry<String, String> entry : map.entrySet()) {
                pairs.add(new ApiIdPlanIdPair(entry.getKey(), entry.getValue()));
            }
        }
        return new ApiIdPlanIdPairs(pairs);
    }
}
