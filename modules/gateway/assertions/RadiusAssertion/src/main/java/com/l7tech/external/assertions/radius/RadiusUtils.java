package com.l7tech.external.assertions.radius;

import net.jradius.exception.RadiusException;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.RadiusAttribute;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RadiusUtils {

    /**
     * Retrieve the radius attribute.
     *
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     * @return The RadiusAttribute
     * @throws RadiusException Throw RadiusException when the provided attribute name is not a valid Radius attribute.
     */
    public static RadiusAttribute getAttribute(String name, String value) throws RadiusException {
        if (name == null || value == null) {
            throw new IllegalArgumentException();
        }

        RadiusAttribute radiusAttribute = AttributeFactory.newAttribute(name);
        try {
            byte b = Byte.parseByte(value);
            radiusAttribute.setValue(new byte[]{b});
        } catch (NumberFormatException e) {
            radiusAttribute.setValue(value);
        }
        return radiusAttribute;
    }

}
