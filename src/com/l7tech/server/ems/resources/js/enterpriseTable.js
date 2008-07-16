/**
 * Constructor for a table with SecureSpan enterprise entities.
 *
 * A enterprise table is a table where some of its rows are enterprise entities.
 * This class will add context menu to those rows.
 *
 * To use this class,
 * 1. This class depends on YUI. Include these in the HTML header:
 *    <link rel="stylesheet" type="text/css" href="../yui/menu/assets/skins/sam/menu.css">
 *    <script type="text/javascript" src="../yui/yahoo-dom-event/yahoo-dom-event.js"></script>
 *    <script type="text/javascript" src="../yui/container/container_core-min.js"></script>
 *    <script type="text/javascript" src="../yui/menu/menu-min.js"></script>
 *    <script type="text/javascript" src="../js/enterpriseTable.js"></script>
 * 2. And include the class name "yui-skin-sam" in the BODY element.
 * 3. Include one of the following classes in TR elements for enterprise entities:
 *    enterpriseGroup                 - group
 *    enterprisePartition             - partition
 *    enterpriseClusterNode           - cluster node
 *    enterpriseFolder                - folder
 *    enterprisePublishedService      - published service
 *    enterprisePolicyFragment        - policy fragment
 *    enterprisePublishedServiceAlias - published service alias
 *    enterprisePolicyFragmentAlias   - policy fragment alias
 * 4. Additional TR classes:
 *    l7_enterpriseTable_disabled         - do not show context menu
 *    l7_enterpriseTable_cannotDelete     - to disable any menu item for delete
 * 5. Invoke l7_enterpriseTable_init() upon the onload event of the document.
 *    It will initialize all TABLE element with the class 'l7_enterpriseTable' and add
 *    the property 'l7_enterpriseTable' to the table object.
 * 6. When the table changes, invoke <my table object>.l7_enterpriseTable.init().
 *
 * @param table     the HTML TABLE element to construct from
 */
function l7_enterpriseTable(table, imgFolder) {

    /** The assoicated TABLE element. */
    this.table = table;

    /** The currently selected TR. */
    var selectedTR = null;

    var Dom = YAHOO.util.Dom;

    table.onclick = function(event) {
        var target;
        if (!event) {
            event = window.event;
        }
        if (event.target) {
            target = event.target;
        } else if (event.srcElement) {
            target = event.srcElement;
        }
        if (target.nodeType == 3) { // defeat Safari bug
            target = target.parentNode;
        }

        // Proceed only if click target is a TD element, i.e.,
        // not a checkbox, radio button, plus/minus sign, etc.
        if (target.nodeName.toUpperCase() != "TD") return;

        selectRow(Dom.getAncestorByTagName(target, "TR"));
    }

    function selectRow(row) {
        var oldRow = selectedTR;

        // Clear highlighting of previous selected TR, if any.
        if (selectedTR != null) removeClass(selectedTR, "selected");

        // Highlight the new selected TR.
        if (row != null) addClass(row, "selected");

        selectedTR = row;
        table.l7_enterpriseTable.onRowSelectionChange(oldRow, row);
    }

    /**
     * "beforeshow" event handler for the ContextMenu instance -
     * replaces the content of the ContextMenu instance based
     * on the CSS class name of the TR element that triggered
     * its display.
     */
    function onContextMenuBeforeShow(p_sType, p_aArgs) {
        var target = this.contextEventTarget;
        if (this.getRoot() == this) {
            // Get the TR element that was the target of the "contextmenu" event.
            var row = target.nodeName.toUpperCase() == "TR" ?
                      target : Dom.getAncestorByTagName(target, "TR");
            selectRow(row);

            // Get the array of MenuItems for the CSS class name from the "oContextMenuItems" map.
            // TODO: assign URLs
            var menuItems = null;
            if (hasClass(selectedTR, 'l7_enterpriseTable_disabled')) {
                menuItems = null;
            } else if (hasClass(selectedTR, 'enterpriseGroup')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/addGroup.png" style="position:absolute; left:4px;"> Create subgroup'},
                    {text: '<img src="' + imgFolder + '/addPartition.png" style="position:absolute; left:4px;"> Add partition'},
                    {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;"> Move'},
                    {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;"> Delete', disabled: hasClass(selectedTR, 'l7_enterpriseTable_cannotDelete')}];
            } else if (hasClass(selectedTR, 'enterprisePartition')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/addClusterNode.png" style="position:absolute; left:4px;"> Create cluster node'},
                    {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;"> Move'},
                    {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;"> Delete'},
                    {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;"> Launch SecureSpan Manager'}];
            } else if (hasClass(selectedTR, 'enterpriseClusterNode')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/destroy.png" style="position:absolute; left:4px;"> Destroy'}];
            } else if (hasClass(selectedTR, 'enterpriseFolder')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/addGroup.png" style="position:absolute; left:4px;"> Create subfolder'},
                    {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;"> Move'},
                    {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;"> Delete', disabled: hasClass(selectedTR, 'l7_enterpriseTable_cannotDelete')}];
            } else if (hasClass(selectedTR, 'enterprisePublishedService')) {
                // TODO
            } else if (hasClass(selectedTR, 'enterprisePolicyFragment')) {
                // TODO
            } else if (hasClass(selectedTR, 'enterprisePublishedServiceAlias')) {
                // TODO
            } else if (hasClass(selectedTR, 'enterprisePolicyFragmentAlias')) {
                // TODO
            }

            // Remove the existing content from the ContentMenu instance
            this.clearContent();

            // Add the new set of items to the ContentMenu instance
            this.addItems(menuItems);

            // Render the ContextMenu instance with the new content
            this.render();
        }
    }

    // "hide" event handler for the ContextMenu - used to clear the selected <tr> element in the table.
    function onContextMenuHide(p_sType, p_aArgs) {
        selectRow(null);
    }

    /**
     * Event handler for row selection change.
     * Override this to customize, e.g., to enable toolbar buttons selectively.
     *
     * @param oldRow    previously selected row; null if none
     * @param newRow    newly selected row; null if none
     */
    this.onRowSelectionChange = function(oldRow, newRow) {}

    this.getSelectedRow = function() {
        return selectedTR;
    }

    /**
     * (Re)initializes this object.
     *
     * @param table     the associated TABLE element
     */
    this.init = function() {
        // Instantiate a ContextMenu:  The first argument passed to
        // the constructor is the id of the element to be created; the
        // second is an object literal of configuration properties.
        var oContextMenu = new YAHOO.widget.ContextMenu("contextmenu", {
            trigger: table,
            lazyload: true
        });

        // Subscribe to the ContextMenu instance's "beforeshow" and "hide" events.
        oContextMenu.subscribe("beforeShow", onContextMenuBeforeShow);
        oContextMenu.subscribe("hide", onContextMenuHide);
    }

    this.init();
}

/**
 * Call this in body.onload. It will initializes all HTML TABLE element with the
 * class 'l7_treeTable'.
 *
 * @param imageFolder   image folder path
 */
function l7_enterpriseTable_init(imageFolder) {
    var tables = getElementsByClassName('l7_enterpriseTable', null, 'table');
    for (var i = 0; i < tables.length; ++i) {
        var table = tables[i];
        table.l7_enterpriseTable = new l7_enterpriseTable(table, imageFolder);
    }
}
