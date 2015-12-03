package com.l7tech.external.assertions.extensiblesocketconnectorassertion.server.ISO8583;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 07/01/13
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class ISO8583DataElement {
    //datatype
    private ISO8583DataType dataType = null;
    private boolean variable = false;
    private int variableFieldLength = 0;
    private int length = 0;
    private ISO8583EncoderType encoderType = null;

    public ISO8583DataType getDataType() {
        return dataType;
    }

    public void setDataType(ISO8583DataType dataType) {
        this.dataType = dataType;
    }

    public boolean isVariable() {
        return variable;
    }

    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    public int getVariableFieldLength() {
        return variableFieldLength;
    }

    public void setVariableFieldLength(int variableFieldLength) {
        this.variableFieldLength = variableFieldLength;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public ISO8583EncoderType getEncoderType() {
        return encoderType;
    }

    public void setEncoderType(ISO8583EncoderType encoderType) {
        this.encoderType = encoderType;
    }

    @Override
    public String toString() {
        return dataType.toString() + " " + variableFieldLength + " " + length + " " + encoderType.toString();
    }
}
