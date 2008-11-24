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
 * @param sortDir initial sort direction 'yui-dt-asc' or 'yui-dt-desc'
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
        template           : 'First {FirstPageLink} {PreviousPageLink} {NextPageLink} {LastPageLink} Last<img src="../images/spacer.png" height="1" width="30" alt="" />Show {RowsPerPageDropdown} per page'
    });
    myPaginator.setAttributeConfig('firstPageLinkLabel',    {value:'<img src="../images/gotoFirst.png" alt="" />', validator:YAHOO.lang.isString});
    myPaginator.setAttributeConfig('previousPageLinkLabel', {value:'<img src="../images/gotoPrevious.png" alt="" />', validator:YAHOO.lang.isString});
    myPaginator.setAttributeConfig('nextPageLinkLabel',     {value:'<img src="../images/gotoNext.png" alt="" />', validator:YAHOO.lang.isString});
    myPaginator.setAttributeConfig('lastPageLinkLabel',     {value:'<img src="../images/gotoLast.png" alt="" />', validator:YAHOO.lang.isString});

    var myInitRequest = 'sort=' + sortBy + '&dir=' + ((sortDir == YAHOO.widget.DataTable.CLASS_DESC) ? 'desc' : 'asc') + '&startIndex=0&results=' + defaultPageSize;
    var myConfig = {
        dynamicData : true,
        selectionMode : 'single',
        paginator : myPaginator,
        sortedBy : {
            key : sortBy,
            dir : sortDir
        },
        initialRequest : myInitRequest 
    };

    // Instantiate DataTable
    myDataTable = new YAHOO.widget.DataTable(
        tableId,             // The dom element to contain the DataTable
        tableColumns,        // What columns will display
        myDataSource,        // The DataSource for our records
        myConfig             // Other configurations
    );

    myDataTable.handleDataReturnPayload = function(oRequest, oResponse, oPayload) {
        oPayload.totalRecords = oResponse.meta.totalRecords;
        return oPayload;
    }

    if ( selectionControlIds || selectionCallback ) {
        // Subscribes to events for row selection.
        myDataTable.subscribe("rowMouseoverEvent", myDataTable.onEventHighlightRow);
        myDataTable.subscribe("rowMouseoutEvent", myDataTable.onEventUnhighlightRow);
        myDataTable.subscribe("rowClickEvent", myDataTable.onEventSelectRow);

        // Subscribes to row select changes for enabling/disabling toolbar buttons.
        var enableOrDisableControls = function (event, target) {
            var selectedRows = myDataTable.getSelectedRows();
            var hasSelectedRows = selectedRows && (selectedRows.length != 0);

            if ( selectionControlIds ) {
                var controlId;
                for ( controlId in selectionControlIds ) {
                    var control = document.getElementById( selectionControlIds[controlId] );
                    if ( control ) {
                        control.disabled = !hasSelectedRows;
                    }
                }
            }

            var selectionControl = document.getElementById( selectionId );
            if ( selectionControl ) {
                if ( hasSelectedRows ) {
                    selectionControl.value = this.getRecord( selectedRows[0] ).getData(idProperty);
                } else {
                    selectionControl.value = "";
                }

                if( selectionCallback ) {
                    selectionCallback( selectionControl.value );
                }
            }
        }
        
        myDataTable.subscribe("rowSelectEvent", enableOrDisableControls);
        myDataTable.subscribe("rowUnSelectEvent", enableOrDisableControls);

        enableOrDisableControls();
    }
};
