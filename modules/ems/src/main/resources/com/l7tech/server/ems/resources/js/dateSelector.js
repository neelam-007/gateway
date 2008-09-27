/**
 * Javascript for YUI Calendar component
 */

/**
 * Initialize a DateSelector
 *
 * @param inputId DOM id for related form text input control
 * @param calContainerId DOM id for div holding the calendar.
 * @param calBodyId DOM id for div holding the calendar body.
 */
function initDateSelector( inputId, calContainerId, calBodyId ) {
    var dialog, calendar;
    calendar = new YAHOO.widget.Calendar(calBodyId, {iframe:false,hide_blank_weeks:true});

    dialog = new YAHOO.widget.Dialog(calContainerId, {
        context:[ inputId, "tl", "bl" ],
        width:"18em",
        draggable:false,
        close:false
    });

    function selectHandler(type,args,obj) {
        var selected = args[0];
        var selDate = calendar.toDate(selected[0]);

        var dStr = "" + selDate.getDate();
        var mStr = "" + (selDate.getMonth() + 1);
        var yStr = "" + selDate.getFullYear();
        if ( dStr.length == 1 ) dStr = "0" + dStr;
        if ( mStr.length == 1 ) mStr = "0" + mStr;
        YAHOO.util.Dom.get(inputId).value = mStr + "/" + dStr + "/" + yStr;

        dialog.hide();
    }

    calendar.renderEvent.subscribe(function() { dialog.fireEvent("changeContent"); });
    calendar.selectEvent.subscribe(selectHandler, calendar, true);

    calendar.render();
    dialog.render();
    dialog.hide();

    YAHOO.util.Event.on(inputId, "focus", function() {
        var txtDate = YAHOO.util.Dom.get(inputId).value;
        if (txtDate != "") {
            calendar.select(txtDate);
            var selected = calendar.getSelectedDates()
            if ( selected != null && selected.length > 0 ) {
                calendar.setMonth( selected[0].getMonth() );
                calendar.setYear( selected[0].getFullYear() );
                calendar.render();
            }
        }

        dialog.align("tl", "bl");
        dialog.show();
        if (YAHOO.env.ua.opera && document.documentElement) {
            document.documentElement.className += "";
        }
    });

    YAHOO.util.Event.on(inputId, "blur", function() {
        setTimeout( function() { dialog.hide(); }, 500 );
    });
};
