package com.l7tech.external.assertions.radius;

import com.l7tech.util.HexUtils;
import net.jradius.exception.RadiusException;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.RadiusAttribute;
import net.jradius.packet.attribute.value.AttributeValue;
import net.jradius.packet.attribute.value.DateValue;
import net.jradius.packet.attribute.value.IntegerValue;
import net.jradius.packet.attribute.value.StringValue;

import java.text.ParseException;
import java.util.Date;
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

    /**
     * Retrieve the radius attribute.
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     * @return The RadiusAttribute
     * @throws net.jradius.exception.RadiusException Throw RadiusException when the provided attribute name is not a valid Radius attribute.
     */
    public static RadiusAttribute newAttribute(String name, byte[] value) throws RadiusException {
        if (name == null || value == null) {
            throw new RadiusException("Attribute name or value is null");
        }

        RadiusAttribute radiusAttribute = AttributeFactory.newAttribute(name);

        try{
            radiusAttribute.setValue(value);
        } catch(NumberFormatException nfe) {
            throw new RadiusException("Invalid numeric value", nfe);
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
     * @param s Input string
     * @return Integer value represented by the string
     * @throws ParseException
     */
    public static int parseIntValue(String s) throws ParseException {
        try {
            return Integer.parseInt(s);
        } catch(NumberFormatException ex){
            throw new ParseException("Unable to parse value", 0);
        }
    }

    /**
     *  Helper method to pull out the attribute value based on its type. If it is not
     *  a date or Integer or String, is is base64 encoded
     * @param attribute Radius attribute
     * @return  value of the attribute - Object
     */
    public static Object extractAttributeValue(RadiusAttribute attribute) {
        Object value;
        AttributeValue valueObject = attribute.getValue();
        if (valueObject instanceof DateValue) {
            value = new Date(((DateValue) valueObject).getValue());
        } else if (valueObject instanceof IntegerValue) {
            value = ((IntegerValue) valueObject).getValue().intValue();
        } else if (valueObject instanceof StringValue) {
            value = new String(valueObject.getBytes());
        } else {
            value = HexUtils.encodeBase64(valueObject.getBytes(), true);
        }
        return value;
    }


}
