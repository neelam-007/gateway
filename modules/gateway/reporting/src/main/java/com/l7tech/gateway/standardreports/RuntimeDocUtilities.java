/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Dec 16, 2008
 * Time: 3:47:14 PM
 */
package com.l7tech.gateway.standardreports;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.CDATASection;

import java.util.*;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.TextUtils;

public class RuntimeDocUtilities {
    public static final String DEFAULT_CENTER_ALIGNED = "DefaultCenterAligned";
    private static final String SUB_REPORT_WIDTH = "subReportWidth";
    private static final String IS_CONTEXT_MAPPING = "isContextMapping";
    private static final String CHART_LEGEND = "chartLegend";
    private static final String CHART_HEIGHT = "chartHeight";
    private static final String CHART_LEGEND_FRAME_YPOS = "chartLegendFrameYPos";
    private static final String CHART_LEGEND_HEIGHT = "chartLegendHeight";
    private static final String BAND_HEIGHT = "bandHeight";
    private static final String CHART_FRAME_HEIGHT = "chartFrameHeight";
    private static final String PAGE_HEIGHT = "pageHeight";
    private static final String CHART_ELEMENT = "chartElement";
    private static final int CONSTANT_HEADER_HEIGHT = 54;
    private static final int FRAME_MIN_WIDTH = 820;
    public static final String ALL_BORDERS_OPAQUE_CENTER_BROWN = "AllBordersOpaqueCenterBrown";
    private static final String ALL_BORDERS_GREY_CENTER = "AllBordersGreyCenter";
    private static final int USAGE_HEADING_VALUE_MAX_SIZE = 35;
    private static final String LEFT_PADDED_HEADING_HTML = "LeftPaddedHeadingHtml";

    /**
     * Get the runtime document used as the transform parameter for a usage sub report.<br>
     * The usage sub report needs to know about all the distinct sets of values found in the database,
     * that the report will find when it runs. The same keys, services / operations, auth_user chosen by the user that
     * was used to create the set distinctMappingSets will be the same parameters given to the report at runtime. This
     * ensures that all the distinct lists of mapping values found in distinctMappingSets, will also be found by the
     * report when it runs
     *
     * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
     *                            that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                            followed by 5 mapping values
     * @return a Document which can be used as parameter to transform a template jrxml file
     */
    public static Document getUsageSubReportRuntimeDoc(LinkedHashSet<List<String>> distinctMappingSets) {

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if (distinctMappingValues == null || distinctMappingValues.isEmpty()) {
            distinctMappingValues = new LinkedHashSet<String>();
        }

        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();

        //Create variables element
        Element variables = doc.createElement(Utilities.VARIABLES);
        rootNode.appendChild(variables);

        for (int i = 0; i < numMappingValues; i++) {
            addVariableToElement(doc, variables, "COLUMN_" + (i + 1), "java.lang.Long", "None", null, "Nothing", "getColumnValue", "COLUMN_" + (i + 1));
        }

        Element serviceAndOperationFooterElement = doc.createElement(Utilities.SERVICE_AND_OPERATION_FOOTER);
        rootNode.appendChild(serviceAndOperationFooterElement);

        int xPos = 0;
        int yPos = 0;

        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-" + (i + 1), "java.lang.Long", "($V{COLUMN_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_" + (i + 1) + "}",
                    DEFAULT_CENTER_ALIGNED, false, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal", "java.lang.Long", "$V{TOTAL}",
                Utilities.ALL_BORDERS_GREY_CENTER, true, false);

        Element noDataElement = doc.createElement(Utilities.NO_DATA);
        rootNode.appendChild(noDataElement);

        xPos = 0;

        for (int i = 0; i < numMappingValues; i++) {
            addStaticTextToElement(doc, noDataElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "noDataStaticText-" + (i + 1), "NA", DEFAULT_CENTER_ALIGNED, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addStaticTextToElement(doc, noDataElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "noDataStaticText-Total", "NA", Utilities.ALL_BORDERS_GREY_CENTER, true);
        xPos += Utilities.TOTAL_COLUMN_WIDTH;

        //frame width is the same as page width for the subreport
        Element pageWidth = doc.createElement(Utilities.PAGE_WIDTH);
        rootNode.appendChild(pageWidth);
        pageWidth.setTextContent(String.valueOf(xPos));

        return doc;
    }

    /*
    * Get the runtime document used as the transform parameter for a usage interval master report.<br>
    * The usage sub report needs to know about all the distinct sets of values found in the database,
    * that the report will find when it runs. The same keys, services / operations, auth_user chosen by the user that
    * was used to create the set distinctMappingSets will be the same parameters given to the report at runtime. This
    * ensures that all the distinct lists of mapping values found in distinctMappingSets, will also be found by the
    * report when it runs
    *
    * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
    * that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
    * followed by 5 mapping values
    *
    * @return a Document which can be used as parameter to transform a template jrxml file
    * */
    public static Document getUsageSubIntervalMasterRuntimeDoc(LinkedHashSet<List<String>> distinctMappingSets) {

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if (distinctMappingValues == null || distinctMappingValues.isEmpty()) {
            distinctMappingValues = new LinkedHashSet<String>();
        }

        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();

        //Create variables element
        Element variables = doc.createElement(Utilities.VARIABLES);
        rootNode.appendChild(variables);

        //Create the COLUMN_X variables first
        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_1" class="java.lang.Long" resetType="Report" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_" + (i + 1), "java.lang.Long", "Report", null, "Sum");
        }

        //Subreport return values
        //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_1" calculation="Sum"/>
        Element subReport = doc.createElement(Utilities.SUB_REPORT);
        rootNode.appendChild(subReport);

        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_" + (i + 1), "COLUMN_" + (i + 1), "Sum");
        }

        //determine how wide the sub report should be
        int subReportWidth = 0;
        for (int i = 0; i < numMappingValues; i++) {
            subReportWidth += Utilities.DATA_COLUMN_WIDTH;
        }
        subReportWidth += Utilities.TOTAL_COLUMN_WIDTH;

