package com.l7tech.external.assertions.radius;

import net.jradius.exception.RadiusException;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.RadiusAttribute;

import java.text.ParseException;
import java.util.LinkedHashMap;

public class RadiusUtils {
    /**
     * Retrieve the radius attribute.
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     * @return The RadiusAttribute
     * @throws RadiusException Throw RadiusException when the provided attribute name is not a valid Radius attribute.
     */
    public static RadiusAttribute newAttribute(String name, String value) throws RadiusException {
        if (name == null || value == null) {
            throw new RadiusException("Attribute name or value is null");
        }

        RadiusAttribute radiusAttribute = AttributeFactory.newAttribute(name);

        try{
            radiusAttribute.setValue(value);
        } catch(NumberFormatException nfe) {
            throw new RadiusException("Invallid numeric value", nfe);
        }

        return radiusAttribute;
    }

    public static boolean isAttributeValid(String name) {
        if(name == null || name.trim().length() == 0) return false;

        LinkedHashMap<String, Class<?>> radiusAttributeNameMap = AttributeFactory.getAttributeNameMap();
        return radiusAttributeNameMap.containsKey(name);
    }

    /**
     * wrapper for Integer.parseInt
     * @param s
     * @return
     * @throws ParseException
     */
    public static int parseIntValue(String s) throws ParseException {
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException ex){
            throw new ParseException("Unable to parse value", 0);
        }
    }


}
