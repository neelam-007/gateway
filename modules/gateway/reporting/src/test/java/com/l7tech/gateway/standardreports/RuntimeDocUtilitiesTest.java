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
    public void testGetUsageRuntimeDoc_NullParams(){
        boolean exception = false;
        try{
            RuntimeDocUtilities.getUsageRuntimeDoc(null, null);
        }catch(IllegalArgumentException iae){
            exception = true;
        }
        Assert.assertTrue(exception);
    }

    @Test
    public void testGetUsageRuntimeDoc_NotNull(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Assert.assertNotNull(doc);
    }

    /**
     * Don't change the number of elements, as you will break the tests which use this as convenience
     * @return
     */
    private LinkedHashMap<String, List<ReportApi.FilterPair>> getTestKeys(){
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
     * @return
     */
    private LinkedHashSet<List<String>> getTestDistinctMappingSets(){
        LinkedHashSet<List<String>> distinctMappingSets = new LinkedHashSet<List<String>>();
        for(int i = 0; i < 4; i++){
            List<String> valueList = new ArrayList<String>();
            valueList.add("Donal");
            valueList.add("127.0.0.1");
            valueList.add("Bronze"+i);//make each list unique - set is smart, not just object refs
            distinctMappingSets.add(valueList);
        }
        return distinctMappingSets;
    }

    @Test
    public void testGetUsageRuntimeDoc_FirstLevelElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.CONSTANT_HEADER);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.SERVICE_ID_FOOTER);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.CONSTANT_FOOTER);
        Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.COLUMN_WIDTH);
                Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.FRAME_WIDTH);
                Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.LEFT_MARGIN);
                Assert.assertTrue(list.getLength() == 1);

        list = doc.getElementsByTagName(Utilities.RIGHT_MARGIN);
                Assert.assertTrue(list.getLength() == 1);
    }

    @Test
    public void testGetUsageRuntimeDoc_Variables(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();
        //3 sets of variables X 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.VARIABLES+" element should have " + (3 * distinctMappingSets.size()) + " elements",
                list.getLength() == (3 * distinctMappingSets.size()));

    }

    /**
     * Validate the variables created, Check that their name, class, calculation, resetType and resetGroup if applicable
     * are correct.
     */
    @Test
    public void testGetUsageRuntimeDoc_VariablesNames(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();
        String [] variableNames = new String[]{"COLUMN_","COLUMN_MAPPING_TOTAL_","COLUMN_SERVICE_TOTAL_"};
        String [] calculations = new String[]{"Nothing","Sum","Sum"};
        String [] resetTypes = new String[]{"Group","Group","Group"};
        String [] resetGroups = new String[]{"SERVICE_AND_OPERATION","CONSTANT","SERVICE_ID"};

        int index = 0;
        int varIndex = 1;
        for(int i = 0; i < list.getLength(); i++,varIndex++){
            if(i > 0 && (i % distinctMappingSets.size()  == 0)){
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            String expectedValue = variableNames[index % distinctMappingSets.size()]+varIndex;
            Assert.assertTrue("Name attribute should equal " + expectedValue+" actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            expectedValue = calculations[index % distinctMappingSets.size()];
            Node calcName = attributes.getNamedItem("calculation");

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = resetTypes[index % distinctMappingSets.size()];
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            if(resetType.equals("Group")){
                //check the actual group used to reset the variable is correct
                Node varResetGroup = attributes.getNamedItem("resetGroup");
                String resetGroup = varResetGroup.getNodeValue();

                expectedValue = resetGroups[index % distinctMappingSets.size()];
                Assert.assertTrue("resetGroup attribute should equal " + expectedValue+ " Actual value was: " + resetGroup
                        ,resetGroup.equals(expectedValue));
            }
        }
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantHeader(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_HEADER);
        list = list.item(0).getChildNodes();
        //1 text fields which are hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.CONSTANT_HEADER+" element should have " + (distinctMappingSets.size() + 1) + " elements, " +
                "instead it had " + list.getLength(),
                list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ConstantHeader_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_HEADER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);
            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            NamedNodeMap attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            String expectedValue = "java.lang.String";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.MAPPING_VALUE_FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            if(i == 0) expectedIntValue = Utilities.CONSTANT_HEADER_START_X;
            else expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            if(i < list.getLength() - 1) expectedValue = Utilities.USAGE_TABLE_HEADING_STYLE;
            else expectedValue = Utilities.USAGE_TABLE_HEADING_ROW_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));


        }
    }

    private Node findFirstChildElementByName( Node parent, String name ) {
        if ( name == null ) throw new IllegalArgumentException( "name must be non-null!" );
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node n = children.item(i);
            if ( n.getNodeType() == Node.ELEMENT_NODE && name.equals( n.getNodeName())){
                return n;
            }
        }
        return null;
    }

    @Test
    public void testGetUsageRuntimeDoc_ServiceOperationFooter(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.SERVICE_AND_OPERATION_FOOTER+" element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ServiceOperationFooter_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //printOutDocument(doc);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if(i < list.getLength() - 1) expectedValue = "TableCell";
            else expectedValue = Utilities.ROW_TOTAL_MINOR_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            if(i == list.getLength() - 1){
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                        ,actualValue.equals(expectedValue));
            }

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "($V{COLUMN_"+(i+1)+"} == null)?new Long(0):$V{COLUMN_"+(i+1)+"}";
            else expectedValue = "$V{SERVICE_AND_OR_OPERATION_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void testGetUsageRuntimeDoc_SerivceIdFooter(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_ID_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.SERVICE_ID_FOOTER+" element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ServiceIdFooter_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_ID_FOOTER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = null;
            if(i < list.getLength() - 1) expectedValue = Utilities.ROW_TOTAL_STYLE;
            else expectedValue = Utilities.ROW_GRAND_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "($V{COLUMN_SERVICE_TOTAL_"+(i+1)+"} == null || $V{COLUMN_SERVICE_TOTAL_"+(i+1)+"}.intValue() == 0)?new Long(0):$V{COLUMN_SERVICE_TOTAL_"+(i+1)+"}";
            else expectedValue = "$V{SERVICE_ONLY_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void testGetUsageRuntimeDoc_ConstantFooter(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        //Ensure all the first level elements exist
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_FOOTER);
        list = list.item(0).getChildNodes();
        //1 text fields which is hardcoded, and 4 value sets from getTestMappingValues()
        Assert.assertTrue(Utilities.CONSTANT_FOOTER+" element should have " + (distinctMappingSets.size() + 1)
                + " elements", list.getLength() == distinctMappingSets.size() + 1);
    }

    @Test
    public void getUsageRuntimeDoc_ConstantFooter_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.CONSTANT_FOOTER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = null;
            if(i < list.getLength() - 1) expectedValue = Utilities.REPORT_ROW_TOTAL_STYLE;
            else expectedValue = Utilities.REPORT_ROW_GRAND_TOTAL_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "$V{COLUMN_MAPPING_TOTAL_"+(i+1)+"}";
            else expectedValue = "$V{GRAND_TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageRuntimeDoc_CheckWidths(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Element rootNode = doc.getDocumentElement();
        testWidths(rootNode, distinctMappingSets.size());
    }

    /**
     * Validate the variables created, Check that their name, class, calculation, resetType and resetGroup if applicable
     * are correct.
     */
    @Test
    public void getUsageIntervalMasterRuntimeDoc_Variables(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();
        String [] variableNames = new String[]{"COLUMN_SERVICE_","COLUMN_OPERATION_","COLUMN_REPORT_"};
        String [] resetTypes = new String[]{"Group","Group","Report"};
        String [] resetGroups = new String[]{"SERVICE","SERVICE_OPERATION",null};
        String [] calculations = new String[]{"Sum","Sum","Sum"};

        int index = 0;
        int varIndex = 1;
        for(int i = 0; i < list.getLength(); i++,varIndex++){
            if(i > 0 && (i % distinctMappingSets.size()  == 0)){
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = variableNames[index % distinctMappingSets.size()]+varIndex;
            Assert.assertTrue("Name attribute should equal " + expectedValue+ " Actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = resetTypes[index % distinctMappingSets.size()];
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            if(resetType.equals("Group")){
                //check the actual group used to reset the variable is correct
                Node varResetGroup = attributes.getNamedItem("resetGroup");
                String resetGroup = varResetGroup.getNodeValue();

                expectedValue = resetGroups[index % distinctMappingSets.size()];
                Assert.assertTrue("resetGroup attribute should equal " + expectedValue+ " Actual value was: " + resetGroup
                        ,resetGroup.equals(expectedValue));
            }

            expectedValue = calculations[index % distinctMappingSets.size()];
            Node calcName = attributes.getNamedItem("calculation");

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceHeader_CheckElements(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_HEADER);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.MAPPING_VALUE_FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i == list.getLength() - 1) expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;
            else expectedIntValue = Utilities.DATA_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            if(i == 0) expectedIntValue = Utilities.CONSTANT_HEADER_START_X;
            else expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);

            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = null;
            if(i < list.getLength() - 1) expectedValue = Utilities.USAGE_TABLE_HEADING_STYLE;
            else expectedValue = Utilities.USAGE_TABLE_HEADING_END_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.String";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_CheckSubReport(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.RETURN_VALUE);
        list = list.item(0).getChildNodes();
        String [] variableNames = new String[]{"COLUMN_SERVICE_","COLUMN_OPERATION_","COLUMN_REPORT_"};
        String [] calculations = new String[]{"Sum","Sum","Sum"};

        int index = 0;
        int varIndex = 1;
        for(int i = 0; i < list.getLength(); i++,varIndex++){
            if(i > 0 && (i % distinctMappingSets.size()  == 0)){
                index++;
                varIndex = 1;
            }

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node calculation = attributes.getNamedItem("calculation");

            String expectedValue = calculations[index % distinctMappingSets.size()];
            String actualValue = calculation.getNodeValue();
            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node subreportVariable = attributes.getNamedItem("subreportVariable");
            expectedValue = "COLUMN_" + varIndex;
            actualValue = subreportVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node toVariable = attributes.getNamedItem("toVariable");
            expectedValue = variableNames[index % distinctMappingSets.size()] + varIndex;
            actualValue = toVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

        }
     }

    private void testGroupTotalRow(final String elementName, final String columnVariable, final String totalVariable,
                                   final boolean includeNullTest, final String rowTotalStyle, final String rowGrandTotalStyle){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        NodeList list = doc.getElementsByTagName(elementName);
        list = list.item(0).getChildNodes();
        for(int i = 0 ; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * i);
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue = null;
            if(i < list.getLength() - 1) expectedValue = rowTotalStyle;
            else expectedValue = rowGrandTotalStyle;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            Node mode = attributes.getNamedItem("mode");
            expectedValue = "Opaque";
            String actualValue = mode.getNodeValue();
            Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1){
                if(includeNullTest){
                    expectedValue = "($V{"+columnVariable+(i+1)+"} == null)?new Long(0):$V{"+columnVariable+(i+1)+"}";
                }else{
                    expectedValue = "$V{"+columnVariable+(i+1)+"}";                    
                }
            }
            else expectedValue = "$V{"+totalVariable+"}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));
        }
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceAndOperationFooter_CheckElements(){
        testGroupTotalRow(Utilities.SERVICE_AND_OPERATION_FOOTER,"COLUMN_OPERATION_","ROW_OPERATION_TOTAL",
                true, Utilities.ROW_TOTAL_STYLE, Utilities.ROW_GRAND_TOTAL_STYLE);
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_serviceIdFooter_CheckElements(){
        testGroupTotalRow(Utilities.SERVICE_ID_FOOTER,"COLUMN_SERVICE_","ROW_SERVICE_TOTAL", true,
                Utilities.ROW_TOTAL_STYLE, Utilities.ROW_GRAND_TOTAL_STYLE);
    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_summary_CheckElements(){
        testGroupTotalRow(Utilities.SUMMARY,"COLUMN_REPORT_","ROW_REPORT_TOTAL", false,
                Utilities.REPORT_ROW_TOTAL_STYLE, Utilities.REPORT_ROW_GRAND_TOTAL_STYLE);
    }

    private void testWidths(Node rootNode, int numMappingValues){
        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        int expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * numMappingValues)
                + Utilities.TOTAL_COLUMN_WIDTH + Utilities.LEFT_MARGIN_WIDTH + Utilities.RIGHT_MARGIN_WIDTH;
        int actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node columnWidth = findFirstChildElementByName(rootNode, "columnWidth");
        expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * numMappingValues)
                + Utilities.TOTAL_COLUMN_WIDTH;
        actualIntValue = Integer.valueOf(columnWidth.getTextContent());

        Assert.assertTrue("columnWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node frameWidth = findFirstChildElementByName(rootNode, "frameWidth");
        expectedIntValue = Utilities.CONSTANT_HEADER_START_X + (Utilities.DATA_COLUMN_WIDTH * numMappingValues)
                + Utilities.TOTAL_COLUMN_WIDTH;
        actualIntValue = Integer.valueOf(frameWidth.getTextContent());

        Assert.assertTrue("frameWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node leftMargin = findFirstChildElementByName(rootNode, "leftMargin");
        expectedIntValue = Utilities.LEFT_MARGIN_WIDTH;
        actualIntValue = Integer.valueOf(leftMargin.getTextContent());

        Assert.assertTrue("leftMargin element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node rightMargin = findFirstChildElementByName(rootNode, "rightMargin");
        expectedIntValue = Utilities.RIGHT_MARGIN_WIDTH;
        actualIntValue = Integer.valueOf(rightMargin.getTextContent());

        Assert.assertTrue("rightMargin element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

    }

    @Test
    public void getUsageIntervalMasterRuntimeDoc_CheckWidths(){
        LinkedHashMap<String, List<ReportApi.FilterPair>> keysToFilterPairs = getTestKeys();
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        Element rootNode = doc.getDocumentElement();
        testWidths(rootNode, distinctMappingSets.size());
    }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_Variables(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = "COLUMN_"+(i+1);
            Assert.assertTrue("Name attribute should equal " + expectedValue+ " Actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = "Report";
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            Node calcName = attributes.getNamedItem("calculation");
            expectedValue = "Sum";

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_CheckSubReport(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();
        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.RETURN_VALUE);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node calculation = attributes.getNamedItem("calculation");

            String expectedValue = "Sum";
            String actualValue = calculation.getNodeValue();
            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node subreportVariable = attributes.getNamedItem("subreportVariable");
            expectedValue = "COLUMN_" + (i+1);
            actualValue = subreportVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

            Node toVariable = attributes.getNamedItem("toVariable");
            expectedValue = "COLUMN_" + (i+1);
            actualValue = toVariable.getNodeValue();

            Assert.assertTrue("subreportVariable attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                    ,actualValue.equals(expectedValue));

        }
     }

    @Test
    public void getUsageSubIntervalMasterRuntimeDoc_CheckWidths(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        Element rootNode = doc.getDocumentElement();

        Node subReportWidth = findFirstChildElementByName(rootNode, "subReportWidth");
        int expectedIntValue = (Utilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + Utilities.TOTAL_COLUMN_WIDTH;
        int actualIntValue = Integer.valueOf(subReportWidth.getTextContent());

        Assert.assertTrue("subReportWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        expectedIntValue = (Utilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + Utilities.TOTAL_COLUMN_WIDTH
                + Utilities.SUB_INTERVAL_STATIC_WIDTH;
        actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

    }

    @Test
    public void getUsageSubReportRuntimeDoc_Variables(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.VARIABLES);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){

            NamedNodeMap attributes = list.item(i).getAttributes();
            Node varName = attributes.getNamedItem("name");

            //Going to check the name, class, resetType and resetGroup
            String expectedValue = "COLUMN_"+(i+1);
            Assert.assertTrue("Name attribute should equal " + expectedValue+ " Actual value was: " + varName.getNodeValue()
                    ,varName.getNodeValue().equals(expectedValue));

            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            Node varResetType = attributes.getNamedItem("resetType");
            String resetType = varResetType.getNodeValue();

            expectedValue = "None";
            Assert.assertTrue("resetType attribute should equal " + expectedValue+ " Actual value was: " + resetType
                    ,resetType.equals(expectedValue));

            Node calcName = attributes.getNamedItem("calculation");
            expectedValue = "Nothing";

            Assert.assertTrue("calculation attribute should equal " + expectedValue+ " Actual value was: " +
                    calcName.getNodeValue() ,calcName.getNodeValue().equals(expectedValue));

            //variableExpression
            Node variableExpression = findFirstChildElementByName(list.item(i), "variableExpression");

            expectedValue = "((UsageSummaryAndSubReportHelper)$P{REPORT_SCRIPTLET}).getColumnValue(\"COLUMN_"+(i+1)+"\", " +
                    "$F{AUTHENTICATED_USER},new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3}," +
                    "$F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})";
            String actualValue = variableExpression.getTextContent();
            Assert.assertTrue("variableExpression elements text should equal " + expectedValue+ " Actual value was: " +
                    actualValue ,actualValue.equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_serviceAndOperationFooter_CheckElements(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.SERVICE_AND_OPERATION_FOOTER);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){
            Node textFieldNode = list.item(i);

            Node reportElementNode = findFirstChildElementByName(textFieldNode, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.DATA_COLUMN_WIDTH * i;
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if(i < list.getLength() - 1) expectedValue = "TableCell";
            else expectedValue = Utilities.ROW_TOTAL_MINOR_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            if(i == list.getLength() - 1){
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                        ,actualValue.equals(expectedValue));
            }

            Node textFieldExprNode = findFirstChildElementByName(textFieldNode, "textFieldExpression");
            attributes = textFieldExprNode.getAttributes();
            Node varClass = attributes.getNamedItem("class");
            String classType = varClass.getNodeValue();

            expectedValue = "java.lang.Long";
            Assert.assertTrue("Class attribute should equal " + expectedValue+ " Actual value was: " + classType
                    ,classType.equals(expectedValue));

            if(i < list.getLength() - 1) expectedValue = "($V{COLUMN_"+(i+1)+"} == null)?new Long(0):$V{COLUMN_"+(i+1)+"}";
            else expectedValue = "$V{TOTAL}";

            Assert.assertTrue("text expression should equal " + expectedValue+ " Actual value was: " +
                    textFieldExprNode.getTextContent() ,textFieldExprNode.getTextContent().equals(expectedValue));

            //make sure the html markup attribute is also specified
            Node textElementNode = findFirstChildElementByName(textFieldNode, "textElement");
            attributes = textElementNode.getAttributes();
            Node markUp = attributes.getNamedItem("markup");
            expectedValue = "html";
            Assert.assertTrue("markup attribute should equal " + expectedValue+ " Actual value was: " +
                    markUp.getNodeValue() ,markUp.getNodeValue().equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_noData_CheckElements(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        NodeList list = doc.getElementsByTagName(Utilities.NO_DATA);
        list = list.item(0).getChildNodes();

        for(int i = 0; i < list.getLength(); i++){
            Node staticTextField = list.item(i);

            Node reportElementNode = findFirstChildElementByName(staticTextField, "reportElement");
            NamedNodeMap attributes = reportElementNode.getAttributes();
            Node height = attributes.getNamedItem("height");
            int actualIntValue = Integer.valueOf(height.getNodeValue());
            int expectedIntValue = Utilities.FIELD_HEIGHT;

            Assert.assertTrue("height attribute should equal " + Utilities.FIELD_HEIGHT+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node width = attributes.getNamedItem("width");
            actualIntValue = Integer.valueOf(width.getNodeValue());
            if(i < list.getLength() - 1) expectedIntValue = Utilities.DATA_COLUMN_WIDTH;
            else expectedIntValue = Utilities.TOTAL_COLUMN_WIDTH;

            Assert.assertTrue("width attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node x = attributes.getNamedItem("x");
            expectedIntValue = Utilities.DATA_COLUMN_WIDTH * i;
            actualIntValue = Integer.valueOf(x.getNodeValue());

            Assert.assertTrue("x attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node y = attributes.getNamedItem("y");
            expectedIntValue = 0;
            actualIntValue = Integer.valueOf(y.getNodeValue());

            Assert.assertTrue("y attribute should equal " + expectedIntValue+ " Actual value was: "
                    + actualIntValue, actualIntValue == expectedIntValue);

            Node style = attributes.getNamedItem("style");
            String expectedValue;
            if(i < list.getLength() - 1) expectedValue = "TableCell";
            else expectedValue = Utilities.ROW_TOTAL_MINOR_STYLE;

            Assert.assertTrue("style attribute should equal " + expectedValue+ " Actual value was: " + style.getNodeValue()
                    ,style.getNodeValue().equals(expectedValue));

            if(i == list.getLength() - 1){
                Node mode = attributes.getNamedItem("mode");
                expectedValue = "Opaque";
                String actualValue = mode.getNodeValue();
                Assert.assertTrue("mode attribute should equal " + expectedValue+ " Actual value was: " + actualValue
                        ,actualValue.equals(expectedValue));
            }

            Node textNode = findFirstChildElementByName(staticTextField, "text");
            expectedValue = "NA";
            String actualValue = textNode.getTextContent();

            Assert.assertTrue("text element should equal " + expectedValue+ " Actual value was: " +
                    actualValue ,actualValue.equals(expectedValue));

        }
    }

    @Test
    public void getUsageSubReportRuntimeDoc_CheckWidths(){
        LinkedHashSet<List<String>> distinctMappingSets = getTestDistinctMappingSets();

        Document doc = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        Element rootNode = doc.getDocumentElement();

        Node pageWidth = findFirstChildElementByName(rootNode, "pageWidth");
        int expectedIntValue = (Utilities.DATA_COLUMN_WIDTH * distinctMappingSets.size()) + Utilities.TOTAL_COLUMN_WIDTH;
        int actualIntValue = Integer.valueOf(pageWidth.getTextContent());

        Assert.assertTrue("pageWidth element should equal " + expectedIntValue+ " Actual value was: " +
                actualIntValue ,actualIntValue == expectedIntValue);

    }

}