        Element subReportWidthElement = doc.createElement(SUB_REPORT_WIDTH);
        rootNode.appendChild(subReportWidthElement);
        subReportWidthElement.setTextContent(String.valueOf(subReportWidth));

        int pageWidth = subReportWidth + Utilities.SUB_INTERVAL_STATIC_WIDTH;
        Element pageWidthElement = doc.createElement(Utilities.PAGE_WIDTH);
        rootNode.appendChild(pageWidthElement);
        pageWidthElement.setTextContent(String.valueOf(pageWidth));

        return doc;
    }

    /**
     * Only used in usage reports. In usage report output there is one table of data, with each column representing
     * a distinct mapping value set. The title of the table is the set of mapping keys, including AUTH_USER, which
     * was selected and what the columns of data represent the distinct sets of values for
     *
     * @param keysToFilters a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                      represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                      key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                      and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                      hash map.
     * @return a string which can be used to display as the heading for the table of data shown in usage reports
     */
    private static String getContextKeysDiaplayString(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters) {
        StringBuilder sb = new StringBuilder();

        List<String> keys = new ArrayList<String>();
        keys.addAll(keysToFilters.keySet());

        for (int i = 0; i < keys.size(); i++) {
            String s = keys.get(i);
            if (i != 0) {
                if (i != keys.size() - 1) sb.append(", ");
                else sb.append(" and ");
            }
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Get the runtime doc for performance statistics reports. Need to know what size to make the chart, create data
     * for it's legend and remove unnecessary chart elements
     *
     * @param isContextMapping    are mapping keys being used or not
     * @param groupToMappingValue a map of a shortened string to the string representing a set of mapping values
     *                            to display as the category value in a chart:- <br>
     *                            e.g. group 1 instead of IpAddress=...Customer=..., or service 1
     *                            instead of Warehouse [routing uri].....
     * @param isUsingKeys         are keys 1-5 or auth user being used? Used in conjunction with isContextMapping to tell
     *                            the report if context mapping is being used ONLY to get at operation level data.
     * @return a Document which can be used as parameter to transform a template jrxml file
     */
    public static Document getPerfStatAnyRuntimeDoc(boolean isContextMapping, boolean isUsingKeys,
                                                    LinkedHashMap<String, String> groupToMappingValue) {
        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        Node rootNode = doc.getFirstChild();
        //Create variables element
        Element isCtxMapElement = doc.createElement(IS_CONTEXT_MAPPING);
        rootNode.appendChild(isCtxMapElement);
        //the style sheet uses just this element to know whether to show the normal chart or the group chart
        //see the jrxml files which have two charts defined.
        if (isContextMapping && isUsingKeys) {
            isCtxMapElement.setTextContent("1");
        } else {
            isCtxMapElement.setTextContent("0");
        }

        //Create all the text fields for the chart legend
        Element chartLegend = doc.createElement(CHART_LEGEND);
        rootNode.appendChild(chartLegend);
        int x = 0;
        int y = 0;
        int vSpace = 2;
        int height = 18;
        int frameWidth = FRAME_MIN_WIDTH;

        int index = 0;
        for (Map.Entry<String, String> me : groupToMappingValue.entrySet()) {
            addTextFieldToElement(doc, chartLegend, x, y, frameWidth, height, "chartLegendKey" + (index + 1), "java.lang.String",
                    "<b>" + me.getKey() + ":</b> " + Utilities.escapeHtmlCharacters(me.getValue()), LEFT_PADDED_HEADING_HTML, false, true);

            y += height + vSpace;
            index++;
        }

        //Chart height is minimum 130, if there are more than 2 mapping value sets then increase it
        int chartHeight = 130;
        int numMappingSets = groupToMappingValue.size();
        if (numMappingSets > 2) {
            chartHeight += 30 * (numMappingSets - 2);
        }

        Element chartHeightElement = doc.createElement(CHART_HEIGHT);
        rootNode.appendChild(chartHeightElement);
        chartHeightElement.setTextContent(String.valueOf(chartHeight));

        //start of chart legend = chart height + 18 for the title of the chart frame
        int chartLegendFrameYPos = chartHeight;// + height;
        Element chartLegendYPosElement = doc.createElement(CHART_LEGEND_FRAME_YPOS);
        rootNode.appendChild(chartLegendYPosElement);
        chartLegendYPosElement.setTextContent(String.valueOf(chartLegendFrameYPos));

        //height of chart legend = num mapping sets * height + vSpace
        int chartLegendHeight = numMappingSets * (height + vSpace);
        Element chartLegendHeightElement = doc.createElement(CHART_LEGEND_HEIGHT);
        rootNode.appendChild(chartLegendHeightElement);
        chartLegendHeightElement.setTextContent(String.valueOf(chartLegendHeight));

        int chartFrameHeight = chartHeight + 18 + chartLegendHeight;
        Element chartFrameHeightElement = doc.createElement(CHART_FRAME_HEIGHT);
        rootNode.appendChild(chartFrameHeightElement);
        chartFrameHeightElement.setTextContent(String.valueOf(chartFrameHeight));

        //Calculate the height of the band
        int bandHeight = chartFrameHeight + height + height;//18 from the summary frame + 18 for a gap
        Element bandHeightElement = doc.createElement(BAND_HEIGHT);
        rootNode.appendChild(bandHeightElement);
        bandHeightElement.setTextContent(String.valueOf(bandHeight));

        int titleHeight = 186;
        int margins = 20 + 20;
        int totalFirstPageHeight = titleHeight + margins + bandHeight;
        int minPageHeight = 595;
        if (totalFirstPageHeight < minPageHeight) totalFirstPageHeight = minPageHeight;

        Element pageHeightElement = doc.createElement(PAGE_HEIGHT);
        rootNode.appendChild(pageHeightElement);
        pageHeightElement.setTextContent(String.valueOf(totalFirstPageHeight));

        return doc;
    }

    /**
     * Create a document, given the input properties, which will be used to transform the
     * template usage report.
     *
     * @param keysToFilters       a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                            represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                            key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                            and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                            hash map.
     * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
     *                            that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                            followed by 5 mapping values
     * @return a Document which can be used as parameter to transform a template jrxml file
     */
    public static Document getUsageIntervalMasterRuntimeDoc(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                            LinkedHashSet<List<String>> distinctMappingSets) {

        //is detail is not considered a valid key for usage queries
        Utilities.checkMappingQueryParams(keysToFilters, false, true);

        LinkedHashSet<String> mappingValuesLegend = getMappingLegendValues(keysToFilters, distinctMappingSets);
        /*
        * distinctMappingValues The set of distinct mapping values, which were determined earlier based on
        * the users selection of keys, key values, time and other constraints. Each string in the set is the
        * concatanated value of authenticated user, mapping1_value, mapping2_value, mapping3_value, mapping4_value
        * and mapping5_value.
        */
        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if (distinctMappingValues == null || distinctMappingValues.isEmpty()) {
            distinctMappingValues = new LinkedHashSet<String>();
        }

        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();
        //Create variables element
        Element variables = doc.createElement(Utilities.VARIABLES);
        rootNode.appendChild(variables);

        //Create the COLUMN_X variables first
        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_SERVICE_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_SERVICE_" + (i + 1), "java.lang.Long", "Group", "SERVICE", "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_OPERATION_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE_OPERATION" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_OPERATION_" + (i + 1), "java.lang.Long", "Group", "SERVICE_OPERATION", "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_REPORT_1" class="java.lang.Long" resetType="Report" calculation="Sum">
            addVariableToElement(doc, variables, "COLUMN_REPORT_" + (i + 1), "java.lang.Long", "Report", null, "Sum");
        }

        //serviceHeader
        Element serviceHeader = doc.createElement(Utilities.SERVICE_HEADER);
        rootNode.appendChild(serviceHeader);

        int xPos = Utilities.CONSTANT_HEADER_START_X;
        int yPos = 0;

        String keyDisplayValue = getContextKeysDiaplayString(keysToFilters);
        Element keyInfoElement = doc.createElement("keyInfo");
        rootNode.appendChild(keyInfoElement);
        CDATASection cData = doc.createCDATASection(keyDisplayValue);
        keyInfoElement.appendChild(cData);

        List<String> listMappingValues = new ArrayList<String>();
        listMappingValues.addAll(distinctMappingValues);

        //add a text field for each column
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, serviceHeader, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.MAPPING_VALUE_FIELD_HEIGHT,
                    "textField-serviceHeader-" + (i + 1), "java.lang.String", listMappingValues.get(i), Utilities.TOP_LEFT_BOTTOM_CENTER_BROWN,
                    true, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceHeader, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.MAPPING_VALUE_FIELD_HEIGHT,
                "textField-serviceHeader-ServiceTotals", "java.lang.String", "Service Totals", Utilities.ALL_BORDERS_OPAQUE_CENTER_BROWN,
                true, false);

        xPos += Utilities.TOTAL_COLUMN_WIDTH;

        int docTotalWidth = xPos + Utilities.LEFT_MARGIN_WIDTH + Utilities.RIGHT_MARGIN_WIDTH;
        int frameWidth = xPos;
        Element pageWidth = doc.createElement(Utilities.PAGE_WIDTH);
        pageWidth.setTextContent(String.valueOf(docTotalWidth));
        Element columnWidthElement = doc.createElement(Utilities.COLUMN_WIDTH);
        columnWidthElement.setTextContent(String.valueOf(frameWidth));
        Element frameWidthElement = doc.createElement(Utilities.FRAME_WIDTH);
        frameWidthElement.setTextContent(String.valueOf(frameWidth));

        //sub report variables
        Element subReport = doc.createElement(Utilities.SUB_REPORT);
        rootNode.appendChild(subReport);

        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_SERVICE_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_" + (i + 1), "COLUMN_SERVICE_" + (i + 1), "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_OPERATION_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_" + (i + 1), "COLUMN_OPERATION_" + (i + 1), "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_REPORT_1" calculation="Sum"/>
            addSubReportReturnVariable(doc, subReport, "COLUMN_" + (i + 1), "COLUMN_REPORT_" + (i + 1), "Sum");
        }

        //SERVICE_OPERATION footer
        Element serviceAndOperationFooterElement = doc.createElement(Utilities.SERVICE_AND_OPERATION_FOOTER);
        rootNode.appendChild(serviceAndOperationFooterElement);
        //todo [Donal] this is out by 5, 113 instead of 118, affects summary and master transforms, fix when reports completed
        xPos = Utilities.CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-" + (i + 1), "java.lang.Long", "($V{COLUMN_OPERATION_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_OPERATION_" + (i + 1) + "}",
                    Utilities.TOP_LEFT_BOTTOM_CENTER_GREY, true, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal", "java.lang.Long", "$V{ROW_OPERATION_TOTAL}",
                Utilities.ALL_BORDERS_GREY_CENTER, true, false);

        //serviceIdFooter
        Element serviceIdFooterElement = doc.createElement(Utilities.SERVICE_ID_FOOTER);
        rootNode.appendChild(serviceIdFooterElement);

        xPos = Utilities.CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "textField-ServiceIdFooter-" + (i + 1), "java.lang.Long", "($V{COLUMN_SERVICE_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_SERVICE_" + (i + 1) + "}",
                    Utilities.TOP_LEFT_BOTTOM_CENTER_GREY, true, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "textField-ServiceIdFooterTotal", "java.lang.Long", "$V{ROW_SERVICE_TOTAL}", Utilities.ALL_BORDERS_GREY_CENTER, true, false);

        //summary
        Element summaryElement = doc.createElement(Utilities.SUMMARY);
        rootNode.appendChild(summaryElement);

        xPos = Utilities.CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, summaryElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "textField-constantFooter-" + (i + 1), "java.lang.Long", "$V{COLUMN_REPORT_" + (i + 1) + "}",
                    Utilities.TOP_LEFT_GREY_CENTER, true, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, summaryElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "textField-constantFooterTotal", "java.lang.Long", "$V{ROW_REPORT_TOTAL}", Utilities.TOP_LEFT_RIGHT_GREY_CENTER, true, false);

        rootNode.appendChild(pageWidth);
        //columnWidth -is page width - left + right margin
        rootNode.appendChild(columnWidthElement);
        rootNode.appendChild(frameWidthElement);
        Element leftMarginElement = doc.createElement(Utilities.LEFT_MARGIN);
        leftMarginElement.setTextContent(String.valueOf(Utilities.LEFT_MARGIN_WIDTH));
        rootNode.appendChild(leftMarginElement);

        Element rightMarginElement = doc.createElement(Utilities.RIGHT_MARGIN);
        rightMarginElement.setTextContent(String.valueOf(Utilities.LEFT_MARGIN_WIDTH));
        rootNode.appendChild(rightMarginElement);

        LinkedHashMap<String, String> groupToLegendDisplayStringMap = getGroupToLegendDisplayStringMap(mappingValuesLegend);
        addChartXMLToDocument(doc, groupToLegendDisplayStringMap, frameWidth, 595);

        return doc;
    }

    /**
     * Only used by usage reports<br>
     * Convert each list in distinctMappingSets into a string representation of all its values.<br>
     * Each string in the linked hash set can then be used to represent a list from distinctMappingSets.<br>
     * <p/>
     * This function is used in the transform to know what string value to place into the column headings in the report
     * output. The returnd set also tells the runtime function, how many columns are needed, although it can also know
     * this information from the size of the distinctMappingSets parameter
     * <br>
     * Background: A Service has a context message assertion with a set of keys. At runtime the keys get values.
     * A query can specify keys, a set of services, and only those service which have that key will be found. The
     * service may have other keys too not in the query. The data is grouped by the distinct sets of value for the keys
     * which are found at runtime. This is what distinctMappingSets represents
     *
     * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
     *                            that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                            followed by 5 mapping values
     * @return a linked has set, where each string in it represents a string representation of the list at the
     *         corresponding index in distinctMappingSets
     */
    public static LinkedHashSet<String> getMappingValues(LinkedHashSet<List<String>> distinctMappingSets) {
        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();

        for (List<String> set : distinctMappingSets) {
            List<String> mappingStrings = new ArrayList<String>();
            boolean first = true;
            String authUser = null;
            for (String s : set) {
                if (first) {
                    authUser = s;
                    first = false;
                    continue;
                }
                mappingStrings.add(s);
            }
            String mappingValue = getMappingValueString(authUser, mappingStrings.toArray(new String[]{}));
            mappingValues.add(mappingValue);
        }

        return mappingValues;
    }

    /**
     * Only used by usage reports<br>
     * Convert the parameter mappingValuesLegend into a map, where each key in the map is a short textual representation
     * of the string at the same index in mappingValuesLegend. Currentlly the value at index 0 will have a key of
     * 'Group 1', the key at index 1 will have a key of 'Group 2'.<br>
     * <p/>
     * The returned map is used when creating the chart xml. The category's on the chart will be the short 'Group x'
     * values and the legend will show the link between 'Group x' and the string in index x in mappingValuesLegend
     *
     * @param mappingValuesLegend a linked hash set of the distinct mapping value sets, where each string, is a string
     *                            representation of all the values found for the keys at runtime. Note: A set of keys are needed to create the
     *                            data structure mappingValuesLegend, see usages for how this created.
     * @return a linked hash map with short key value mapped to the orginal value at the same index in mappingValuesLegend
     */
    private static LinkedHashMap<String, String> getGroupToLegendDisplayStringMap(LinkedHashSet<String> mappingValuesLegend) {
        LinkedHashMap<String, String> groupToDisplayString = new LinkedHashMap<String, String>();
        int index = 1;
        for (String s : mappingValuesLegend) {
            String group = "Group " + index;
            groupToDisplayString.put(group, s);
            index++;
        }

        return groupToDisplayString;
    }

    /**
     * Create a document, given the input properties, which will be used to transform the
     * template usage report.
     *
     * @param keysToFilters       a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                            represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                            key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                            and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                            hash map.
     * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
     *                            that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                            followed by 5 mapping values
     * @return a Document which can be used as parameter to transform a template jrxml file
     */
    public static Document getUsageRuntimeDoc(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                              LinkedHashSet<List<String>> distinctMappingSets) {

        //usage queires do not use isDetail to determine validity of parameters, it's not considered a key for usage reports
        Utilities.checkMappingQueryParams(keysToFilters, false, true);

        LinkedHashSet<String> mappingValuesLegend = getMappingLegendValues(keysToFilters, distinctMappingSets);

        /*
        * distinctMappingValues The set of distinct mapping values, which were determined earlier based on
        * the users selection of keys, key values, time and other constraints. Each string in the set is the
        * concatanated value of authenticated user, mapping1_value, mapping2_value, mapping3_value, mapping4_value
        * and mapping5_value.
        */

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if (distinctMappingValues == null || distinctMappingValues.isEmpty()) {
            distinctMappingValues = new LinkedHashSet<String>();
        }

        Document doc = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);

        int numMappingValues = distinctMappingValues.size();

        Node rootNode = doc.getFirstChild();
        //Create variables element
        Element variables = doc.createElement(Utilities.VARIABLES);
        rootNode.appendChild(variables);

        //Create the COLUMN_X variables first
        for (int i = 0; i < numMappingValues; i++) {
            addVariableToElement(doc, variables, "COLUMN_" + (i + 1), "java.lang.Long", "Group", "SERVICE_AND_OPERATION",
                    "Nothing", "getColumnValue", "COLUMN_" + (i + 1));
        }

        //then the COLUMN_MAPPING_TOTAL_X variables
        for (int i = 0; i < numMappingValues; i++) {
            addVariableToElement(doc, variables, "COLUMN_MAPPING_TOTAL_" + (i + 1), "java.lang.Long", "Group", "CONSTANT",
                    "Sum", "getVariableValue", "COLUMN_" + (i + 1));
        }

        //then the COLUMN_SERVICE_TOTAL_X variables
        for (int i = 0; i < numMappingValues; i++) {
            addVariableToElement(doc, variables, "COLUMN_SERVICE_TOTAL_" + (i + 1), "java.lang.Long", "Group", "SERVICE_ID",
                    "Sum", "getVariableValue", "COLUMN_" + (i + 1));
        }

        //create constantHeader element
        Element constantHeader = doc.createElement(Utilities.CONSTANT_HEADER);
        rootNode.appendChild(constantHeader);

        //Constant header starts at x = 113
        //The widths for the entire document are determined from this first header.
        //it has slightly different make up as it has an additional text field, however all other headers
        //should always work out to the be the same width. If they are wider, the report will not compile
        int xPos = Utilities.CONSTANT_HEADER_START_X;
        int yPos = 0;

        String keyDisplayValue = getContextKeysDiaplayString(keysToFilters);
        Element keyInfoElement = doc.createElement("keyInfo");
        rootNode.appendChild(keyInfoElement);
        CDATASection cData = doc.createCDATASection(keyDisplayValue);
        keyInfoElement.appendChild(cData);

        List<String> listMappingValues = new ArrayList<String>();
        listMappingValues.addAll(distinctMappingValues);

        //add a text field for each column
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, constantHeader, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.MAPPING_VALUE_FIELD_HEIGHT,
                    "textField-constantHeader-" + (i + 1), "java.lang.String", listMappingValues.get(i), Utilities.TOP_LEFT_BOTTOM_CENTER_BROWN,
                    false, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }
        //move x pos along for width of a column

        addTextFieldToElement(doc, constantHeader, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.MAPPING_VALUE_FIELD_HEIGHT,
                "textField-constantHeader-ServiceTotals", "java.lang.String", "Service Totals", ALL_BORDERS_OPAQUE_CENTER_BROWN,
                false, false);

        xPos += Utilities.TOTAL_COLUMN_WIDTH;

        int docTotalWidth = xPos + Utilities.LEFT_MARGIN_WIDTH + Utilities.RIGHT_MARGIN_WIDTH;
        int frameWidth = xPos;
        Element pageWidth = doc.createElement(Utilities.PAGE_WIDTH);
        pageWidth.setTextContent(String.valueOf(docTotalWidth));
        Element columnWidthElement = doc.createElement(Utilities.COLUMN_WIDTH);
        columnWidthElement.setTextContent(String.valueOf(frameWidth));
        Element frameWidthElement = doc.createElement(Utilities.FRAME_WIDTH);
        frameWidthElement.setTextContent(String.valueOf(frameWidth));

        //serviceAndOperationFooter
        Element serviceAndOperationFooterElement = doc.createElement(Utilities.SERVICE_AND_OPERATION_FOOTER);
        rootNode.appendChild(serviceAndOperationFooterElement);
        xPos = Utilities.CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-" + (i + 1), "java.lang.Long", "($V{COLUMN_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_" + (i + 1) + "}",
                    DEFAULT_CENTER_ALIGNED, false, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceAndOperationFooterElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal", "java.lang.Long", "$V{SERVICE_AND_OR_OPERATION_TOTAL}",
                ALL_BORDERS_GREY_CENTER, true, false);

        //serviceIdFooter
        Element serviceIdFooterElement = doc.createElement(Utilities.SERVICE_ID_FOOTER);
        rootNode.appendChild(serviceIdFooterElement);

        xPos = Utilities.CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "textField-ServiceIdFooter-" + (i + 1), "java.lang.Long", "($V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "} == null || $V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}.intValue() == 0)?new Long(0):$V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}",
                    Utilities.TOP_LEFT_BOTTOM_CENTER_GREY, true, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, serviceIdFooterElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "textField-ServiceIdFooterTotal", "java.lang.Long", "$V{SERVICE_ONLY_TOTAL}", Utilities.ALL_BORDERS_GREY_CENTER, true, false);

        //constantFooter
        Element constantFooterElement = doc.createElement(Utilities.CONSTANT_FOOTER);
        rootNode.appendChild(constantFooterElement);

        xPos = Utilities.CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            addTextFieldToElement(doc, constantFooterElement, xPos, yPos, Utilities.DATA_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                    "textField-constantFooter-" + (i + 1), "java.lang.Long", "$V{COLUMN_MAPPING_TOTAL_" + (i + 1) + "}",
                    Utilities.TOP_LEFT_GREY_CENTER, true, false);
            xPos += Utilities.DATA_COLUMN_WIDTH;
        }

        addTextFieldToElement(doc, constantFooterElement, xPos, yPos, Utilities.TOTAL_COLUMN_WIDTH, Utilities.FIELD_HEIGHT,
                "textField-constantFooterTotal", "java.lang.Long", "$V{GRAND_TOTAL}", Utilities.TOP_LEFT_RIGHT_GREY_CENTER, true, false);


        rootNode.appendChild(pageWidth);
        //columnWidth -is page width - left + right margin
        rootNode.appendChild(columnWidthElement);
        rootNode.appendChild(frameWidthElement);
        Element leftMarginElement = doc.createElement(Utilities.LEFT_MARGIN);
        leftMarginElement.setTextContent(String.valueOf(Utilities.LEFT_MARGIN_WIDTH));
        rootNode.appendChild(leftMarginElement);

        Element rightMarginElement = doc.createElement(Utilities.RIGHT_MARGIN);
        rightMarginElement.setTextContent(String.valueOf(Utilities.LEFT_MARGIN_WIDTH));
        rootNode.appendChild(rightMarginElement);

        LinkedHashMap<String, String> groupToLegendDisplayStringMap = getGroupToLegendDisplayStringMap(mappingValuesLegend);
        addChartXMLToDocument(doc, groupToLegendDisplayStringMap, frameWidth, 595);
        return doc;
    }

    /**
     * This implementation is used primiarly from within reports and also from other Utility functions.
     * As the report is the main use, the make up of distinctMappingSets is fixed, as the report will pump in field
     * values into some method will which give it the required LinkedHashSet<List<java.lang.String>>
     * Auth User is always the first element of each list in distinctMappingSets
     *
     * @param keysToFilters       a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                            represent it's constraints. All keys should have at least one FilterPair supplied. If no constraint was added for a
     *                            key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                            and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                            hash map.
     * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
     *                            that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                            followed by 5 mapping values
     * @return
     */
    public static LinkedHashSet<String> getMappingLegendValues(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                               LinkedHashSet<List<String>> distinctMappingSets) {
        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();

        for (List<String> set : distinctMappingSets) {
            String[] mappingStringsArray = new String[Utilities.NUM_MAPPING_KEYS];
            boolean first = true;
            String authUser = null;
            int index = 0;
            for (String s : set) {
                if (first) {
                    authUser = s;
                    first = false;
                    continue;
                }
                mappingStringsArray[index++] = s;
            }
            //fill out array with placeholders
            for (int i = index; i < mappingStringsArray.length; i++) {
                mappingStringsArray[i] = Utilities.SQL_PLACE_HOLDER;
            }
            String mappingValue = Utilities.getMappingValueDisplayString(keysToFilters,
                    authUser, mappingStringsArray, false, null);
            mappingValues.add(mappingValue);
        }

        return mappingValues;
    }

    /**
     * Add the dynamic elements of the chart xml to the supplied document, which is used to transform template jrxml
     * files. The groupToMappingValue is used for creating the legend jrxml elements.
     *
     * @param doc                 the document to update
     * @param groupToMappingValue represents the distinct mapping sets for the keys in the query. The key is a shorted
     *                            version of the string it maps to. This information is displayed in a legend element created by this function
     * @param frameWidth          the frame width in the jrxml file which will contain the chart
     * @param minPageHeight       the minimum page height of the report which will be created using the updated document
     *                            passed into this function as it's transform parameter
     */
    private static void addChartXMLToDocument(Document doc, LinkedHashMap<String, String> groupToMappingValue,
                                              int frameWidth, int minPageHeight) {
        //Create all the text fields for the chart legend
        Node rootNode = doc.getFirstChild();
        Element chartElement = doc.createElement(CHART_ELEMENT);
        rootNode.appendChild(chartElement);

        Element chartLegend = doc.createElement(CHART_LEGEND);
        chartElement.appendChild(chartLegend);
        int x = 0;
        int y = 0;
        int vSpace = 2;
        int height = 18;

        int index = 0;
        if (frameWidth < FRAME_MIN_WIDTH) frameWidth = FRAME_MIN_WIDTH;
        for (Map.Entry<String, String> me : groupToMappingValue.entrySet()) {

            addTextFieldToElement(doc, chartLegend, x, y, frameWidth, height, "chartLegendKey" + (index + 1), "java.lang.String",
                    "<b>" + me.getKey() + ":</b> " + Utilities.escapeHtmlCharacters(me.getValue()), LEFT_PADDED_HEADING_HTML, false, true);

            y += height + vSpace;
            index++;
        }

        //Chart height is minimum 130, if there are more than 2 mapping value sets then increase it
        int chartHeight = 130;
        int numMappingSets = groupToMappingValue.size();
        if (numMappingSets > 2) {
            chartHeight += 30 * (numMappingSets - 2);
        }

        Element chartHeightElement = doc.createElement(CHART_HEIGHT);
        chartElement.appendChild(chartHeightElement);
        chartHeightElement.setTextContent(String.valueOf(chartHeight));

        //start of chart legend = chart height + 18 for the title of the chart frame
        int chartLegendFrameYPos = chartHeight;// + height;
        Element chartLegendYPosElement = doc.createElement(CHART_LEGEND_FRAME_YPOS);
        chartElement.appendChild(chartLegendYPosElement);
        chartLegendYPosElement.setTextContent(String.valueOf(chartLegendFrameYPos));

        //height of chart legend = num mapping sets * height + vSpace
        int chartLegendHeight = numMappingSets * (height + vSpace);
        Element chartLegendHeightElement = doc.createElement(CHART_LEGEND_HEIGHT);
        chartElement.appendChild(chartLegendHeightElement);
        chartLegendHeightElement.setTextContent(String.valueOf(chartLegendHeight));

        int chartFrameHeight = chartHeight + 18 + chartLegendHeight;
        Element chartFrameHeightElement = doc.createElement(CHART_FRAME_HEIGHT);
        chartElement.appendChild(chartFrameHeightElement);
        chartFrameHeightElement.setTextContent(String.valueOf(chartFrameHeight));

        //Calculate the height of the band
        int bandHeight = chartFrameHeight + height + height;//18 from the summary frame + 18 for a gap
        Element bandHeightElement = doc.createElement(BAND_HEIGHT);
        chartElement.appendChild(bandHeightElement);
        bandHeightElement.setTextContent(String.valueOf(bandHeight));

        int titleHeight = 243;
        int margins = 20 + 20;
        int totalFirstPageHeight = titleHeight + margins + bandHeight + CONSTANT_HEADER_HEIGHT;
        if (totalFirstPageHeight < minPageHeight) totalFirstPageHeight = minPageHeight;

        Element pageHeightElement = doc.createElement(PAGE_HEIGHT);
        chartElement.appendChild(pageHeightElement);
        pageHeightElement.setTextContent(String.valueOf(totalFirstPageHeight));

    }

    /**
     * Only used in usage reports. This function is used indirectly in reports via UsageSummaryAndSubReportHelper<br>
     * and also in getMapingQuery in this class<br>
     * Create a string representation of the auth user and all the mapping values to be displayed to an end user.<br>
     * Place holder values are just ignored. The returned string is formatted such that each
     * value is on it's own row, all values are separated by a line break. The returned string is used as column
     * headings but it's used in the report helper as a key into a map of distinct mapping value set to a variable
     * name. See the report scriptlet to understand this fully.
     *
     * @param authUser      string representation of the AUTH_USER
     * @param mappingValues String array of all mapping key values found at runtime.
     * @return a string representation of all parameters
     */
    public static String getMappingValueString(String authUser, String[] mappingValues) {
        StringBuilder sb = new StringBuilder();

        boolean first = true;
        if (!authUser.equals(Utilities.SQL_PLACE_HOLDER)) {
            sb.append(TextUtils.truncStringMiddleExact(authUser, USAGE_HEADING_VALUE_MAX_SIZE));
            first = false;
        }

        for (String s : mappingValues) {
            if (!first) {
                if (!s.equals(Utilities.SQL_PLACE_HOLDER)) sb.append("\\n");
            }
            first = false;
            if (!s.equals(Utilities.SQL_PLACE_HOLDER))
                sb.append(TextUtils.truncStringMiddleExact(s, USAGE_HEADING_VALUE_MAX_SIZE));
        }

        return sb.toString();
    }

    /**
     * Add a text field jrxml element to the supplied document. The xml created will look like:
     * <pre>
     * &lt textField isStretchWithOverflow="true" isBlankWhenNull="false" evaluationTime="Now"
     * hyperlinkType="None" hyperlinkTarget="Self" &gt
     * &lt reportElement
     * x="50"
     * y="0"
     * width="68"
     * height="36"
     * key="textField-MappingKeys"/ &gt  <br>
     * &lt box> &lt /box &gt <br>
     * &lt textElement markup="html" &gt <br>
     * &lt font/ &gt <br>
     * &lt /textElement &gt <br>
     * &lt textFieldExpression class="java.lang.String" &gt <br>
     * &lt ![CDATA["IP_ADDRESS<br>CUSTOMER"]] &gt &lt /textFieldExpression &gt <br>
     * &lt /textField &gt
     * </pre>
     *
     * @param doc                      the document which is used to create new elements from
     * @param frameElement             the element from the doc to update
     * @param x                        x value
     * @param y                        y value
     * @param width                    width
     * @param height                   height
     * @param key                      the text field key value
     * @param textFieldExpressionClass what type of data the text field will hold - String, Integer etc...
     * @param markedUpCData            if data is to be included, then it's put inside a CDATA section to avoid any illegal chars
     * @param style                    the style to apply to the text field
     * @param isHtmlFormatted
     */
    private static void addTextFieldToElement(Document doc, Element frameElement, int x, int y, int width, int height,
                                              String key, String textFieldExpressionClass, String markedUpCData,
                                              String style, boolean opaque, boolean isHtmlFormatted) {
        Element textField = doc.createElement("textField");
        textField.setAttribute("isStretchWithOverflow", "true");
        textField.setAttribute("isBlankWhenNull", "false");
        textField.setAttribute("evaluationTime", "Now");
        textField.setAttribute("hyperlinkType", "None");
        textField.setAttribute("hyperlinkTarget", "Self");

        Element reportElement = doc.createElement("reportElement");
        reportElement.setAttribute("x", String.valueOf(x));
        reportElement.setAttribute("y", String.valueOf(y));
        reportElement.setAttribute("width", String.valueOf(width));
        reportElement.setAttribute("height", String.valueOf(height));
        reportElement.setAttribute("key", key);
        reportElement.setAttribute("style", style);
        if (opaque) reportElement.setAttribute("mode", "Opaque");

        textField.appendChild(reportElement);

        Element boxElement = doc.createElement("box");
        textField.appendChild(boxElement);

        Element textElement = doc.createElement("textElement");
        if (isHtmlFormatted) textElement.setAttribute("markup", "html");

        Element fontElement = doc.createElement("font");
        textElement.appendChild(fontElement);
        textField.appendChild(textElement);

        Element textFieldExpressionElement = doc.createElement("textFieldExpression");
        textFieldExpressionElement.setAttribute("class", textFieldExpressionClass);

        CDATASection cDataSection;
        if (textFieldExpressionClass.equals("java.lang.String")) {
            cDataSection = doc.createCDATASection("\"" + markedUpCData + "\"");
        } else {
            cDataSection = doc.createCDATASection(markedUpCData);
        }

        textFieldExpressionElement.appendChild(cDataSection);
        textField.appendChild(textFieldExpressionElement);

        frameElement.appendChild(textField);
    }

    /**
     * Add a static text jrxml element to the supplied document
     * <staticText>
     * <reportElement
     * x="0"
     * y="0"
     * width="50"
     * height="17"
     * key="staticText-1"/>
     * <box></box>
     * <textElement>
     * <font/>
     * </textElement>
     * <text><![CDATA[NA]]></text>
     * </staticText>
     *
     * @param doc           the document which is used to create new elements from
     * @param frameElement  the element from the doc to update
     * @param x             x value
     * @param y             y value
     * @param width         width
     * @param height        height
     * @param key           the text field key value
     * @param markedUpCData if data is to be included, then it's put inside a CDATA section to avoid any illegal chars
     * @param style         the style to apply to the text field
     * @param opaque        if true the text field is not see through, if false the style of the element behind it will come
     *                      through
     */
    private static void addStaticTextToElement(Document doc, Element frameElement, int x, int y, int width, int height,
                                               String key, String markedUpCData, String style, boolean opaque) {
        Element staticText = doc.createElement("staticText");

        Element reportElement = doc.createElement("reportElement");
        reportElement.setAttribute("x", String.valueOf(x));
        reportElement.setAttribute("y", String.valueOf(y));
        reportElement.setAttribute("width", String.valueOf(width));
        reportElement.setAttribute("height", String.valueOf(height));
        reportElement.setAttribute("key", key);
        reportElement.setAttribute("style", style);
        if (opaque) reportElement.setAttribute("mode", "Opaque");
        staticText.appendChild(reportElement);

        Element boxElement = doc.createElement("box");
        staticText.appendChild(boxElement);

        Element textElement = doc.createElement("textElement");
        textElement.setAttribute("markup", "html");
        Element fontElement = doc.createElement("font");
        textElement.appendChild(fontElement);
        staticText.appendChild(textElement);

        Element text = doc.createElement("text");
        CDATASection cDataSection = doc.createCDATASection(markedUpCData);
        text.appendChild(cDataSection);
        staticText.appendChild(text);
        frameElement.appendChild(staticText);
    }

    /**
     * Create a variable and add it to the supplied document
     * <variable name="COLUMN_1_MAPPING_TOTAL" class="java.lang.Long" resetType="Group" resetGroup="CONSTANT"
     * calculation="Sum">
     * <variableExpression><![CDATA[((UsageReportHelper)$P{REPORT_SCRIPTLET})
     * .getVariableValue("COLUMN_1", $F{AUTHENTICATED_USER},
     * new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3},
     * $F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})]]></variableExpression>
     * </variable>
     *
     * @param doc          the document which is used to create new elements from
     * @param variables    the element from the doc to update
     * @param varName      the variable name
     * @param varClass     the java type the variable is
     * @param resetType    when the variable gets reset
     * @param resetGroup   if the resetType is 'Group', then which group resets the variable
     * @param calc         what calculation the variable performs
     * @param functionName the function name within the report scriptlet which the variable will call
     * @param columnName   parameter to the function which will be called at runtime when this variable is being
     *                     evaluated. The column name is used within the report scriptlet to look up the correct value for this variable
     */
    private static void addVariableToElement(Document doc, Element variables, String varName, String varClass,
                                             String resetType, String resetGroup, String calc, String functionName,
                                             String columnName) {
        Element newVariable = doc.createElement(Utilities.VARIABLE);
        newVariable.setAttribute("name", varName);
        newVariable.setAttribute("class", varClass);
        newVariable.setAttribute("resetType", resetType);
        if (resetGroup != null && !resetGroup.equals("")) newVariable.setAttribute("resetGroup", resetGroup);
        newVariable.setAttribute("calculation", calc);

        Element variableExpression = doc.createElement("variableExpression");
        String cData = "((UsageSummaryAndSubReportHelper)$P{REPORT_SCRIPTLET})." + functionName + "(\"" + columnName + "\"," +
                " $F{AUTHENTICATED_USER},new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3}," +
                "$F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})";
        CDATASection cDataSection = doc.createCDATASection(cData);
        variableExpression.appendChild(cDataSection);

        newVariable.appendChild(variableExpression);
        variables.appendChild(newVariable);

    }

    /**
     * Add an element to the supplied Element variables
     * <variable name="COLUMN_SERVICE_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE" calculation="Sum">
     *
     * @param doc        the document which is used to create new elements from
     * @param variables  the element to add the variable to
     * @param varName    variable name
     * @param varClass   variable java class type
     * @param resetType  when the variable gets reset
     * @param resetGroup if the resetType is 'Group', then which group resets the variable
     * @param calc       what calculation the variable performs
     */
    private static void addVariableToElement(Document doc, Element variables, String varName, String varClass,
                                             String resetType, String resetGroup, String calc) {
        Element newVariable = doc.createElement(Utilities.VARIABLE);
        newVariable.setAttribute("name", varName);
        newVariable.setAttribute("class", varClass);
        newVariable.setAttribute("resetType", resetType);
        if (resetGroup != null && !resetGroup.equals("")) newVariable.setAttribute("resetGroup", resetGroup);
        newVariable.setAttribute("calculation", calc);
        variables.appendChild(newVariable);

    }

    /**
     * Add a sub report return variable to the jrxml
     * <returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_SERVICE_1" calculation="Sum"/>
     *
     * @param doc               the document which is used to create new elements from
     * @param subReport         the element to add the new element to
     * @param subreportVariable the sub report variable name
     * @param toVariable        the variabel name of the report this element will belong to, into which the sub report value
     *                          will be placed
     * @param calc              what calculation the variable performs
     */
    private static void addSubReportReturnVariable(Document doc, Element subReport, String subreportVariable,
                                                   String toVariable, String calc) {
        Element newVariable = doc.createElement(Utilities.RETURN_VALUE);
        newVariable.setAttribute("subreportVariable", subreportVariable);
        newVariable.setAttribute("toVariable", toVariable);
        newVariable.setAttribute("calculation", calc);
        subReport.appendChild(newVariable);
    }

    /**
     * The linked hash map this function creates is used by the usage sub reports. The usage sub reports have the
     * complicated requirement of pretending that data formatted with one row per distinct set of mapping values
     * is actually one row for <em>every</em> distinct set of mapping values.
     * <br>
     * To understand this first look at the output from a usage report. Notice how the data is column based, one
     * column per distinct set of mapping values. The report thinks the data comes back from the database in this
     * format. Behind the smoke and mirrors is the linked hash map created here.<br>
     * The linked hash map allows the report scriptlet to find the column name for a distinct set of mapping values.
     *
     * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
     *                            that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                            followed by 5 mapping values
     * @return a linked hash map of a string the report can make itself at runtime a virtual column name. This way
     *         the usage report helper can known when a row of data is for a particular column
     */
    public static LinkedHashMap<String, String> getKeyToColumnValues(LinkedHashSet<List<String>> distinctMappingSets) {
        LinkedHashSet<String> mappingValues = getMappingValues(distinctMappingSets);
        LinkedHashMap<String, String> keyToColumnName = new LinkedHashMap<String, String>();
        int count = 1;
        for (String s : mappingValues) {
            keyToColumnName.put(s, "COLUMN_" + count);
            count++;
        }
        return keyToColumnName;
    }
}
