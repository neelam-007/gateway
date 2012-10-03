package com.l7tech.external.assertions.apiportalintegration.server.resource;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper class for a collection of api id - plan pairs required for xml marshalling/unmarshalling.
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ApiIdPlanIdPairs {
    @XmlElement(name = "Api", namespace = JAXBResourceMarshaller.NAMESPACE)
    private List<ApiIdPlanIdPair> pairs = new ArrayList<ApiIdPlanIdPair>();

    public ApiIdPlanIdPairs(final List<ApiIdPlanIdPair> pairs) {
        this.pairs = pairs;
    }

    public ApiIdPlanIdPairs() {

    }

    public List<ApiIdPlanIdPair> getPairs() {
        return pairs;
    }

    public void setPairs(final List<ApiIdPlanIdPair> pairs) {
        this.pairs = pairs;
    }
}
