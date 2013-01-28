package com.l7tech.objectmodel;

import org.junit.Test;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;
import java.util.HashMap;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

import static org.junit.Assert.assertNull;

/**
 * @author jbufu
 */
public class JaxbMapTypeTest {

    @XmlRootElement
    public static class A {

        private Map<String, String> map = new HashMap<String,String>(){{
            put("oneKey", "oneValue");
            put("oneOtherKey", "someOtherValue");
        }};

        @XmlJavaTypeAdapter(JaxbMapType.JaxbMapTypeAdapter.class)
        public Map<String, String> getMap() {
            return map;
        }

        public void setMap(Map<String, String> map) {
            this.map = map;
        }
    }

    @Test
    public void testJaxbMap() throws Exception {

        A a = new A();

        OutputStream out = new ByteArrayOutputStream();
        JAXB.marshal(a, out);

        System.out.println(out.toString());

        JaxbMapType m = JAXB.unmarshal(new ByteArrayInputStream(out.toString().getBytes()), JaxbMapType.class);

        System.out.println(m.toMap().entrySet());
    }

    @Test
    public void adapterHandlesNull() throws Exception {
        final JaxbMapType.JaxbMapTypeAdapter adapter = new JaxbMapType.JaxbMapTypeAdapter();
        assertNull(adapter.marshal(null));
        assertNull(adapter.unmarshal(null));
    }
}
