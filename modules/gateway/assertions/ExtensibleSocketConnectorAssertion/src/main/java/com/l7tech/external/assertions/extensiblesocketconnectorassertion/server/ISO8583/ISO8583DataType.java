package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 07/01/13
 * Time: 3:16 PM
 * To change this template use File | Settings | File Templates.
 */
public enum ISO8583DataType {

    ALPHA("a", "^[a-zA-Z ]+$"),            //Alpha, include blanks
    NUMERIC("n", "^[0-9]+$"),        //Numeric values only
    SPECIAL("s", "^[^\\d\\sa-zA-Z]+$"),        //Special characters only
    ALPHANUMERIC("an", "^[0-9a-zA-Z]+$"),    //Alphanumeric
    ALPHASPECIAL("as", "^[^\\d\\s]+$"),    //Alpha & special characters only
    NUMERICSPECIAL("ns", "^[^\\sa-zA-Z]+$"),    //Numeric & special characters only
    TEXT("ans", ""),            //Alpha, numeric, and special characters
    BINARY("b", ""),            //Binary data
    TRACK2ISO7813("z", "^;([\\d]{19})=([\\d]{4}|=)([\\d]{3})([\\d]{1,8})?\\?$"); //Tracks 2 code set as defined in ISO/IEC 7813 respectively

    private String code;
    private Pattern regex;

    ISO8583DataType(String _code, String _regex) {
        code = _code;
        regex = Pattern.compile(_regex);
    }

    public static ISO8583DataType fromString(String value) {

        ISO8583DataType[] dataTypes = ISO8583DataType.values();

        for (ISO8583DataType dataType : dataTypes) {
            if (dataType.toString().equals(value))
                return dataType;
        }

        return null;
    }

    public Pattern getDataCheckPattern() {
        return regex;
    }

    @Override
    public String toString() {
        return code;
    }
}
