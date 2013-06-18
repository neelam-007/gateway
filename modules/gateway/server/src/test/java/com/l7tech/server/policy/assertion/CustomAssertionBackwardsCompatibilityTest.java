package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;
import com.l7tech.policy.assertion.ext.validator.CustomPolicyValidator;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.WspReader;
//import com.l7tech.policy.wsp.WspWriter;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.List;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

/**
 * Test CustomAssertions backwards compatibility test
 * @author  tveninov
 */
public class CustomAssertionBackwardsCompatibilityTest {

//////////////////////////////////////////////////////////////////////////////////////////////////////////////
// The policy XML corresponds to the class signature below
// Simulates policy containing an old legacy assertion being stored in the DB
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String LEGACY_CA_POLICY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:CustomAssertion>\n" +
            "            <L7p:base64SerializedValue>rO0ABXNyADFjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uSG9sZGVyZtcreFwddTICAANMAAhjYXRlZ29yeXQAKkxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0NhdGVnb3J5O0wAD2N1c3RvbUFzc2VydGlvbnQAMUxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0N1c3RvbUFzc2VydGlvbjtMAA9kZXNjcmlwdGlvblRleHR0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAlY29tLmw3dGVjaC5wb2xpY3kuYXNzZXJ0aW9uLkFzc2VydGlvbttfY5k8vaKxAgACWgAHZW5hYmxlZEwAEGFzc2VydGlvbkNvbW1lbnR0AC9MY29tL2w3dGVjaC9wb2xpY3kvYXNzZXJ0aW9uL0Fzc2VydGlvbiRDb21tZW50O3hwAHNyAC1jb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uQXNzZXJ0aW9uJENvbW1lbnTBF6Z3C8B2pgIAAUwACnByb3BlcnRpZXN0AA9MamF2YS91dGlsL01hcDt4cHNyABFqYXZhLnV0aWwuSGFzaE1hcAUH2sHDFmDRAwACRgAKbG9hZEZhY3RvckkACXRocmVzaG9sZHhwP0AAAAAAAAx3CAAAABAAAAACdAANUklHSFQuQ09NTUVOVHQADXJpZ2h0IGNvbW1lbnR0AAxMRUZULkNPTU1FTlR0AAxsZWZ0IGNvbW1lbnR4c3IAKGNvbS5sN3RlY2gucG9saWN5LmFzc2VydGlvbi5leHQuQ2F0ZWdvcnlasJxloUT+NQIAAkkABW15S2V5TAAGbXlOYW1lcQB+AAN4cAAAAAB0AA1BY2Nlc3NDb250cm9sc3IAdWNvbS5sN3RlY2guc2VydmVyLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uQmFja3dhcmRzQ29tcGF0aWJpbGl0eVRlc3QkVGVzdEN1c3RvbUFzc2VydGlvbkJhY2t3YXJkQ29tcGF0aWJpbGl0eZJxefMOXb+/AwAESQAGZmllbGQxRAAGZmllbGQzTAAGZmllbGQycQB+AANMAAZmaWVsZDR0ABBMamF2YS9sYW5nL0xvbmc7eHIAWmNvbS5sN3RlY2guc2VydmVyLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uQmFja3dhcmRzQ29tcGF0aWJpbGl0eVRlc3QkVGVzdEJhc2VDbGFzc5tnHL6+qs2HAgACRAAKYmFzZUZpZWxkMkwACmJhc2VGaWVsZDFxAH4AA3hwQFjHLkjopx50ABBuZXcgYmFzZSBmaWVsZCAxAAAAZUBZ1VZs9B8hdAAKbmV3IGZpZWxkMnNyAA5qYXZhLmxhbmcuTG9uZzuL5JDMjyPfAgABSgAFdmFsdWV4cgAQamF2YS5sYW5nLk51bWJlcoaslR0LlOCLAgAAeHAAAAAAAAAAaHQAGXRyYW5zaWVudCBmaWVsZDEgbW9kaWZpZWR0ABZzdGF0aWMgZmllbGQxIG1vZGlmaWVkeHQAHU9yaWdpbmFsIEN1c3RvbUFzc2VydGlvbiBEZXNj</L7p:base64SerializedValue>\n" +
            "        </L7p:CustomAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
// the original class signature for the policy above
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    // base class must be Serializable in order to be saved
//    public static class TestBaseClass implements Serializable {
//        private static final long serialVersionUID = -7248793469661295225L;
//
//        protected String baseField1;
//        protected double baseField2;
//
//        public TestBaseClass() {
//            this("base field 1", 1234.5678D);
//        }
//
//        protected TestBaseClass(final String baseField1, double baseField2) {
//            setBaseField1(baseField1);
//            setBaseField2(baseField2);
//        }
//
//        public String getBaseField1() { return baseField1; }
//        public void setBaseField1(String baseField1) { this.baseField1 = baseField1; }
//        public double getBaseField2() { return baseField2; }
//        public void setBaseField2(double baseField2) { this.baseField2 = baseField2; }
//    }
//    //////////////////////////////////////////////////////////////////////////////////////////////////////////////
//    public static class TestCustomAssertionBackwardCompatibility extends TestBaseClass implements CustomAssertion {
//        private static final long serialVersionUID = -7894394586978730049L;
//
//        private int field1;
//        private String field2;
//        private double field3;
//        private Long field4;
//
//        public static String staticField1;
//        public transient String transientField1;
//
//        public TestCustomAssertionBackwardCompatibility() {
//            this(1, "field2", 3.3331D, 4L, "base field 1", 1234.5678D);
//        }
//
//        public TestCustomAssertionBackwardCompatibility(int field1, final String field2, double field3, final Long field4) {
//            this(field1, field2, field3, field4, "base field 1", 1234.5678D);
//        }
//
//        public TestCustomAssertionBackwardCompatibility(int field1, final String field2, double field3, final Long field4, final String baseField1, double baseField2) {
//            super(baseField1, baseField2);
//            this.field1 = field1;
//            this.field2 = field2;
//            this.field3 = field3;
//            this.field4 = field4;
//
//            staticField1 = "static field";
//            transientField1 = "transient field";
//        }
//
//        private void writeObject(final java.io.ObjectOutputStream stream) throws IOException {
//            stream.defaultWriteObject();
//            // write optional fields (omitted by defaultWriteObject, since they are static and transient)
//            transientField1 = "transient field1 modified";
//            stream.writeObject(transientField1);
//            staticField1 = "static field1 modified";
//            stream.writeObject(staticField1);
//        }
//
//        private void readObject(final java.io.ObjectInputStream stream) throws IOException, ClassNotFoundException {
//            stream.defaultReadObject();
//            // read optional fields (omitted by defaultWriteObject, since they are static and transient)
//            transientField1 = (String)stream.readObject();
//            staticField1 = (String)stream.readObject();
//        }
//
//        @Override
//        public String getName() {
//            return "Legacy Backward Compatible CustomAssertion";
//        }
//    }
//////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Before
    public void setUp() throws Exception {
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
// This is how the assertion was saved into LEGACY_CA_POLICY
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
//        final AllAssertion allAssertion = new AllAssertion();
//        final CustomAssertionHolder ca = new CustomAssertionHolder();
//        Assertion.Comment comment = new Assertion.Comment();
//        comment.setComment("left comment", Assertion.Comment.LEFT_COMMENT);
//        comment.setComment("right comment", Assertion.Comment.RIGHT_COMMENT);
//        ca.setAssertionComment(comment);
//        ca.setEnabled(false);
//        ca.setDescriptionText("Original CustomAssertion Desc");
//        ca.setCategory(Category.ACCESS_CONTROL);
//        ca.setCustomAssertion(new TestCustomAssertionBackwardCompatibility(
//                101, "new field2", 103.3334D, 104L,
//                "new base field 1", 99.1122D));
//        allAssertion.addChild(ca);
//        String outString = WspWriter.getPolicyXml(allAssertion); // outString goes to LEGACY_CA_POLICY
//////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    public static class NewTestBaseClass implements Serializable {
        private static final long serialVersionUID = -3126416173451789567L;

        private String newBaseField1 = "base field 1";
        private String newBaseField2 = "base field 2";

        public String getNewBaseField1() { return newBaseField1; }
        public void setNewBaseField1(String newBaseField1) { this.newBaseField1 = newBaseField1; }
        public String getNewBaseField2() { return newBaseField2; }
        public void setNewBaseField2(String newBaseField2) { this.newBaseField2 = newBaseField2; }
    }

