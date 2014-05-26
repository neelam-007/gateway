package com.l7tech.external.assertions.jsondocumentstructure.server;

import com.l7tech.json.InvalidJsonException;
import org.junit.Before;
import org.junit.Test;

import static com.l7tech.external.assertions.jsondocumentstructure.server.JsonDocumentStructureTestHelper.*;
import static com.l7tech.external.assertions.jsondocumentstructure.server.JsonDocumentStructureValidationException.ConstraintViolation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class JsonDocumentStructureValidatorTest {

    private JsonDocumentStructureValidator validator;

    @Before
    public void setUp() {
        validator = new JsonDocumentStructureValidator();
    }

    @Test
    public void testValidate_GivenSingleTypedValueDocumentAndNoConstraints_ValidationSucceeds() throws Exception {
        validator.validate(getInputStream(SINGLE_TYPED_VALUE_DOCUMENT));
    }

    @Test
    public void testValidate_GivenSingleObjectDocumentAndNoConstraints_ValidationSucceeds() throws Exception {
        validator.validate(getInputStream(SINGLE_OBJECT_DOCUMENT));
    }

    /**
     * An empty message is not a valid JSON document - the validate method should throw an InvalidJsonException.
     */
    @Test(expected = InvalidJsonException.class)
    public void testValidate_GivenEmptyMessage_FailsWithException() throws Exception {
        validator.validate(getInputStream(""));

        fail("Expected InvalidJsonException");
    }

    /**
     * Given a document starting with a typed value followed by other tokens, the validate method should throw
     * an InvalidJsonException.
     */
    @Test(expected = InvalidJsonException.class)
    public void testValidate_GivenBadSingleTypedValueDocumentAndNoConstraints_FailsWithException() throws Exception {
        validator.validate(getInputStream(POORLY_FORMED_SINGLE_TYPED_VALUE_DOCUMENT));

        fail("Expected InvalidJsonException");
    }

    /**
     * With all constraints enabled but none violated when given a valid document, the validate method should succeed.
     */
    @Test
    public void testValidate_AllConstraintsEnabledButNoneViolated_ValidationSucceeds() throws Exception {
        validator.setMaxContainerDepth(20);
        validator.setCheckContainerDepth(true);
        validator.setMaxArrayEntryCount(20);
        validator.setCheckArrayEntryCount(true);
        validator.setMaxObjectEntryCount(20);
        validator.setCheckObjectEntryCount(true);
        validator.setMaxEntryNameLength(20);
        validator.setCheckEntryNameLength(true);
        validator.setMaxStringValueLength(20);
        validator.setCheckStringValueLength(true);

        validator.validate(getInputStream(SINGLE_OBJECT_DOCUMENT));
    }

    /**
     * Given a poorly formed document the validate method should throw an InvalidJsonException
     * when encountering the first unexpected token.
     */
    @Test(expected = InvalidJsonException.class)
    public void testValidate_GivenBadJsonAndNoConstraints_FailsWithException() throws Exception {
        validator.validate(getInputStream(POORLY_FORMED_DOCUMENT));

        fail("Expected InvalidJsonException");
    }

    /**
     * Given a poorly formed document that violates a constraint before the parser encounters the first unexpected
     * token, the validate method should throw a JsonDocumentStructureValidationException specifying the violation.
     */
    @Test
    public void testValidate_GivenBadJsonViolatingEntryNameLengthConstraint_FailsWithException() throws Exception {
        validator.setMaxEntryNameLength(2);
        validator.setCheckEntryNameLength(true);

        try {
            validator.validate(getInputStream(POORLY_FORMED_DOCUMENT));

            fail("Expected JsonDocumentStructureValidationException");
        } catch (JsonDocumentStructureValidationException e) {
            assertEquals(ConstraintViolation.ENTRY_NAME_LENGTH, e.getViolation());
            assertEquals(2, e.getLine());
            assertEquals(19, e.getColumn());
        }
    }

    /**
     * When the container depth of the document exceeds the defined constraint, the validate method should
     * throw a JsonDocumentStructureValidationException specifying there was a CONTAINER_DEPTH violation.
     */
    @Test
    public void testValidate_ContainerDepthConstraintViolated_FailsWithException() throws Exception {
        validator.setMaxContainerDepth(2);
        validator.setCheckContainerDepth(true);

        try {
            validator.validate(getInputStream(SINGLE_OBJECT_DOCUMENT));

            fail("Expected JsonDocumentStructureValidationException");
        } catch (JsonDocumentStructureValidationException e) {
            assertEquals(ConstraintViolation.CONTAINER_DEPTH, e.getViolation());
            assertEquals(14, e.getLine());
            assertEquals(9, e.getColumn());
        }
    }

    /**
     * When the entry count of an array exceeds the defined constraint, the validate method should
     * throw a JsonDocumentStructureValidationException specifying there was a ARRAY_ENTRY_COUNT violation.
     */
    @Test
    public void testValidate_ArrayEntryCountConstraintViolated_FailsWithException() throws Exception {
        validator.setMaxArrayEntryCount(4);
        validator.setCheckArrayEntryCount(true);

        try {
            validator.validate(getInputStream(NESTED_ARRAYS_DOCUMENT));

            fail("Expected JsonDocumentStructureValidationException");
        } catch (JsonDocumentStructureValidationException e) {
            assertEquals(ConstraintViolation.ARRAY_ENTRY_COUNT, e.getViolation());
            assertEquals(10, e.getLine());
            assertEquals(9, e.getColumn());
        }
    }

    /**
     * This checks that a low array entry count doesn't give a false-positive when each individual array doesn't
     * exceed the constraint, but the total number of entries in a set of nested arrays does exceed it.
     */
    @Test
    public void testValidate_GivenDeepNestedArraysComplyingWithEntryConstraint_ValidationSucceeds() throws Exception {
        validator.setMaxArrayEntryCount(5);
        validator.setCheckArrayEntryCount(true);

        validator.validate(getInputStream(NESTED_ARRAYS_DOCUMENT));
    }

    /**
     * When the entry count of an object exceeds the defined constraint, the validate method should
     * throw a JsonDocumentStructureValidationException specifying there was a OBJECT_ENTRY_COUNT violation.
     */
    @Test
    public void testValidate_ObjectEntryCountConstraintViolated_FailsWithException() throws Exception {
        validator.setMaxObjectEntryCount(6);
        validator.setCheckObjectEntryCount(true);

        try {
            validator.validate(getInputStream(SINGLE_OBJECT_DOCUMENT));

            fail("Expected JsonDocumentStructureValidationException");
        } catch (JsonDocumentStructureValidationException e) {
            assertEquals(ConstraintViolation.OBJECT_ENTRY_COUNT, e.getViolation());
            assertEquals(13, e.getLine());
            assertEquals(22, e.getColumn());
        }
    }

    /**
     * When the length of an entry name exceeds the defined constraint, the validate method should
     * throw a JsonDocumentStructureValidationException specifying there was a ENTRY_NAME_LENGTH violation.
     */
    @Test
    public void testValidate_EntryNameLengthConstraintViolated_FailsWithException() throws Exception {
        validator.setMaxEntryNameLength(10);
        validator.setCheckEntryNameLength(true);

        try {
            validator.validate(getInputStream(SINGLE_OBJECT_DOCUMENT));

            fail("Expected JsonDocumentStructureValidationException");
        } catch (JsonDocumentStructureValidationException e) {
            assertEquals(ConstraintViolation.ENTRY_NAME_LENGTH, e.getViolation());
            assertEquals(8, e.getLine());
            assertEquals(27, e.getColumn());
        }
    }

    /**
     * When the length of a string value exceeds the defined constraint, the validate method should
     * throw a JsonDocumentStructureValidationException specifying there was a STRING_VALUE_LENGTH violation.
     */
    @Test
    public void testValidate_StringValueLengthConstraintViolated_FailsWithException() throws Exception {
        validator.setMaxStringValueLength(7);
        validator.setCheckStringValueLength(true);

        try {
            validator.validate(getInputStream(SINGLE_OBJECT_DOCUMENT));

            fail("Expected JsonDocumentStructureValidationException");
        } catch (JsonDocumentStructureValidationException e) {
            assertEquals(ConstraintViolation.STRING_VALUE_LENGTH, e.getViolation());
            assertEquals(8, e.getLine());
            assertEquals(41, e.getColumn());
        }
    }
}
