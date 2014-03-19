package com.l7tech.gateway.api;

import com.l7tech.gateway.api.impl.IdListToStringTypeAdapter;
import com.l7tech.gateway.api.impl.PropertiesMapType;
import org.jetbrains.annotations.NotNull;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is the mapping object. It is used to describe the mapping used for import and export.
 */
@XmlRootElement(name = "Mapping")
@XmlType(name = "MappingType", propOrder = {"properties"})
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
    private List<String> dependencies;

    public static enum Action {
        NewOrExisting, NewOrUpdate, AlwaysCreateNew, Ignore
    }

    public static enum ActionTaken {
        UsedExisting, CreatedNew, UpdatedExisting, Ignored
    }

    public static enum ErrorType {
        TargetExists, TargetNotFound, UniqueKeyConflict, CannotReplaceDependency, InvalidResource
    }

    Mapping(){}

    Mapping(Mapping mapping) {
        this.type = mapping.getType();
        this.srcId = mapping.getSrcId();
        this.referencePath = mapping.getReferencePath();
        this.srcUri = mapping.getSrcUri();
        this.targetUri = mapping.getTargetUri();
        this.targetId = mapping.getTargetId();
        this.action = mapping.getAction();
        this.actionTaken = mapping.getActionTaken();
        this.errorType = mapping.getErrorType();
        if(mapping.getProperties() != null) {
            this.properties = new HashMap<>(mapping.getProperties());
        }
    }

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

    @XmlAttribute(name = "dependencies")
    @XmlJavaTypeAdapter(IdListToStringTypeAdapter.class)
    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * This will retrieve a property from the properties map
     *
     * @param key The property to retrieve
     * @param <T> The Type of the property
     * @return The property value or null if there is no such property.
     */
    public <T> T getProperty(String key) {
        return properties != null ? (T) properties.get(key) : null;
    }

    /**
     * This will add a property to the properties map. It will create a new map if the current one is null
     *
     * @param key The property key to use
     * @param value The value of the property to add
     */
    public void addProperty(@NotNull String key, Object value) {
        if(properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }
}
