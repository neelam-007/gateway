package com.l7tech.common.xml;
import javax.xml.namespace.QName;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * Class <code>XmlSchemaConstants</code> is a bag of XML schema constants
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class XmlSchemaConstants {
    /**
     * this class cannot be instantiated
     */
    private XmlSchemaConstants() {
    }
    public static final QName QNAME_TYPE_ANY_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "anyType");
    public static final QName QNAME_TYPE_ANY_URI = new QName("http://www.w3.org/2001/XMLSchema", "anyURI");
    public static final QName QNAME_TYPE_BASE64_BINARY = new QName("http://www.w3.org/2001/XMLSchema", "base64Binary");
    public static final QName QNAME_TYPE_BOOLEAN = new QName("http://www.w3.org/2001/XMLSchema", "boolean");
    public static final QName QNAME_TYPE_BYTE = new QName("http://www.w3.org/2001/XMLSchema", "byte");
    public static final QName QNAME_TYPE_DATE = new QName("http://www.w3.org/2001/XMLSchema", "date");
    public static final QName QNAME_TYPE_DATE_TIME = new QName("http://www.w3.org/2001/XMLSchema", "dateTime");
    public static final QName QNAME_TYPE_DECIMAL = new QName("http://www.w3.org/2001/XMLSchema", "decimal");
    public static final QName QNAME_TYPE_DOUBLE = new QName("http://www.w3.org/2001/XMLSchema", "double");
    public static final QName QNAME_TYPE_FLOAT = new QName("http://www.w3.org/2001/XMLSchema", "float");
    public static final QName QNAME_TYPE_GDAY = new QName("http://www.w3.org/2001/XMLSchema", "gDay");
    public static final QName QNAME_TYPE_GMONTH = new QName("http://www.w3.org/2001/XMLSchema", "gMonth");
    public static final QName QNAME_TYPE_GMONTH_DAY = new QName("http://www.w3.org/2001/XMLSchema", "gMonthDay");
    public static final QName QNAME_TYPE_GYEAR = new QName("http://www.w3.org/2001/XMLSchema", "gYear");
    public static final QName QNAME_TYPE_GYEAR_MONTH = new QName("http://www.w3.org/2001/XMLSchema", "gYearMonth");
    public static final QName QNAME_TYPE_HEX_BINARY = new QName("http://www.w3.org/2001/XMLSchema", "hexBinary");
    public static final QName QNAME_TYPE_INT = new QName("http://www.w3.org/2001/XMLSchema", "int");
    public static final QName QNAME_TYPE_INTEGER = new QName("http://www.w3.org/2001/XMLSchema", "integer");
    public static final QName QNAME_TYPE_LONG = new QName("http://www.w3.org/2001/XMLSchema", "long");
    public static final QName QNAME_TYPE_NAME = new QName("http://www.w3.org/2001/XMLSchema", "Name");
    public static final QName QNAME_TYPE_NEGATIVE_INTEGER = new QName("http://www.w3.org/2001/XMLSchema", "negativeInteger");
    public static final QName QNAME_TYPE_NON_NEGATIVE_INTEGER = new QName("http://www.w3.org/2001/XMLSchema", "nonNegativeInteger");
    public static final QName QNAME_TYPE_NON_POSITIVE_INTEGER = new QName("http://www.w3.org/2001/XMLSchema", "nonPositiveInteger");
    public static final QName QNAME_TYPE_NORMALIZED_STRING = new QName("http://www.w3.org/2001/XMLSchema", "normalizedString");
    public static final QName QNAME_TYPE_POSITIVE_INTEGER = new QName("http://www.w3.org/2001/XMLSchema", "positiveInteger");
    public static final QName QNAME_TYPE_SHORT = new QName("http://www.w3.org/2001/XMLSchema", "short");
    public static final QName QNAME_TYPE_STRING = new QName("http://www.w3.org/2001/XMLSchema", "string");
    public static final QName QNAME_TYPE_TIME = new QName("http://www.w3.org/2001/XMLSchema", "time");
    public static final QName QNAME_TYPE_TOKEN = new QName("http://www.w3.org/2001/XMLSchema", "token");
    public static final QName QNAME_TYPE_UNSIGNED_BYTE = new QName("http://www.w3.org/2001/XMLSchema", "unsignedByte");
    public static final QName QNAME_TYPE_UNSIGNED_INT = new QName("http://www.w3.org/2001/XMLSchema", "unsignedInt");
    public static final QName QNAME_TYPE_UNSIGNED_LONG = new QName("http://www.w3.org/2001/XMLSchema", "unsignedLong");
    public static final QName QNAME_TYPE_UNSIGNED_SHORT = new QName("http://www.w3.org/2001/XMLSchema", "unsignedShort");

    public static Set QNAMES = new HashSet();
    static {
        QNAMES.add(QNAME_TYPE_ANY_TYPE);
        QNAMES.add(QNAME_TYPE_ANY_URI);
        QNAMES.add(QNAME_TYPE_BASE64_BINARY);
        QNAMES.add(QNAME_TYPE_BOOLEAN);
        QNAMES.add(QNAME_TYPE_BYTE);
        QNAMES.add(QNAME_TYPE_DATE);
        QNAMES.add(QNAME_TYPE_DATE_TIME);
        QNAMES.add(QNAME_TYPE_DECIMAL);
        QNAMES.add(QNAME_TYPE_DOUBLE);
        QNAMES.add(QNAME_TYPE_FLOAT);
        QNAMES.add(QNAME_TYPE_GDAY);
        QNAMES.add(QNAME_TYPE_GMONTH);
        QNAMES.add(QNAME_TYPE_GMONTH_DAY);
        QNAMES.add(QNAME_TYPE_GYEAR);
        QNAMES.add(QNAME_TYPE_GYEAR_MONTH);
        QNAMES.add(QNAME_TYPE_HEX_BINARY);
        QNAMES.add(QNAME_TYPE_INT);
        QNAMES.add(QNAME_TYPE_INTEGER);
        QNAMES.add(QNAME_TYPE_LONG);
        QNAMES.add(QNAME_TYPE_NAME);
        QNAMES.add(QNAME_TYPE_NEGATIVE_INTEGER);
        QNAMES.add(QNAME_TYPE_NON_NEGATIVE_INTEGER);
        QNAMES.add(QNAME_TYPE_NON_POSITIVE_INTEGER);
        QNAMES.add(QNAME_TYPE_NORMALIZED_STRING);
        QNAMES.add(QNAME_TYPE_POSITIVE_INTEGER);
        QNAMES.add(QNAME_TYPE_SHORT);
        QNAMES.add(QNAME_TYPE_STRING);
        QNAMES.add(QNAME_TYPE_TIME);
        QNAMES.add(QNAME_TYPE_TOKEN);
        QNAMES.add(QNAME_TYPE_UNSIGNED_BYTE);
        QNAMES.add(QNAME_TYPE_UNSIGNED_INT);
        QNAMES.add(QNAME_TYPE_UNSIGNED_LONG);
        QNAMES.add(QNAME_TYPE_UNSIGNED_SHORT);
        QNAMES = Collections.unmodifiableSet(QNAMES);
    }
}