    /**
     * Future version of the custom assertion stored under {@link #LEGACY_CA_POLICY}
     *
     * As per Java Serialisation guidelines (http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html):
     * 1. serialVersionUID must remain the same
     * 2. all original fields must remain i.e. we cannot delete original fields
     * 3. add new Field/s (field5, sourceTarget)
     * 4. add new classes in the hierarchy (NewTestBaseClass)
     * 5. remove classes from the hierarchy (TestBaseClass)
     * 6. add readObject as long as defaultReadObject is called
     * 7. remove writeObject
     * 8. change access to a field (field2)
     * 9. Change field from static to non-static or transient to non-transient (staticField1 and transientField1, respectively)
     *
     */
    public static class TestCustomAssertionBackwardCompatibility
            // 4. add new classes in the hierarchy (NewTestBaseClass)
            extends NewTestBaseClass
            // 5. remove classes from the hierarchy (TestBaseClass)
            //extends TestBaseClass
            implements CustomAssertion , SetsVariables, UsesVariables, CustomPolicyValidator, CustomMessageTargetable
    {
        // 1. serialVersionUID must remain the same
        private static final long serialVersionUID = -7894394586978730049L;

        private static final String OTHER_VARIABLE = "otherVariable";

        //////////////////////////////////////////////////////////////////////////////////////
        // 2. all original fields must remain i.e. we cannot delete original fields
        //////////////////////////////////////////////////////////////////////////////////////
        private int field1;
        // 8. change access to a field (field2)
        public String field2;
        private double field3;
        private Long field4;
        //////////////////////////////////////////////////////////////////////////////////////

        // 3. add new Field/s (field5, sourceTarget)
        private Map field5 = new HashMap();
        private CustomMessageTargetableSupport sourceTarget = new CustomMessageTargetableSupport(CustomMessageTargetableSupport.TARGET_REQUEST, false);

        // 9. Change field from static to non-static or transient to non-transient (staticField1 and transientField1, respectively)
        public String staticField1;
        public String transientField1;

        //  4. add readObject as long as defaultReadObject is called
        private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
            in.defaultReadObject();

            if (getField5() == null) {
                //noinspection serial
                setField5(new HashMap<String, Integer>() {{
                    put("one", 1);
                    put("two", 2);
                    put("three", 3);
                }});
            }
            if (staticField1 == null) {
                staticField1 = "new non-static field";
            }
            if (transientField1 == null) {
                //transientField1 = "new non-transient field";
                transientField1 = (String)in.readObject();
            }
            if (sourceTarget == null) {
                sourceTarget = new CustomMessageTargetableSupport("otherVariable");
                sourceTarget.setTargetModifiedByGateway(true);
            }

            if (getNewBaseField1() == null) {
                setNewBaseField1("new base field1");
            }
            if (getNewBaseField2() == null) {
                setNewBaseField2("new base field2");
            }
        }
        
