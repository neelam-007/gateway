/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Mar 19, 2009
 * Time: 9:48:07 AM
 */
package com.l7tech.gateway.standardreports;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.CDATASection;
import com.l7tech.common.io.XmlUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * JasperDocument represents the dynamic XML which is created at runtime as part of the report running process.
 * This class encapsulates the DOM which contains this dynamic XML. This is done to ensure that any content entering
 * the DOM is safe, as data can originate from external systems and make its way into the database via message
 * context assertions.
 * <p/>
 * The methods in this class are specific to the task of creating valid Jasper XML. The XML created in this class
 * is extrated to produce java source which is compiled and executed at runtime. As a result there is a very real risk
 * of code injection if any malicious input in the database can cause String literals to be closed and arbitary java
 * statements to be added to the dynamically generated java source.
 * Please see bug http://sarek.l7tech.com/bugzilla/show_bug.cgi?id=6892
 * <p/>
 * This class is final and package protected. It has a very specific use and there is no need to extend or make this
 * class available to any other packages.
 */
public final class JasperDocument {
    public static final String RETURN_VALUE = "returnValue";
    public static final int TOTAL_COLUMN_WIDTH = 80;

    enum ElementName {
        VARIABLES("variables"),
        CONSTANT_HEADER("constantHeader"),
        KEY_INFO("keyInfo"),
        PAGE_WIDTH("pageWidth"),
        SERVICE_AND_OPERATION_FOOTER("serviceAndOperationFooter"),
        COLUMN_WIDTH("columnWidth"),
        FRAME_WIDTH("frameWidth"),
        SERVICE_ID_FOOTER("serviceIdFooter"),
        CONSTANT_FOOTER("constantFooter"),
        LEFT_MARGIN("leftMargin"),
        RIGHT_MARGIN("rightMargin"),
        ROOT_NODE("rootNode"),
        CHART_ELEMENT("chartElement"),
        CHART_LEGEND("chartLegend"),
        CHART_HEIGHT("chartHeight"),
        CHART_LEGEND_FRAME_YPOS("chartLegendFrameYPos"),
        CHART_LEGEND_HEIGHT("chartLegendHeight"),
        CHART_FRAME_HEIGHT("chartFrameHeight"),
        BAND_HEIGHT("bandHeight"),
        PAGE_HEIGHT("pageHeight"),
        SERVICE_HEADER("serviceHeader"),
        SUB_REPORT("subReport"),
        SUMMARY("summary"),
        SUB_REPORT_WIDTH("subReportWidth"),
        NO_DATA("noData"),
        IS_CONTEXT_MAPPING("isContextMapping");

        ElementName(final String name) {
            this.name = name;
        }

        final String getName() {
            return name;
        }

        private final String name;
    }

    enum CLASS_TYPES {
        JAVA_LONG("java.lang.Long"),
        JAVA_STRING("java.lang.String");

        CLASS_TYPES(String type) {
            this.type = type;
        }

        String getType() {
            return type;
        }

        private final String type;
    }

    private final Document document;
    private final Node rootNode;
    public final static String VARIABLE = "variable";
    /**
     * stringNameToElement allows us to look up all created elements by the name used to create them
     */
    private final Map<ElementName, Element> nameToElement;

    JasperDocument() {
        document = XmlUtil.createEmptyDocument("JasperRuntimeTransformation", null, null);
        rootNode = document.getFirstChild();
        nameToElement = new HashMap<ElementName, Element>();
    }

    public final Document getDocument() {
        return document;
    }

    /**
     * Create an element with the supplied name. Once created to add to this element, pass the same string to
     * other methods
     *
     * @param elementName name of element to create
     * @throws IllegalStateException if an element with the same name already exists
     */
    final void createRootDirectAncestorElement(final ElementName elementName) {
        if (nameToElement.containsKey(elementName))
            throw new IllegalStateException("ElementName: " + elementName.getName() + " already exists as an Element");

        Element element = document.createElement(elementName.getName());
        nameToElement.put(elementName, element);
        rootNode.appendChild(element);
    }

