/**
 * @module enterpriseTable
 * @namespace l7.EnterpriseTable
 * @requires l7.js
 * @requires YUI module "dom"
 * @requires YUI module "menu"
 */

if (!l7.EnterpriseTable) {
    (function() {

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
         * 2. Include the CSS class name "yui-skin-sam" in the BODY element.
         * 3. Include the CSS class name "l7-enterpriseTable" in TABLE elements to be applied.
         * 4. Include the CSS class name "l7-enterpriseTable-rowSelectable" in the TABLE element if rows
         *    should be selectable (i.e., highlighted when left-clicked), e.g., to work with a toolbar.
         * 5. Include one of the following CSS classes in TR elements for enterprise entities:
         *    l7-enterpriseFolder      - folder for grouping SSG Clusters
         *    l7-SSGCluster            - SSG Cluster
         *    l7-SSGNode               - SSG Node
         *    l7-policyFolder          - folder for group published services and policy fragments
         *    l7-publishedService      - published service
         *    l7-policyFragment        - policy fragment
         *    l7-publishedServiceAlias - published service alias
         *    l7-policyFragmentAlias   - policy fragment alias
         * 6. Additional CSS classes for TR elements:
         *    l7-enterpriseTable-disabled     - do not show context menu
         *    l7-enterpriseTable-cannotDelete - to disable any menu item for delete or move
         * 7. To localize, overrides these label text after referencing this file:
         *    l7.EnterpriseTable.NEW_FOLDER
         *    l7.EnterpriseTable.ADD_SSG_CLUSTER
         *    l7.EnterpriseTable.CREATE_SSG_NODE
         *    l7.EnterpriseTable.CREATE_SSG_NODE
         *    l7.EnterpriseTable.DELETE_SSG_NODE
         *    l7.EnterpriseTable.RENAME
         *    l7.EnterpriseTable.MOVE
         *    l7.EnterpriseTable.DELETE
         *    l7.EnterpriseTable.START
         *    l7.EnterpriseTable.STOP
         *    l7.EnterpriseTable.LAUNCH_SSM
         * 8. Invoke l7.EnterpriseTable.initAll() upon the onload event of the document.
         *    It will initialize all TABLE element with the CSS class 'l7-enterpriseTable' and add
         *    the property 'l7_enterpriseTable' to the table object.
         * 9. When the table changes, invoke <my l7.EnterpriseTable object>.init().
         *
         * @constructor
         * @param {object} table         the HTML TABLE element to construct from
         * @param {string} imgFolder     folder location of enterprise entity icons
         */
        l7.EnterpriseTable = function(table, imgFolder) {

            /** The assoicated TABLE element. */
            this.table = table;

            /** The currently selected TR. */
            var selectedTR = null;

            var Dom = YAHOO.util.Dom;

            function selectRow(row) {
                var oldRow = selectedTR;

                // Clear highlighting of previous selected TR, if any.
                if (selectedTR != null) l7.Util.removeClass(selectedTR, "selected");

                // Highlight the new selected TR.
                if (row != null) l7.Util.addClass(row, "selected");

                selectedTR = row;
                table.l7_enterpriseTable.onRowSelectionChange(oldRow, row);
            }

            /**
             * "beforeshow" event handler for the ContextMenu instance -
             * replaces the content of the ContextMenu instance based
             * on the CSS class name of the TR element that triggered
             * its display.
             *
             * @param {string} p_sType  String representing the name of the event that was fired.
             * @param {array} p_aArgs   Array of arguments sent when the event was fired.
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
                    if (l7.Util.hasClass(selectedTR, 'l7-enterpriseTable-disabled')) {
                        menuItems = null;
                    } else if (l7.Util.hasClass(selectedTR, 'l7-enterpriseFolder')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/addFolder.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.NEW_FOLDER},
                            {text: '<img src="' + imgFolder + '/addSSGCluster.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.ADD_SSG_CLUSTER},
                            {text: '<img src="' + imgFolder + '/createSSGCluster.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.CREATE_SSG_CLUSTER},
                            {text: '<img src="' + imgFolder + '/rename.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.RENAME},
                            {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.MOVE, disabled: l7.Util.hasClass(selectedTR, 'l7-enterpriseTable-cannotDelete')},
                            {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.DELETE, disabled: l7.Util.hasClass(selectedTR, 'l7-enterpriseTable-cannotDelete')}];
                    } else if (l7.Util.hasClass(selectedTR, 'l7-SSGCluster')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/createSSGNode.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.CREATE_SSG_NODE},
                            {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.MOVE},
                            {text: '<img src="' + imgFolder + '/rename.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.RENAME},
                            {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.DELETE},
                            {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.LAUNCH_SSM}];
                    } else if (l7.Util.hasClass(selectedTR, 'l7-SSGNode')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/deleteSSGNode.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.DELETE_SSG_NODE},
                            {text: '<img src="' + imgFolder + '/start.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.START},
                            {text: '<img src="' + imgFolder + '/stop.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.STOP}];
                    } else if (l7.Util.hasClass(selectedTR, 'l7-policyFolder')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/addFolder.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.NEW_FOLDER},
                            {text: '<img src="' + imgFolder + '/rename.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.RENAME},
                            {text: '<img src="' + imgFolder + '/move.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.MOVE},
                            {text: '<img src="' + imgFolder + '/delete.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.DELETE, disabled: l7.Util.hasClass(selectedTR, 'l7-enterpriseTable-cannotDelete')}];
                    } else if (l7.Util.hasClass(selectedTR, 'l7-publishedService')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.LAUNCH_SSM}];
                    } else if (l7.Util.hasClass(selectedTR, 'l7-policyFragment')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.LAUNCH_SSM}];
                    } else if (l7.Util.hasClass(selectedTR, 'l7-publishedServiceAlias')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.LAUNCH_SSM}];
                    } else if (l7.Util.hasClass(selectedTR, 'l7-policyFragmentAlias')) {
                        menuItems = [
                            {text: '<img src="' + imgFolder + '/ssm.png" style="position:absolute; left:4px;">&nbsp;' + l7.EnterpriseTable.LAUNCH_SSM}];
                    }

                    // Remove the existing content from the ContentMenu instance
                    this.clearContent();

                    // Add the new set of items to the ContentMenu instance
                    this.addItems(menuItems);

                    // Render the ContextMenu instance with the new content
                    this.render();
                }
            }

            /**
             * "hide" event handler for the ContextMenu - used to clear the selected <tr> element in the table.
             *
             * @param {string} p_sType  String representing the name of the event that was fired.
             * @param {array} p_aArgs   Array of arguments sent when the event was fired.
             */
            function onContextMenuHide(p_sType, p_aArgs) {
                selectRow(null);
            }

            /**
             * Event handler for row selection change.
             * Override this to customize, e.g., to enable toolbar buttons selectively.
             *
             * @param {object} oldRow    previously selected row; null if none
             * @param {object} newRow    newly selected row; null if none
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
             * @param {object} table     the associated TABLE element
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

                this.setRowSelectable(l7.Util.hasClass(table, 'l7-enterpriseTable-rowSelectable'));
            }

            this.init();
        }

        // Default values of internationalized strings.
        // To localize, overrides them on your web page after referencing this file.
        /** @static */ l7.EnterpriseTable.NEW_FOLDER = 'New Folder';
        /** @static */ l7.EnterpriseTable.ADD_SSG_CLUSTER = 'Add Cluster';
        /** @static */ l7.EnterpriseTable.CREATE_SSG_CLUSTER = 'Create Cluster';
        /** @static */ l7.EnterpriseTable.CREATE_SSG_NODE = 'Create Node';
        /** @static */ l7.EnterpriseTable.DELETE_SSG_NODE = 'Delete Node';
        /** @static */ l7.EnterpriseTable.RENAME = 'Rename';
        /** @static */ l7.EnterpriseTable.MOVE = 'Move';
        /** @static */ l7.EnterpriseTable.DELETE = 'Delete';
        /** @static */ l7.EnterpriseTable.START = 'Start';
        /** @static */ l7.EnterpriseTable.STOP = 'Stop';
        /** @static */ l7.EnterpriseTable.LAUNCH_SSM = 'Launch SecureSpan Manager';

        /**
         * Call this in body.onload. It will initializes all HTML TABLE element with the
         * CSS class 'l7-treeTable'.
         *
         * @static
         * @param {string} imageFolder   image folder path
         */
        l7.EnterpriseTable.initAll = function(imageFolder) {
            var tables = l7.Util.getElementsByClassName('l7-enterpriseTable', null, 'table');
            for (var i = 0; i < tables.length; ++i) {
                var table = tables[i];
                table.l7_enterpriseTable = new l7.EnterpriseTable(table, imageFolder);
            }
        }

    })();
}
