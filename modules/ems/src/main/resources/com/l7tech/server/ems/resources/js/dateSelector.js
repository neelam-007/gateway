/**
 * Javascript for YUI Calendar component
 */

var showing_dialog = null;

/**
 * Initialize a DateSelector
 *
 * @param inputId DOM id for related form text input control
 * @param calContainerId DOM id for div holding the calendar.
 * @param calBodyId DOM id for div holding the calendar body.
 * @param longDates true to use the long date format
 * @param txtDate the date to display (mm/dd/yyyy)
 * @param enddate the highest date selectable
 * @param ignoreFirstSelect set true to suppress pop-up on first select (for form focus issue)
 */
function initDateSelector( inputId, calContainerId, calBodyId, longDates, txtDate, enddate, ignoreFirstSelect ) {
    var over_cal = false;
    var dialog, calendar;

    var emstdateFormat = function( oDate ) {
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
        return sMonth + " " + sDate + ", " + sYear;
    };

    var emsdateFormat = function( oDate ) {
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
        return sYear + "-" + sMonth + "-" + sDate;
    };

    var dateFormat = longDates ? emstdateFormat : emsdateFormat;

    calendar = new YAHOO.widget.Calendar(calBodyId, {iframe:false,hide_blank_weeks:true,maxdate:enddate});

    dialog = new YAHOO.widget.Dialog(calContainerId, {
        context:[ inputId, "tl", "bl" ],
        width:"18em",
        draggable:false,
        close:false
    });

    var shown = false;
    function selectHandler(type,args,obj) {
        var selected = args[0];
        var selDate = calendar.toDate(selected[0]);

        if ( selected[0] ) {
            var dStr = "" + selDate.getDate();
            var mStr = "" + (selDate.getMonth() + 1);
            var yStr = "" + selDate.getFullYear();
            if ( dStr.length == 1 ) dStr = "0" + dStr;
            if ( mStr.length == 1 ) mStr = "0" + mStr;
            txtDate = mStr + "/" + dStr + "/" + yStr;
        }
        var input = YAHOO.util.Dom.get(inputId);
        if ( input ) {
            input.value = dateFormat( selDate );
            if ( input.onchange ) {
                input.onchange();
            }
        }

        dialog.hide();
    }

    calendar.renderEvent.subscribe(function() { dialog.fireEvent("changeContent"); });
    calendar.selectEvent.subscribe(selectHandler, calendar, true);

    var firstSelect = ignoreFirstSelect;
    YAHOO.util.Event.on(inputId, "focus", function() {
        if ( firstSelect ) {
            firstSelect = false;
        } else {
            if (txtDate != "") {
                calendar.select(txtDate);
                var selected = calendar.getSelectedDates()
                if ( selected != null && selected.length > 0 ) {
                    calendar.setMonth( selected[0].getMonth() );
                    calendar.setYear( selected[0].getFullYear() );
                }
            }

            calendar.render();

            dialog.render();
            dialog.align("tl", "bl");
            doShow();

            if (YAHOO.env.ua.opera && document.documentElement) {
                document.documentElement.className += "";
            }
        }
    });

    var inputFoc = false;
    var calMouse = false;

    YAHOO.util.Event.on(inputId, "blur", function() {
        inputFoc = false;
        checkClose();
    });

    YAHOO.util.Event.on(inputId, "focus", function() {
        inputFoc = true;
        checkClose();
    });

    YAHOO.util.Event.addListener(calContainerId, 'mouseover', function() {
        calMouse = true;
        checkClose();
    });

    YAHOO.util.Event.addListener(calContainerId, 'mouseout', function() {
        calMouse = false;
        checkClose();
    });

    var usingCal = true;
    
    function checkClose() {
        var using = inputFoc || calMouse;
        if (using != usingCal) {
            usingCal = using;
            if (!usingCal)
                setCloseTimer();
        }
    }

    var timer = null;

    function clearCloseTimer() {
        if ( timer != null ) {
            window.clearTimeout(timer);
            timer = null;
        }
    }

    function setCloseTimer() {
        clearCloseTimer();
        timer = window.setTimeout(function() {
                if (!usingCal)
                    doHide();
        }, 660);
    }

    function doShow() {
        if ( showing_dialog != null) showing_dialog.hide();
        dialog.show();
        showing_dialog = dialog;
    }

    function doHide() {
        dialog.hide();
        if ( dialog == showing_dialog )
            showing_dialog = null;
    }
};
