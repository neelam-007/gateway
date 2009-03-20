package com.l7tech.server.ems.ui.pages;

import com.l7tech.server.ems.util.TypedPropertyColumn;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;
import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.WicketAjaxReference;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.ISortState;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WicketEventReference;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebResponse;
import org.apache.wicket.util.convert.IConverter;
import org.mortbay.util.ajax.JSON;

import java.io.Serializable;
import java.util.*;
import java.util.logging.Level;

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

    /**
     * Create a YUI DataTable component with server side sort/page.
     *
     * @param id The component identifier
     * @param columns The table column definitions
     * @param sortProperty The property to sort by
     * @param sortAscending True to sort in ascending order
     * @param sortableDataProvider The data provider for the table data
     */
    public YuiDataTable( final String id,
                         final List<PropertyColumn> columns,
                         final String sortProperty,
                         final boolean sortAscending,
                         final ISortableDataProvider sortableDataProvider ) {
        this( id, columns, sortProperty, sortAscending, sortableDataProvider, true, null, false, null, false, null );
    }

    /**
     * Create a YUI DataTable component with server side sort/page.
     *
     * @param id The component identifier
     * @param columns The table column definitions
     * @param sortProperty The property to sort by
     * @param sortAscending True to sort in ascending order
     * @param sortableDataProvider The data provider for the table data
     * @param selectionComponent The component to hold row selection values
     * @param idProperty The property to use as the selection id
     * @param hideIdColumn True if the id column should be hidden
     * @param selectionSensitiveComponents Buttons that are sensitive to row selection
     */
    public YuiDataTable( final String id,
                         final List<PropertyColumn> columns,
                         final String sortProperty,
                         final boolean sortAscending,
                         final ISortableDataProvider sortableDataProvider,
                         final HiddenField selectionComponent,
                         final String idProperty,
                         final boolean hideIdColumn,
                         final Button[] selectionSensitiveComponents ) {
        this( id, columns, sortProperty, sortAscending, sortableDataProvider,
              true, selectionComponent, false, idProperty, hideIdColumn, selectionSensitiveComponents );
    }

    /**
     * Create a YUI DataTable component with server side sort/page.
     *
     * @param id The component identifier
     * @param columns The table column definitions
     * @param sortProperty The property to sort by
     * @param sortAscending True to sort in ascending order
     * @param sortableDataProvider The data provider for the table data
     * @param selectionComponent The component to hold row selection values
     * @param multiSelect True to permit multiselect
     * @param idProperty The property to use as the selection id
     * @param hideIdColumn True if the id column should be hidden
     * @param selectionSensitiveComponents Buttons that are sensitive to row selection
     */
    public YuiDataTable( final String id,
                         final List<PropertyColumn> columns,
                         final String sortProperty,
                         final boolean sortAscending,
                         final ISortableDataProvider sortableDataProvider,
                         final HiddenField selectionComponent,
                         final boolean multiSelect,
                         final String idProperty,
                         final boolean hideIdColumn,
                         final Button[] selectionSensitiveComponents ) {
        this( id, columns, sortProperty, sortAscending, sortableDataProvider,
              true, selectionComponent, multiSelect, idProperty, hideIdColumn, selectionSensitiveComponents );
    }

    /**
     * Create a YUI DataTable component with client side sort.
     *
     * @param id The component identifier
     * @param columns The table column definitions
     * @param sortProperty The property to sort by
     * @param sortAscending True to sort in ascending order
     * @param data The table data
     */
    public YuiDataTable( final String id,
                         final List<PropertyColumn> columns,
                         final String sortProperty,
                         final boolean sortAscending,
                         final Collection<?> data ) {
        this( id, columns, sortProperty, sortAscending, asSortableDataProvider(data, sortProperty, sortAscending), false, null, false, null, false, null );
    }

    /**
     * Create a YUI DataTable component with client side sort.
     *
     * @param id The component identifier
     * @param columns The table column definitions
     * @param sortProperty The property to sort by
     * @param sortAscending True to sort in ascending order
     * @param data The table data
     * @param selectionComponent The component to hold row selection values
     * @param multiSelect True to allow multiple selection with checkboxes
     * @param idProperty The property to use as the selection id
     * @param hideIdColumn True if the id column should be hidden
     * @param selectionSensitiveComponents Buttons that are sensitive to row selection
     */
    public YuiDataTable( final String id,
                         final List<PropertyColumn> columns,
                         final String sortProperty,
                         final boolean sortAscending,
                         final Collection<?> data,
                         final HiddenField selectionComponent,
                         final boolean multiSelect,
                         final String idProperty,
                         final boolean hideIdColumn,
                         final Button[] selectionSensitiveComponents ) {
        this( id, columns, sortProperty, sortAscending, asSortableDataProvider(data, sortProperty, sortAscending),
              false, selectionComponent, multiSelect, idProperty, hideIdColumn, selectionSensitiveComponents );
    }

    /**
     * Utility method to unescape ID values that were from a YUI id value.
     *
     * @param identifier The identifier to unescape.
     * @return The unescaped value.
     */
    public static String unescapeIdentitifer( final String identifier ) {
        String id = identifier;

        if ( id != null ) {
            // remove any HTML escaping
            id = id.replace("&amp;", "&");
            id = id.replace("&quot;", "\"");
            id = id.replace("&#039;", "'");
        }

        return id;
    }

    /**
     * Internal constructor.
     */
    private YuiDataTable( final String id,
                          final List<PropertyColumn> columns,
                          final String sortProperty,
                          final boolean sortAscending,
                          final ISortableDataProvider sortableDataProvider,
                          final boolean isServerSortAndPage,
                          final HiddenField selectionComponent,
                          final boolean multiSelect,
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
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DRAGDROP ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_PAGINATOR ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_CONNECTION ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_JSON ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DATASOURCE ) );
        add( HeaderContributor.forJavaScript( YuiCommon.RES_JS_DATATABLE ) );

        add( HeaderContributor.forJavaScript( "js/dataTable.js" ) );

        JSON json = new JSON();
        json.addConvertor( PropertyColumn.class, new PropertyColumnConvertor( hideIdColumn ? idProperty : null ) );
        json.addConvertor( Level.class, new LevelConverter() );
        json.addConvertor( Date.class, new DateConverter() );
        json.addConvertor( Model.class, new PropertyModelConvertor() );

        final StringBuffer columnBuffer = new StringBuffer();
        json.append(columnBuffer, columns);

        final StringBuffer fieldsBuffer = new StringBuffer();
        json.append(fieldsBuffer, fieldsList(columns));

        final StringBuffer dataBuffer;
        if ( !isServerSortAndPage ) {
            dataBuffer = new StringBuffer();
            json.append(dataBuffer, dataTable(sortableDataProvider, 0, 1000));
        } else {
            dataBuffer = null;
        }

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

        if ( !isServerSortAndPage ) {
            pagingContainer.setVisible( false );    
        }

        add( pagingContainer );
        add( tableContainer );

        final String tableId = tableContainer.getMarkupId();
        final String pagingId = pagingContainer.getMarkupId();
        final String selectionId = selectionComponent == null ? "null" : selectionComponent.getMarkupId();
        if (selectionComponent != null && !selectionComponent.getOutputMarkupId()) {
            throw new IllegalArgumentException("Hidden component markup id must be output.");
        }

        final AbstractAjaxBehavior callbackBehaviour = new AbstractAjaxBehavior(){
            @Override
            public void renderHead( final IHeaderResponse iHeaderResponse ) {
                super.renderHead( iHeaderResponse );

                iHeaderResponse.renderJavascriptReference(WicketEventReference.INSTANCE);
                iHeaderResponse.renderJavascriptReference(WicketAjaxReference.INSTANCE);

                StringBuilder scriptBuilder = new StringBuilder(512);

                scriptBuilder.append( "function dataTableSelectionCallback").append(tableId).append("( id ) {\n");
                scriptBuilder.append( " wicketAjaxGet('").append(getCallbackUrl(true)).append("&selection=true&id=' + encodeURIComponent(id), function() { }, function() { });\n");
                scriptBuilder.append( "}\n" );

                iHeaderResponse.renderJavascript(scriptBuilder.toString(), YuiDataTable.this.getClass().getName() + "." + tableId);
            }

            @Override
            public void onRequest() {

                boolean isPageVersioned = true;
                Page page = getComponent().getPage();
                try {
                    isPageVersioned = page.isVersioned();
                    page.setVersioned(false);

                    RequestCycle.get().setRequestTarget(new IRequestTarget() {
                        @Override
                        public void detach(RequestCycle requestCycle) {}

                        @Override
                        public void respond(RequestCycle requestCycle) {
                            if ( requestCycle.getRequest().getParameter("selection") != null ) {
                                WebApplication app = (WebApplication)getComponent().getApplication();
                                AjaxRequestTarget target = app.newAjaxRequestTarget(getComponent().getPage());
                                RequestCycle.get().setRequestTarget(target);
                                String id = requestCycle.getRequest().getParameter("id");
                                if ( multiSelect ) {
                                    onSelect( target, Arrays.asList( unescapeIdentitifer(id).split(",") ) );                                    
                                } else {
                                    onSelect( target, Collections.singletonList(unescapeIdentitifer(id)) );
                                }
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

                                WebResponse response = (WebResponse)requestCycle.getResponse();
                                response.setContentType("application/json; charset=UTF-8");
                                response.setHeader("Expires", "Mon, 26 Jul 1997 05:00:00 GMT");
                                response.setHeader("Cache-Control", "no-cache, must-revalidate");
                                response.setHeader("Pragma", "no-cache");
                                response.write(data);
                            }
                        }
                    });
                } finally {
                     page.setVersioned(isPageVersioned);
                }
            }
        };
        add( callbackBehaviour );

        Serializable scriptHolder = new Serializable(){
            @Override
            public String toString() {
                StringBuilder scriptBuilder = new StringBuilder(1024);
                scriptBuilder.append( "(function(){" );
                scriptBuilder.append( " initDataTable( '" );
                scriptBuilder.append( tableId );
                scriptBuilder.append( "', ");
                scriptBuilder.append( columnBuffer.toString() );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( pagingId );
                scriptBuilder.append( "', '" );
                scriptBuilder.append( callbackBehaviour.getCallbackUrl(true).toString() );
                scriptBuilder.append( "&data=true&', " );
                scriptBuilder.append( fieldsBuffer.toString() );
                scriptBuilder.append( ", " );
                scriptBuilder.append( dataBuffer == null ? "null" : "'" + escapeSingleQuotes(dataBuffer.toString()) + "'" );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( escapeSingleQuotes(sortProperty) );
                scriptBuilder.append( "', '");
                scriptBuilder.append( sortAscending ? "yui-dt-asc" : "yui-dt-desc" );
                scriptBuilder.append( "', " );
                scriptBuilder.append( buttons );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( selectionId );
                scriptBuilder.append( "', " );
                scriptBuilder.append( multiSelect );
                scriptBuilder.append( ", " );
                scriptBuilder.append( "dataTableSelectionCallback" ).append( tableId );
                scriptBuilder.append( ", '" );
                scriptBuilder.append( escapeSingleQuotes(idProperty) );
                scriptBuilder.append( "'); })();" );
                return scriptBuilder.toString();
            }
        };

        Label jsContainer = new Label("script", new Model(scriptHolder));
        add( jsContainer.setEscapeModelStrings(false) );
    }

    private String escapeSingleQuotes(final String string) {
        return string == null ? null : string.replace("'", "\\'");
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

    @Override
    public void detachModels() {
        super.detachModels();
        provider.detach();            
    }

    //- PROTECTED

    /**
     * Override to perform an action on selection.
     *
     * <p>If using multiple selections this may be called multiple times.</p>
     *
     * @param value The selected value
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final String value ) {        
    }

    /**
     * Override to perform an action on selection.
     *
     * @param values The selected value
     */
    protected void onSelect( final AjaxRequestTarget ajaxRequestTarget, final Collection<String> values ) {
        for ( String value : values ) {
            onSelect( ajaxRequestTarget, value );                
        }
    }

    //- PRIVATE

    private final List<PropertyColumn> columns;
    private final ISortableDataProvider provider;

    @SuppressWarnings({"unchecked"})
    private static ISortableDataProvider asSortableDataProvider(
            final Collection<?> data,
            final String sortProperty,
            final boolean sortAscending ) {
        final List sortedData = new ArrayList(data);
        Collections.sort( sortedData, new ResolvingComparator(new Resolver(){
            @SuppressWarnings({"unchecked"})
            @Override
            public Object resolve(Object dataObject) {
                Object object = new PropertyModel(dataObject, sortProperty).getObject();
                if ( object == null ) {
                    return "";
                } else {
                    String propValue = object.toString();
                    if ( propValue==null ) {
                        propValue = "";
                    } else {
                        propValue = propValue.toLowerCase();    
                    }
                    return propValue;
                }
            }
        }, false) );

        return new SortableDataProvider(){
            {
                setSort( sortProperty, sortAscending );
            }

            @Override
            public Iterator iterator(int first, int count) {
                return newDataIter(sortedData, first,first+count);
            }

            @Override
            public int size() {
                return sortedData.size();
            }

            @Override
            public IModel model(final Object dataObject) {
                if ( dataObject instanceof IModel ) {
                    return (IModel) dataObject;
                } else {
                    return new AbstractReadOnlyModel() {
                        @Override
                        public Object getObject() {
                            return dataObject;
                        }
                    };
                }
            }

            @Override
            public void detach() {
            }
        };
    }

    private static Iterator newDataIter(
            final Collection<?> data,
            final int start,
            final int end ) {
        List<?> list = new ArrayList<Object>( data );
        return list.subList(start, Math.min(end, data.size())).iterator();
    }

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

    private Map dataTable( final ISortableDataProvider provider, int startIndex, int results ) {
        return Collections.singletonMap("data", dataList(provider, startIndex, results));
    }

    private List<IModel> dataList( final ISortableDataProvider provider, int startIndex, int results ) {
        List<IModel> dataList = new ArrayList<IModel>(results);

        Iterator iter = provider.iterator( startIndex, results );
        while ( iter.hasNext() ) {
            dataList.add(new Model((Serializable)iter.next()));
        }

        return dataList;
    }

    private static final class LevelConverter implements JSON.Convertor {
        @Override
        public void toJSON(Object o, JSON.Output output) {
            Level level = (Level) o;
            output.add( "order", level.intValue() );
            output.add( "label", level.getName() );
        }

        @Override
        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }

    private final class DateConverter implements JSON.Convertor {
        @Override
        public void toJSON(Object o, JSON.Output output) {
            Date date = (Date) o;
            String label;
            IConverter converter = getConverter( Date.class );
            if ( converter != null ) {
                label = converter.convertToString( date, Locale.getDefault() );
            } else {
                label = o.toString();
            }

            output.add( "order", date.getTime() );
            output.add( "label", label );
        }

        @Override
        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }

    /**
     * {key:'userId', label:'User ID', sortable:true, resizeable:true}
     */
    private static final class PropertyColumnConvertor implements JSON.Convertor {
        private final String hiddenColumn;

        public PropertyColumnConvertor( final String hiddenColumnName ) {
            this.hiddenColumn = hiddenColumnName;    
        }

        @Override
        public void toJSON(Object o, JSON.Output output) {
            PropertyColumn column = (PropertyColumn) o;
            output.add( "key", column.getPropertyExpression() );
            if ( hiddenColumn != null && hiddenColumn.equals(column.getPropertyExpression()) ) {
                output.add( "hidden", true );
            } else {
                output.add( "label", column.getDisplayModel().getObject() );
                output.add( "sortable", column.isSortable() );
                output.add( "resizeable", true );

                if ( column instanceof TypedPropertyColumn ) {
                    TypedPropertyColumn typedPropertyColumn = (TypedPropertyColumn) column;
                    if ( typedPropertyColumn.getColumnClass() != null ) {
                        if ( Date.class.isAssignableFrom(typedPropertyColumn.getColumnClass()) ) {
                            output.add( "formatter", "ordered" );
                            output.add( "sortOptions", Collections.singletonMap("sortFunction", "ordered") );
                        } else if ( Level.class.isAssignableFrom(typedPropertyColumn.getColumnClass()) ) {
                            output.add( "formatter", "ordered" );
                            output.add( "sortOptions", Collections.singletonMap("sortFunction", "ordered") );
                        } else if ( Number.class.isAssignableFrom(typedPropertyColumn.getColumnClass()) ) {
                            output.add( "formatter", "number" );
                            output.add( "className", "right");
                        } else if ( typedPropertyColumn.isMultiline() ) {
                            output.add( "formatter", "multiline" );
                        } else if (((TypedPropertyColumn)column).isEscapePropertyValue()) {
                            output.add( "formatter", "text" );
                        }
                    }
                } else {
                    output.add( "formatter", "text" );
                }
            }
        }

        @Override
        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }

    /**
     *
     */
    private static final class PageConvertor implements JSON.Convertor {
        @Override
        public void toJSON(Object o, JSON.Output output) {
            JSONPage page = (JSONPage) o;            
            output.add( "records", page.getRecords() );
            output.add( "totalRecords", page.getTotalRecords() );
            output.add( "recordsReturned", page.getRecordsReturned() );
            output.add( "startIndex", page.getStartIndex() );
            output.add( "sort", page.getSort() );
            output.add( "dir", page.getDir() );
        }

        @Override
        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }

    private class PropertyModelConvertor implements JSON.Convertor {
        @Override
        public void toJSON(Object o, JSON.Output output) {
            Model data = (Model) o;

            for ( PropertyColumn column : columns ) {
                Object object = new PropertyModel(data.getObject(), column.getPropertyExpression()).getObject();
                if ( object == null ) {
                    output.add( column.getPropertyExpression(), null);
                } else {
                    String dataString = getConverter(object.getClass()).convertToString(object, Locale.getDefault());
                    output.add( column.getPropertyExpression(), dataString);
                }
            }
        }

        @Override
        public Object fromJSON(Map map) {
           throw new UnsupportedOperationException("Mapping fom JSON not supported.");
        }
    }
}
