/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 8, 2008
 * Time: 2:39:52 PM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.base.JRBaseStyle;
import net.sf.jasperreports.engine.fill.*;

import java.util.List;
import java.util.Map;
import java.awt.*;
//todo [Donal] this class is 100% on style names from the report's its called from. Need a unit test to validate string
//style names

/**
 * Lifecycle class, called on report filling lifecycle events. Allows runtime modifications to be made to 
 * report. Initial use is to modify styles at runtime.
 */
public class ScriptletHelper extends JRDefaultScriptlet {

    private final static String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    private final static String SERVICE_ID_GROUP = "SERVICE_ID_GROUP";
    private final static String SERVICE_OPERATION_VALUE = "SERVICE_OPERATION_VALUE";
    private final static String FRAME_DETAIL_TABLE_ROW = "FrameDetailTableRow";
    private final static String TABLE_CELL = "TableCell";

    @Override
    public void beforeGroupInit(String s) throws JRScriptletException {
        if(s.equals(SERVICE_ID_GROUP)){
            JRFillParameter fp = (JRFillParameter) this.parametersMap.get(STYLES_FROM_TEMPLATE);
            Map styleMap = (Map) fp.getValue();
            if(!styleMap.containsKey(FRAME_DETAIL_TABLE_ROW)){
                throw new IllegalStateException(FRAME_DETAIL_TABLE_ROW + " style not found");
            }

            if(!styleMap.containsKey(TABLE_CELL)){
                throw new IllegalStateException(TABLE_CELL+ " style not found");
            }
            
            JRStyle nonDetailStyle = (JRStyle) styleMap.get(FRAME_DETAIL_TABLE_ROW);
            JRStyle tableStyle = (JRStyle) styleMap.get(TABLE_CELL);

            if(nonDetailStyle == null) throw new IllegalStateException(FRAME_DETAIL_TABLE_ROW+" not found");

            JRFillField jrFillField = (JRFillField) this.fieldsMap.get(SERVICE_OPERATION_VALUE);
            String operation = (String) jrFillField.getValue();
            if(operation == null) return;//no data in report
            if(operation.equals(Utilities.SQL_PLACE_HOLDER)){
                JRFillGroup jrFillGroup = null;
                for(JRFillGroup fillGroup: this.groups){
                    if(fillGroup.getName().equals(s)){
                        jrFillGroup = fillGroup;
                        break;
                    }
                }
                if(jrFillGroup == null) throw new IllegalStateException("Group " + s + " not found");

                JRBand jrBand = jrFillGroup.getGroupFooter();
                List children = jrBand.getChildren();
                for(Object o: children){
                    //This band should only have 1 frame
                    if(o instanceof JRFillFrame){
                        JRFillFrame jrFillFrame = (JRFillFrame) o;
                        jrFillFrame.setStyle(nonDetailStyle);
                        List frameChildren = jrFillFrame.getChildren();
                        for(Object o1: frameChildren){
                            if(o1 instanceof JRFillTextField){
                                JRFillTextField field = (JRFillTextField) o1;
                                //JRStyle style = field.getStyle();
                                //if(style.getName().equals("")) continue;//don't process the static text field
                                field.setStyle(tableStyle);
                            }
                        }

                        break;
                    }
                }
            }
        }
    }

    private Long throughputSum;


}
