package com.l7tech.gateway.common.audit;

import org.w3c.dom.Element;

import javax.xml.bind.MarshalException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * unmarshalling of an audit record's additional properties from a DOM element.
 * Properties includes:
 *          None.*/
public class AuditRecordPropertiesDomUnmarshaller {
    private static final String NS = "http://l7tech.com/audit/rec";

    /**
     * Add the specified AuditRecord as a DOM element as a child of the specified parent element.
     *
     * @param element  the audit record properties element to unmarshal
     * @return a map of the properties
     */
    public Map<String,Object> unmarshal(Element element) throws MarshalException {
        Map<String,Object> result = new HashMap<String,Object>();
        result.putAll(getAuditRecordFields(element));
        result.putAll(addAdminRecordFields(element));
        result.putAll(addMessageRecordFields(element));
        result.putAll(addSystemRecordFields(element));
        return result;
    }


    private Map<String,Object> getAuditRecordFields(Element e) {
        return Collections.emptyMap();
    }


    private Map<String,Object> addAdminRecordFields(Element e) {
        return Collections.emptyMap();
    }

    private Map<String,Object> addMessageRecordFields(Element e) {
        return Collections.emptyMap();
    }

    private Map<String,Object> addSystemRecordFields(Element e) {
        return Collections.emptyMap();
    }
}
