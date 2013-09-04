package com.l7tech.server.entity;

import com.l7tech.util.ClassFilter;
import com.l7tech.util.SafeXMLDecoder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

/**
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class GenericEntityManagerEncodingTest {
    @Mock
    private ClassFilter openFilter;

    @Before
    public void initMocks() {
        when(openFilter.permitClass(Matchers.anyString())).thenReturn(true);
        when(openFilter.permitMethod(Matchers.<Method>anyObject())).thenReturn(true);
        when(openFilter.permitConstructor(Matchers.<Constructor>anyObject())).thenReturn(true);
    }

    @Test
    public void testDecodeGenericEntity() {
        String xml =
            "<java version=\"1.7.0_17\" class=\"java.beans.XMLDecoder\">\n" +
                " <object class=\"com.l7tech.server.entity.TestDemoGenericEntity\">\n" +
                "  <void property=\"name\">\n" +
                "   <string>asdfsdf</string>\n" +
                "  </void>\n" +
                "  <void property=\"valueXml\">\n" +
                "   <string></string>\n" +
                "  </void>\n" +
                " </object>\n" +
                "</java>";
        SafeXMLDecoder decoder = new SafeXMLDecoder(openFilter, new ByteArrayInputStream(xml.getBytes()), null, null, TestDemoGenericEntity.class.getClassLoader());
        Object obj = decoder.readObject();
        decoder.close();
        assertNotNull(obj);
        assertEquals(TestDemoGenericEntity.class, obj.getClass());
    }
}
