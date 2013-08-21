package com.l7tech.objectmodel;

import com.l7tech.objectmodel.imp.NamedEntityImp;
import org.junit.Test;

import javax.persistence.Column;

import static junit.framework.Assert.assertEquals;

/**
 * Test case for EntityUtil.
 */
public class EntityUtilTest {
    @Test
    public void testGetLengthWithDefault() {
        assertEquals(989, EntityUtil.getMaxFieldLength(AnnotationTestEntity.class, "fakePropertyThatIsntThere", 989));
    }

    @Test
    public void testGetLengthFromPublicField() {
        assertEquals(821, EntityUtil.getMaxFieldLength(AnnotationTestEntity.class, "publicField", -1));
    }

    @Test
    public void testGetLengthFromProtectedField() {
        assertEquals(28237, EntityUtil.getMaxFieldLength(AnnotationTestEntity.class, "protectedField", -1));
    }
    
    @Test
    public void testGetLengthFromPrivateField() {
        assertEquals(72743, EntityUtil.getMaxFieldLength(AnnotationTestEntity.class, "privateField", -1));
    }

    @Test
    public void testGetLengthFromGetter() {
        assertEquals(373, EntityUtil.getMaxFieldLength(AnnotationTestEntity.class, "otherfield", -1));
    }

    //Disabled as the field length of 128 is not the max length for all implementing classes
    @Test
    public void testGetLengthFromInheritedGetter() {
        assertEquals(128, EntityUtil.getMaxFieldLength(AnnotationTestEntity.class, "name", -1));
    }

    @Test
    public void testGetLengthFromInheritedPublicField() {
        assertEquals("Expected to fail and return default", -5, EntityUtil.getMaxFieldLength(AnnotationTestEntitySubclass.class, "publicField", -5));
    }

    @Test
    public void testGetLengthFromInheritedProtectedField() {
        assertEquals("Expected to fail and return default", -6, EntityUtil.getMaxFieldLength(AnnotationTestEntitySubclass.class, "protectedField", -6));
    }
    
    @Test
    public void testAttemptToGetLengthFromSuperclassPrivateField() {
        assertEquals("Must fail and return default", -7, EntityUtil.getMaxFieldLength(AnnotationTestEntitySubclass.class, "privateField", -7));
    }

    public static class AnnotationTestEntity extends NamedEntityImp {
        @SuppressWarnings({"UnusedDeclaration"})
        @Column(name="publicField", length=821)
        private String publicField;

        @Column(name="protectedField", length=28237)
        private String protectedField;

        @Column(name="privateField", nullable=false, length=72743)
        private String privateField = "";

        private String otherfield;

        @Column(name="otherfield", length=373)
        public String getOtherfield() {
            return otherfield;
        }

        public void setOtherfield(String otherfield) {
            this.otherfield = otherfield;
        }

        public String getProtectedField() {
            return protectedField;
        }

        public void setProtectedField(String protectedField) {
            this.protectedField = protectedField;
        }

        public String getPrivateField() {
            return privateField;
        }

        public void setPrivateField(String privateField) {
            this.privateField = privateField;
        }
    }

    public static class AnnotationTestEntitySubclass extends AnnotationTestEntity {
        private String myOwnThing;
    }
}