        // 7. remove writeObject
//        private void writeObject(final java.io.ObjectOutputStream stream) throws IOException {
//            stream.defaultWriteObject();
//            // write optional fields (omitted by defaultWriteObject, since they are static and transient)
//            transientField1 = "transient field1 modified";
//            stream.writeObject(transientField1);
//            staticField1 = "static field1 modified";
//            stream.writeObject(staticField1);
//        }

        public TestCustomAssertionBackwardCompatibility() {
            field1 = 1;
            field2 = "field2";
            field3 = 3.3331d;
            field4 = 4l;
        }

        @Override
        public String getName() {
            return "Future version of CustomAssertion";
        }

        private int getField1() { return field1; }
        private String getField2() { return field2; }
        private double getField3() { return field3; }
        private Long getField4() { return field4; }

        public Map getField5() { return field5; }
        public void setField5(final Map field5) { this.field5 = field5; }

        @Override
        public String getTargetMessageVariable() {
            return sourceTarget.getTargetMessageVariable();
        }

        @Override
        public void setTargetMessageVariable(String otherMessageVariable) {
            sourceTarget.setTargetMessageVariable(otherMessageVariable);
        }

        @Override
        public String getTargetName() {
            return sourceTarget.getTargetName();
        }

        @Override
        public boolean isTargetModifiedByGateway() {
            return sourceTarget.isTargetModifiedByGateway();
        }

        @Override
        public List<String> getWarningMessages(Map<String, Object> consoleContext) {
            //noinspection serial
            return new LinkedList<String>() {{
                add("Warning 1");
                add("Warning 2");
            }};
        }

        @Override
        public List<String> getErrorMessages(Map<String, Object> consoleContext) {
            //noinspection serial
            return new LinkedList<String>() {{
                add("Error 1");
                add("Error 2");
            }};
        }

        @Override
        public VariableMetadata[] getVariablesSet() {
            return new VariableMetadata[]{
                    new VariableMetadata("backTest.setParam1", true, false, null, false),
                    new VariableMetadata("backTest.setParam2", true, false, null, false)
            };
        }

