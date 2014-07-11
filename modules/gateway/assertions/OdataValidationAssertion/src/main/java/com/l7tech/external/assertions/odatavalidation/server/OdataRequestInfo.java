package com.l7tech.external.assertions.odatavalidation.server;

import org.apache.olingo.odata2.api.edm.EdmEntitySet;
import org.apache.olingo.odata2.api.edm.EdmProperty;
import org.apache.olingo.odata2.api.uri.NavigationPropertySegment;
import org.apache.olingo.odata2.api.uri.PathSegment;
import org.apache.olingo.odata2.api.uri.SelectItem;
import org.apache.olingo.odata2.api.uri.expression.FilterExpression;
import org.apache.olingo.odata2.api.uri.expression.OrderByExpression;
import org.apache.olingo.odata2.core.uri.UriInfoImpl;
import org.apache.olingo.odata2.core.uri.UriType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
* @author Jamie Williams - jamie.williams2@ca.com
*/
public class OdataRequestInfo {
    private final UriInfoImpl uriInfo;

    private final String expandExpression;
    private final String selectExpression;
    private final List<PathSegment> odataSegments;

    public OdataRequestInfo(@NotNull UriInfoImpl uriInfo,
                            @Nullable String expandExpression, @Nullable String selectExpression, List<PathSegment> odataSegments) {
        this.uriInfo = uriInfo; // TODO jwilliams: maybe add a flag to indicate if a payload is expected or not, based on the URI type?
        this.expandExpression = expandExpression;
        this.selectExpression = selectExpression;
        this.odataSegments = odataSegments;
    }

    public boolean isServiceDocumentRequest() {
        return UriType.URI0 == uriInfo.getUriType();
    }

    public boolean isMetadataRequest() {
        return UriType.URI8 == uriInfo.getUriType();
    }

    public boolean isValueRequest() {
        return uriInfo.isValue();
    }

    public boolean isCount() {
        return uriInfo.isCount();
    }

    public List<ArrayList<NavigationPropertySegment>> getExpandNavigationProperties() {
        return uriInfo.getExpand();
    }

    public String getExpandExpressionString() {
        return expandExpression;
    }

    public FilterExpression getFilterExpression() {
        return uriInfo.getFilter();
    }

    public String getFilterExpressionString() {
        return null == uriInfo.getFilter()
                ? null
                : uriInfo.getFilter().getExpressionString();
    }

    public String getFormat() {
        return uriInfo.getFormat();
    }

    public String getInlineCount() {
        if (null == uriInfo.getInlineCount()) {
            return "none";
        }

        switch (uriInfo.getInlineCount()) {
            case ALLPAGES:
                return "allpages";
            default:
                return "none";
        }
    }

    public OrderByExpression getOrderByExpression() {
        return uriInfo.getOrderBy();
    }

    public String getOrderByExpressionString() {
        return null == uriInfo.getOrderBy()
                ? null
                : uriInfo.getOrderBy().getExpressionString();
    }

    public List<SelectItem> getSelectItemList() {
        return uriInfo.getSelect();
    }

    public String getSelectExpressionString() {
        return selectExpression;
    }

    public Integer getSkip() {
        return uriInfo.getSkip();
    }

    public Integer getTop() {
        return uriInfo.getTop();
    }

    public Map<String, String> getCustomQueryOptions() {
        return uriInfo.getCustomQueryOptions();
    }

    public EdmEntitySet getTargetEntitySet() {
        return uriInfo.getTargetEntitySet();
    }

    public EdmProperty getTargetProperty() {
        final List<EdmProperty> propertyPath = uriInfo.getPropertyPath();
        return propertyPath == null || propertyPath.isEmpty() ? null : propertyPath.get(propertyPath.size() - 1);
    }

    public UriType getUriType() {
        return uriInfo.getUriType();
    }

    public List<PathSegment> getOdataSegments() {
        return odataSegments;
    }
}