    final void createElementAncestorElement(final ElementName elementName, final ElementName appendToElementName) {
        if (nameToElement.containsKey(elementName))
            throw new IllegalStateException("ElementName: " + elementName.getName() + " already exists as an Element");

        if (!nameToElement.containsKey(appendToElementName))
            throw new IllegalStateException("ElementName: " + appendToElementName.getName()
                    + " does not exist as an Element to append to");

        final Element element = document.createElement(elementName.getName());
        nameToElement.put(elementName, element);
        final Element appendToElement = nameToElement.get(appendToElementName);
        appendToElement.appendChild(element);
    }

    /**
     * Get the element for elementName, if it doesn't exist it will be created
     *
     * @param elementName         element to retrieve, or create if required
     * @param appendToElementName if the element needs to be created, it will be appeneded to the Element or Node
     *                            that this ElementName represents
     * @return Element the requested Element
     */
    private Element getElement(final ElementName elementName, final ElementName appendToElementName) {
        if (!nameToElement.containsKey(elementName)) {
            if (appendToElementName == ElementName.ROOT_NODE) {
                createRootDirectAncestorElement(elementName);
            } else {
                createElementAncestorElement(elementName, appendToElementName);
            }
        }
        return nameToElement.get(elementName);
    }

    /**
     * Add the specified String to the specified Element in a CDATA section. If the Element represented by elementName
     * does not exist, it will be created and appended to the root node of the Document
     *
     * @param elementName       the ElementName representing the node to add the CDATA section to
     * @param elementCDataValue the String value to add to the CDATA section
     */
    final void addCDataElement(final ElementName elementName, final String elementCDataValue) {
        addCDataElement(elementName, elementCDataValue, ElementName.ROOT_NODE);
    }

    /**
     * Add a String to an Element in a CDATA section. If the Element represented by elementName
     * does not exist, it will be created and appended to the Element represented by appendToElement
     *
     * @param elementName       the ElementName representing the node to add the CDATA section to
     * @param elementCDataValue the String value to add to the CDATA section
     * @param appendToElement   represents the Element to append the Element represented by elementName to, if it does
     *                          not already exist
     */
    final void addCDataElement(final ElementName elementName,
                               final String elementCDataValue,
                               final ElementName appendToElement) {
        Element element = getElement(elementName, appendToElement);
        final String escapedData = escapeJavaSringLiteralChars(elementCDataValue);
        CDATASection cData = document.createCDATASection(escapedData);
        element.appendChild(cData);
    }

    /**
     * Add the specified int to the specified Element as text content. If the Element represented by elementName
     * does not exist, it will be created and appended to the root node of the Document
     *
     * @param elementName the ElementName representing the node to add the int to
     * @param value       the int value to add as text content
     */
    final void addIntElement(final ElementName elementName, final int value) {
        addIntElement(elementName, value, ElementName.ROOT_NODE);
    }

    /**
     * Add a int to an Element as text content. If the Element represented by elementName
     * does not exist, it will be created and appended to the Element represented by appendToElement
     *
     * @param elementName     the ElementName representing the node to add the CDATA section to
     * @param value           the int value to add as text content
     * @param appendToElement represents the Element to append the Element represented by elementName to, if it does
     *                        not already exist
     */
    final void addIntElement(final ElementName elementName, final int value, final ElementName appendToElement) {
        final Element element = getElement(elementName, appendToElement);
        element.setTextContent(String.valueOf(value));
    }

