/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 21, 2008
 * Time: 3:14:12 PM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillVariable;

import java.util.*;

public class UsageReportHelper extends JRDefaultScriptlet {

    private LinkedHashMap<String,String> keyToColumnMap;
    private LinkedHashMap<Integer, String> groupIndexToGroupMap;

    public static final String COLUMN_REPORT = "COLUMN_REPORT_";
    
    public String chartKey;

    public void setKeyToColumnMap(LinkedHashMap<String, String> keyToColumnMap){
        this.keyToColumnMap = keyToColumnMap;
    }

    public void setIndexToGroupMap(LinkedHashMap<Integer, String> groupIndexToGroup) {
        this.groupIndexToGroupMap = groupIndexToGroup;
    }

    public LinkedHashMap<Integer, String> getGroupIndexToGroupMap() {
        return groupIndexToGroupMap;
    }

    public Long getDirectVariableValue(String varName){
        if(!this.variablesMap.containsKey(varName)){
//            System.out.println("Printing out available variables");
//            for(Object o: this.variablesMap.keySet()){
//                System.out.println("Var: " + o);
//            }
            throw new IllegalArgumentException("varName: " + varName+" not found");
        }

        System.out.println("Getting variable: " + varName);
        JRFillVariable jrFillVariable = (JRFillVariable) this.variablesMap.get(varName);
        System.out.println("Getting variable: " + varName + " with value " + jrFillVariable.getValue());
        return (Long) jrFillVariable.getValue();
    }

    public void setChartKey(String key){
        this.chartKey = key;
    }

    public Map<String, Long> getReportTotalsMap(){
        Map<String, Long> returnMap = new LinkedHashMap<String, Long>();//order is important always

        for(Map.Entry<String, String> e: keyToColumnMap.entrySet()){
            String columnName = e.getValue();
            String index = columnName.substring(columnName.indexOf("_")+1, columnName.length());
            System.out.println("Index is: " + index);
            int i = Integer.valueOf(index);
            System.out.println("Column name is: " + COLUMN_REPORT+i);
            Long returnValue = this.getDirectVariableValue(COLUMN_REPORT+i);
            returnMap.put(columnName, returnValue);
        }
        return returnMap;
    }

    public void beforeReportInit() throws JRScriptletException {
        if(chartKey == null || chartKey.equals("")) throw new IllegalStateException("chartKey has not been set");
        Utilities.addHelper(chartKey, this);
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
