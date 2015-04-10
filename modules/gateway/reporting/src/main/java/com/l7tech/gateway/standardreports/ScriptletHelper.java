/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Oct 8, 2008
 * Time: 2:39:52 PM
 */
package com.l7tech.gateway.standardreports;

import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.*;

import java.util.List;
import java.util.Map;
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
    private final static String LEFT_RIGHT_LIGHT_GREY = "LeftRightLightGrey";
    private static final String LEFT_PADDED_HEADING_HTML = "LeftPaddedHeadingHtml";
    private static final String DEFAULT_CENTER_ALIGNED = "DefaultCenterAligned";

    /**
     * The performance summary report handles both mapping and non mapping queries, just as the interval report does.
     * However there is one section of the performance summary report where we need different styles based on
     * whether the report is mapping and if it's a detail report.
     * What happens below is that if the report is a mapping report, and it's not a detail report, it means
     * that we aer going to display the service name as the first column in band SERVICE_ID_GROUP (to understand this
     * you need to look at the jrxml file). The default style for this bands frame is that it has borders and has a
     * grey colour to indicate that it has totals. When it's not a detail report we need to change the style so that
     * only the left and right hand sides have a border, and the background is white so that it's clear that the data
     * is not a total
     *
     * @param s
     * @throws JRScriptletException
     */
    @Override
    public void beforeGroupInit(String s) throws JRScriptletException {
        if (s.equals(SERVICE_ID_GROUP)) {
            JRFillParameter fp = (JRFillParameter) this.parametersMap.get(STYLES_FROM_TEMPLATE);
            Map styleMap = (Map) fp.getValue();
            if (!styleMap.containsKey(LEFT_RIGHT_LIGHT_GREY)) {
                throw new IllegalStateException(LEFT_RIGHT_LIGHT_GREY + " style not found");
            }

            if (!styleMap.containsKey(LEFT_PADDED_HEADING_HTML)) {
                throw new IllegalStateException(LEFT_PADDED_HEADING_HTML + " style not found");
            }

            JRStyle nonDetailStyle = (JRStyle) styleMap.get(LEFT_RIGHT_LIGHT_GREY);
            JRStyle serviceTextFieldColumnStyle = (JRStyle) styleMap.get(LEFT_PADDED_HEADING_HTML);
            JRStyle tableColumnStyle = (JRStyle) styleMap.get(DEFAULT_CENTER_ALIGNED);

            if (nonDetailStyle == null) throw new IllegalStateException(LEFT_RIGHT_LIGHT_GREY + " not found");

            JRFillField jrFillField = (JRFillField) this.fieldsMap.get(SERVICE_OPERATION_VALUE);
            String operation = (String) jrFillField.getValue();
            if (operation == null) return;//no data in report
            if (operation.equals(Utilities.SQL_PLACE_HOLDER)) {
                JRFillGroup jrFillGroup = null;
                for (JRFillGroup fillGroup : this.groups) {
                    if (fillGroup.getName().equals(s)) {
                        jrFillGroup = fillGroup;
                        break;
                    }
                }
                if (jrFillGroup == null) throw new IllegalStateException("Group " + s + " not found");

                JRSection jrSection = jrFillGroup.getGroupFooterSection();
                JRBand[] jrBands = jrSection.getBands();
                for (JRBand jrBand : jrBands) {
                    List children = jrBand.getChildren();
                    for (Object o : children) {
                        //This band should only have 1 frame
                        if (o instanceof JRFillFrame) {
                            JRFillFrame jrFillFrame = (JRFillFrame) o;
                            jrFillFrame.setStyle(nonDetailStyle);
                            List frameChildren = jrFillFrame.getChildren();
                            for (Object o1 : frameChildren) {
                                if (o1 instanceof JRFillTextField) {
                                    JRFillTextField field = (JRFillTextField) o1;
                                    if (field.getKey().startsWith("serviceName")) {
                                        field.setStyle(serviceTextFieldColumnStyle);
                                    } else {
                                        field.setStyle(tableColumnStyle);
                                    }
                                }
                            }

                            break;
                        }
                    }
                }
            }
        }
    }

}
