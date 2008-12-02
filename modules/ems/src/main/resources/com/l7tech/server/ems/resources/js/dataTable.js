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
 * @param data fields in JSON records
 * @param sortBy initial sort column
 * @param sortDir initial sort direction 'yui-dt-asc' or 'yui-dt-desc'
 * @param selectionControlIds DOM ids for components to be enabled on selection
 * @param selectionId DOM id of form field to set value of on selection
 * @param selectionCallback function to call on selection (passing identifier)
 * @param idProperty the property name to set on selection
 */
function initDataTable( tableId, tableColumns, pagingId, dataUrl, dataFields, tableData, sortBy, sortDir, selectionControlIds, selectionId, selectionCallback, idProperty  ) {
    var myPaginator,  // to hold the Paginator instance
        myDataSource, // to hold the DataSource instance
        myDataTable;  // to hold the DataTable instance

    var defaultPageSize = 10;

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

    if ( tableData ) {
        for ( var field in tableColumns ) {
            if ( tableColumns[field].formatter == 'emstdate' ) {
                tableColumns[field].formatter = emstdateFormat;
            } else if ( tableColumns[field].formatter == 'emsdate' ) {
                tableColumns[field].formatter = emsdateFormat;
            } else if ( tableColumns[field].formatter == 'ordered' ) {
                tableColumns[field].formatter = orderedFormat;
            }

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

        myDataTable = new YAHOO.widget.DataTable(
            tableId,             // The dom element to contain the DataTable
            tableColumns,        // What columns will display
            myDataSource,        // The DataSource for our records
            {scrollable: true, height: "18em", sortedBy:{key:sortBy, dir:sortDir}}
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