    /**
     * Create a variable and add it to the element represented by elementName
     * <variable name="COLUMN_1_MAPPING_TOTAL" class="java.lang.Long" resetType="Group" resetGroup="CONSTANT"
     * calculation="Sum">
     * <variableExpression><![CDATA[((UsageReportHelper)$P{REPORT_SCRIPTLET})
     * .getVariableValue("COLUMN_1", $F{AUTHENTICATED_USER},
     * new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3},
     * $F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})]]></variableExpression>
     * </variable>
     * <p/>
     * <p/>
     * All string parameters are escaped with escapeJavaSringLiteralChars()
     *
     * @param elementName  the ElementName representing the element to which the variable will be added
     * @param varName      the variable name
     * @param varClass     the java type the variable is
     * @param resetType    when the variable gets reset
     * @param resetGroup   if the resetType is 'Group', then which group resets the variable
     * @param calc         what calculation the variable performs
     * @param functionName the function name within the report scriptlet which the variable will call
     * @param columnName   parameter to the function which will be called at runtime when this variable is being
     *                     evaluated. The column name is used within the report scriptlet to look up the correct value for this variable
     */
    final void addVariableToElement(final ElementName elementName,
                                    final String varName,
                                    final String varClass,
                                    final String resetType,
                                    final String resetGroup,
                                    final String calc,
                                    final String functionName,
                                    final String columnName) {
        final Element variableElement = getElement(elementName, ElementName.ROOT_NODE);

        final Element newVariable = document.createElement(VARIABLE);
        newVariable.setAttribute("name", escapeJavaSringLiteralChars(varName));
        newVariable.setAttribute("class", escapeJavaSringLiteralChars(varClass));
        newVariable.setAttribute("resetType", escapeJavaSringLiteralChars(resetType));
        if (resetGroup != null && !resetGroup.equals(""))
            newVariable.setAttribute("resetGroup", escapeJavaSringLiteralChars(resetGroup));
        newVariable.setAttribute("calculation", escapeJavaSringLiteralChars(calc));

        final Element variableExpression = document.createElement("variableExpression");
        final String cData = "((UsageSummaryAndSubReportHelper)$P{REPORT_SCRIPTLET})." + escapeJavaSringLiteralChars(functionName) + "(\""
                + escapeJavaSringLiteralChars(columnName) + "\"," +
                " $F{AUTHENTICATED_USER},new String[]{$F{MAPPING_VALUE_1}, $F{MAPPING_VALUE_2}, $F{MAPPING_VALUE_3}," +
                "$F{MAPPING_VALUE_4}, $F{MAPPING_VALUE_5}})";
        final CDATASection cDataSection = document.createCDATASection(cData);
        variableExpression.appendChild(cDataSection);

        newVariable.appendChild(variableExpression);
        variableElement.appendChild(newVariable);
    }

    /**
     * Add a variable to the Element represented by elementName
     * <p/>
     * All string parameters are escaped with escapeJavaSringLiteralChars()
     * <p/>
     * <variable name="COLUMN_SERVICE_1" class="java.lang.Long" resetType="Group" resetGroup="SERVICE" calculation="Sum">
     *
     * @param elementName the ElementName representing the element to which the variable will be added
     * @param varName     variable name
     * @param varClass    variable java class type
     * @param resetType   when the variable gets reset
     * @param resetGroup  if the resetType is 'Group', then which group resets the variable
     * @param calc        what calculation the variable performs
     */
    final void addVariableToElement(final ElementName elementName,
                                    final String varName,
                                    final String varClass,
                                    final String resetType,
                                    final String resetGroup,
                                    final String calc) {
        final Element variableElement = getElement(elementName, ElementName.ROOT_NODE);

        final Element newVariable = document.createElement(VARIABLE);
        newVariable.setAttribute("name", escapeJavaSringLiteralChars(varName));
        newVariable.setAttribute("class", escapeJavaSringLiteralChars(varClass));
        newVariable.setAttribute("resetType", escapeJavaSringLiteralChars(resetType));
        if (resetGroup != null && !resetGroup.equals(""))
            newVariable.setAttribute("resetGroup", escapeJavaSringLiteralChars(resetGroup));
        newVariable.setAttribute("calculation", escapeJavaSringLiteralChars(calc));
        variableElement.appendChild(newVariable);
    }

    /**
     * Add a text field with a String expression. String will always be escaped
     */
    final void addTextFieldToElement(final ElementName elementName,
                                     final int x,
                                     final int y,
                                     final int width,
                                     final int height,
                                     final String key,
                                     final String markedUpCData,
                                     final String style,
                                     final boolean opaque,
                                     final boolean isHtmlFormatted,
                                     final boolean floatElement,
                                     final boolean stretchElement) {

        addTextFieldToElement(elementName, x, y, width, height, key, CLASS_TYPES.JAVA_STRING, markedUpCData, null, style,
                opaque, isHtmlFormatted, floatElement, stretchElement);
    }

