/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Dec 17, 2008
 * Time: 4:38:56 PM
 */
package com.l7tech.gateway.standardreports;

import org.junit.Test;
import org.junit.Assert;
import org.w3c.dom.*;
import com.l7tech.server.management.api.node.ReportApi;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;

public class RuntimeDocUtilitiesTest {
    @Test
    public void testGetUsageRuntimeDoc_NullParams() {
        boolean exception = false;
        try {
            RuntimeDocUtilities.getUsageRuntimeDoc(null, null);
        } catch (IllegalArgumentException iae) {
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testGetUsageRuntimeDoc_NotNull() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        Assert.assertNotNull(doc);
    }

    /**
     * Don't change the number of elements, as you will break the tests which use this as convenience
     *
     * @return
     */
    private LinkedHashMap<String, List<ReportApi.FilterPair>> getTestKeys() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = new LinkedHashMap<String, List<ReportApi.FilterPair>>();
        List<ReportApi.FilterPair> ipFilters = new ArrayList<ReportApi.FilterPair>();
        ipFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("IP_ADDRESS", ipFilters);

        List<ReportApi.FilterPair> custFilters = new ArrayList<ReportApi.FilterPair>();
        custFilters.add(new ReportApi.FilterPair());
        keysToFilterPairs.put("CUSTOMER", custFilters);

        return keysToFilterPairs;
    }

    /**
     * Don't change the number of elements, as you will break the tests which use this as convenience
     *
     * @return
     */
    private LinkedHashSet<List<String>> getTestDistinctMappingSets() {
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        for (int i = 0; i < 4; i++) {
            List<String> valueList = new ArrayList<String>();
            valueList.add("Donal");
            valueList.add("127.0.0.1");
            valueList.add("Bronze" + i);//make each list unique - set is smart, not just object refs
            distinctMappingSets.add(valueList);
        }
        return distinctMappingSets;
    }

