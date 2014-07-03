package com.l7tech.external.assertions.odatavalidation.server;

import org.apache.olingo.odata2.api.ep.entry.ODataEntry;

/**
* @author Jamie Williams - jamie.williams2@ca.com
*/
public class OdataPayloadInfo {
    private final ODataEntry odataEntry;

    public OdataPayloadInfo(ODataEntry odataEntry) {
        this.odataEntry = odataEntry;
    }

    public boolean containsOpenTypeEntity() {
        return false;
    }
}