    /**
     * Add a text field with a String expression. String will always be escaped
     */
    final void addTextFieldToElement(final ElementName elementName,
                                     final int x,
                                     final int y,
                                     final int width,
                                     final int height,
                                     final String key,
                                     final JasperValidLongExpression longExpression,
                                     final String style,
                                     final boolean opaque,
                                     final boolean isHtmlFormatted,
                                     final boolean floatElement,
                                     final boolean stretchElement) {

        addTextFieldToElement(elementName, x, y, width, height, key, CLASS_TYPES.JAVA_LONG, null, longExpression, style,
                opaque, isHtmlFormatted, floatElement, stretchElement);
    }

    /**
     * Add a text field jrxml element to the Element represented by elementName. The xml created will look like:
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
     * <p/>
     * All string parameters are escaped with escapeJavaSringLiteralChars()
     *
     * @param elementName     The ElementName representing the Element to add this text field to
     * @param x               x value
     * @param y               y value
     * @param width           width
     * @param height          height
     * @param key             the text field key value
     * @param classType       what type of data the text field will hold - String, Integer etc...
     * @param markedUpCData   if data is to be included, then it's put inside a CDATA section to avoid any illegal chars
     * @param longExpression  object representing a long expression
     * @param style           the style to apply to the text field
     * @param opaque          if true, the text element's mode attribute is set to Opaque, otherwise its not set
     * @param isHtmlFormatted if true, the text element's markup attribute is set to HTML
     * @param floatElement    if true, the text element's positionType attribute is set to be Float,
     *                        otherwise it's not set
     * @param stretchElement  if true, the text element's stretchType attribute is set to RelativeToTallestObject,
     *                        otherwise it's not set
     */
    private void addTextFieldToElement(final ElementName elementName,
                                       final int x,
                                       final int y,
                                       final int width,
                                       final int height,
                                       final String key,
                                       final CLASS_TYPES classType,
                                       final String markedUpCData,
                                       final JasperValidLongExpression longExpression,
                                       final String style,
                                       final boolean opaque,
                                       final boolean isHtmlFormatted,
                                       final boolean floatElement,
                                       final boolean stretchElement) {
        final Element textField = document.createElement("textField");
        textField.setAttribute("isStretchWithOverflow", "true");
        textField.setAttribute("isBlankWhenNull", "false");
        textField.setAttribute("evaluationTime", "Now");
        textField.setAttribute("hyperlinkType", "None");
        textField.setAttribute("hyperlinkTarget", "Self");

        final Element reportElement = document.createElement("reportElement");
        reportElement.setAttribute("x", String.valueOf(x));
        reportElement.setAttribute("y", String.valueOf(y));
        reportElement.setAttribute("width", String.valueOf(width));
        reportElement.setAttribute("height", String.valueOf(height));
        reportElement.setAttribute("key", escapeJavaSringLiteralChars(key));
        reportElement.setAttribute("style", escapeJavaSringLiteralChars(style));

        if (stretchElement) reportElement.setAttribute("stretchType", "RelativeToTallestObject");

        if (floatElement) reportElement.setAttribute("positionType", "Float");

        if (opaque) reportElement.setAttribute("mode", "Opaque");

        textField.appendChild(reportElement);

        final Element boxElement = document.createElement("box");
        textField.appendChild(boxElement);

        final Element textElement = document.createElement("textElement");
        if (isHtmlFormatted) textElement.setAttribute("markup", "html");

        final Element fontElement = document.createElement("font");
        textElement.appendChild(fontElement);
        textField.appendChild(textElement);

        final Element textFieldExpressionElement = document.createElement("textFieldExpression");
        textFieldExpressionElement.setAttribute("class", classType.getType());

        CDATASection cDataSection;
        switch (classType) {
            case JAVA_LONG:
                if (longExpression == null)
                    throw new NullPointerException("longExpression cannot be null when classType represents a long");
                cDataSection = document.createCDATASection(longExpression.getVariableString());
                break;
            case JAVA_STRING:
                //markedupcdata can be the empty string
                if (markedUpCData == null)
                    throw new NullPointerException("markedUpCData cannot be null when classType represents a String");
                cDataSection = document.createCDATASection("\"" + escapeJavaSringLiteralChars(markedUpCData) + "\"");
                break;
            default:
                throw new IllegalStateException("Unsupported CLASS_TYPE");
        }


        textFieldExpressionElement.appendChild(cDataSection);
        textField.appendChild(textFieldExpressionElement);

        final Element frameElement = getElement(elementName, ElementName.ROOT_NODE);
        frameElement.appendChild(textField);
    }

