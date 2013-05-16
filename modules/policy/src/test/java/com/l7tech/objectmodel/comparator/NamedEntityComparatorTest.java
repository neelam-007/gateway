package com.l7tech.objectmodel.comparator;

import com.l7tech.objectmodel.NamedEntity;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class NamedEntityComparatorTest {
    private NamedEntityComparator comparator;
    private NamedEntityStub ne1;
    private NamedEntityStub ne2;

    @Before
    public void setup() {
        comparator = new NamedEntityComparator();
        ne1 = new NamedEntityStub();
        ne2 = new NamedEntityStub();
    }

    @Test
    public void bothNull() {
        assertEquals(0, comparator.compare(null, null));
    }

    @Test
    public void firstNull() {
        assertTrue(comparator.compare(null, ne1) < 0);
    }

    @Test
    public void secondNull() {
        assertTrue(comparator.compare(ne1, null) > 0);
    }

    @Test
    public void lexicalOrder() {
        ne1.setName("z");
        ne2.setName("a");
        assertTrue(comparator.compare(ne1, ne2) > 0);
        assertTrue(comparator.compare(ne2, ne1) < 0);
    }

    @Test
    public void lexicalOrderMixedCase() {
        ne1.setName("Z");
        ne2.setName("a");
        assertTrue(comparator.compare(ne1, ne2) > 0);
        assertTrue(comparator.compare(ne2, ne1) < 0);
    }

    @Test
    public void bothNullName() {
        ne1.setName(null);
        ne2.setName(null);
        assertEquals(0, comparator.compare(ne1, ne2));
    }

    @Test
    public void firstNullName() {
        ne1.setName(null);
        ne2.setName("test");
        assertTrue(comparator.compare(ne1, ne2) < 0);
    }

    @Test
    public void secondNullName() {
        ne1.setName("test");
        ne2.setName(null);
        assertTrue(comparator.compare(ne1, ne2) > 0);
    }

    private class NamedEntityStub implements NamedEntity {
        private String name;

        public void setName(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getId() {
            return null;
        }
    }

}
