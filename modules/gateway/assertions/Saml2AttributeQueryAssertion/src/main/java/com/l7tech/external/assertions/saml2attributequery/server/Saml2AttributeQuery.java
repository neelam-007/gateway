package com.l7tech.external.assertions.saml2attributequery.server;

import com.l7tech.external.assertions.saml2attributequery.SamlToLdapMap;
import com.l7tech.security.saml.SamlConstants;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 12:35:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQuery {
    public static class QueriedAttribute {
        public String samlName;
        public String nameFormat;

        public QueriedAttribute(String samlName, String nameFormat) {
            this.samlName = samlName;
            this.nameFormat = nameFormat;
        }
    }

    public static class AttributeFilter {
        private String saml2AttributeName;
        private String ldapAttributeName;
        private String nameFormat;
        private Set<String> allowedValues = new HashSet<String>();

        public AttributeFilter(String saml2AttributeName, String ldapAttributeName, String nameFormat) {
            this.saml2AttributeName = saml2AttributeName;
            this.ldapAttributeName = ldapAttributeName;
            this.nameFormat = nameFormat;
        }

        public void addAllowedValue(String allowedValue) {
            allowedValues.add(allowedValue);
        }

        public boolean isValueAllowed(String value) {
            return allowedValues.isEmpty() || allowedValues.contains(value);
        }

        protected Set<String> getAllowedValues() {
            return allowedValues;
        }

        public String getSaml2AttributeName() {
            return saml2AttributeName;
        }

        public String getLdapAttributeName() {
            return ldapAttributeName;
        }

        public String getNameFormat() {
            return nameFormat;
        }

        public void setNameFormat(String nameFormat) {
            this.nameFormat = nameFormat;
        }
    }

    private SamlToLdapMap map;
    private String id;
    private String subject;
    private String subjectNameFormat;
    private String subjectNameQualifier;
    private List<AttributeFilter> attributeFilters = new ArrayList<AttributeFilter>();

    public Saml2AttributeQuery(Element attributeQueryElement, SamlToLdapMap map) throws SamlAttributeNotMappedException {
        this.map = map;
        
        id = attributeQueryElement.getAttribute("ID");
        
        NodeList elements = attributeQueryElement.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Subject");
        if(elements.getLength() > 0) {
            Element subjectElement = (Element)elements.item(0);
            elements = subjectElement.getElementsByTagNameNS(SamlConstants.NS_SAML2, "NameID");
            if(elements.getLength() > 0) {
                Element nameIdElement = (Element)elements.item(0);
                subject = nameIdElement.getTextContent();
                subjectNameFormat = nameIdElement.getAttribute("Format");
                if(nameIdElement.hasAttribute("NameQualifier")) {
                    subjectNameQualifier = nameIdElement.getAttribute("NameQualifier");
                }
            }
        }

        elements = attributeQueryElement.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Attribute");
        for(int i = 0;i < elements.getLength();i++) {
            Element attributeElement = (Element)elements.item(i);

            String saml2AttributeName = attributeElement.getAttribute("Name");
            String format = attributeElement.hasAttribute("NameFormat") ? attributeElement.getAttribute("NameFormat") : SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED;

            if(!map.containsSamlName(saml2AttributeName) || !map.getNameFormatFromSamlName(saml2AttributeName).equals(format)) {
                throw new SamlAttributeNotMappedException(saml2AttributeName);
            }

            AttributeFilter attributeFilter = new AttributeFilter(saml2AttributeName, map.getLdapName(saml2AttributeName), format);

            NodeList valueElements = attributeElement.getElementsByTagNameNS(SamlConstants.NS_SAML2, "AttributeValue");
            for(int j = 0;j < valueElements.getLength();j++) {
                Element valueElement = (Element)valueElements.item(j);
                attributeFilter.addAllowedValue(valueElement.getTextContent());
            }

            attributeFilters.add(attributeFilter);
        }
    }

    protected Saml2AttributeQuery(String id,
                                  String subject,
                                  String subjectNameFormat,
                                  String subjectNameQualifier,
                                  List<AttributeFilter> attributeFilters)
    {
        this.id = id;
        this.subject = subject;
        this.subjectNameFormat = subjectNameFormat;
        this.subjectNameQualifier = subjectNameQualifier;

        this.attributeFilters = attributeFilters;
    }

    public String getID() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getSubjectNameFormat() {
        return subjectNameFormat;
    }

    public String getSubjectNameQualifier() {
        return subjectNameQualifier;
    }

    public String[] getLdapAttributeNames() {
        String[] retval = new String[attributeFilters.size()];

        for(int i = 0;i < attributeFilters.size();i++) {
            retval[i] = attributeFilters.get(i).ldapAttributeName;
        }

        return retval;
    }

    public List<AttributeFilter> getAttributeFilters() {
        return Collections.unmodifiableList(attributeFilters);
    }

    public void filterValues(HashMap<String, Object> values) {
        if(attributeFilters.size() > 0) { // Filter using the requested attributes
            for(AttributeFilter filter : attributeFilters) {
                if(values.containsKey(filter.ldapAttributeName)) {
                    Object v = values.get(filter.ldapAttributeName);
                    if(v instanceof String) {
                        if(!filter.isValueAllowed((String)v)) {
                            values.remove(filter.ldapAttributeName);
                        }
                    } else if(v instanceof List) {
                        List<String> valueList = (List<String>)v;
                        for(Iterator<String> it = valueList.iterator();it.hasNext();) {
                            String testValue = it.next();
                            if(!filter.isValueAllowed(testValue)) {
                                it.remove();
                            }
                        }
                    }
                }
            }
        } else { // Filter using the map
            for(Iterator<Map.Entry<String, Object>> it = values.entrySet().iterator();it.hasNext();) {
                Map.Entry<String, Object> entry = it.next();

                if(!map.containsLdapName(entry.getKey())) {
                    it.remove();
                }
            }
        }
    }

    public static List<QueriedAttribute> getQueriedAttributes(Element attributeQueryElement) {
        List<QueriedAttribute> retval = new ArrayList<QueriedAttribute>();

        NodeList elements = attributeQueryElement.getElementsByTagNameNS(SamlConstants.NS_SAML2, "Attribute");
        for(int i = 0;i < elements.getLength();i++) {
            Element attributeElement = (Element)elements.item(i);

            retval.add(new QueriedAttribute(attributeElement.getAttribute("Name"), attributeElement.getAttribute("NameFormat")));
        }

        return retval;
    }

    public static String getQueryId(Element attributeQueryElement) {
        return attributeQueryElement.getAttribute("ID");
    }
}
