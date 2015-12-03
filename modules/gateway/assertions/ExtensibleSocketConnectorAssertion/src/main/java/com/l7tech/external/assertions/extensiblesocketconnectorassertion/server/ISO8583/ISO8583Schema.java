package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 07/01/13
 * Time: 3:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583Schema {

    private final String REGEX = "[^.]";
    private final String DEFAULT = "DEFAULT";
    private final String PROPERTY_FILE = "com/l7tech/external/assertions/extensiblesocketconnectorassertion/iso8583.properties";

    private final int maxFields = 192;
    private ISO8583DataElement[] schema = null;

    private enum DataElementFields {
        DATATYPE,
        VARIABLE,
        LENGTH,
        ENCODER;
    }

    public ISO8583Schema() {
        //initialize the schema array
        schema = new ISO8583DataElement[maxFields];
    }

    public void initialize(String propertyFileLocation) throws ISO8583SchemaException {
        Properties prop = new Properties();
        InputStream streamIn = null;
        String file = "";

        //load properties containing the ISO8583 data element schema
        if (propertyFileLocation != null &&
                !propertyFileLocation.isEmpty() &&
                !propertyFileLocation.toUpperCase().equals(DEFAULT)) {
            file = propertyFileLocation;

            try {
                streamIn = new FileInputStream(propertyFileLocation);
            } catch (FileNotFoundException fnfE) {
                throw new ISO8583SchemaException("Property file not found: " + propertyFileLocation);
            }

        } else {
            file = PROPERTY_FILE;
            streamIn = ISO8583Schema.class.getClassLoader().getResourceAsStream(PROPERTY_FILE);

            if (streamIn == null)
                throw new ISO8583SchemaException("Property file not found: " + PROPERTY_FILE);
        }

        try {
            prop.load(streamIn);
        } catch (IOException e1) {
            throw new ISO8583SchemaException("Cannot read property file: " + file);
        }

        ISO8583DataElement dElement = null;
        String propertyKey = "";
        String propertyData = "";
        int fieldPosition = 0;
        Set<String> propertyKeyList = prop.stringPropertyNames();
        Iterator<String> propertyKeyListIter = propertyKeyList.iterator();

        //cycle through the properties and load the data into an array of ISO8583DataElement.
        while (propertyKeyListIter.hasNext()) {
            propertyKey = propertyKeyListIter.next();

            if (isInteger(propertyKey)) {
                fieldPosition = Integer.parseInt(propertyKey);
                propertyData = prop.getProperty(propertyKey);

                //we have property data... use it to create a iso8583 data element
                if (propertyData != null && !propertyData.isEmpty()) {

                    try {
                        dElement = processPropertyData(propertyData);
                        schema[fieldPosition - 1] = dElement;
                    } catch (ISO8583SchemaException iSE) {
                        System.out.println("Error at key " + propertyKey + ": " + iSE.getMessage()); //log error message
                    }

                } else
                    System.out.println("Invlid data. No data present for key: " + propertyKey); //log error message
            } else
                //log error message here
                System.out.println("Invalid key.  Key should be integer between 1 - 192 at: " + propertyKey);
        }

    }

    private ISO8583DataElement processPropertyData(String propertyData) throws ISO8583SchemaException {

        ISO8583DataElement dElement = new ISO8583DataElement();
        String[] fields = null;

        ISO8583DataType dataType = null;
        ISO8583EncoderType encoderType = null;
        int variableFieldLength = 0;
        int length = 0;

        fields = propertyData.split(",");

        if (fields.length == 4) {

            //get datatype
            dataType = processDataType(fields[DataElementFields.DATATYPE.ordinal()]);
            dElement.setDataType(dataType);

            //get variable length data
            variableFieldLength = processVariableFieldLength(fields[DataElementFields.VARIABLE.ordinal()]);
            if (variableFieldLength > 0) {
                dElement.setVariable(true);
                dElement.setVariableFieldLength(variableFieldLength);
            }

            //get max length
            length = processLength(fields[DataElementFields.LENGTH.ordinal()]);
            dElement.setLength(length);

            //process encoder
            encoderType = processEncoderType(fields[DataElementFields.ENCODER.ordinal()]);
            dElement.setEncoderType(encoderType);

        } else
            throw new ISO8583SchemaException("Incorrect number of fields for data element.  " +
                    "4 fields should be present.");

        return dElement;
    }

    private ISO8583DataType processDataType(String value) throws ISO8583SchemaException {

        ISO8583DataType dataType = null;

        dataType = ISO8583DataType.fromString(value);

        if (dataType == null)
            throw new ISO8583SchemaException("Invalid value for data type.  " +
                    "Data type should be one of: [a, n, s, an, as, ns, ans, b].");

        return dataType;
    }

    private int processVariableFieldLength(String value) throws ISO8583SchemaException {

        if (value != null && !value.isEmpty()) {

            Pattern pattern = Pattern.compile(REGEX);
            Matcher matcher = pattern.matcher(value);

            if (matcher.find())
                throw new ISO8583SchemaException("Invalud value for variable field indicator.  " +
                        "Value should be one of: [., .., ...].");
            else
                return value.length();
        }

        return 0;
    }

    private int processLength(String value) throws ISO8583SchemaException {

        int length = 0;

        try {
            length = Integer.parseInt(value);
        } catch (NumberFormatException nFE) {
            throw new ISO8583SchemaException("Invalid value for length.  " +
                    "Length needs to be an integer between 1 and 999.");
        }

        return length;
    }

    private ISO8583EncoderType processEncoderType(String value) throws ISO8583SchemaException {

        ISO8583EncoderType encoderType = null;

        encoderType = ISO8583EncoderType.fromString(value);

        if (encoderType == null) {
            String errorMessage = "Invalid value for encoder type.  Encoder type should be one of: [";

            EnumSet<ISO8583EncoderType> encoderSet = EnumSet.allOf(ISO8583EncoderType.class);
            for (ISO8583EncoderType et : encoderSet) {
                errorMessage += et.toString() + " ";
            }

            errorMessage += "].";

            throw new ISO8583SchemaException(errorMessage);
        }

        return encoderType;
    }

    private boolean isInteger(String data) {

        try {
            Integer.parseInt(data);
        } catch (NumberFormatException nFE) {
            return false;
        }

        return true;
    }

    public ISO8583DataElement[] getSchema() {
        return schema;
    }
}
