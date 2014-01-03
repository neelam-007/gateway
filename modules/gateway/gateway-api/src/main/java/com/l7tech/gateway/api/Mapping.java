package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.PropertiesMapType;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.Map;

/**
 * This is the mapping object. It is used to describe the mapping used for import and export.
 */
public class Mapping {
    private String type;
    private String srcId;
    private String referencePath;
    private String srcUri;
    private String targetUri;
    private String targetId;
    private Action action;
    private ActionTaken actionTaken;
    private ErrorType errorType;
    private Map<String, Object> properties;

    public static enum Action {
        NewOrExisting, NewOrUpdate, AlwaysCreateNew, Ignore;
    }

    public static enum ActionTaken {
        UsedExisting, CreatedNew, UpdatedExisting, Ignored;
    }

    public static enum ErrorType {
        TargetExists, TargetNotFound, UniqueKeyConflict;
    }

    Mapping(){}

    @XmlAttribute(name = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @XmlAttribute(name = "srcId")
    public String getSrcId() {
        return srcId;
    }

    public void setSrcId(String srcId) {
        this.srcId = srcId;
    }

    @XmlAttribute(name = "referencePath")
    public String getReferencePath() {
        return referencePath;
    }

    public void setReferencePath(String referencePath) {
        this.referencePath = referencePath;
    }

    @XmlAttribute(name = "srcUri")
    public String getSrcUri() {
        return srcUri;
    }

    public void setSrcUri(String srcUri) {
        this.srcUri = srcUri;
    }

    @XmlAttribute(name = "targetUri")
    public String getTargetUri() {
        return targetUri;
    }

    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri;
    }

    @XmlAttribute(name = "targetId")
    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    @XmlAttribute(name = "action")
    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    @XmlAttribute(name = "actionTaken")
    public ActionTaken getActionTaken() {
        return actionTaken;
    }

    public void setActionTaken(ActionTaken actionTaken) {
        this.actionTaken = actionTaken;
    }

    @XmlAttribute(name = "errorType")
    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    @XmlElement(name = "Properties")
    @XmlJavaTypeAdapter(PropertiesMapType.PropertiesMapTypeAdapter.class)
    public Map<String,Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String,Object> properties) {
        this.properties = properties;
    }
}
