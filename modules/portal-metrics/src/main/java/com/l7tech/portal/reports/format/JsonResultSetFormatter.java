package com.l7tech.portal.reports.format;

import com.l7tech.portal.reports.parameter.ApiQuotaUsageReportParameters;
import com.l7tech.portal.reports.parameter.DefaultReportParameters;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

/**
 * JSON specific ResultSet formatter.
 */
public class JsonResultSetFormatter extends AbstractResultSetFormatter {
    private static final Logger LOGGER = Logger.getLogger(JsonResultSetFormatter.class);
    private static final String EMPTY_JSON_OBJECT = "{}";
    private static final String EMPTY_JSON_ARRAY = "[]";
    /**
     * Size of indent used when returning formatted JSON. Set to null if no indenting is required.
     */
    private Integer indentSize;

    public Integer getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(final Integer indentSize) {
        this.indentSize = indentSize;
    }

    /**
     * Not ideal to use 'null' but otherwise json library used will not include the group.
     */
    @Override
    String getNullValueForGroupingColumn() {
        return "null";
    }

    @Override
    String mapToString(final Map<String, List<Map<String, Object>>> resultSetMap, final ResultSetFormatOptions options) {
        String json = EMPTY_JSON_OBJECT;
        final JSONObject jsonObject = new JSONObject(resultSetMap);
        try {
            if (indentSize != null) {
                json = jsonObject.toString(indentSize);
            } else {
                json = jsonObject.toString();
            }
        } catch (final JSONException e) {
            LOGGER.error("Error converting resultSetMap to string: " + e.getMessage(), e);
        }
        return json;
    }

    @Override
    String listToString(final List<Map<String, Object>> resultSetList, final ResultSetFormatOptions options) {
        String json = EMPTY_JSON_ARRAY;
        final JSONArray jsonArray = new JSONArray(resultSetList);
        try {
            if (indentSize != null) {
                json = jsonArray.toString(indentSize);
            } else {
                json = jsonArray.toString();
            }
        } catch (final JSONException e) {
            LOGGER.error("Error converting resultSetList to string: " + e.getMessage(), e);
        }
        return json;
    }

    public String formatQuotaUsageJSON(Map<DefaultReportParameters.QuotaRange, String> data, ApiQuotaUsageReportParameters params) throws JSONException{
        String apiKey = params.getApiKey();
        List<Map<String, String>> records_collection = new ArrayList<Map<String, String>>();
        JSONObject ret = new JSONObject();
        int o=0;
        for(DefaultReportParameters.QuotaRange quota : data.keySet()){
            List<String> original_uuids = params.getApiRanges().get(quota);
            String jsonStr = data.get(quota);
            JSONObject ob = new JSONObject(jsonStr);
            List<HashMap<String,String>> empty_uuid_list = new ArrayList<HashMap<String,String>>();

            for(String uuid : original_uuids){
                if (!jsonStr.contains(uuid)){
                    HashMap<String,String> one_record = new HashMap<String, String>();
                    one_record.put("uuid",uuid);
                    one_record.put("hits", "0");
                    one_record.put("range", Integer.toString(quota.ordinal()+1));
                    empty_uuid_list.add(one_record);
                }
            }
            Iterator<String> apiKeyIterator = ob.keys();
            if(apiKeyIterator.hasNext()){
                JSONArray values = (JSONArray)ob.get(apiKey);
                for(int a=0;a<values.length();a++){
                    HashMap<String,String> one_record = new HashMap<String, String>();
                    String hits;
                    switch(quota){
                        case SECOND:
                            hits = ((JSONObject)values.get(a)).get("per_sec_avg").toString();
                            break;
                        case MINUTE:
                            hits = ((JSONObject)values.get(a)).get("per_min_avg").toString();
                            break;
                        default:
                            hits = ((JSONObject)values.get(a)).get("hits").toString();
                    }
                    String uuid = ((JSONObject)values.get(a)).get("uuid").toString();
                    one_record.put("uuid",uuid);
                    one_record.put("hits", hits);
                    one_record.put("range", Integer.toString(quota.ordinal()+1));
                    records_collection.add(one_record);
                }

            }
            for(HashMap<String,String> empty : empty_uuid_list){
                records_collection.add(empty);
            }
        }
        ret.put(apiKey,records_collection);
        String json = EMPTY_JSON_OBJECT;
        if(indentSize != null){
            json = ret.toString(indentSize);
        }else{
            json = ret.toString();
        }
        return json;

    }

}
