package com.l7tech.external.assertions.cassandra;

import com.datastax.driver.core.DataType;

import java.io.Serializable;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 13/02/14
 * Time: 10:37 AM
 * To change this template use File | Settings | File Templates.
 */
public class CassandraNamedParameter implements Serializable {
    String parameterName;
    String parameterValue;
    String parameterDataType;

    public String getParameterDataType() {
        return parameterDataType;
    }

    public void setParameterDataType(String parameterDataType) {
        this.parameterDataType = parameterDataType;
    }

    public String getParameterName() {
        return parameterName;

    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getParameterValue() {
        return parameterValue;
    }

    public void setParameterValue(String parameterValue) {
        this.parameterValue = parameterValue;
    }

}
