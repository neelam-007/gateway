/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 21, 2008
 * Time: 3:14:12 PM
 */
package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.fill.JRFillVariable;

import java.util.*;

public class UsageReportHelper extends JRDefaultScriptlet {

    private LinkedHashMap<String,String> keyToColumnMap;
    private LinkedHashMap<Integer, String> groupIndexToGroupMap;

    public static final String COLUMN_REPORT = "COLUMN_REPORT_";

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

        //System.out.println("Getting variable: " + varName);
        JRFillVariable jrFillVariable = (JRFillVariable) this.variablesMap.get(varName);
        //System.out.println("Getting variable: " + varName + " with value " + jrFillVariable.getValue());
        return (Long) jrFillVariable.getValue();
    }

    public Map<String, Long> getReportTotalsMap(){
        Map<String, Long> returnMap = new LinkedHashMap<String, Long>();//order is important always

        for(Map.Entry<String, String> e: keyToColumnMap.entrySet()){
            String columnName = e.getValue();
            String index = columnName.substring(columnName.indexOf("_")+1, columnName.length());
            //System.out.println("Index is: " + index);
            int i = Integer.valueOf(index);
            //System.out.println("Column name is: " + COLUMN_REPORT+i);
            Long returnValue = this.getDirectVariableValue(COLUMN_REPORT+i);
            returnMap.put(columnName, returnValue);
        }
        return returnMap;
    }

    public JRDataSource getChartDataSource() throws JRException {

        List<ReportTotalBean> beans = new ArrayList<ReportTotalBean>();

        for(Object o: this.variablesMap.keySet()){
            JRFillVariable fV = (JRFillVariable) this.variablesMap.get(o);
            if(fV.getName().startsWith(COLUMN_REPORT)){
                String columnName = fV.getName();
                String index = columnName.substring(columnName.lastIndexOf("_")+1, columnName.length());
                //System.out.println("Index is: " + index);
                int i = Integer.valueOf(index);
                if(!groupIndexToGroupMap.containsKey(i)) throw new IllegalStateException("key: " + i+" not found in groupIndexToGroupMap");
                String groupName = this.groupIndexToGroupMap.get(i);
                ReportTotalBean reportTotalBean = new ReportTotalBean(groupName, (Long)fV.getValue());
                beans.add(reportTotalBean);
            }
        }
        Collections.sort(beans);
        //System.out.println("Printing out beans");

//        for(ReportTotalBean bean: beans){
//            System.out.println(bean.getName()+" " + bean.getValue());
//        }
//        System.out.println(beans.size()+ " beans found");
        JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(beans);
        return dataSource;
     }


}
