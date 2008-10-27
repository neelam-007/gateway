/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 21, 2008
 * Time: 3:14:12 PM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillVariable;
import java.util.LinkedHashMap;

public class UsageReportHelper extends JRDefaultScriptlet {

    private LinkedHashMap<String,String> keyToColumnMap;

    public void setKeyToColumnMap(LinkedHashMap<String, String> keyToColumnMap){
        this.keyToColumnMap = keyToColumnMap;
    }

    public Long getColumnValue(String varName, String authUser, String [] mappingValues){
        String mappingValue = Utilities.getMappingValueString(authUser, mappingValues);

        JRFillField field = (JRFillField) this.fieldsMap.get("USAGE_SUM");
        String colVarName = keyToColumnMap.get(mappingValue);

        System.out.println("Looking up: " + mappingValue);

        if(varName.equals(colVarName)){
            System.out.println(colVarName+" is " + field.getValue());
            return (Long) field.getValue();
        }

        //return it's current value
        JRFillVariable jrFillVariable = (JRFillVariable) this.variablesMap.get(varName);
        return (Long) jrFillVariable.getValue();
    }

    public Long getVariableValue(String varName, String authUser, String [] mappingValues){
        String mappingValue = Utilities.getMappingValueString(authUser, mappingValues);

        JRFillField field = (JRFillField) this.fieldsMap.get("USAGE_SUM");
        String colVarName = keyToColumnMap.get(mappingValue);

        System.out.println("Looking up: " + mappingValue);
        if(varName.equals(colVarName)){
            System.out.println(colVarName+" is " + field.getValue());
            return (Long) field.getValue();
        }

        //return 0 so that sum functions are unaffected by this function being called for every detail row
        return 0L;
    }

}
