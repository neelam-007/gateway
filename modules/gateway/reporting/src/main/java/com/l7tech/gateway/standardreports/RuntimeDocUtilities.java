/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Dec 16, 2008
 * Time: 3:47:14 PM
 */
package com.l7tech.gateway.standardreports;

import java.util.*;

import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.TextUtils;
import com.l7tech.util.Pair;

public class RuntimeDocUtilities {
    static final String DEFAULT_CENTER_ALIGNED = "DefaultCenterAligned";
    private static final int CONSTANT_HEADER_HEIGHT = 54;
    private static final int USAGE_CHART_STATIC_WIDTH = 820;
    private static final int FRAME_MIN_WIDTH = 820;
    static final String ALL_BORDERS_OPAQUE_CENTER_BROWN = "AllBordersOpaqueCenterBrown";
    static final String ALL_BORDERS_GREY_CENTER = "AllBordersGreyCenter";
    private static final String LEFT_PADDED_HEADING_HTML = "LeftPaddedHeadingHtml";
    static final int FIELD_HEIGHT = 18;
    static final int RIGHT_MARGIN_WIDTH = 15;
    static final int LEFT_MARGIN_WIDTH = RIGHT_MARGIN_WIDTH;
    static final int DATA_COLUMN_WIDTH = 160;
    static final int SUB_INTERVAL_STATIC_WIDTH = 113;
    static final int MAPPING_VALUE_FIELD_HEIGHT = 36;
    //service text field is 5 from left margin
    static final int CONSTANT_HEADER_START_X = 113;
    static final String TOP_LEFT_BOTTOM_CENTER_GREY = "TopLeftBottomCenterGrey";
    static final String TOP_LEFT_GREY_CENTER = "TopLeftGreyCenter";
    static final String TOP_LEFT_RIGHT_GREY_CENTER = "TopLeftRightGreyCenter";
    static final String TOP_LEFT_BOTTOM_CENTER_BROWN = "TopLeftBottomCenterBrown";

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
     * @return a JasperDocument encapsulating a Document, which can be used as parameter to transform a template jrxml file
     */
    public static JasperDocument getUsageSubReportRuntimeDoc(LinkedHashSet<List<String>> distinctMappingSets) {

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if (distinctMappingValues == null || distinctMappingValues.isEmpty()) {
            distinctMappingValues = new LinkedHashSet<String>();
        }

        JasperDocument jasperDoc = new JasperDocument();
        int numMappingValues = distinctMappingValues.size();

        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_" + (i + 1), "java.lang.Long", "None", null, "Nothing", "getColumnValue", "COLUMN_" + (i + 1));
        }

