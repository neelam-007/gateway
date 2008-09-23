/**
 * Javascript for YUI Calendar component
 */

/**
 * Initialize a DateSelector
 *
 * @param inputId DOM id for related form text input control
 * @param buttonId DOM id for the button to show the calendar
 * @param calContainerId DOM id for div holding the calendar.
 * @param calBodyId DOM id for div holding the calendar body.
 */
function initDateSelector( inputId, buttonId, calContainerId, calBodyId ) {
    var dialog, calendar;
    calendar = new YAHOO.widget.Calendar(calBodyId, {iframe:false,hide_blank_weeks:true});

    function okHandler() {
        if (calendar.getSelectedDates().length > 0) {
            var selDate = calendar.getSelectedDates()[0];
            var dStr = "" + selDate.getDate();
            var mStr = "" + (selDate.getMonth() + 1);
            var yStr = "" + selDate.getFullYear();
            if ( dStr.length == 1 ) dStr = "0" + dStr;
            if ( mStr.length == 1 ) mStr = "0" + mStr;
            YAHOO.util.Dom.get(inputId).value = mStr + "/" + dStr + "/" + yStr;
        } else {
            YAHOO.util.Dom.get(inputId).value = "";
        }
        this.hide();
    }

    function cancelHandler() {
        this.hide();
    }

    dialog = new YAHOO.widget.Dialog(calContainerId, {
        context:[buttonId, "tl", "bl"],
        buttons:[ {text:"Select", isDefault:true, handler: okHandler},
                  {text:"Cancel", handler: cancelHandler}],
        width:"16em",
        draggable:false,
        close:true
    });

    calendar.render();
    dialog.render();
    dialog.hide();

    calendar.renderEvent.subscribe(function() { dialog.fireEvent("changeContent"); });

    YAHOO.util.Event.on(buttonId, "click", function() {
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
};
