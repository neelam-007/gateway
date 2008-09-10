/**
 * Javascript for YUI DataTable component
 */

/**
 * Initialize a DataTable
 *
 * @param tableId DOM id for div holding table.
 * @param tableColumns The table column definitions [{key:"id", label:"ID", sortable:true}, ... ]
 * @param pagingId DOM id for div holding pager.
 * @param dataUrl relative URL for accessing JSON data
 * @param dataFields fields in JSON records ["id", "name", ...]
 * @param sortBy initial sort column
 * @param sortDir initial sort direction 'asc' or 'desc'
 * @param selectionControlIds DOM ids for components to be enabled on selection
 * @param selectionId DOM id of form field to set value of on selection
 * @param selectionCallback function to call on selection (passing identifier)
 * @param idProperty the property name to set on selection
 */
function initDataTable( tableId, tableColumns, pagingId, dataUrl, dataFields, sortBy, sortDir, selectionControlIds, selectionId, selectionCallback, idProperty  ) {
    var myPaginator,  // to hold the Paginator instance
        myDataSource, // to hold the DataSource instance
        myDataTable;  // to hold the DataTable instance

    var defaultPageSize = 10;

    // function to generate a query string for the DataSource.  Also used
    // as the state indicator for the History Manager
    var generateStateString = function (start,count,key,dir) {
        start = start || 0;
        key   = key || sortBy;
        dir   = dir || sortDir;
        return "results="+count+"&startIndex="+start+"&sort="+key+"&dir="+dir;
    };

    // function to extract the key values from the state string
    var parseStateString = function (state) {
        return {
            results    : /\bresults=(\d+)/.test(state)    ? parseInt(RegExp.$1) : defaultPageSize,
            startIndex : /\bstartIndex=(\d+)/.test(state) ? parseInt(RegExp.$1) : 0,
            sort       : /\bsort=(\w+)/.test(state)       ? RegExp.$1 : sortBy,
            dir        : /\bdir=(\w+)/.test(state)        ? RegExp.$1 : sortDir
        };
    };

    // function to handle onStateChange events from Browser History Manager
    var doRequest = function (state, paginationState) {
        // Use the DataTable's baked in server-side pagination handler
        var callback = null;
        if ( paginationState ) {
            callback = {
                success  : myDataTable.onDataReturnSetRows,
                failure  : myDataTable.onDataReturnSetRows,
                argument : {
                    startIndex : paginationState.recordOffset,
                    pagination : paginationState
                },
                scope : myDataTable };
        } else {
            callback = {
                success  : myDataTable.onDataReturnSetRows,
                failure  : myDataTable.onDataReturnSetRows,
                scope : myDataTable };
        }

        myDataSource.sendRequest(state, callback);
    };

    var handlePagination = function (state,datatable) {
        var sortedBy  = datatable.get('sortedBy');

        var newState = generateStateString(
                            state.recordOffset,
                            state.rowsPerPage,
                            sortedBy.key,
                            sortedBy.dir);

        doRequest(newState, state);
    };

    // function used to intercept sorting requests
    var handleSorting = function (oColumn) {
        // Which direction
        var sDir = "asc";

        // Already sorted?
        if(oColumn.key === this.get("sortedBy").key) {
            sDir = (this.get("sortedBy").dir === YAHOO.widget.DataTable.CLASS_ASC) ?
                    "asc" : "desc";
        }

        var count = this.get("paginator").get("rowsPerPage");
        var newState = generateStateString(0, count, oColumn.key, sDir);

        doRequest(newState);
    };
    

    // Create initial state.  Parse the state string into an object literal.
    var initialRequest = generateStateString(0,defaultPageSize,sortBy,sortDir),
        state          = parseStateString(initialRequest);

    // Create the DataSource
    myDataSource = new YAHOO.util.DataSource(dataUrl);
    myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSON;
    myDataSource.responseSchema = {
        resultsList: "records",
        fields: dataFields,
        metaFields: {
            totalRecords: "totalRecords",
            paginationRecordOffset : "startIndex",
            sortKey: "sort",
            sortDir: "dir"
        }
    };

    // Create the DataTable configuration and Paginator
    myPaginator = new YAHOO.widget.Paginator({
        containers         : [pagingId],
        pageLinks          : 5,
        rowsPerPage        : defaultPageSize,
        rowsPerPageOptions : [10,20],
        template           : 'Oldest {FirstPageLink} {PreviousPageLink} {NextPageLink} {LastPageLink} Newest<img src="../images/spacer.png" height="1" width="30" alt="" />Show {RowsPerPageDropdown} per page'
    });
    myPaginator.setAttributeConfig('firstPageLinkLabel',    {value:'<img src="../images/gotoFirst.png" alt="" />', validator:YAHOO.lang.isString});
    myPaginator.setAttributeConfig('previousPageLinkLabel', {value:'<img src="../images/gotoPrevious.png" alt="" />', validator:YAHOO.lang.isString});
    myPaginator.setAttributeConfig('nextPageLinkLabel',     {value:'<img src="../images/gotoNext.png" alt="" />', validator:YAHOO.lang.isString});
    myPaginator.setAttributeConfig('lastPageLinkLabel',     {value:'<img src="../images/gotoLast.png" alt="" />', validator:YAHOO.lang.isString});

    var myConfig = {
        paginator : myPaginator,
        paginationEventHandler : handlePagination,
        // generateRequest : generateStateString, // moot
        sortedBy : {
            key : state.sort,
            dir : state.dir
        },
        initialRequest : initialRequest
    };

    // Instantiate DataTable
    myDataTable = new YAHOO.widget.DataTable(
        tableId,             // The dom element to contain the DataTable
        tableColumns,        // What columns will display
        myDataSource,        // The DataSource for our records
        myConfig             // Other configurations
    );

    // Listen to header link clicks to sort the column
    myDataTable.subscribe('theadCellClickEvent', myDataTable.onEventSortColumn);

    // Override the DataTable's sortColumn method with our intercept handler
    myDataTable.sortColumn = handleSorting;

    if ( selectionControlIds || selectionCallback ) {
        // Subscribes to events for row selection.
        myDataTable.subscribe("rowMouseoverEvent", myDataTable.onEventHighlightRow);
        myDataTable.subscribe("rowMouseoutEvent", myDataTable.onEventUnhighlightRow);
        myDataTable.subscribe("rowClickEvent", myDataTable.onEventSelectRow);

        // Subscribes to row select changes for enabling/disabling toolbar buttons.
        var enableOrDisableControls = function (event, target) {
            var selectedRows = myDataTable.getSelectedRows();
            var hasSelectedRows = (selectedRows != null) && (selectedRows.length != 0);

            if ( selectionControlIds ) {
                var controlId;
                for ( controlId in selectionControlIds ) {
                    var control = document.getElementById( controlId );
                    if ( control ) control.disabled = !hasSelectedRows;
                }
            }

            var selectionControl = document.getElementById( selectionId );
            if ( selectionControl ) {
                if ( hasSelectedRows ) {
                    selectionControl.value = this.getRecord( selectedRows[0] ).getData(idProperty);
                } else {
                    selectionControl.value = "-1";
                }

                if( selectionCallback ) {
                    selectionCallback( selectionControl.value );
                }
            }
        }
        
        myDataTable.subscribe("rowSelectEvent", enableOrDisableControls);
        myDataTable.subscribe("rowUnSelectEvent", enableOrDisableControls);
    }
};