        @Override
        public String[] getVariablesUsed() {
            return new String[]{"backTest.useParam1", "backTest.useParam2"};
        }
    }

    /**
     * Test if CustomAssertionHolder backwards compatibility is preserved.
     * <p/>
     * Since CustomAssertionHolder is serialized along with the CustomAssertion
     * in order to provide backwards compatibility as per Java Serialisation guidelines
     * (http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html): section 5.6.1: Deleting fields etc.
     * we need to make sure the following Fields are persistent (not deleted/renamed) in all new versions of CustomAssertionHolder:
     * <p><b>customAssertion, category, descriptionText</b> (from CustomAssertionHolder)</p>
     * <p><b>enabled</b> and <b>assertionComment</b> (from CustomAssertionHolder super class i.e. Assertion)</p>
     */
    @Test
    public void testCustomAssertionHolder() throws Exception {
        String filedName = "";
        Class assertionClass = CustomAssertionHolder.class;
        try {
            filedName = "customAssertion";
            assertionClass.getDeclaredField(filedName);

            filedName = "category";
            assertionClass.getDeclaredField(filedName);

            filedName = "descriptionText";
            assertionClass.getDeclaredField(filedName);

            Class superClass = assertionClass.getSuperclass();
            while (null != superClass) {
                if (superClass.equals(Assertion.class)) {
                    assertionClass = superClass;
                    break;
                }
                superClass = superClass.getSuperclass();
            }
            assertNotNull("Failed to find CustomAssertionHolder super class (Assertion)", assertionClass);

            filedName = "enabled";
            assertionClass.getDeclaredField(filedName);

            filedName = "assertionComment";
            assertionClass.getDeclaredField(filedName);
        } catch (final NoSuchFieldException e) {
            fail("Field [" + filedName + "] from class [" + assertionClass.getSimpleName() + "] is missing and its mandatory for backwards compatibility");
        }
    }

    /**
     * Test if CustomAssertion backwards compatibility is preserved.
     * <p/>
     * As per Java Serialisation guidelines (http://docs.oracle.com/javase/6/docs/platform/serialization/spec/version.html)
     */
    @Test
    public void testCustomAssertion() throws Exception {
        Assertion readPolicy = WspReader.getDefault().parseStrictly(LEGACY_CA_POLICY, WspReader.INCLUDE_DISABLED);
        assertTrue("is instanceof AllAssertion", readPolicy instanceof AllAssertion);
        AllAssertion allAssertion = (AllAssertion)readPolicy;

        assertTrue("have one assertion", allAssertion.getChildren().size() == 1);
        for (Iterator i = allAssertion.children(); i.hasNext(); )
        {
            Object it = i.next();
            assertTrue("the assertion is instanceof CustomAssertionHolder", it instanceof CustomAssertionHolder);
            CustomAssertionHolder customAssertionHolder = (CustomAssertionHolder)it;

            CustomAssertion ca = customAssertionHolder.getCustomAssertion();
            assertTrue("is instanceof TestCustomAssertionBackwardCompatibility", ca instanceof TestCustomAssertionBackwardCompatibility);
            TestCustomAssertionBackwardCompatibility testCustomAssertion = (TestCustomAssertionBackwardCompatibility)ca;

            //noinspection ConstantConditions
            assertTrue("Removing Serializable from CustomAssertionHolder is an incompatible change", customAssertionHolder instanceof Serializable);

            assertFalse("Assertion is disabled", customAssertionHolder.isEnabled());
            assertEquals("Category is ACCESS_CONTROL", customAssertionHolder.getCategory(), Category.ACCESS_CONTROL);
            assertTrue("Have both Comments property", customAssertionHolder.getAssertionComment() == null || customAssertionHolder.getAssertionComment() != null);
            assertEquals("Description is properly de-serialized", customAssertionHolder.getDescriptionText(), "Original CustomAssertion Desc");
            assertEquals("Message Target is properly de-serialized", customAssertionHolder.getTargetName(), "${" + TestCustomAssertionBackwardCompatibility.OTHER_VARIABLE + "}");
            assertTrue("Message Target is modified by gateway", customAssertionHolder.isTargetModifiedByGateway());

            assertEquals("field1 is properly de-serialized", testCustomAssertion.getField1(), 101);
            assertEquals("field2 is properly de-serialized", testCustomAssertion.getField2(), "new field2");
            assertEquals("field3 is properly de-serialized", testCustomAssertion.getField3(), 103.3334d);
            assertEquals("field4 is properly de-serialized", testCustomAssertion.getField4(), (Long)104l);

            assertEquals("newBaseField1 is properly de-serialized", testCustomAssertion.getNewBaseField1(), "new base field1");
            assertEquals("newBaseField2 is properly de-serialized", testCustomAssertion.getNewBaseField2(), "new base field2");
        }
    }
}
