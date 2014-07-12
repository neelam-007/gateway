package com.l7tech.external.assertions.odatavalidation.server;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;

import java.util.List;
import java.util.Map;

/**
* @author Jamie Williams - jamie.williams2@ca.com
*/
public class OdataPayloadInfo {
    private final ODataEntry odataEntry;
    private final List<String> link;
    private final Object propertyValue;
    private final Map<String, Object> propertyValues;
    private final boolean media;

    public OdataPayloadInfo(ODataEntry odataEntry, List<String> link,
                            Object propertyValue, Map<String, Object> propertyValues, boolean media) {
        this.odataEntry = odataEntry;
        this.link = link;
        this.propertyValue = propertyValue;
        this.propertyValues = propertyValues;
        this.media = media;
    }

    public ODataEntry getOdataEntry() {
        return odataEntry;
    }

    public List<String> getLinks() {
        return link;
    }

    public Object getValue() {
        return propertyValue;
    }

    public Map<String, Object> properties() {
        return propertyValues;
    }

    public boolean isMedia() {
        return media;
    }
}
