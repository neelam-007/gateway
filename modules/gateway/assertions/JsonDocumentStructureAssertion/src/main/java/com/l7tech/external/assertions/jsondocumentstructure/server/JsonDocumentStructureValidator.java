package com.l7tech.external.assertions.jsondocumentstructure.server;

import com.l7tech.json.InvalidJsonException;
import org.codehaus.jackson.*;

import java.io.IOException;
import java.io.InputStream;

import static com.l7tech.external.assertions.jsondocumentstructure.server.JsonDocumentStructureValidationException.ConstraintViolation;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class JsonDocumentStructureValidator {
    private long maxContainerDepth = Long.MAX_VALUE;
    private long maxObjectEntryCount = Long.MAX_VALUE;
    private long maxArrayEntryCount = Long.MAX_VALUE;
    private long maxEntryNameLength = Long.MAX_VALUE;
    private long maxStringValueLength = Long.MAX_VALUE;

    private boolean checkContainerDepth = false;
    private boolean checkObjectEntryCount = false;
    private boolean checkArrayEntryCount = false;
    private boolean checkEntryNameLength = false;
    private boolean checkStringValueLength = false;

    int currContainerDepth = 0;

    public void validate(InputStream inputStream)
            throws JsonDocumentStructureValidationException, InvalidJsonException, IOException {
        try (JsonParser parser = new JsonFactory().createJsonParser(inputStream)) {
            if (null == parser.nextToken()) {
                // fail on empty stream
                throw new InvalidJsonException("InputStream is empty.");
            }

            switch (parser.getCurrentToken()) {
                case START_OBJECT:
                    validateObject(parser);
                    break;
                case START_ARRAY:
                    validateArray(parser);
                    break;
                case VALUE_STRING:
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                case VALUE_TRUE:
                case VALUE_FALSE:
                case VALUE_NULL:
                    validateSingleTypedValueDocument(parser);
                    break;
                default:
                    // doesn't start with open object, array, or typed value - not well-formed JSON
                    throw new InvalidJsonException("Unexpected first token: " + parser.getCurrentToken());
            }
        } catch (JsonParseException e) {
            throw new InvalidJsonException(e);
        } finally {
            zeroCounters();
        }
    }

    private void validateObject(JsonParser parser)
            throws JsonDocumentStructureValidationException, InvalidJsonException, IOException {
        currContainerDepth++;

        if (checkContainerDepth) {
            checkContainerDepth(parser);
        }

        int objectEntryCount = 0;

        while (null != parser.nextToken()) {
            switch (parser.getCurrentToken()) {
                case FIELD_NAME:
                    if (checkObjectEntryCount) {
                        objectEntryCount++;
                        checkObjectEntryCount(parser, objectEntryCount);
                    }

                    validateField(parser); // also handles value
                    break;
                case END_OBJECT:
                    currContainerDepth--;
                    return;
                default:
                    // poorly-formed JSON
                    throw new InvalidJsonException("Unexpected token encountered: " + parser.getCurrentToken());
            }
        }
    }

    private void validateArray(JsonParser parser)
            throws JsonDocumentStructureValidationException, InvalidJsonException, IOException {
        currContainerDepth++;

        if (checkContainerDepth) {
            checkContainerDepth(parser);
        }

        int arrayEntryCount = 0;

        while (null != parser.nextToken()) {
            switch (parser.getCurrentToken()) {
                case START_OBJECT:
                    if (checkArrayEntryCount) {
                        arrayEntryCount++;
                        checkArrayEntryCount(parser, arrayEntryCount);
                    }

                    validateObject(parser);
                    break;
                case VALUE_STRING:
                    if (checkArrayEntryCount) {
                        arrayEntryCount++;
                        checkArrayEntryCount(parser, arrayEntryCount);
                    }

                    if (checkStringValueLength) {
                        validateStringValue(parser);
                    }
                    break;
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                case VALUE_TRUE:
                case VALUE_FALSE:
                case VALUE_NULL:
                    if (checkArrayEntryCount) {
                        arrayEntryCount++;
                        checkArrayEntryCount(parser, arrayEntryCount);
                    }

                    break;
                case START_ARRAY:
                    if (checkArrayEntryCount) {
                        arrayEntryCount++;
                        checkArrayEntryCount(parser, arrayEntryCount);
                    }

                    validateArray(parser);
                    break;
                case END_ARRAY:
                    currContainerDepth--;
                    return;
                default:
                    // poorly-formed JSON
                    throw new InvalidJsonException("Unexpected token encountered: " + parser.getCurrentToken());
            }
        }
    }

    private void validateField(JsonParser parser)
            throws JsonDocumentStructureValidationException, InvalidJsonException, IOException {
        if (checkEntryNameLength) {
            checkEntryNameLength(parser);
        }

        switch (parser.nextToken()) {
            case START_OBJECT:
                validateObject(parser);
                break; // handled Object value, field handling complete
            case START_ARRAY:
                validateArray(parser);
                break; // handled Array value, field handling complete
            case VALUE_STRING:
                if (checkStringValueLength) {
                    validateStringValue(parser);
                }

                break;
            case VALUE_NUMBER_INT:
            case VALUE_NUMBER_FLOAT:
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NULL:
                break; // handled primitive value, field handling complete
            default:
                // poorly-formed JSON
                throw new InvalidJsonException("Unexpected token encountered: " + parser.getCurrentToken());
        }
    }

    private void validateSingleTypedValueDocument(JsonParser parser)
            throws JsonDocumentStructureValidationException, IOException {
        if (checkStringValueLength && JsonToken.VALUE_STRING == parser.getCurrentToken()) {
            validateStringValue(parser);
        }

        parser.nextToken(); // if any further tokens are encountered a JsonParseException will be thrown
    }

    private void checkContainerDepth(JsonParser parser) throws JsonDocumentStructureValidationException {
        if (currContainerDepth > maxContainerDepth) {
            JsonLocation location = parser.getCurrentLocation();

            throw new JsonDocumentStructureValidationException(ConstraintViolation.CONTAINER_DEPTH,
                    location.getLineNr(), location.getColumnNr());
        }
    }

    private void checkObjectEntryCount(JsonParser parser, int objectEntryCount)
            throws JsonDocumentStructureValidationException {
        if (objectEntryCount > maxObjectEntryCount) {
            JsonLocation location = parser.getCurrentLocation();

            throw new JsonDocumentStructureValidationException(ConstraintViolation.OBJECT_ENTRY_COUNT,
                    location.getLineNr(), location.getColumnNr());
        }
    }

    private void checkArrayEntryCount(JsonParser parser, int arrayEntryCount)
            throws JsonDocumentStructureValidationException {
        if (arrayEntryCount > maxArrayEntryCount) {
            JsonLocation location = parser.getCurrentLocation();

            throw new JsonDocumentStructureValidationException(ConstraintViolation.ARRAY_ENTRY_COUNT,
                    location.getLineNr(), location.getColumnNr());
        }
    }

    private void checkEntryNameLength(JsonParser parser) throws JsonDocumentStructureValidationException, IOException {
        if (parser.getCurrentName().length() > maxEntryNameLength) {
            JsonLocation location = parser.getCurrentLocation();

            throw new JsonDocumentStructureValidationException(ConstraintViolation.ENTRY_NAME_LENGTH,
                    location.getLineNr(), location.getColumnNr());
        }
    }

    private void validateStringValue(JsonParser parser) throws JsonDocumentStructureValidationException, IOException {
        if (parser.getText().length() > maxStringValueLength) {
            JsonLocation location = parser.getCurrentLocation();

            throw new JsonDocumentStructureValidationException(ConstraintViolation.STRING_VALUE_LENGTH,
                    location.getLineNr(), location.getColumnNr());
        }
    }

    private void zeroCounters() {
        currContainerDepth = 0;
    }

    public void setMaxContainerDepth(long maxContainerDepth) {
        this.maxContainerDepth = maxContainerDepth;
    }

    public void setMaxObjectEntryCount(long maxObjectEntryCount) {
        this.maxObjectEntryCount = maxObjectEntryCount;
    }

    public void setMaxArrayEntryCount(long maxArrayEntryCount) {
        this.maxArrayEntryCount = maxArrayEntryCount;
    }

    public void setMaxEntryNameLength(long maxEntryNameLength) {
        this.maxEntryNameLength = maxEntryNameLength;
    }

    public void setMaxStringValueLength(long maxStringValueLength) {
        this.maxStringValueLength = maxStringValueLength;
    }

    public void setCheckContainerDepth(boolean checkContainerDepth) {
        this.checkContainerDepth = checkContainerDepth;
    }

    public void setCheckObjectEntryCount(boolean checkObjectEntryCount) {
        this.checkObjectEntryCount = checkObjectEntryCount;
    }

    public void setCheckArrayEntryCount(boolean checkArrayEntryCount) {
        this.checkArrayEntryCount = checkArrayEntryCount;
    }

    public void setCheckEntryNameLength(boolean checkEntryNameLength) {
        this.checkEntryNameLength = checkEntryNameLength;
    }

    public void setCheckStringValueLength(boolean checkStringValueLength) {
        this.checkStringValueLength = checkStringValueLength;
    }
}
