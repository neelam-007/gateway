/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 19, 2008
 * Time: 12:02:44 PM
 */
package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillVariable;

import java.util.LinkedHashMap;
import java.util.logging.Logger;

public class UsageSummaryAndSubReportHelper extends JRDefaultScriptlet {

    private static final Logger logger = Logger.getLogger(UsageSummaryAndSubReportHelper.class.getName());

    public static final String COLUMN_MAPPING_TOTAL = "COLUMN_MAPPING_TOTAL_";
    
    private LinkedHashMap<String,String> keyToColumnMap;

    public void setKeyToColumnMap(LinkedHashMap<String, String> keyToColumnMap){
        this.keyToColumnMap = keyToColumnMap;
    }

    public Long getColumnValue(String varName, String authUser, String [] mappingValues){
        String mappingValue = RuntimeDocUtilities.getMappingValueString(authUser, mappingValues);

        JRFillField field = (JRFillField) this.fieldsMap.get("USAGE_SUM");
        String colVarName = keyToColumnMap.get(mappingValue);

        if(varName.equals(colVarName)){
            return (Long) field.getValue();
        }

        //return it's current value
        JRFillVariable jrFillVariable = (JRFillVariable) this.variablesMap.get(varName);
        return (Long) jrFillVariable.getValue();
    }

    public Long getVariableValue(String varName, String authUser, String [] mappingValues){
        String mappingValue = RuntimeDocUtilities.getMappingValueString(authUser, mappingValues);

        JRFillField field = (JRFillField) this.fieldsMap.get("USAGE_SUM");
        String colVarName = keyToColumnMap.get(mappingValue);

        if(varName.equals(colVarName)){
            return (Long) field.getValue();
        }

        //return 0 so that sum functions are unaffected by this function being called for every detail row
        return 0L;
    }

    public Long getDirectVariableValue(String varName){
        if(!this.variablesMap.containsKey(varName)){
            throw new IllegalArgumentException("varName: " + varName+" not found");
        }

        JRFillVariable jrFillVariable = (JRFillVariable) this.variablesMap.get(varName);
        return (Long) jrFillVariable.getValue();
    }
    
    /**
     * Helper method used in creating the dataset for usage charts. For each row of data, a row relates to a unique
     * set of mapping values (includes auth user). This unique set can repeat throughout the reports data, as in if there
     * are 5 unique data sets, each with 3 rows of data then the first set occurs on the 1, 6 and 11th row of the dataset
     * As each set occurs, the chart will get a value for it and place it in a bucket for that category. This function
     * will return the long bucket value. This is done by first identifying what the column name is, and then supplying
     * this to the report scriptlet which can return the value for that column. The design of the scriptlet methods
     * is based on how the main report works / behaves, in the use case here, our columnName supplied to UsageReportHelper
     * .getVariableValue will always match the value it derives internally from the authUser and mappingValues
     *
     * This is a helper method as this could be done directly in the report but it would be very hard to read in xml
     * @param authUser
     * @param mappingValues
     * @return Long, the value for the column, which represents the unique set of mapping values supplied
     */
    public Long getUsageChartCategoryValue(String authUser, String [] mappingValues, String variablePrefix) throws JRScriptletException {
        String mappingValue = RuntimeDocUtilities.getMappingValueString(authUser, mappingValues);

        if(!keyToColumnMap.containsKey(mappingValue)){
            System.out.println("Throwing exception");
            throw new IllegalArgumentException("Key: " + mappingValue +" not found in keyToColumnName map");
        }
        String columnName = keyToColumnMap.get(mappingValue);
        String index = columnName.substring(columnName.indexOf("_")+1, columnName.length());
        int i = Integer.valueOf(index);
        Long returnValue = this.getDirectVariableValue(variablePrefix+i);
        return returnValue;
    }
}
