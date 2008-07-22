package com.l7tech.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyDescriptor;
import java.util.Set;

import com.l7tech.util.BeanUtils;

/**
 * Test cases for BeanUtils.
 */
public class BeanUtilsTest extends TestCase {
    public BeanUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(BeanUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static class TestBean {
        private String str1;
        private int int1;
        private boolean bool1;
        private JTable table1;
        private Object notAProperty;

        public String getStr1() {
            return str1;
        }

        public void setStr1(String str1) {
            this.str1 = str1;
        }

        public int getInt1() {
            return int1;
        }

        public void setInt1(int int1) {
            this.int1 = int1;
        }

        public boolean isBool1() {
            return bool1;
        }

        public void setBool1(boolean bool1) {
            this.bool1 = bool1;
        }

        public JTable getTable1() {
            return table1;
        }

        public void setTable1(JTable table1) {
            this.table1 = table1;
        }

        public Object notAGetter() {
            if (notAProperty == null) notAProperty = new Object();
            return new Object();
        }

        public void notASetter(Object fakeArg) {
            if (notAProperty != null) notAProperty = fakeArg;
        }
    }

    public void testCopyProperties() throws Exception {
        TestBean b = new TestBean();
        b.setStr1("blah");
        b.setInt1(32);
        b.setBool1(true);
        JTable table = new JTable();
        table.setBackground(Color.PINK);
        b.setTable1(table);

        TestBean b2 = new TestBean();
        BeanUtils.copyProperties(b, b2);

        assertEquals(b.str1, b2.str1);
        assertEquals(b.int1, b2.int1);
        assertEquals(b.bool1, b2.bool1);
        assertEquals(b.table1, b2.table1);
        assertEquals(Color.PINK, b2.table1.getBackground());
        assertTrue("must be shallow copy", b.table1 == b2.table1);
    }

    public void testFiltering() throws Exception {
        Set<PropertyDescriptor> props = BeanUtils.getProperties(TestBean.class);
        assertEquals(4, props.size());

        Set<PropertyDescriptor> omitted = BeanUtils.omitProperties(props, "bool1", "table1");
        assertEquals(2, omitted.size());

        Set<PropertyDescriptor> allowed = BeanUtils.includeProperties(props, "int1", "str1");
        assertEquals("must reach same result whether omitting one set or including only its complement", omitted, allowed);

        TestBean b1 = new TestBean();
        b1.setStr1("testb1");
        b1.setInt1(1);
        b1.setTable1(new JTable());

        TestBean b2 = new TestBean();
        b2.setStr1("testb2");
        b2.setInt1(2);
        b2.setTable1(new JTable());

        BeanUtils.copyProperties(b1, b2, allowed);
        assertEquals("included property must be copied", "testb1", b2.str1);
        assertEquals("included property must be copied", 1, b2.int1);
        assertTrue("omitted property must not be copied", b2.table1 != b1.table1);
    }
}
