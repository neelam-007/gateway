package com.l7tech.external.assertions.radius;

import net.jradius.exception.RadiusException;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.RadiusAttribute;
import net.jradius.packet.attribute.value.DateValue;
import net.jradius.packet.attribute.value.IPAddrValue;
import net.jradius.packet.attribute.value.IntegerValue;
import net.jradius.packet.attribute.value.StringValue;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Copyright: Layer 7 Technologies, 2013
 * User: ymoiseyenko
 * Date: 11/27/13
 */
public class RadiusUtilsTest {

    @BeforeClass
    public static void setUp() throws Exception {
        AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");
    }

    @Test
    public void testNewAttributeWithIntegerValue() throws Exception {
        RadiusAttribute attr = RadiusUtils.newAttribute("Idle-Timeout", "5000");
        assertEquals(new IntegerValue(5000).getValue(),attr.getValue().getValueObject());
    }

    @Test(expected = RadiusException.class)
    public void testNewAttributeWithIntegerValue_nonNumeric() throws Exception {
        RadiusUtils.newAttribute("Idle-Timeout", "bla");
    }

    @Test
    public void testNewAttributeWithStringValue() throws Exception {
        RadiusAttribute attr = RadiusUtils.newAttribute("Framed-Filter-Id", "test-id");
        StringValue expectedValue =  new StringValue();
        expectedValue.setString("test-id");
        assertEquals(expectedValue.getValueObject(), attr.getValue().getValueObject());
    }

    @Test
    public void testNewAttributeWithIpAddrValue() throws Exception {
        RadiusAttribute attr = RadiusUtils.newAttribute("Framed-IP-Address", "10.0.0.1");
        IPAddrValue expectedValue =  new IPAddrValue("10.0.0.1");
        assertEquals(expectedValue.getValueObject(), attr.getValue().getValueObject());
    }

    @Test
    public void testNewAttributeWithDateValue() throws Exception {
        Calendar cl = GregorianCalendar.getInstance();
        cl.set(2013,10,28,9,10,11);
        Date currentDate = cl.getTime();
        cl.clear(Calendar.MILLISECOND);
        RadiusAttribute attr = RadiusUtils.newAttribute("Expiration", Integer.toString((int)(currentDate.getTime()/1000)));
        DateValue expectedValue =  new DateValue();
        expectedValue.setValue(currentDate.getTime()/1000);
        assertEquals(expectedValue.getValueObject(), attr.getValue().getValueObject());
    }



    @Test
    public void testIsAttributeValid() throws Exception {
        assertTrue(RadiusUtils.isAttributeValid("Idle-Timeout"));
    }

    @Test
    public void testParseIntValue() throws Exception {

    }
}
