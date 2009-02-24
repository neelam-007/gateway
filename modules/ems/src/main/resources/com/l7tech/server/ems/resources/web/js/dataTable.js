/**
 * Javascript for YUI DataTable component
 */

// -----------------------------------------------------------------------------
// Creates the l7 global namespace object, if not already created.
// -----------------------------------------------------------------------------
if (typeof l7 == "undefined" || !l7) {
    /**
     * The l7 global namespace object.
     *
     * @class l7
     * @static
     */
    var l7 = {};
}

// -----------------------------------------------------------------------------
// DataTable
// -----------------------------------------------------------------------------
if (!l7.DataTable) {
    (function(){
        /**
         * The l7 DataTable namespace object.
         *
         * @class l7.DataTable
         */
        l7.DataTable = {};

        /**
         * The l7 PageSize map, used to preserve page size selection on refresh of component..
         */
        l7.DataTable.PageSize = {};
    })();
}

/**
 * Initialize a DataTable
 *
 * @param tableId DOM id for div holding table.
 * @param tableColumns The table column definitions [{key:"id", label:"ID", sortable:true}, ... ]
 * @param pagingId DOM id for div holding pager.
 * @param dataUrl relative URL for accessing JSON data
 * @param dataFields fields in JSON records ["id", "name", ...]
 * @param data fields in JSON records
 * @param sortBy initial sort column
 * @param sortDir initial sort direction 'yui-dt-asc' or 'yui-dt-desc'
 * @param selectionControlIds DOM ids for components to be enabled on selection
 * @param selectionId DOM id of form field to set value of on selection
 * @param multiSelect True to allow multiple selection with per-row checkboxes
 * @param selectionCallback function to call on selection (passing identifier)
 * @param idProperty the property name to set on selection
 */
