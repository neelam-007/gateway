// Default values of internationalized strings.
// To localize, overrides them on your web page after referencing this file.
var NEW_FOLDER = 'New Folder';
var ADD_SSG_CLUSTER = 'Add SSG Cluster';
var CREATE_SSG_NODE = 'Create SSG Node';
var RENAME = 'Rename';
var MOVE = 'Move';
var DELETE = 'Delete';
var DESTROY = 'Destroy';
var LAUNCH_SSM = 'Launch SecureSpan Manager';

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
 * 2. Include the class name "yui-skin-sam" in the BODY element.
 * 3. Include the class name "l7_enterpriseTable" in TABLE elements to be applied.
 * 4. Include the class name "l7_enterpriseTable_rowSelectable" in the TABLE element if rows
 *    should be selectable (i.e., highlighted when left-clicked), e.g., to work with a toolbar.
 * 5. Include one of the following classes in TR elements for enterprise entities:
 *    l7_folder                - folder
 *    l7_SSGCluster            - SSG Cluster
 *    l7_SSGNode               - SSG Node
 *    l7_publishedService      - published service
 *    l7_policyFragment        - policy fragment
 *    l7_publishedServiceAlias - published service alias
 *    l7_policyFragmentAlias   - policy fragment alias
 * 6. Additional TR classes:
 *    l7_enterpriseTable_disabled     - do not show context menu
 *    l7_enterpriseTable_cannotDelete - to disable any menu item for delete
 * 7. Invoke l7_enterpriseTable_init() upon the onload event of the document.
 *    It will initialize all TABLE element with the class 'l7_enterpriseTable' and add
 *    the property 'l7_enterpriseTable' to the table object.
 * 8. When the table changes, invoke <my table object>.l7_enterpriseTable.init().
 *
 * @param table         the HTML TABLE element to construct from
 * @param imgFolder     folder location of enterprise entity icons
 */
function l7_enterpriseTable(table, imgFolder) {

    /** The assoicated TABLE element. */
    this.table = table;

    /** The currently selected TR. */
    var selectedTR = null;

    var Dom = YAHOO.util.Dom;

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
            } else if (hasClass(selectedTR, 'l7_folder')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/addFolder.png" style="position:absolute; left:4px;">&nbsp;' + NEW_FOLDER},
                    {text: '<img src="' + imgFolder + '/addSSGCluster.png" style="position:absolute; left:4px;">&nbsp;' + ADD_SSG_CLUSTER},
                    {text: '<img src="' + imgFolder + '/rename.png" style="position:absolute; left:4px;">&nbsp;' + RENAME},
                    {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;">&nbsp;' + MOVE},
                    {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;">&nbsp;' + DELETE, disabled: hasClass(selectedTR, 'l7_enterpriseTable_cannotDelete')}];
            } else if (hasClass(selectedTR, 'l7_SSGCluster')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/addSSGNode.png" style="position:absolute; left:4px;">&nbsp;' + CREATE_SSG_NODE},
                    {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;">&nbsp;' + MOVE},
                    {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;">&nbsp;' + DELETE},
                    {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + LAUNCH_SSM}];
            } else if (hasClass(selectedTR, 'l7_SSGNode')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/destroy.png" style="position:absolute; left:4px;">&nbsp;' + DESTROY}];
            } else if (hasClass(selectedTR, 'l7_folder')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/addFolder.png" style="position:absolute; left:4px;">&nbsp;' + NEW_FOLDER},
                    {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;">&nbsp;' + MOVE},
                    {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;">&nbsp;' + DELETE, disabled: hasClass(selectedTR, 'l7_enterpriseTable_cannotDelete')}];
            } else if (hasClass(selectedTR, 'l7_publishedService')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + LAUNCH_SSM}];
            } else if (hasClass(selectedTR, 'l7_policyFragment')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + LAUNCH_SSM}];
            } else if (hasClass(selectedTR, 'l7_publishedServiceAlias')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + LAUNCH_SSM}];
            } else if (hasClass(selectedTR, 'l7_policyFragmentAlias')) {
                menuItems = [
                    {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + LAUNCH_SSM}];
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

    this.setRowSelectable = function(b) {
        if (b) {
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
        } else {
            table.onclick = null;
        }
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

        this.setRowSelectable(hasClass(table, 'l7_enterpriseTable_rowSelectable'));
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