    /**
     * Add a sub report return variable to the jrxml
     * <p/>
     * All string parameters are escaped with escapeJavaSringLiteralChars()
     * <p/>
     * <returnValue subreportVariable="COLUMN_1" toVariable="COLUMN_SERVICE_1" calculation="Sum"/>
     *
     * @param subReportVariable the variable from the sub report which will be passed into toVariable
     * @param toVariable        the variable name of the report this element will belong to, into which the sub report value
     *                          will be placed
     * @param calc              what calculation the variable performs
     */
    final void addSubReportReturnVariable(final String subReportVariable, final String toVariable, final String calc) {

        final Element subReportElement = getElement(ElementName.SUB_REPORT, ElementName.ROOT_NODE);
        final Element newVariable = document.createElement(RETURN_VALUE);
        newVariable.setAttribute("subreportVariable", escapeJavaSringLiteralChars(subReportVariable));
        newVariable.setAttribute("toVariable", escapeJavaSringLiteralChars(toVariable));
        newVariable.setAttribute("calculation", escapeJavaSringLiteralChars(calc));
        subReportElement.appendChild(newVariable);
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
     * <p/>
     * All string parameters are escaped with escapeJavaSringLiteralChars()
     *
     * @param frameElementName the ElementName representing the Element to add the text field to
     * @param x                x value
     * @param y                y value
     * @param width            width
     * @param height           height
     * @param key              the text field key value
     * @param markedUpCData    if data is to be included, then it's put inside a CDATA section to avoid any illegal chars
     * @param style            the style to apply to the text field
     * @param opaque           if true the text field is not see through, if false the style of the element behind it will come
     *                         through
     */
    final void addStaticTextToElement(final ElementName frameElementName,
                                      final int x,
                                      final int y,
                                      final int width,
                                      final int height,
                                      final String key,
                                      final String markedUpCData,
                                      final String style,
                                      final boolean opaque) {
        final Element frameElement = getElement(frameElementName, ElementName.ROOT_NODE);
        final Element staticText = document.createElement("staticText");

        final Element reportElement = document.createElement("reportElement");
        reportElement.setAttribute("x", String.valueOf(x));
        reportElement.setAttribute("y", String.valueOf(y));
        reportElement.setAttribute("width", String.valueOf(width));
        reportElement.setAttribute("height", String.valueOf(height));
        reportElement.setAttribute("key", escapeJavaSringLiteralChars(key));
        reportElement.setAttribute("style", escapeJavaSringLiteralChars(style));
        if (opaque) reportElement.setAttribute("mode", "Opaque");
        staticText.appendChild(reportElement);

        final Element boxElement = document.createElement("box");
        staticText.appendChild(boxElement);

        final Element textElement = document.createElement("textElement");
        textElement.setAttribute("markup", "html");
        final Element fontElement = document.createElement("font");
        textElement.appendChild(fontElement);
        staticText.appendChild(textElement);

        final Element text = document.createElement("text");
        final CDATASection cDataSection = document.createCDATASection(escapeJavaSringLiteralChars(markedUpCData));
        text.appendChild(cDataSection);
        staticText.appendChild(text);
        frameElement.appendChild(staticText);
    }

    /**
     * Escape any characters which are illegal in a java string, used by runtime methods
     * Values used in the xml created by methods in this class are placed into jasper xml files and compiled.
     * As a result it's possible for values found in the database to be illegal in java statements and to cause
     * code injection. All places where strings are placed into the runtime xml documents are ran through
     * this method to escape / removed any problamatic strings
     * <p/>
     * This method is called from addTextFieldToElement, addSubReportReturnVariable and addVariableToElement
     *
     * @param stringToEscape The string which may contain characters which could cause code injection
     * @return a string safe to use in XML which will be used as input to java source with no risk of code injection
     */
    static String escapeJavaSringLiteralChars(final String stringToEscape) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stringToEscape.length(); i++) {
            final char c = stringToEscape.charAt(i);

            switch (c) {
                case '\\':
                    sb.append("\\\\");
                    break;
                case '"':
                    sb.append("\\\"");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb.toString();
    }
}