function initDataTable( tableId, tableColumns, pagingId, dataUrl, dataFields, tableData, sortBy, sortDir, selectionControlIds, selectionId, multiSelect, selectionCallback, idProperty  ) {
    var myPaginator,  // to hold the Paginator instance
        myDataSource, // to hold the DataSource instance
        myDataTable;  // to hold the DataTable instance

    var defaultPageSize = 10;

    if ( l7.DataTable.PageSize[tableId] ) {
        defaultPageSize = l7.DataTable.PageSize[tableId];   
    }

    var emstdateFormat = function(elCell, oRecord, oColumn, oData) {
        var oDate = oData;
	    var sMonth;
	    switch(oDate.getMonth()) {
	        case 0:
	            sMonth = "Jan";
	            break;
            case 1:
	            sMonth = "Feb";
	            break;
	        case 2:
	            sMonth = "Mar";
	            break;
	        case 3:
	            sMonth = "Apr";
	            break;
	        case 4:
	            sMonth = "May";
	            break;
	        case 5:
	            sMonth = "Jun";
	            break;
	        case 6:
	            sMonth = "Jul";
	            break;
	        case 7:
	            sMonth = "Aug";
	            break;
	        case 8:
	            sMonth = "Sep";
	            break;
	        case 9:
                sMonth = "Oct";
	            break;
	        case 10:
	            sMonth = "Nov";
	            break;
	        case 11:
	            sMonth = "Dec";
	            break;
	    }
        var sDate = new String(oDate.getDate());
        while ( sDate.length < 2 ) {
            sDate = "0" + sDate;
        }
        var sYear = new String(oDate.getFullYear());
        while ( sYear.length < 4 ) {
            sYear = "0" + sYear;
        }
	    elCell.innerHTML = sMonth + " " + sDate + ", " + sYear;
	};

    var emsdateFormat = function(elCell, oRecord, oColumn, oData) {
        var oDate = oData;
        var sMonth = new String(oDate.getMonth() + 1);
        while ( sMonth.length < 2 ) {
            sMonth = "0" + sMonth;
        }
        var sDate = new String(oDate.getDate());
        while ( sDate.length < 2 ) {
            sDate = "0" + sDate;
        }
        var sYear = new String(oDate.getFullYear());
        while ( sYear.length < 4 ) {
            sYear = "0" + sYear;
        }
	    elCell.innerHTML = sYear + "-" + sMonth + "-" + sDate;
	};


    var orderedFormat = function(elCell, oRecord, oColumn, oData) {
	    elCell.innerHTML = oData.label;
	};

    var multilineFormat = function(elCell, oRecord, oColumn, oData) {
	    elCell.innerHTML = new String(oData).replace(/&/g, "&#38;").replace(/</g, "&#60;").replace(/>/g, "&#62;").replace('\n','<br/>');
	};

    for ( var tcField in tableColumns ) {
        if ( tableColumns[tcField].formatter == 'emstdate' ) {
            tableColumns[tcField].formatter = emstdateFormat;
        } else if ( tableColumns[tcField].formatter == 'emsdate' ) {
            tableColumns[tcField].formatter = emsdateFormat;
        } else if ( tableColumns[tcField].formatter == 'ordered' ) {
            tableColumns[tcField].formatter = orderedFormat;
        } else if ( tableColumns[tcField].formatter == 'multiline' ) {
            tableColumns[tcField].formatter = multilineFormat;
        }
    }

    // Custom sort handler to sort "ordered" data
    var sortOrderForCurry = function(field, a, b, desc) {
        // Deal with empty values
        if(!YAHOO.lang.isValue(a)) {
            return (!YAHOO.lang.isValue(b)) ? 0 : 1;
        }
        else if(!YAHOO.lang.isValue(b)) {
            return -1;
        }

        var aNum = new Number( a.getData()[field].order );
        var bNum = new Number( b.getData()[field].order );

        if ( aNum == bNum ) {
            return 0;
        }

        return "asc" == desc ? (bNum > aNum ? 1 : -1) : (aNum > bNum ? 1 : -1);
    };

    curry = function(func, a) {
        return function(){ return func(a, arguments[0], arguments[1], arguments[2]); };
    }

    var updateControlStates = function( enable ) {
        if ( selectionControlIds ) {
            var controlIndex;
            for ( controlIndex in selectionControlIds ) {
                var controlId = selectionControlIds[controlIndex];
                var yuiButton = YAHOO.widget.Button.getButton( controlId );
                if ( yuiButton ) {
                    yuiButton.set('disabled', !enable);
                } else {
                    var control = document.getElementById( controlId );
                    if ( control ) {
                        control.disabled = !enable;
                    }
                }
            }
        }
    }

    var dataCallback = null;
    var multiSelectUpdate = null;
    if ( multiSelect && ( selectionControlIds || selectionCallback ) ) {
        // Add column metadata for multi select checkboxes
        var multiCol = new Object();
        multiCol.key = "checkbox";
        multiCol.label = '<input type="checkbox" onclick="document.getElementById(\''+tableId+'\').updateItemSelections(this.checked);"/>';
        tableColumns = new Array(multiCol).concat(tableColumns);

        multiSelectUpdate = function() {
            var selectedItemIds = new Array();
            var ids = document.getElementById( tableId ).currentPageIdentifiers;
            for ( var item in ids ) {
                var itemCheckbox = document.getElementById( tableId+'-'+ids[item] );
                if ( itemCheckbox && itemCheckbox.checked ) {
                    selectedItemIds = selectedItemIds.concat( new Array(ids[item]) );
                }
            }

            var selectionControl = document.getElementById( selectionId );
            if ( selectionControl ) {
                selectionControl.value = selectedItemIds.join();
            }

            updateControlStates( selectedItemIds.length > 0 );

            if( selectionCallback ) {
                selectionCallback( selectedItemIds.join() );
            }
        };

        // Add callback to populate checkbox data
        // doBeforeCallback Object doBeforeCallback ( oRequest , oFullResponse , oParsedResponse , oCallback )
        dataCallback = function( oRequest , oFullResponse , oParsedResponse , oCallback ){
            var currentPageIdentifiers = new Array();

            for ( var item in oParsedResponse.results ) {
                var itemId = oParsedResponse.results[item][idProperty];
                currentPageIdentifiers = currentPageIdentifiers.concat( new Array(itemId) );
                oParsedResponse.results[item].checkbox = '<input id="'+tableId+'-'+itemId+'" type="checkbox" onclick="document.getElementById(\''+tableId+'\').updateItemSelection(\''+itemId+'\', this.checked);"/>';
            }

            document.getElementById( tableId ).currentPageIdentifiers = currentPageIdentifiers;
            multiSelectUpdate();

            return oParsedResponse;
        };
    }

    if ( tableData ) {
        for ( var field in tableColumns ) {
            if ( tableColumns[field].sortOptions && tableColumns[field].sortOptions.sortFunction == 'ordered' ) {
                tableColumns[field].sortOptions.sortFunction = curry(sortOrderForCurry, new String(tableColumns[field].key), this);  
            }
        }

        // Create the DataSource for client side data
        var tableDataObject = YAHOO.lang.JSON.parse( tableData );

        myDataSource = new YAHOO.util.DataSource(tableDataObject.data);
        myDataSource.responseType = YAHOO.util.DataSource.TYPE_JSARRAY;
        myDataSource.responseSchema = {
            fields: dataFields
        };
        if ( dataCallback ) myDataSource.doBeforeCallback = dataCallback;

        myDataTable = new YAHOO.widget.DataTable(
            tableId,             // The dom element to contain the DataTable
            tableColumns,        // What columns will display
            myDataSource,        // The DataSource for our records
            {scrollable: true, selectionMode : 'single', height: "18em", sortedBy:{key:sortBy, dir:sortDir}}
        );
    } else {
        // Create the DataSource for server side data
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
        if ( dataCallback ) myDataSource.doBeforeCallback = dataCallback;

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

        myPaginator.subscribe( "rowsPerPageChange", function (event, target) {
            l7.DataTable.PageSize[tableId] = myPaginator.getRowsPerPage();
        } );

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
    }

    if ( selectionControlIds || selectionCallback ) {
        // Subscribes to events for row selection.
        myDataTable.subscribe("rowMouseoverEvent", myDataTable.onEventHighlightRow);
        myDataTable.subscribe("rowMouseoutEvent", myDataTable.onEventUnhighlightRow);
        if ( !multiSelect ) myDataTable.subscribe("rowClickEvent", myDataTable.onEventSelectRow);

        // Subscribes to row select changes for enabling/disabling toolbar buttons.
        var enableOrDisableControls = function (event, target) {
            var selectedRows = myDataTable.getSelectedRows();
            var hasSelectedRows = selectedRows && (selectedRows.length != 0);

            updateControlStates( hasSelectedRows );

            var selectedValue = "";
            if ( hasSelectedRows ) {
                selectedValue = this.getRecord( selectedRows[0] ).getData(idProperty);
            }

            var selectionControl = document.getElementById( selectionId );
            if ( selectionControl ) {
                selectionControl.value = selectedValue;
            }

            if( selectionCallback ) {
                selectionCallback( selectedValue );
            }
        }

        if ( !multiSelect ) {
            myDataTable.subscribe("rowSelectEvent", enableOrDisableControls);
            myDataTable.subscribe("rowUnSelectEvent", enableOrDisableControls);
        } else {
            document.getElementById( tableId ).updateItemSelection = function( identifier, selected ) {
                multiSelectUpdate();
            }

            document.getElementById( tableId ).updateItemSelections = function( selected ) {
                var ids = document.getElementById( tableId ).currentPageIdentifiers;
                for ( var item in ids ) {
                    var itemCheckbox = document.getElementById( tableId+'-'+ids[item] );
                    if ( itemCheckbox ) {
                        itemCheckbox.checked = selected;                        
                    }
                }
                multiSelectUpdate();
            }
        }

        enableOrDisableControls();
    }
};
