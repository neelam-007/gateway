package com.l7tech.objectmodel;

import junit.framework.TestCase;

import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Map;
import java.util.HashMap;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

/**
 * @author jbufu
 */
public class JaxbMapTypeTest extends TestCase {

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

    public void testJaxbMap() throws Exception {

        A a = new A();

        OutputStream out = new ByteArrayOutputStream();
        JAXB.marshal(a, out);

        System.out.println(out.toString());

        JaxbMapType m = JAXB.unmarshal(new ByteArrayInputStream(out.toString().getBytes()), JaxbMapType.class);

        System.out.println(m.toMap().entrySet());
    }
}