    @Test
    public void testGetUsageRuntimeDoc_FirstLevelElements() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.VARIABLES.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.CONSTANT_HEADER.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_ID_FOOTER.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.CONSTANT_FOOTER.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.COLUMN_WIDTH.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.FRAME_WIDTH.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.LEFT_MARGIN.getName());
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(JasperDocument.ElementName.RIGHT_MARGIN.getName());
        Assert.assertTrue(list.getLength() == 1);
    }

    @Test
    public void testGetUsageRuntimeDoc_Variables() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.VARIABLES.getName());
        list = list.item(0).getChildNodes();
        //3 sets of variables X 4 value sets from getTestMappingValues()
        Assert.assertTrue(JasperDocument.ElementName.VARIABLES.getName()
                + " element should have " + (3 * distinctMappingSets.size()) + " elements",
                list.getLength() == (3 * distinctMappingSets.size()));

    }

    /**
     * Validate the variables created, Check that their name, class, calculation, resetType and resetGroup if applicable
     * are correct.
     */
    @Test
    public void testGetUsageRuntimeDoc_VariablesNames() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.VARIABLES.getName());
        list = list.item(0).getChildNodes();
        String[] variableNames = new String[]{"COLUMN_", "COLUMN_MAPPING_TOTAL_", "COLUMN_SERVICE_TOTAL_"};
        String[] calculations = new String[]{"Nothing", "Sum", "Sum"};
        String[] resetTypes = new String[]{"Group", "Group", "Group"};
        String[] resetGroups = new String[]{"SERVICE_AND_OPERATION", "CONSTANT", "SERVICE_ID"};

        int index = 0;
        int varIndex = 1;
        for (int i = 0; i < list.getLength(); i++, varIndex++) {
            if (i > 0 && (i % distinctMappingSets.size() == 0)) {
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            String expectedValue = variableNames[index % distinctMappingSets.size()] + varIndex;
            Assert.assertTrue("Name attribute should equal " + expectedValue + " actual value was: " + varName.getNodeValue()
                    , varName.getNodeValue().equals(expectedValue));

            expectedValue = calculations[index % distinctMappingSets.size()];
            Node calcName = attributes.getNamedItem("calculation");

            Assert.assertTrue("calculation attribute should equal " + expectedValue + " Actual value was: " +
                    calcName.getNodeValue(), calcName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = resetTypes[index % distinctMappingSets.size()];
            Assert.assertTrue("resetType attribute should equal " + expectedValue + " Actual value was: " + resetType
                    , resetType.equals(expectedValue));

            if (resetType.equals("Group")) {
                //check the actual group used to reset the variable is correct
                Node varResetGroup = attributes.getNamedItem("resetGroup");
                String resetGroup = varResetGroup.getNodeValue();

                expectedValue = resetGroups[index % distinctMappingSets.size()];
                Assert.assertTrue("resetGroup attribute should equal " + expectedValue + " Actual value was: " + resetGroup
                        , resetGroup.equals(expectedValue));
            }
        }
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantHeader() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.CONSTANT_HEADER.getName());
        list = list.item(0).getChildNodes();
        //1 text fields which are hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(JasperDocument.ElementName.CONSTANT_HEADER.getName() + " element should have "
                + (distinctMappingSets.size() + 1) + " elements, " +
                "instead it had " + list.getLength(),
                list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ConstantHeader_CheckElements() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.CONSTANT_HEADER.getName());
        list = list.item(0).getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node textFieldNode = list.item(i);
            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            NamedNodeMap attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            String expectedValue = "java.lang.String";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.MAPPING_VALUE_FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i < list.getLength() - 1) expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            if (i == 0) expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X;
            else
                expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            if (i < list.getLength() - 1) expectedValue = RuntimeDocUtilities.TOP_LEFT_BOTTOM_CENTER_BROWN;
            else expectedValue = RuntimeDocUtilities.ALL_BORDERS_OPAQUE_CENTER_BROWN;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));
        }
    }

    private Node findFirstChildElementByName(Node parent, String name) {
        if (name == null) throw new IllegalArgumentException("name must be non-null!");
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE && name.equals(n.getNodeName())) {
                return n;
            }
        }
        return null;
    }

    @Test
    public void testGetUsageRuntimeDoc_ServiceOperationFooter() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER.getName());
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER.getName()
                + " element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ServiceOperationFooter_CheckElements() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        //printOutDocument(doc);
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER.getName());
        list = list.item(0).getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + RuntimeDocUtilities.FIELD_HEIGHT + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i < list.getLength() - 1) expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if (i < list.getLength() - 1) expectedValue = RuntimeDocUtilities.DEFAULT_CENTER_ALIGNED;
            else expectedValue = RuntimeDocUtilities.ALL_BORDERS_GREY_CENTER;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));

            if (i == list.getLength() - 1) {
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue + " Actual value was: " + actualValue
                        , actualValue.equals(expectedValue));
            }

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            if (i < list.getLength() - 1)
                expectedValue = "($V{COLUMN_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_" + (i + 1) + "}";
            else expectedValue = "$V{SERVICE_AND_OR_OPERATION_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue + " Actual value was: " +
                    textFieldExprNode.getTextContent(), textFieldExprNode.getTextContent().equals(expectedValue));

            //No longer needed as html formatting not used, leaving in case we change back
            //make sure the html markup attribute is also specified
//            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
//            attributes = textElementNode.getAttributes();
//            Node markUp = attributes.getNamedItem("markup");
//            expectedValue = "html";
//            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
//                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void testGetUsageRuntimeDoc_SerivceIdFooter() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_ID_FOOTER.getName());
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(JasperDocument.ElementName.SERVICE_ID_FOOTER.getName()
                + " element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ServiceIdFooter_CheckElements() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_ID_FOOTER.getName());
        list = list.item(0).getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + RuntimeDocUtilities.FIELD_HEIGHT + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i < list.getLength() - 1) expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if (i < list.getLength() - 1) expectedValue = RuntimeDocUtilities.TOP_LEFT_BOTTOM_CENTER_GREY;
            else expectedValue = RuntimeDocUtilities.ALL_BORDERS_GREY_CENTER;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            if (i < list.getLength() - 1)
                expectedValue = "($V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "} == null || $V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}.intValue() == 0)?new Long(0):$V{COLUMN_SERVICE_TOTAL_" + (i + 1) + "}";
            else expectedValue = "$V{SERVICE_ONLY_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue + " Actual value was: " +
                    textFieldExprNode.getTextContent(), textFieldExprNode.getTextContent().equals(expectedValue));

            //No longer needed as html formatting not used, leaving in case we change back
            //make sure the html markup attribute is also specified
