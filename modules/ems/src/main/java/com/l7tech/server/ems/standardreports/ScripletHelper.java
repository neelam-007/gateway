/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 8, 2008
 * Time: 2:39:52 PM
 */
package com.l7tech.server.ems.standardreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.JRFillParameter;
import net.sf.jasperreports.engine.fill.JRFillField;
import net.sf.jasperreports.engine.fill.JRFillGroup;
import net.sf.jasperreports.engine.fill.JRFillFrame;

import java.util.List;
//todo [Donal] this class is 100% on style names from the report's its called from. Need a unit test to validate string
//style names

/**
 * Lifecycle class, called on report filling lifecycle events. Allows runtime modifications to be made to 
 * report. Initial use is to modify styles at runtime.
 */
public class ScripletHelper extends JRDefaultScriptlet {

    private final static String JR_REPORT = "JR_REPORT";
    private final static String SERVICE_ID_GROUP = "SERVICE_ID_GROUP";
    private final static String SERVICE_OPERATION_VALUE = "SERVICE_OPERATION_VALUE";
    private final static String FRAME_DETAIL_TABLE_ROW = "FrameDetailTableRow";


    @Override
    public void beforeGroupInit(String s) throws JRScriptletException {
        if(s.equals(SERVICE_ID_GROUP)){
            JRFillParameter jrFillParameter = (JRFillParameter) this.parametersMap.get(JR_REPORT);
            JasperReport jr = (JasperReport) jrFillParameter.getValue();

            JRStyle [] jrStyles = jr.getStyles();
            JRStyle nonDetailStyle = null;
            for(JRStyle styles: jrStyles){
                if(styles.getName().equals(FRAME_DETAIL_TABLE_ROW)){
                    nonDetailStyle = styles;
                    break;
                }
            }

            if(nonDetailStyle == null) throw new IllegalStateException(FRAME_DETAIL_TABLE_ROW+" not found");

            JRFillField jrFillField = (JRFillField) this.fieldsMap.get(SERVICE_OPERATION_VALUE);
            String operation = (String) jrFillField.getValue();
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
                    //Design decision is 1 frame per band, so this band should only have one Frame
                    if(o instanceof JRFillFrame){
                        JRFillFrame jrFillFrame = (JRFillFrame) o;
                        jrFillFrame.setStyle(nonDetailStyle);
                        break;
                    }
                }
            }
        }
    }



}
