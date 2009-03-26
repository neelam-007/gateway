/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Nov 19, 2008
 * Time: 12:02:44 PM
 */
package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.JRDefaultScriptlet;
import net.sf.jasperreports.engine.JRScriptletException;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillVariable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;

public class UsageSummaryAndSubReportHelper extends JRDefaultScriptlet {

    private static final Logger logger = Logger.getLogger(UsageSummaryAndSubReportHelper.class.getName());

    public static final String COLUMN_MAPPING_TOTAL = "COLUMN_MAPPING_TOTAL_";

    private LinkedHashMap<String, String> keyToColumnMap;

    private LinkedHashMap<Integer, String> groupIndexToGroupMap;

    public void setKeyToColumnMap(LinkedHashMap<String, String> keyToColumnMap) {
        this.keyToColumnMap = keyToColumnMap;
    }

    public void setIndexToGroupMap(LinkedHashMap<Integer, String> groupIndexToGroup) {
        this.groupIndexToGroupMap = groupIndexToGroup;
    }

    public Long getColumnValue(String varName, String authUser, String[] mappingValues) {
        String mappingValue = RuntimeDocUtilities.getMappingValueString(authUser, mappingValues);

        JRFillField field = (JRFillField) this.fieldsMap.get("USAGE_SUM");
        String colVarName = keyToColumnMap.get(mappingValue);

        if (varName.equals(colVarName)) {
            return (Long) field.getValue();
        }

        //return it's current value
        JRFillVariable jrFillVariable = (JRFillVariable) this.variablesMap.get(varName);
        return (Long) jrFillVariable.getValue();
    }

    public Long getVariableValue(String varName, String authUser, String[] mappingValues) {
        String mappingValue = RuntimeDocUtilities.getMappingValueString(authUser, mappingValues);

        JRFillField field = (JRFillField) this.fieldsMap.get("USAGE_SUM");
        String colVarName = keyToColumnMap.get(mappingValue);

        if (varName.equals(colVarName)) {
            return (Long) field.getValue();
        }

        //return 0 so that sum functions are unaffected by this function being called for every detail row
        return 0L;
    }

    public JRDataSource getChartDataSource() throws JRException {

        List<ReportTotalBean> beans = new ArrayList<ReportTotalBean>();

        for (Object o : this.variablesMap.keySet()) {
            JRFillVariable fV = (JRFillVariable) this.variablesMap.get(o);
            if (fV.getName().startsWith(COLUMN_MAPPING_TOTAL)) {
                String columnName = fV.getName();
                String index = columnName.substring(columnName.lastIndexOf("_") + 1, columnName.length());
                //System.out.println("Index is: " + index);
                int i = Integer.valueOf(index);
                if (!groupIndexToGroupMap.containsKey(i))
                    throw new IllegalStateException("key: " + i + " not found in groupIndexToGroupMap");
                String groupName = this.groupIndexToGroupMap.get(i);
                ReportTotalBean reportTotalBean = new ReportTotalBean(groupName, (Long) fV.getValue());
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
