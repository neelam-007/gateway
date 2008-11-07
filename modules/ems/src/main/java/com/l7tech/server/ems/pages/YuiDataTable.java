package com.l7tech.server.ems.pages;

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.mortbay.util.ajax.JSON;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Wicket component for YUI table
 */
public class YuiDataTable extends Panel {

    //- PUBLIC

    public static void contributeHeaders( final Component component ) {
        component.add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_BUTTON ) );
        component.add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_MENU ) );
        component.add( HeaderContributor.forCss( YuiCommon.RES_CSS_SAM_DATATABLE ) );
    }

    public YuiDataTable( final String id,
                         final List<PropertyColumn> columns,
                         final String sortProperty,
                         final boolean sortAscending,
                         final ISortableDataProvider sortableDataProvider ) {
        this( id, columns, sortProperty, sortAscending, sortableDataProvider, null, null, false, null );
    }

    public YuiDataTable( final String id,
                         final List<PropertyColumn> columns,
                         final String sortProperty,
                         final boolean sortAscending,
                         final ISortableDataProvider sortableDataProvider,
                         final HiddenField selectionComponent,
                         final String idProperty,
                         final boolean hideIdColumn,
                         final Button[] selectionSensitiveComponents ) {
        super(id);

        this.columns = columns;
        this.provider = sortableDataProvider;

        setOutputMarkupId(true);

        contributeHeaders( this );

        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DOM_EVENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_ELEMENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_BUTTON ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DOM ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_EVENT ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_MENU ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DRAGDROP ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_CONTAINER ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DATASOURCE ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DATATABLE ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_PAGINATOR ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_CONNECTION ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_JSON ) );

        add( HeaderContributor.forJavaScript( new ResourceReference( YuiDataTable.class, "../resources/js/dataTable.js" ) ) );

        JSON json = new JSON();
        json.addConvertor( PropertyColumn.class, new PropertyColumnConvertor( hideIdColumn ? idProperty : null ) );

        final StringBuffer columnBuffer = new StringBuffer();
        json.append(columnBuffer, columns);

        final StringBuffer fieldsBuffer = new StringBuffer();
        json.append(fieldsBuffer, fieldsList(columns));

        final String buttons;
        if ( selectionSensitiveComponents != null ) {
            List<String> buttonIds = new ArrayList<String>();
            for ( Button button : selectionSensitiveComponents ) {
                buttonIds.add( "'" +button.getMarkupId() + "'" );
            }
            buttons = buttonIds.toString();
        } else {
            buttons = "null";
        }
        WebMarkupContainer pagingContainer = new WebMarkupContainer("paging");
        WebMarkupContainer tableContainer = new WebMarkupContainer("table");

        pagingContainer.setOutputMarkupId( true );
        tableContainer.setOutputMarkupId( true );

        add( pagingContainer );
        add( tableContainer );

        final String tableId = tableContainer.getMarkupId();
        final String pagingId = pagingContainer.getMarkupId();
        final String selectionId = selectionComponent == null ? "null" : selectionComponent.getMarkupId();

        Label jsContainer = new Label("script", "YAHOO.util.Event.onDOMReady( function(){ initDataTable"+tableId+"(); } );");
        add( jsContainer );

        add( new AbstractAjaxBehavior(){
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );

                iHeaderResponse.renderJavascriptReference(WicketEventReference.INSTANCE);
                iHeaderResponse.renderJavascriptReference(WicketAjaxReference.INSTANCE);

                StringBuilder scriptBuilder = new StringBuilder(1024);

                scriptBuilder.append( "function dataTableSelectionCallback").append(tableId).append("( id ) {\n");
                scriptBuilder.append( " wicketAjaxGet('").append(getCallbackUrl(true)).append("&selection=true&id=' + id, function() { }, function() { });\n");
                scriptBuilder.append( "}\n" );

                scriptBuilder.append( "function initDataTable" );
                scriptBuilder.append( tableId );
                scriptBuilder.append( "(){ initDataTable( '" );
                scriptBuilder.append( tableId );
                scriptBuilder.append( "', ");
                scriptBuilder.append( columnBuffer );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( pagingId );
                scriptBuilder.append( "', '" );
                scriptBuilder.append( getCallbackUrl(true) );
                scriptBuilder.append( "&data=true&', " );
                scriptBuilder.append( fieldsBuffer );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( sortProperty );
                scriptBuilder.append( "', '");
                scriptBuilder.append( sortAscending ? "yui-dt-asc" : "yui-dt-desc" );
                scriptBuilder.append( "', " );
                scriptBuilder.append( buttons );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( selectionId );
                scriptBuilder.append( "', " );
                scriptBuilder.append( "dataTableSelectionCallback" ).append( tableId );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( idProperty );
                scriptBuilder.append( "')}" );

                iHeaderResponse.renderJavascript(scriptBuilder.toString(), null);
            }

            public void onRequest() {

                boolean isPageVersioned = true;
                Page page = getComponent().getPage();
                try {
                    isPageVersioned = page.isVersioned();
                    page.setVersioned(false);

                    RequestCycle.get().setRequestTarget(new IRequestTarget() {
                        public void detach(RequestCycle requestCycle) {}

                        public void respond(RequestCycle requestCycle) {
                            if ( requestCycle.getRequest().getParameter("selection") != null ) {
                                WebApplication app = (WebApplication)getComponent().getApplication();
                                AjaxRequestTarget target = app.newAjaxRequestTarget(getComponent().getPage());
                                RequestCycle.get().setRequestTarget(target);
                                onSelect( target, requestCycle.getRequest().getParameter("id") );        
                            } else {

                                int startIndex = Integer.parseInt(requestCycle.getRequest().getParameter("startIndex"));
                                int results = Integer.parseInt(requestCycle.getRequest().getParameter("results"));
                                if ( results > 100 ) results = 100;

                                String sortRaw = requestCycle.getRequest().getParameter("sort");
                                boolean dir = !"desc".equals(requestCycle.getRequest().getParameter("dir"));
                                String sort = columnToSortProperty( sortRaw );

                                // Get data
                                if ( sort != null ) {
                                    provider.getSortState().setPropertySortOrder( sort, dir ? ISortState.ASCENDING : ISortState.DESCENDING );
                                }
                                String data = buildResultsPage(startIndex, results, sortRaw, dir);

                                requestCycle.getResponse().setContentType("application/json");
                                requestCycle.getResponse().write(data);
                            }
                        }
                    });
                } finally {
                     page.setVersioned(isPageVersioned);
                }
            }
        } );
    }

    private String buildResultsPage(int startIndex, int results, String sortRaw, boolean dir) {
        List<Model> data = new ArrayList<Model>(results);
        Iterator iter = provider.iterator( startIndex, results );
        while ( iter.hasNext() ) {
            data.add(new Model((Serializable)iter.next()));
        }

        JSONPage page = new JSONPage( data, provider.size(), startIndex, sortRaw, dir );

        // Add JSON script to the response
        JSON json = new JSON();
        json.addConvertor( JSONPage.class, new PageConvertor() );
        json.addConvertor( Model.class, new PropertyModelConvertor() );

        StringBuffer dataBuffer = new StringBuffer(2048);
        json.append(dataBuffer, page);

        return dataBuffer.toString();
    }

    public void detachModels() {
        super.detachModels();
        provider.detach();            
    }

    //- PROTECTED

    /**
     * Override to perform an action on selection.
     *
     * @param value The selected value
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String value ) {        
    }

    //- PRIVATE

    private final List<PropertyColumn> columns;
    private final ISortableDataProvider provider;

    private String columnToSortProperty( final String columnName ) {
        String sort = columns.get(0).getSortProperty();

        for ( PropertyColumn column : columns ) {
            if ( column.getPropertyExpression().equals(columnName) ) {
                sort = column.getSortProperty();
                break;
            }
        }

        return sort;
    }

    private static final class JSONPage {
        private final List<Model> records;
        private final int totalRecords;
        private final int recordsReturned;
        private final int startIndex;
        private final String sort;
        private final String dir;

        private JSONPage(final List<Model> records,
                         final int total,
                         final int startIndex,
                         final String sortProperty,
                         final boolean ascending) {
            this.records = records;
            this.totalRecords = total;
            this.recordsReturned = records.size();
            this.startIndex = startIndex;
            this.sort = sortProperty;
            this.dir = ascending ? "asc" : "desc";
        }

        public List<Model> getRecords() {
            return records;
        }

        public int getTotalRecords() {
            return totalRecords;
        }

        public int getRecordsReturned() {
            return recordsReturned;
        }

        public int getStartIndex() {
            return startIndex;
        }

        public String getSort() {
            return sort;
        }

        public String getDir() {
            return dir;
        }
    }

    private List<String> fieldsList( final List<PropertyColumn> columns ) {
        List<String> fieldsList = new ArrayList<String>();

        for ( PropertyColumn column : columns ) {
            fieldsList.add( column.getPropertyExpression() );
        }

        return fieldsList;
    }

    /**
     * {key:'userId', label:'User ID', sortable:true, resizeable:true}
     */
    private static final class PropertyColumnConvertor implements JSON.Convertor {
        private final String hiddenColumn;

        public PropertyColumnConvertor( final String hiddenColumnName ) {
            this.hiddenColumn = hiddenColumnName;    
        }

        public void toJSON(Object o, JSON.Output output) {
            PropertyColumn column = (PropertyColumn) o;
            output.add( "key", column.getPropertyExpression() );
            if ( hiddenColumn != null && hiddenColumn.equals(column.getPropertyExpression()) ) {
                output.add( "hidden", true );
            } else {
                output.add( "label", column.getDisplayModel().getObject() );
                output.add( "sortable", column.isSortable() );
                output.add( "resizeable", true );
            }
        }

        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }

    /**
     *
     */
    private static final class PageConvertor implements JSON.Convertor {
        public void toJSON(Object o, JSON.Output output) {
            JSONPage page = (JSONPage) o;            
            output.add( "records", page.getRecords() );
            output.add( "totalRecords", page.getTotalRecords() );
            output.add( "recordsReturned", page.getRecordsReturned() );
            output.add( "startIndex", page.getStartIndex() );
            output.add( "sort", page.getSort() );
            output.add( "dir", page.getDir() );
        }

        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }

    private class PropertyModelConvertor implements JSON.Convertor {
        public void toJSON(Object o, JSON.Output output) {
            Model data = (Model) o;

            for ( PropertyColumn column : columns ) {
                Object object = new PropertyModel(data.getObject(), column.getPropertyExpression()).getObject();
                if ( object == null ) {
                    output.add( column.getPropertyExpression(), null);
                } else {
                    output.add( column.getPropertyExpression(), getConverter(object.getClass()).convertToString(object, null));
                }
            }
        }

        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }
}