//            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
//            attributes = textElementNode.getAttributes();
//            Node markUp = attributes.getNamedItem("markup");
//            expectedValue = "html";
//            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
//                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantFooter() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.CONSTANT_FOOTER.getName());
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(JasperDocument.ElementName.CONSTANT_FOOTER.getName() + " element should have "
                + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ConstantFooter_CheckElements() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.CONSTANT_FOOTER.getName());
        list = list.item(0).getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + RuntimeDocUtilities.FIELD_HEIGHT + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i < list.getLength() - 1) expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if (i < list.getLength() - 1) expectedValue = RuntimeDocUtilities.TOP_LEFT_GREY_CENTER;
            else expectedValue = RuntimeDocUtilities.TOP_LEFT_RIGHT_GREY_CENTER;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            if (i < list.getLength() - 1) expectedValue = "$V{COLUMN_MAPPING_TOTAL_" + (i + 1) + "}";
            else expectedValue = "$V{GRAND_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue + " Actual value was: " +
                    textFieldExprNode.getTextContent(), textFieldExprNode.getTextContent().equals(expectedValue));

            //No longer needed as html formatting not used, leaving in case we change back
            //make sure the html markup attribute is also specified
//            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
//            attributes = textElementNode.getAttributes();
//            Node markUp = attributes.getNamedItem("markup");
//            expectedValue = "html";
//            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
//                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageRuntimeDoc_CheckWidths() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        Element rootNode = doc.getDocumentElement();
        testWidths(rootNode, distinctMappingSets.size());
    }

    /**
     * Validate the variables created, Check that their name, class, calculation, resetType and resetGroup if applicable
     * are correct.
     */
    @Test
    public void getUsageIntervalMasterRuntimeDoc_Variables() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc =
                RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.VARIABLES.getName());
        list = list.item(0).getChildNodes();
        String[] variableNames = new String[]{"COLUMN_SERVICE_", "COLUMN_OPERATION_", "COLUMN_REPORT_"};
        String[] resetTypes = new String[]{"Group", "Group", "Report"};
        String[] resetGroups = new String[]{"SERVICE", "SERVICE_OPERATION", null};
        String[] calculations = new String[]{"Sum", "Sum", "Sum"};

        int index = 0;
        int varIndex = 1;
        for (int i = 0; i < list.getLength(); i++, varIndex++) {
            if (i > 0 && (i % distinctMappingSets.size() == 0)) {
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = variableNames[index % distinctMappingSets.size()] + varIndex;
            Assert.assertTrue("Name attribute should equal " + expectedValue + " Actual value was: " + varName.getNodeValue()
                    , varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = resetTypes[index % distinctMappingSets.size()];
            Assert.assertTrue("resetType attribute should equal " + expectedValue + " Actual value was: " + resetType
                    , resetType.equals(expectedValue));

            if (resetType.equals("Group")) {
                //check the actual group used to reset the variable is correct
                Node varResetGroup = attributes.getNamedItem("resetGroup");
                String resetGroup = varResetGroup.getNodeValue();

                expectedValue = resetGroups[index % distinctMappingSets.size()];
                Assert.assertTrue("resetGroup attribute should equal " + expectedValue + " Actual value was: " + resetGroup
                        , resetGroup.equals(expectedValue));
            }

            expectedValue = calculations[index % distinctMappingSets.size()];
            Node calcName = attributes.getNamedItem("calculation");

            Assert.assertTrue("calculation attribute should equal " + expectedValue + " Actual value was: " +
                    calcName.getNodeValue(), calcName.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceHeader_CheckElements() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc =
                RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_HEADER.getName());
        list = list.item(0).getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.MAPPING_VALUE_FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i == list.getLength() - 1) expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;
            else expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            if (i == 0) expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X;
            else
                expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * i);

            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if (i < list.getLength() - 1) expectedValue = RuntimeDocUtilities.TOP_LEFT_BOTTOM_CENTER_BROWN;
            else expectedValue = RuntimeDocUtilities.ALL_BORDERS_OPAQUE_CENTER_BROWN;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.String";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            //No longer needed as html formatting not used, leaving in case we change back
            //make sure the html markup attribute is also specified
//            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
//            attributes = textElementNode.getAttributes();
//            Node markUp = attributes.getNamedItem("markup");
//            expectedValue = "html";
//            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
//                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_CheckSubReport() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc =
                RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.RETURN_VALUE);
        list = list.item(0).getChildNodes();
        String[] variableNames = new String[]{"COLUMN_SERVICE_", "COLUMN_OPERATION_", "COLUMN_REPORT_"};
        String[] calculations = new String[]{"Sum", "Sum", "Sum"};

        int index = 0;
        int varIndex = 1;
        for (int i = 0; i < list.getLength(); i++, varIndex++) {
            if (i > 0 && (i % distinctMappingSets.size() == 0)) {
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node calculation = attributes.getNamedItem("calculation");

            String expectedValue = calculations[index % distinctMappingSets.size()];
            String actualValue = calculation.getNodeValue();
            Assert.assertTrue("calculation attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

            Node subreportVariable = attributes.getNamedItem("subreportVariable");
            expectedValue = "COLUMN_" + varIndex;
            actualValue = subreportVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

            Node toVariable = attributes.getNamedItem("toVariable");
            expectedValue = variableNames[index % distinctMappingSets.size()] + varIndex;
            actualValue = toVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

        }
    }

    private void testGroupTotalRow(final String elementName, final String columnVariable, final String totalVariable,
                                   final boolean includeNullTest, final String rowTotalStyle, final String rowGrandTotalStyle) {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc =
                RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(elementName);
        list = list.item(0).getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + RuntimeDocUtilities.FIELD_HEIGHT + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i < list.getLength() - 1) expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if (i < list.getLength() - 1) expectedValue = rowTotalStyle;
            else expectedValue = rowGrandTotalStyle;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            if (i < list.getLength() - 1) {
                if (includeNullTest) {
                    expectedValue = "($V{" + columnVariable + (i + 1) + "} == null)?new Long(0):$V{" + columnVariable + (i + 1) + "}";
                } else {
                    expectedValue = "$V{" + columnVariable + (i + 1) + "}";
                }
            } else expectedValue = "$V{" + totalVariable + "}";

            Assert.assertTrue("text expression should equal " + expectedValue + " Actual value was: " +
                    textFieldExprNode.getTextContent(), textFieldExprNode.getTextContent().equals(expectedValue));

            //No longer needed as html formatting not used, leaving in case we change back
            //make sure the html markup attribute is also specified
//            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
//            attributes = textElementNode.getAttributes();
//            Node markUp = attributes.getNamedItem("markup");
//            expectedValue = "html";
//            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
//                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceAndOperationFooter_CheckElements() {
        testGroupTotalRow(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER.getName(),
                "COLUMN_OPERATION_", "ROW_OPERATION_TOTAL",
                true, RuntimeDocUtilities.TOP_LEFT_BOTTOM_CENTER_GREY, RuntimeDocUtilities.ALL_BORDERS_GREY_CENTER);
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceIdFooter_CheckElements() {
        testGroupTotalRow(JasperDocument.ElementName.SERVICE_ID_FOOTER.getName(),
                "COLUMN_SERVICE_", "ROW_SERVICE_TOTAL", true,
                RuntimeDocUtilities.TOP_LEFT_BOTTOM_CENTER_GREY, RuntimeDocUtilities.ALL_BORDERS_GREY_CENTER);
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_summary_CheckElements() {
        testGroupTotalRow(JasperDocument.ElementName.SUMMARY.getName(), "COLUMN_REPORT_", "ROW_REPORT_TOTAL", false,
                RuntimeDocUtilities.TOP_LEFT_GREY_CENTER, RuntimeDocUtilities.TOP_LEFT_RIGHT_GREY_CENTER);
    }

    private void testWidths(Node rootNode, int numMappingValues) {
        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        int expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * numMappingValues)
                + JasperDocument.TOTAL_COLUMN_WIDTH + RuntimeDocUtilities.LEFT_MARGIN_WIDTH + RuntimeDocUtilities.RIGHT_MARGIN_WIDTH;
        int actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

        Node columnWidth = findFirstChildElementByName(rootNode, "columnWidth");
        expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * numMappingValues)
                + JasperDocument.TOTAL_COLUMN_WIDTH;
        actualIntValue = Integer.valueOf(columnWidth.getTextContent());

        Assert.assertTrue("columnWidth element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

        Node frameWidth = findFirstChildElementByName(rootNode, "frameWidth");
        expectedIntValue = RuntimeDocUtilities.CONSTANT_HEADER_START_X + (RuntimeDocUtilities.DATA_COLUMN_WIDTH * numMappingValues)
                + JasperDocument.TOTAL_COLUMN_WIDTH;
        actualIntValue = Integer.valueOf(frameWidth.getTextContent());

        Assert.assertTrue("frameWidth element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

        Node leftMargin = findFirstChildElementByName(rootNode, "leftMargin");
        expectedIntValue = RuntimeDocUtilities.LEFT_MARGIN_WIDTH;
        actualIntValue = Integer.valueOf(leftMargin.getTextContent());

        Assert.assertTrue("leftMargin element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

        Node rightMargin = findFirstChildElementByName(rootNode, "rightMargin");
        expectedIntValue = RuntimeDocUtilities.RIGHT_MARGIN_WIDTH;
        actualIntValue = Integer.valueOf(rightMargin.getTextContent());

        Assert.assertTrue("rightMargin element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_CheckWidths() {
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc =
                RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets).getDocument();
        Element rootNode = doc.getDocumentElement();
        testWidths(rootNode, distinctMappingSets.size());
    }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_Variables() {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.VARIABLES.getName());
        list = list.item(0).getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = "COLUMN_" + (i + 1);
            Assert.assertTrue("Name attribute should equal " + expectedValue + " Actual value was: " + varName.getNodeValue()
                    , varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = "Report";
            Assert.assertTrue("resetType attribute should equal " + expectedValue + " Actual value was: " + resetType
                    , resetType.equals(expectedValue));

            Node calcName = attributes.getNamedItem("calculation");
            expectedValue = "Sum";

            Assert.assertTrue("calculation attribute should equal " + expectedValue + " Actual value was: " +
                    calcName.getNodeValue(), calcName.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_CheckSubReport() {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();
        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.RETURN_VALUE);
        list = list.item(0).getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node calculation = attributes.getNamedItem("calculation");

            String expectedValue = "Sum";
            String actualValue = calculation.getNodeValue();
            Assert.assertTrue("calculation attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

            Node subreportVariable = attributes.getNamedItem("subreportVariable");
            expectedValue = "COLUMN_" + (i + 1);
            actualValue = subreportVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

            Node toVariable = attributes.getNamedItem("toVariable");
            expectedValue = "COLUMN_" + (i + 1);
            actualValue = toVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue + " Actual value was: " + actualValue
                    , actualValue.equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_CheckWidths() {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets).getDocument();
        Element rootNode = doc.getDocumentElement();

        Node subReportWidth = findFirstChildElementByName(rootNode, JasperDocument.ElementName.SUB_REPORT_WIDTH.getName());
        int expectedIntValue = (RuntimeDocUtilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + JasperDocument.TOTAL_COLUMN_WIDTH;
        int actualIntValue = Integer.valueOf(subReportWidth.getTextContent());

        Assert.assertTrue("subReportWidth element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        expectedIntValue = (RuntimeDocUtilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + JasperDocument.TOTAL_COLUMN_WIDTH
                + RuntimeDocUtilities.SUB_INTERVAL_STATIC_WIDTH;
        actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

    }

    @Test
    public void getUsageSubReportRuntimeDoc_Variables() {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.VARIABLES.getName());
        list = list.item(0).getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = "COLUMN_" + (i + 1);
            Assert.assertTrue("Name attribute should equal " + expectedValue + " Actual value was: " + varName.getNodeValue()
                    , varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = "None";
            Assert.assertTrue("resetType attribute should equal " + expectedValue + " Actual value was: " + resetType
                    , resetType.equals(expectedValue));

            Node calcName = attributes.getNamedItem("calculation");
            expectedValue = "Nothing";

            Assert.assertTrue("calculation attribute should equal " + expectedValue + " Actual value was: " +
                    calcName.getNodeValue(), calcName.getNodeValue().equals(expectedValue));

            //variableExpression
            Node variableExpression = findFirstChildElementByName(list.item(i), "variableExpression");

            expectedValue = "((UsageSummaryAndSubReportHelper)$P{REPORT_SCRIPTLET}).getColumnValue(\"COLUMN_" + (i + 1) + "\", " +
                    "$F{AUTHENTICATED_USER},new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3}," +
                    "$F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})";
            String actualValue = variableExpression.getTextContent();
            Assert.assertTrue("variableExpression elements text should equal " + expectedValue + " Actual value was: " +
                    actualValue, actualValue.equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_serviceAndOperationFooter_CheckElements() {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.SERVICE_AND_OPERATION_FOOTER.getName());
        list = list.item(0).getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + RuntimeDocUtilities.FIELD_HEIGHT + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i < list.getLength() - 1) expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH * i;
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if (i < list.getLength() - 1) expectedValue = RuntimeDocUtilities.DEFAULT_CENTER_ALIGNED;
            else expectedValue = RuntimeDocUtilities.ALL_BORDERS_GREY_CENTER;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));

            if (i == list.getLength() - 1) {
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue + " Actual value was: " + actualValue
                        , actualValue.equals(expectedValue));
            }

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue + " Actual value was: " + classType
                    , classType.equals(expectedValue));

            if (i < list.getLength() - 1)
                expectedValue = "($V{COLUMN_" + (i + 1) + "} == null)?new Long(0):$V{COLUMN_" + (i + 1) + "}";
            else expectedValue = "$V{TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue + " Actual value was: " +
                    textFieldExprNode.getTextContent(), textFieldExprNode.getTextContent().equals(expectedValue));

            //No longer needed as html formatting not used, leaving in case we change back
            //make sure the html markup attribute is also specified
//            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
//            attributes = textElementNode.getAttributes();
//            Node markUp = attributes.getNamedItem("markup");
//            expectedValue = "html";
//            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
//                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_noData_CheckElements() {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets).getDocument();
        NodeList list = doc.getElementsByTagName(JasperDocument.ElementName.NO_DATA.getName());
        list = list.item(0).getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {
            Node staticTextField = list.item(i);

            Node reportElementNode = findFirstChildElementByName(staticTextField, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = RuntimeDocUtilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + RuntimeDocUtilities.FIELD_HEIGHT + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if (i < list.getLength() - 1) expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = JasperDocument.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = RuntimeDocUtilities.DATA_COLUMN_WIDTH * i;
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue + " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if (i < list.getLength() - 1) expectedValue = RuntimeDocUtilities.DEFAULT_CENTER_ALIGNED;
            else expectedValue = RuntimeDocUtilities.ALL_BORDERS_GREY_CENTER;

            Assert.assertTrue("style attribute should equal " + expectedValue + " Actual value was: " + style.getNodeValue()
                    , style.getNodeValue().equals(expectedValue));

            if (i == list.getLength() - 1) {
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue + " Actual value was: " + actualValue
                        , actualValue.equals(expectedValue));
            }

            Node textNode = findFirstChildElementByName(staticTextField, "text");
            expectedValue = "NA";
            String actualValue = textNode.getTextContent();

            Assert.assertTrue("text element should equal " + expectedValue + " Actual value was: " +
                    actualValue, actualValue.equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_CheckWidths() {
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets).getDocument();
        Element rootNode = doc.getDocumentElement();

        Node pageWidth = findFirstChildElementByName(rootNode, JasperDocument.ElementName.PAGE_WIDTH.getName());
        int expectedIntValue = (RuntimeDocUtilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + JasperDocument.TOTAL_COLUMN_WIDTH;
        int actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue + " Actual value was: " +
                actualIntValue, actualIntValue == expectedIntValue);

    }

    /**
     * Test which illustrates that its not possible to supply quotes or back slashes as unicode values
     * and cause the RuntimeDocUtilities.escapeCharacters() method to not be able to detect them
     * <p/>
     * \u005c is the backslash character
     * \u0022 is the double quote character
     */
    @Test
    public void testEscapes() {
        String toEscape = "\u005c\u0022";
        char c = toEscape.charAt(0);
        Assert.assertTrue("This is always true as part of the java language specification", c == '"');
        Assert.assertEquals("This is always true as part of the java language specification", toEscape, "\"");

        String escaped = JasperDocument.escapeJavaSringLiteralChars(toEscape);
        Assert.assertEquals("String incorrectly escaped. Escaped string should equal: \\\" but was: " + escaped, escaped, "\\\"");
    }

    @Test
    public void testCarriageReturn() {
        String s = "\r";
        String expected = "\\r";
        String actualValue = JasperDocument.escapeJavaSringLiteralChars(s);
        Assert.assertEquals("Expected value: " + expected + " actual value: " + actualValue, actualValue, expected);
    }
}
