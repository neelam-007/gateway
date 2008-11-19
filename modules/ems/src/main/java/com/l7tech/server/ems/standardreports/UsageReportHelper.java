/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 21, 2008
 * Time: 3:14:12 PM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillVariable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

public class UsageReportHelper extends JRDefaultScriptlet {

    private LinkedHashMap<String,String> keyToColumnMap;
    private static final String COLUMN_MAPPING_TOTAL = "COLUMN_MAPPING_TOTAL_";

    public void setKeyToColumnMap(LinkedHashMap<String, String> keyToColumnMap){
        this.keyToColumnMap = keyToColumnMap;
    }

    public Long getColumnValue(String varName, String authUser, String [] mappingValues){
        String mappingValue = Utilities.getMappingValueString(authUser, mappingValues);

        JRFillField field = (JRFillField) this.fieldsMap.get("USAGE_SUM");
        String colVarName = keyToColumnMap.get(mappingValue);

        //System.out.println("Looking up: " + mappingValue);

        if(varName.equals(colVarName)){
            //System.out.println(colVarName+" is " + field.getValue());
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

//        System.out.println("Looking up: " + mappingValue);
        if(varName.equals(colVarName)){
//            System.out.println(colVarName+" is " + field.getValue());
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
    public Long getUsageChartCategoryValue(String authUser, String [] mappingValues) throws JRScriptletException {
        String mapping = Utilities.getMappingValueString(authUser, mappingValues);
        System.out.println("Getting column name for: " + mapping);
        if(!keyToColumnMap.containsKey(mapping)){
            System.out.println("Throwing exception");
            throw new IllegalArgumentException("Key: " + mapping+" not found in keyToColumnName map");
        }
        String columnName = keyToColumnMap.get(mapping);
        String index = columnName.substring(columnName.indexOf("_")+1, columnName.length());
        System.out.println("Index is: " + index);
        int i = Integer.valueOf(index);
        System.out.println("Column name is: " + COLUMN_MAPPING_TOTAL+i);
        Long returnValue = this.getDirectVariableValue(COLUMN_MAPPING_TOTAL+i);
        System.out.println("value is: " + returnValue);
        return returnValue;
    }

//    public JRDataSource getChartDataSource() throws JRException {
//
//        System.out.println("Getting data source");
//        List<ReportTotalBean> beans = new ArrayList<ReportTotalBean>();
//
//        for(Object o: this.variablesMap.keySet()){
//            JRFillVariable fV = (JRFillVariable) this.variablesMap.get(o);
//            if(fV.getName().startsWith(COLUMN_MAPPING_TOTAL)){
//                ReportTotalBean reportTotalBean = new ReportTotalBean(fV.getName(), (Long)fV.getValue());
//                beans.add(reportTotalBean);
//            }
//        }
//        System.out.println("Printing out beans");
//
//        for(ReportTotalBean bean: beans){
//            System.out.println(bean.getName()+" " + bean.getValue());
//        }
//        System.out.println(beans.size()+ " beans found");
//        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(beans);
//
//        JRField name = new FieldImpl("name");
//        JRField value = new FieldImpl("value");
//
//        while(dataSource.next()){
//            System.out.println(dataSource.getFieldValue(name) + " " + dataSource.getFieldValue(value));
//        }
//
//        dataSource.moveFirst();
//        return dataSource;
//    }


//    public static class FieldImpl implements JRField{
//
//        private String name;
//
//        public FieldImpl(String name) {
//            this.name = name;
//        }
//
//        public JRPropertiesHolder getParentProperties() {
//            return null;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        public JRPropertiesMap getPropertiesMap() {
//            return null;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        public boolean hasProperties() {
//            return false;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        public Object clone() {
//            return null;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        public String getDescription() {
//            return null;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        public String getName() {
//            return name;
//        }
//
//        public Class getValueClass() {
//            return null;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        public String getValueClassName() {
//            return null;  //To change body of implemented methods use File | Settings | File Templates.
//        }
//
//        public void setDescription(String s) {
//            //To change body of implemented methods use File | Settings | File Templates.
//        }
//    }

}