        int xPos = 0;
        int yPos = 0;

        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER, xPos, yPos,
                    DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-" + (i + 1),
                    new TertiaryJavaLongExpression(TertiaryJavaLongExpression.TERNARY_STRING_VARIABLE.COLUMN_, i + 1),
                    DEFAULT_CENTER_ALIGNED, false, false, false, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal",
                new SimpleJavaLongExpression(SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.TOTAL),
                ALL_BORDERS_GREY_CENTER, true, false, false, false);

        xPos = 0;

        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addStaticTextToElement(JasperDocument.ElementName.NO_DATA, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "noDataStaticText-" + (i + 1), "NA", DEFAULT_CENTER_ALIGNED, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addStaticTextToElement(JasperDocument.ElementName.NO_DATA, xPos, yPos, JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "noDataStaticText-Total", "NA", ALL_BORDERS_GREY_CENTER, true);
        xPos += JasperDocument.TOTAL_COLUMN_WIDTH;

        //frame width is the same as page width for the subreport
        jasperDoc.addIntElement(JasperDocument.ElementName.PAGE_WIDTH, xPos);
        return jasperDoc;
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
    * @return a JasperDocument encapsulating a Document, which can be used as parameter to transform a template jrxml file
    * */
    public static JasperDocument getUsageSubIntervalMasterRuntimeDoc(LinkedHashSet<List<String>> distinctMappingSets) {

        LinkedHashSet<String> distinctMappingValues = getMappingValues(distinctMappingSets);

        if (distinctMappingValues == null || distinctMappingValues.isEmpty()) {
            distinctMappingValues = new LinkedHashSet<String>();
        }

        JasperDocument jasperDoc = new JasperDocument();
        int numMappingValues = distinctMappingValues.size();

        //Create the COLUMN_X variables first
        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_1" class="java.lang.Long" resetType="Report" calculation="Sum">
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_" + (i + 1), "java.lang.Long", "Report", null, "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_1" calculation="Sum"/>
            jasperDoc.addSubReportReturnVariable("COLUMN_" + (i + 1), "COLUMN_" + (i + 1), "Sum");
        }

        //determine how wide the sub report should be
        int subReportWidth = 0;
        for (int i = 0; i < numMappingValues; i++) {
            subReportWidth += DATA_COLUMN_WIDTH;
        }
        subReportWidth += JasperDocument.TOTAL_COLUMN_WIDTH;

        jasperDoc.addIntElement(JasperDocument.ElementName.SUB_REPORT_WIDTH, subReportWidth);

        int pageWidth = subReportWidth + SUB_INTERVAL_STATIC_WIDTH;
        jasperDoc.addIntElement(JasperDocument.ElementName.PAGE_WIDTH, pageWidth);

        return jasperDoc;
    }

    /**
     * Only used in usage reports. In usage report output there is one table of data, with each column representing
     * a distinct mapping value set. The title of the table is the set of mapping keys, including AUTH_USER, which
     * was selected and what the columns of data represent the distinct sets of values for
     *
     * @param keysToFilters       a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                            represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                            key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                            and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                            hash map.
     * @param truncateKeyMaxValue max value for the key value to be displayed
     * @return a string which can be used to display as the heading for the table of data shown in usage reports
     */
    private static String getContextKeysDiaplayString(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters, int truncateKeyMaxValue) {
        StringBuilder sb = new StringBuilder();

        List<String> keys = new ArrayList<String>();
        keys.addAll(keysToFilters.keySet());

        for (int i = 0; i < keys.size(); i++) {
            String mappingKey = keys.get(i);
            if (i != 0) {
                if (i != keys.size() - 1) sb.append(", ");
                else sb.append(" and ");
            }
            sb.append(TextUtils.truncStringMiddleExact(mappingKey, truncateKeyMaxValue));
        }
        return sb.toString();
    }

    private static void addPerfChartXmlMapping(JasperDocument jasperDoc, Map<String, String> displayMap) {
        int y = 0;
        int vSpace = 2;
        int height = 18;
        int frameWidth = FRAME_MIN_WIDTH;
        int index = 0;

        for (Map.Entry<String, String> me : displayMap.entrySet()) {
            String displayValue = "<b>" + Utilities.escapeHtmlCharacters(me.getKey()) + ":</b> " +
                    Utilities.escapeHtmlCharacters(me.getValue()) + "<br>";
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.CHART_LEGEND, 0, y, frameWidth, height,
                    "chartLegendKey" + (index + 1), displayValue,
                    LEFT_PADDED_HEADING_HTML, false, true, true, false);

            y += height + vSpace;
            index++;
        }

        addPerfChartXml(jasperDoc, height, vSpace, displayMap.size());
    }

    private static void addPerfChartXmlNoMapping(JasperDocument jasperDoc, Map<String, Pair<String, String>> displayMap) {
        //Create all the text fields for the chart legend
        int y = 0;
        int vSpace = 2;
        int height = 18;
        int frameWidth = FRAME_MIN_WIDTH;
        int index = 0;

        for (Map.Entry<String, Pair<String, String>> me : displayMap.entrySet()) {
            Pair<String, String> serviceUriPair = me.getValue();
            String displayValue = "<b>" + Utilities.escapeHtmlCharacters(me.getKey()) + ":</b> " +
                    Utilities.escapeHtmlCharacters(serviceUriPair.getKey()) + " [" +
                    Utilities.escapeHtmlCharacters(serviceUriPair.getValue()) + "]<br>";

            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.CHART_LEGEND, 0, y, frameWidth, height,
                    "chartLegendKey" + (index + 1), displayValue,
                    LEFT_PADDED_HEADING_HTML, false, true, true, false);

            y += height + vSpace;
            index++;
        }

        addPerfChartXml(jasperDoc, height, vSpace, displayMap.size());
    }

    private static void addPerfChartXml(JasperDocument jasperDoc, int height, int vSpace, int numMappingSets) {
        //Chart height is minimum 130, if there are more than 2 mapping value sets then increase it
        int chartHeight = 130;
        if (numMappingSets > 2) {
            chartHeight += 30 * (numMappingSets - 2);
        }

        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_HEIGHT, chartHeight);

        //start of chart legend = chart height + 18 for the title of the chart frame
        int chartLegendFrameYPos = chartHeight;// + height;
        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_LEGEND_FRAME_YPOS, chartLegendFrameYPos);

        //height of chart legend = num mapping sets * height + vSpace
        int chartLegendHeight = numMappingSets * (height + vSpace);
        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_LEGEND_HEIGHT, chartLegendHeight);

        int chartFrameHeight = chartHeight + 18 + chartLegendHeight;
        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_FRAME_HEIGHT, chartFrameHeight);

        //Calculate the height of the band
        int bandHeight = chartFrameHeight + height + height;//18 from the summary frame + 18 for a gap
        jasperDoc.addIntElement(JasperDocument.ElementName.BAND_HEIGHT, bandHeight);

        int titleHeight = 186;
        int margins = 20 + 20;
        int totalFirstPageHeight = titleHeight + margins + bandHeight;
        int minPageHeight = 595;
        if (totalFirstPageHeight < minPageHeight) totalFirstPageHeight = minPageHeight;

        jasperDoc.addIntElement(JasperDocument.ElementName.PAGE_HEIGHT, totalFirstPageHeight);
    }

    /**
     * Get the runtime doc for performance statistics reports. Need to know what size to make the chart, create data
     * for it's legend and remove unnecessary chart elements
     *
     * @param groupToMappingValue a map of a shortened string to the string representing a set of mapping values
     *                            to display as the category value in a chart:- <br>
     *                            e.g. group 1 instead of IpAddress=...Customer=..., or service 1
     *                            instead of Warehouse [routing uri].....
     * @return a JasperDocument encapsulating a Document, which can be used as parameter to transform a template jrxml file
     */
    public static JasperDocument getPerfStatAnyRuntimeDoc(LinkedHashMap<String, Pair<String, String>> groupToMappingValue) {
        JasperDocument jasperDoc = new JasperDocument();

        jasperDoc.addIntElement(JasperDocument.ElementName.IS_CONTEXT_MAPPING, 0);
        //the style sheet uses just this element to know whether to show the normal chart or the group chart
        //see the jrxml files which have two charts defined.

        addPerfChartXmlNoMapping(jasperDoc, groupToMappingValue);

        return jasperDoc;
    }

    /**
     * Get the runtime doc for performance statistics reports. Need to know what size to make the chart, create data
     * for it's legend and remove unnecessary chart elements
     * <p/>
     * the report if context mapping is being used ONLY to get at operation level data.
     *
     * @param keysToFilters       a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                            represent it's constraints. All keys should have at least one FilterPair supplied. If no constrain was added for a
     *                            key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                            and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                            hash map.
     * @param distinctMappingSets represents the runtime report meta data, which are the distinct set of mapping values
     *                            that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                            followed by 5 mapping values
     * @return a JasperDocument encapsulating a Document, which can be used as parameter to transform a template jrxml file
     */
    public static JasperDocument getPerfStatAnyRuntimeDoc(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                          LinkedHashSet<List<String>> distinctMappingSets) {
        JasperDocument jasperDoc = new JasperDocument();

        jasperDoc.addIntElement(JasperDocument.ElementName.IS_CONTEXT_MAPPING, 1);
        //the style sheet uses just this element to know whether to show the normal chart or the group chart
        //see the jrxml files which have two charts defined.

        Utilities.checkMappingQueryParams(keysToFilters, false, false);
        LinkedHashSet<String> mappingValuesLegend = getMappingLegendValues(keysToFilters, distinctMappingSets,
                true, Utilities.MAPPING_KEY_MAX_SIZE, Utilities.USAGE_HEADING_VALUE_MAX_SIZE);
        LinkedHashMap<String, String> groupToLegendDisplayStringMap = getGroupToLegendDisplayStringMap(mappingValuesLegend);

        addPerfChartXmlMapping(jasperDoc, groupToLegendDisplayStringMap);

        return jasperDoc;
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
     * @return a JasperDocument encapsulating a Document, which can be used as parameter to transform a template jrxml file
     */
    public static JasperDocument getUsageIntervalMasterRuntimeDoc(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                                  LinkedHashSet<List<String>> distinctMappingSets) {

        //is detail is not considered a valid key for usage queries
        Utilities.checkMappingQueryParams(keysToFilters, false, true);

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

        JasperDocument jasperDoc = new JasperDocument();
        int numMappingValues = distinctMappingValues.size();

        //Create the COLUMN_X variables first
        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_SERVICE_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE" calculation="Sum">
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_SERVICE_" + (i + 1), "java.lang.Long", "Group", "SERVICE", "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_OPERATION_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE_OPERATION" calculation="Sum">
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_OPERATION_" + (i + 1), "java.lang.Long", "Group", "SERVICE_OPERATION", "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<variable name="COLUMN_REPORT_1" class="java.lang.Long" resetType="Report" calculation="Sum">
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_REPORT_" + (i + 1), "java.lang.Long", "Report", null, "Sum");
        }

        int xPos = CONSTANT_HEADER_START_X;
        int yPos = 0;

        String keyDisplayValue = getContextKeysDiaplayString(keysToFilters, Utilities.MAPPING_KEY_MAX_SIZE);

        jasperDoc.addCDataElement(JasperDocument.ElementName.KEY_INFO, keyDisplayValue);

        List<String> listMappingValues = new ArrayList<String>();
        listMappingValues.addAll(distinctMappingValues);

        //add a text field for each column
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_HEADER, xPos, yPos,
                    DATA_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                    "textField-serviceHeader-" + (i + 1),
                    listMappingValues.get(i), TOP_LEFT_BOTTOM_CENTER_BROWN,
                    true, false, false, true);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_HEADER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                "textField-serviceHeader-ServiceTotals", "Service Totals",
                ALL_BORDERS_OPAQUE_CENTER_BROWN,
                true, false, false, true);

        xPos += JasperDocument.TOTAL_COLUMN_WIDTH;

        int docTotalWidth = xPos + LEFT_MARGIN_WIDTH + RIGHT_MARGIN_WIDTH;
        int frameWidth = xPos;
        jasperDoc.addIntElement(JasperDocument.ElementName.PAGE_WIDTH, docTotalWidth);
        jasperDoc.addIntElement(JasperDocument.ElementName.COLUMN_WIDTH, frameWidth);
        jasperDoc.addIntElement(JasperDocument.ElementName.FRAME_WIDTH, frameWidth);


        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_SERVICE_1" calculation="Sum"/>
            jasperDoc.addSubReportReturnVariable("COLUMN_" + (i + 1), "COLUMN_SERVICE_" + (i + 1), "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_OPERATION_1" calculation="Sum"/>
            jasperDoc.addSubReportReturnVariable("COLUMN_" + (i + 1), "COLUMN_OPERATION_" + (i + 1), "Sum");
        }

        for (int i = 0; i < numMappingValues; i++) {
            //<returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_REPORT_1" calculation="Sum"/>
            jasperDoc.addSubReportReturnVariable("COLUMN_" + (i + 1), "COLUMN_REPORT_" + (i + 1), "Sum");
        }

        //todo [Donal] this is out by 5, 113 instead of 118, affects summary and master transforms, fix when reports completed
        xPos = CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER, xPos, yPos,
                    DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-" + (i + 1),
                    new TertiaryJavaLongExpression(TertiaryJavaLongExpression.TERNARY_STRING_VARIABLE.COLUMN_OPERATION_, i + 1),
                    TOP_LEFT_BOTTOM_CENTER_GREY, true, false, false, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal",
                new SimpleJavaLongExpression(SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.ROW_OPERATION_TOTAL),
                ALL_BORDERS_GREY_CENTER, true, false, false, false);

        xPos = CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_ID_FOOTER, xPos, yPos,
                    DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceIdFooter-" + (i + 1),
                    new TertiaryJavaLongExpression(TertiaryJavaLongExpression.TERNARY_STRING_VARIABLE.COLUMN_SERVICE_, i + 1),
                    TOP_LEFT_BOTTOM_CENTER_GREY, true, false, false, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_ID_FOOTER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceIdFooterTotal",
                new SimpleJavaLongExpression(SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.ROW_SERVICE_TOTAL),
                ALL_BORDERS_GREY_CENTER, true, false, false, false);

        xPos = CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SUMMARY, xPos, yPos, DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-constantFooter-" + (i + 1),
                    new SimpleIndexJavaLongExpression(SimpleIndexJavaLongExpression.INDEX_MISSING_VARIABLE.COLUMN_REPORT_, i + 1),
                    TOP_LEFT_GREY_CENTER, true, false, false, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SUMMARY, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-constantFooterTotal",
                new SimpleJavaLongExpression(SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.ROW_REPORT_TOTAL),
                TOP_LEFT_RIGHT_GREY_CENTER, true, false, false, false);

        jasperDoc.addIntElement(JasperDocument.ElementName.LEFT_MARGIN, LEFT_MARGIN_WIDTH);
        jasperDoc.addIntElement(JasperDocument.ElementName.RIGHT_MARGIN, LEFT_MARGIN_WIDTH);

        LinkedHashSet<String> mappingValuesLegend = getMappingLegendValues(keysToFilters, distinctMappingSets,
                true, Utilities.MAPPING_KEY_MAX_SIZE, Utilities.USAGE_HEADING_VALUE_MAX_SIZE);

        LinkedHashMap<String, String> groupToLegendDisplayStringMap = getGroupToLegendDisplayStringMap(mappingValuesLegend);
        addChartXMLToDocument(jasperDoc, groupToLegendDisplayStringMap, USAGE_CHART_STATIC_WIDTH, 595);

        return jasperDoc;
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
     * @return a JasperDocument encapsulating a Document, which can be used as parameter to transform a template jrxml file
     */
    public static JasperDocument getUsageRuntimeDoc(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                    LinkedHashSet<List<String>> distinctMappingSets) {

        //usage queires do not use isDetail to determine validity of parameters, it's not considered a key for usage reports
        Utilities.checkMappingQueryParams(keysToFilters, false, true);

        LinkedHashSet<String> mappingValuesLegend = getMappingLegendValues(keysToFilters, distinctMappingSets,
                true, Utilities.MAPPING_KEY_MAX_SIZE, Utilities.USAGE_HEADING_VALUE_MAX_SIZE);

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

        JasperDocument jasperDoc = new JasperDocument();

        int numMappingValues = distinctMappingValues.size();

        //Create the COLUMN_X variables first
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_" + (i + 1), "java.lang.Long", "Group", "SERVICE_AND_OPERATION",
                    "Nothing", "getColumnValue", "COLUMN_" + (i + 1));
        }

        //then the COLUMN_MAPPING_TOTAL_X variables
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_MAPPING_TOTAL_" + (i + 1), "java.lang.Long", "Group", "CONSTANT",
                    "Sum", "getVariableValue", "COLUMN_" + (i + 1));
        }

        //then the COLUMN_SERVICE_TOTAL_X variables
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addVariableToElement(JasperDocument.ElementName.VARIABLES, "COLUMN_SERVICE_TOTAL_" + (i + 1), "java.lang.Long", "Group", "SERVICE_ID",
                    "Sum", "getVariableValue", "COLUMN_" + (i + 1));
        }

        //Constant header starts at x = 113
        //The widths for the entire document are determined from this first header.
        //it has slightly different make up as it has an additional text field, however all other headers
        //should always work out to the be the same width. If they are wider, the report will not compile
        int xPos = CONSTANT_HEADER_START_X;
        int yPos = 0;

        String keyDisplayValue = getContextKeysDiaplayString(keysToFilters, Utilities.MAPPING_KEY_MAX_SIZE);
        jasperDoc.addCDataElement(JasperDocument.ElementName.KEY_INFO, keyDisplayValue);

        List<String> listMappingValues = new ArrayList<String>();
        listMappingValues.addAll(distinctMappingValues);

        //add a text field for each column
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.CONSTANT_HEADER, xPos, yPos,
                    DATA_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                    "textField-constantHeader-" + (i + 1),
                    listMappingValues.get(i), TOP_LEFT_BOTTOM_CENTER_BROWN,
                    false, false, false, true);
            xPos += DATA_COLUMN_WIDTH;
        }
        //move x pos along for width of a column

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.CONSTANT_HEADER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, MAPPING_VALUE_FIELD_HEIGHT,
                "textField-constantHeader-ServiceTotals", "Service Totals",
                ALL_BORDERS_OPAQUE_CENTER_BROWN,
                false, false, false, true);

        xPos += JasperDocument.TOTAL_COLUMN_WIDTH;

        int docTotalWidth = xPos + LEFT_MARGIN_WIDTH + RIGHT_MARGIN_WIDTH;
        jasperDoc.addIntElement(JasperDocument.ElementName.PAGE_WIDTH, docTotalWidth);
        int frameWidth = xPos;
        jasperDoc.addIntElement(JasperDocument.ElementName.COLUMN_WIDTH, frameWidth);
        jasperDoc.addIntElement(JasperDocument.ElementName.FRAME_WIDTH, frameWidth);

        //serviceAndOperationFooter
        xPos = CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER, xPos, yPos,
                    DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceOperationFooter-" + (i + 1),
                    new TertiaryJavaLongExpression(TertiaryJavaLongExpression.TERNARY_STRING_VARIABLE.COLUMN_, i + 1),
                    DEFAULT_CENTER_ALIGNED, false, false, false, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceOperationFooterTotal",
                new SimpleJavaLongExpression(SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.SERVICE_AND_OR_OPERATION_TOTAL),
                ALL_BORDERS_GREY_CENTER, true, false, false, false);

        //serviceIdFooter
        xPos = CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_ID_FOOTER, xPos, yPos,
                    DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-ServiceIdFooter-" + (i + 1),
                    new TertiaryJavaLongExpression(TertiaryJavaLongExpression.TERNARY_STRING_VARIABLE.COLUMN_SERVICE_TOTAL_, i + 1),
                    TOP_LEFT_BOTTOM_CENTER_GREY, true, false, false, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.SERVICE_ID_FOOTER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-ServiceIdFooterTotal",
                new SimpleJavaLongExpression(SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.SERVICE_ONLY_TOTAL),
                ALL_BORDERS_GREY_CENTER, true, false, false, false);

        //constantFooter
        xPos = CONSTANT_HEADER_START_X;
        for (int i = 0; i < numMappingValues; i++) {
            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.CONSTANT_FOOTER, xPos, yPos,
                    DATA_COLUMN_WIDTH, FIELD_HEIGHT,
                    "textField-constantFooter-" + (i + 1),
                    new SimpleIndexJavaLongExpression(SimpleIndexJavaLongExpression.INDEX_MISSING_VARIABLE.COLUMN_MAPPING_TOTAL_, i + 1),
                    TOP_LEFT_GREY_CENTER, true, false, false, false);
            xPos += DATA_COLUMN_WIDTH;
        }

        jasperDoc.addTextFieldToElement(JasperDocument.ElementName.CONSTANT_FOOTER, xPos, yPos,
                JasperDocument.TOTAL_COLUMN_WIDTH, FIELD_HEIGHT,
                "textField-constantFooterTotal",
                new SimpleJavaLongExpression(SimpleJavaLongExpression.PLAIN_STRING_VARIABLE.GRAND_TOTAL),
                TOP_LEFT_RIGHT_GREY_CENTER, true, false, false, false);


        jasperDoc.addIntElement(JasperDocument.ElementName.LEFT_MARGIN, LEFT_MARGIN_WIDTH);
        jasperDoc.addIntElement(JasperDocument.ElementName.RIGHT_MARGIN, LEFT_MARGIN_WIDTH);

        LinkedHashMap<String, String> groupToLegendDisplayStringMap = getGroupToLegendDisplayStringMap(mappingValuesLegend);
        addChartXMLToDocument(jasperDoc, groupToLegendDisplayStringMap, USAGE_CHART_STATIC_WIDTH, 595);
        return jasperDoc;

    }

    /**
     * Add the dynamic elements of the chart xml to the supplied document, which is used to transform template jrxml
     * files. The groupToMappingValue is used for creating the legend jrxml elements.
     *
     * @param jasperDoc           the JasperDocument to update
     * @param groupToMappingValue represents the distinct mapping sets for the keys in the query. The key is a shorted
     *                            version of the string it maps to. This information is displayed in a legend element created by this function
     * @param frameWidth          the frame width in the jrxml file which will contain the chart
     * @param minPageHeight       the minimum page height of the report which will be created using the updated document
     *                            passed into this function as it's transform parameter
     */
    private static void addChartXMLToDocument(JasperDocument jasperDoc, LinkedHashMap<String, String> groupToMappingValue,
                                              int frameWidth, int minPageHeight) {
        //Create all the text fields for the chart legend

        jasperDoc.createRootDirectAncestorElement(JasperDocument.ElementName.CHART_ELEMENT);

        jasperDoc.createElementAncestorElement(JasperDocument.ElementName.CHART_LEGEND, JasperDocument.ElementName.CHART_ELEMENT);
        int x = 0;
        int y = 0;
        int vSpace = 2;
        int height = 18;

        int index = 0;
        if (frameWidth < FRAME_MIN_WIDTH) frameWidth = FRAME_MIN_WIDTH;
        for (Map.Entry<String, String> me : groupToMappingValue.entrySet()) {

            jasperDoc.addTextFieldToElement(JasperDocument.ElementName.CHART_LEGEND, x, y, frameWidth, height,
                    "chartLegendKey" + (index + 1),
                    "<b>" + me.getKey() + ":</b> " + Utilities.escapeHtmlCharacters(me.getValue()),
                    LEFT_PADDED_HEADING_HTML, false, true, true, false);

            y += height + vSpace;
            index++;
        }

        //Chart height is minimum 130, if there are more than 2 mapping value sets then increase it
        int chartHeight = 130;
        int numMappingSets = groupToMappingValue.size();
        if (numMappingSets > 2) {
            chartHeight += 30 * (numMappingSets - 2);
        }

        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_HEIGHT, chartHeight, JasperDocument.ElementName.CHART_ELEMENT);

        //start of chart legend = chart height + 18 for the title of the chart frame
        int chartLegendFrameYPos = chartHeight;// + height;
        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_LEGEND_FRAME_YPOS, chartLegendFrameYPos, JasperDocument.ElementName.CHART_ELEMENT);

        //height of chart legend = num mapping sets * height + vSpace
        int chartLegendHeight = numMappingSets * (height + vSpace);
        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_LEGEND_HEIGHT, chartLegendHeight, JasperDocument.ElementName.CHART_ELEMENT);

        int chartFrameHeight = chartHeight + 18 + chartLegendHeight;
        jasperDoc.addIntElement(JasperDocument.ElementName.CHART_FRAME_HEIGHT, chartFrameHeight, JasperDocument.ElementName.CHART_ELEMENT);

        //Calculate the height of the band
        int bandHeight = chartFrameHeight + height + height;//18 from the summary frame + 18 for a gap
        jasperDoc.addIntElement(JasperDocument.ElementName.BAND_HEIGHT, bandHeight, JasperDocument.ElementName.CHART_ELEMENT);

        int titleHeight = 243;
        int margins = 20 + 20;
        int totalFirstPageHeight = titleHeight + margins + bandHeight + CONSTANT_HEADER_HEIGHT;
        if (totalFirstPageHeight < minPageHeight) totalFirstPageHeight = minPageHeight;

        jasperDoc.addIntElement(JasperDocument.ElementName.PAGE_HEIGHT, totalFirstPageHeight, JasperDocument.ElementName.CHART_ELEMENT);
    }

    /**
     * This implementation is used primiarly from within reports and also from other Utility functions.
     * As the report is the main use, the make up of distinctMappingSets is fixed, as the report will pump in field
     * values into some method will which give it the required LinkedHashSet<List<java.lang.String>>
     * Auth User is always the first element of each list in distinctMappingSets
     *
     * @param keysToFilters        a LinkedHashMap of each key to use in the query, and for each key 0..* FilterPair's, which
     *                             represent it's constraints. All keys should have at least one FilterPair supplied. If no constraint was added for a
     *                             key then the isEmpty() method of FilterPair should return true. The order of this parameter is very important
     *                             and must be maintained for all functions which use the same instance of keysToFilters, which is why its a linked
     *                             hash map.
     * @param distinctMappingSets  represents the runtime report meta data, which are the distinct set of mapping values
     *                             that the report <em>WILL</em> find when it runs. The first value of each list is always the authenticated user,
     *                             followed by 5 mapping values
     * @param truncateValues       if true truncation will be appplied to keys and values
     * @param truncateKeyMaxSize   if truncateValues is true, then this is the maximum size of a key after truncation
     * @param truncateValueMaxSize if truncateValues is true, then this is the maximum size of a value after truncation
     * @return a LinkedHashSet with a string representing each distint group of valus from distinctMappingSets
     */
    public static LinkedHashSet<String> getMappingLegendValues(LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilters,
                                                               LinkedHashSet<List<String>> distinctMappingSets,
                                                               boolean truncateValues,
                                                               Integer truncateKeyMaxSize,
                                                               Integer truncateValueMaxSize) {
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
                    authUser, mappingStringsArray, false, null, truncateValues, truncateKeyMaxSize, truncateValueMaxSize);
            mappingValues.add(mappingValue);
        }

        return mappingValues;
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
            sb.append(TextUtils.truncStringMiddleExact(authUser, Utilities.USAGE_HEADING_VALUE_MAX_SIZE));
            first = false;
        }

        for (String s : mappingValues) {
            if (!first) {
                if (!s.equals(Utilities.SQL_PLACE_HOLDER)) sb.append("\n");
            }
            first = false;
            if (!s.equals(Utilities.SQL_PLACE_HOLDER))
                sb.append(TextUtils.truncStringMiddleExact(s, Utilities.USAGE_HEADING_VALUE_MAX_SIZE));
        }

        return sb.toString();
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
