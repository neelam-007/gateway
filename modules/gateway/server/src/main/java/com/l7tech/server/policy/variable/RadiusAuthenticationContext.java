package com.l7tech.server.policy.variable;

import com.l7tech.util.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Copyright CA Technologies - 2013
 * @author: yuri
 */
public class RadiusAuthenticationContext {

    private LinkedList<Pair<String, Object>> radiusAttributes = new LinkedList<>();

    public LinkedList<Pair<String, Object>> getRadiusAttributes() {
        return radiusAttributes;
    }

    public void setRadiusAttributes(LinkedList<Pair<String, Object>> radiusAttributes) {
        this.radiusAttributes = radiusAttributes;
    }

    public void addRadiusAttribute(String name, Object value) {
        radiusAttributes.add(new Pair<String, Object>(name, value));
    }

    public Object[] getAttributeValue(String name) {
        List<Object> values = new ArrayList();
        if(name != null){
            for(Pair<String, Object> attribute: radiusAttributes) {
                if(attribute.left.equals(name)){
                    values.add(attribute.right);
                }
            }
        }
        return values.toArray();
    }

}
