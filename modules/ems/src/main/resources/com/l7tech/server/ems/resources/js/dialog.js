/**
 * Javascript for YUI Dialog component
 */

/**
 * Initialize a DataTable.
 *
 * The text content for the dialog buttons is taken from the DOM for the
 * buttons with the given ids.
 *
 * Each button object should define:
 *
 * id - The DOM id for the related button
 * callback - Wait for callback to close dialog (true/false)
 * isDefault - True if this is the default button (true/false) )
 *
 * @param dialogId DOM id for div holding dialog.
 * @param optionButtons the object array for button config
 */
function showDialog( dialogId, optionButtons ) {
        // Track use of ok/close/cancel so we can ignore close after ok/cancel
        var handled = false;

	    // Event handlers and helpers  for Dialog
        var handle = function( id, dialog, callback ) {
            if ( !handled ) {
                handled = !callback;
                var button = document.getElementById( id );
                if ( callback ) {
                    button.wicketSuccessCallback = function() {
                        handled = true;
                        dialog.cancel();
                    };
                }
                button.click();
                if ( !callback ) dialog.cancel();
            } else {
                dialog.cancel();
            }
        };

        var closeHandler = function(){};
        var buttonArray = [];
        for ( var i=0; i<optionButtons.length; i++ ) {
            var scopedConfig = function() {
                var buttonId = optionButtons[i].id;
                var text = document.getElementById( buttonId ).innerHTML;
                var callback = optionButtons[i].callback;
                var isDefault = optionButtons[i].isDefault;
                var handleButton = function() {
                    handle( buttonId, this, callback );
                };
                closeHandler = handleButton; //TODO explicit button for close?
                buttonArray[i] = { text:text, handler:handleButton, isDefault:isDefault };
            }
            scopedConfig();
        }

        // Create the Dialog
	    var dialog = new YAHOO.widget.Dialog(
                                    dialogId,
                                    { width : "40em",
                                      fixedcenter : true,
                                      visible : false,
                                      modal : true,
                                      postmethod : "manual",
                                      hideaftersubmit : false,
                                      constraintoviewport : true,
                                      buttons : buttonArray
                                    });

        // Handle close dialog
        dialog.hideEvent.subscribe(closeHandler);

        // Show
	    dialog.render();
        dialog.show();
}