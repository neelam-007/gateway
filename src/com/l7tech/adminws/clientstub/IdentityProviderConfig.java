/**
 * IdentityProviderConfig.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.l7tech.adminws.clientstub;

public class IdentityProviderConfig  implements java.io.Serializable {
    private java.lang.String description;
    private java.lang.String name;
    private long oid;
    private java.lang.String typeClassName;
    private java.lang.String typeDescription;
    private java.lang.String typeName;
    private long typeOid;

    public IdentityProviderConfig() {
    }

    public java.lang.String getDescription() {
        return description;
    }

    public void setDescription(java.lang.String description) {
        this.description = description;
    }

    public java.lang.String getName() {
        return name;
    }

    public void setName(java.lang.String name) {
        this.name = name;
    }

    public long getOid() {
        return oid;
    }

    public void setOid(long oid) {
        this.oid = oid;
    }

    public java.lang.String getTypeClassName() {
        return typeClassName;
    }

    public void setTypeClassName(java.lang.String typeClassName) {
        this.typeClassName = typeClassName;
    }

    public java.lang.String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(java.lang.String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public java.lang.String getTypeName() {
        return typeName;
    }

    public void setTypeName(java.lang.String typeName) {
        this.typeName = typeName;
    }

    public long getTypeOid() {
        return typeOid;
    }

    public void setTypeOid(long typeOid) {
        this.typeOid = typeOid;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof IdentityProviderConfig)) return false;
        IdentityProviderConfig other = (IdentityProviderConfig) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.description==null && other.getDescription()==null) || 
             (this.description!=null &&
              this.description.equals(other.getDescription()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            this.oid == other.getOid() &&
            ((this.typeClassName==null && other.getTypeClassName()==null) || 
             (this.typeClassName!=null &&
              this.typeClassName.equals(other.getTypeClassName()))) &&
            ((this.typeDescription==null && other.getTypeDescription()==null) || 
             (this.typeDescription!=null &&
              this.typeDescription.equals(other.getTypeDescription()))) &&
            ((this.typeName==null && other.getTypeName()==null) || 
             (this.typeName!=null &&
              this.typeName.equals(other.getTypeName()))) &&
            this.typeOid == other.getTypeOid();
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getDescription() != null) {
            _hashCode += getDescription().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        _hashCode += new Long(getOid()).hashCode();
        if (getTypeClassName() != null) {
            _hashCode += getTypeClassName().hashCode();
        }
        if (getTypeDescription() != null) {
            _hashCode += getTypeDescription().hashCode();
        }
        if (getTypeName() != null) {
            _hashCode += getTypeName().hashCode();
        }
        _hashCode += new Long(getTypeOid()).hashCode();
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(IdentityProviderConfig.class);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://service.adminws.l7tech.com", "IdentityProviderConfig"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("description");
        elemField.setXmlName(new javax.xml.namespace.QName("", "description"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("", "name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("oid");
        elemField.setXmlName(new javax.xml.namespace.QName("", "oid"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("typeClassName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "typeClassName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("typeDescription");
        elemField.setXmlName(new javax.xml.namespace.QName("", "typeDescription"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("typeName");
        elemField.setXmlName(new javax.xml.namespace.QName("", "typeName"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("typeOid");
        elemField.setXmlName(new javax.xml.namespace.QName("", "typeOid"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "long"));
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
