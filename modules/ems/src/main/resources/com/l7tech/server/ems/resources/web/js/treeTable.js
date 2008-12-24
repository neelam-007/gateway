/**
 * @module treeTable
 * @namespace l7.TreeTable
 * @requires l7.js
 */

if (!l7.TreeTable) {
    (function() {

        /**
         * Constructor for a tree table with collapsible togglers and cascading tristate checkboxes.
         *
         * A tree table is a table where some of its rows forms a tree-like relationship.
         * Children rows must be below their parent row.
         * Each parent row has a toggler IMG element that when clicked on, will expand or collapse
         * (i.e., unhide or hide) its child rows.
         * Optionally, each row can have a checkbox INPUT element, that behave as a tri-state
         * checkbox that cascades down and bubbles up.
         *
         * To use this class,
         * 1. Include the CSS class 'l7-treeTable' in the TABLE element.
         * 2. Each TR element belonging to the tree must have an HTML ID.
         * 3. Each parent TR element should have a IMG element with the CSS class 'l7_treeTable_toggler'.
         *    The CSS class 'clickable' will be added to it if not already.
         * 4. Each child TR element must have a TD element with the CSS class 'parentId' whose inner HTML is the parent row ID.
         * 5. Each TR can have a checkbox INPUT element with the CSS class 'l7_treeTable_checkbox'.
         * 6. Invoke l7.TreeTable.initAll() upon the onload event of the document.
         *    It will initialize all TABLE element with the CSS class 'l7-treeTable' and add
         *    the property 'l7_treeTable' to the table object.
         * 7. When the table changes, invoke <my l7.TreeTable object>.init().
         *
         * @constructor
         * @param {object} table     the HTML TABLE element to construct from
         */
        l7.TreeTable = function(table) {

            /**
             * The assoicated TABLE element.
             * @private
             * @type object
             */
            this.table = table;

            /**
             * Array-based hash map where map key is row HTML ID and map value is row index.
             * @private
             * @type array
             */
            this.rowIndices = new Array();

            /**
             * Array of parent ID (null if none) for each row. Array index is row index.
             * @private
             * @type array
             */
            this.parentIds = new Array();

            /**
             * Array of toggler IMG element (null if none) in each row. Array index is row index.
             * @private
             * @type array
             */
            this.togglers = new Array();

            /**
             * Array of checkbox INPUT element (null if none) in each row. Array index is row index.
             * @private
             * @type array
             */
            this.checkboxes = new Array();

            /**
             * Array of array (null if none) of child checkbox INPUT elements in each row. Array index is row index.
             * @private
             * @type array
             */
            this.childCheckboxes = new Array();

            /**
             * On-click event handler for a toggler IMG element.
             */
            function toggle() {

                /** File name portion of toggler image in expanded state. */
                var EXPANDED_TOGGLER_IMG_NAME = 'minus.png';

                /** File name portion of toggler image in collapsed state. */
                var COLLAPSED_TOGGLER_IMG_NAME = 'plus.png';

                var table = this.l7_treeTable.table;
                var parentIds = this.l7_treeTable.parentIds;
                var togglers = this.l7_treeTable.togglers;
                var rowIndex = this.l7_rowIndex;

                /**
                 * Returns true if the give toggler IMG element is in expanded state.
                 *
                 * @param toggler   the toggler IMG element
                 * @return boolean
                 */
                function isExpanded(toggler) {
                    return l7.Util.getFileName(toggler.src) == EXPANDED_TOGGLER_IMG_NAME;
                }

                /**
                 * Sets the state of the give toggler IMG element.
                 *
                 * @param {object} toggler      the toggler IMG element
                 * @param {boolean} expanded    true or false
                 */
                function setExpanded(toggler, expanded) {
                    toggler.src = l7.Util.getParent(toggler.src) + (expanded ? EXPANDED_TOGGLER_IMG_NAME : COLLAPSED_TOGGLER_IMG_NAME);
                }

                /**
                 * Hides all descendents of this row.
                 */
                function hideDescendents() {
                    var rows = table.rows;
                    var  rowsToHideParentIds = new Array(rows[rowIndex].id); // IDs of rows whose children need to be hidden.
                    for (var i = rowIndex + 1; i < rows.length; ++i) {
                        var row = rows[i];
                        var parentId = parentIds[i];
                        if (parentId != null) {
                            if (l7.Util.arrayContains(rowsToHideParentIds, parentId)) {
                                row.style.display = 'none';
                                // If this row has a toggler, then it is a parent and its children needs to be hidden too.
                                if (togglers[i] != null) {
                                    rowsToHideParentIds.push(row.id);
                                }
                            }
                        }
                    }
                }

                /**
                 * Unhide descendents of this row; subject to the state of its parent toggler.
                 */
                function unhideDescendents() {
                    var rows = table.rows;
                    var rowsToUnhideParentIds = new Array(rows[rowIndex].id); // IDs of rows whose children need to be unhidden.
                    for (var i = rowIndex + 1; i < rows.length; ++i) {
                        var row = rows[i];
                        var parentId = parentIds[i];
                        if (parentId != null) {
                            if (l7.Util.arrayContains(rowsToUnhideParentIds, parentId)) {
                                row.style.display = '';
                                // If this row has a toggler, then it is a parent.
                                // And if its toggler is in expanded state, then its children needs to be unhidden too.
                                var toggler = togglers[i];
                                if (toggler != null && isExpanded(toggler)) {
                                    rowsToUnhideParentIds.push(row.id);
                                }
                            }
                        }
                    }
                }

                if (isExpanded(this)) {
                    hideDescendents();
                    setExpanded(this, false);
                } else {
                    unhideDescendents();
                    setExpanded(this, true);
                }
            }

            /**
             * On-click event handler for a cascading tristate checkbox INPUT element.
             */
            function check() {

                var STATE_UNCHECKED = 0;
                var STATE_CHECKED = 1;
                var STATE_MIXED = 2;

                /** Style class that when applied to a checked checkbox, results in a mixed state checkbox. */
                var MIXED_STYLE_CLASS = 'mixedStateCheckbox';

                var table = this.l7_treeTable.table;
                var rowIndices = this.l7_treeTable.rowIndices;
                var parentIds = this.l7_treeTable.parentIds;
                var checkboxes = this.l7_treeTable.checkboxes;
                var childCheckboxes = this.l7_treeTable.childCheckboxes;
                var togglers = this.l7_treeTable.togglers;
                var rowIndex = this.l7_rowIndex;

                /**
                 * Returns the state of a tri-state checkbox.
                 *
                 * @param {object} checkbox     the checkbox INPUT element
                 * @return {int} STATE_UNCHECKED, STATE_CHECKED or STATE_MIXED
                 */
                function getState(checkbox) {
                    if (checkbox.checked) {
                        if (l7.Util.hasClass(checkbox, MIXED_STYLE_CLASS)) {
                            return STATE_MIXED;
                        } else {
                            return STATE_CHECKED;
                        }
                    } else {
                        if (l7.Util.hasClass(checkbox, MIXED_STYLE_CLASS)) {
                            return undefined;
                        } else {
                            return STATE_UNCHECKED;
                        }
                    }
                }

                /**
                 * Sets the display of a checkbox given its state.
                 *
                 * @param {object} checkbox     the checkbox INPUT element
                 * @param {int} state           STATE_UNCHECKED, STATE_CHECKED or STATE_MIXED
                 */
                function setState(checkbox, state) {
                    if (state == STATE_CHECKED) {
                        checkbox.checked = true;
                        l7.Util.removeClass(checkbox, MIXED_STYLE_CLASS);
                    } else if (state == STATE_UNCHECKED) {
                        checkbox.checked = false;
                        l7.Util.removeClass(checkbox, MIXED_STYLE_CLASS);
                    } else if (state == STATE_MIXED) {
                        checkbox.checked = true;
                        l7.Util.addClass(checkbox, MIXED_STYLE_CLASS);
                    }
                }

                /**
                 * Cascades down the checkbox state to the descendents of this row.
                 *
                 * @param {boolean} checked   true or false
                 */
                function checkDescendents(checked) {
                    var rows = table.rows;
                    var rowsToCheckParentIds = new Array(rows[rowIndex].id); // IDs of rows whose children need to be (un)checked.
                    for (var i = rowIndex + 1; i < rows.length; ++i) {
                        var row = rows[i];
                        var parentId = parentIds[i];
                        if (parentId != null) {
                            if (l7.Util.arrayContains(rowsToCheckParentIds, parentId)) {
                                var checkbox = checkboxes[i];
                                if (checkbox != null) {
                                    setState(checkbox, checked ? STATE_CHECKED : STATE_UNCHECKED);
                                    // If this row has a toggler, then it is a parent, then its children needs to be (un)checked too.
                                    if (togglers[i] != null) {
                                        rowsToCheckParentIds.push(row.id);
                                    }
                                }
                            }
                        }
                    }
                }

                /**
                 * Bubbles up the checkbox state to the ancestors of this row.
                 */
                function updateAncestors() {
                    for (var id = parentIds[rowIndex]; id != null && id != -1; id = parentIds[index]) {
                        var index = rowIndices[id];
                        var checkbox = checkboxes[index];
                        if (checkbox == null) {
                            break;
                        }
                        var children = childCheckboxes[index];
                        if (children != null) {
                            var numChecked = 0;
                            var numUnchecked = 0;
                            for (var i = 0; i < children.length; ++i) {
                                var child = children[i];
                                var state = getState(child);
                                if (state == STATE_CHECKED) {
                                    ++numChecked;
                                } else if (state == STATE_UNCHECKED) {
                                    ++numUnchecked;
                                }
                            }
                            if (numChecked == children.length) {
                                setState(checkbox, STATE_CHECKED);
                            } else if (numUnchecked == children.length) {
                                setState(checkbox, STATE_UNCHECKED);
                            } else {
                                setState(checkbox, STATE_MIXED);
                            }
                        }
                    }
                }

                l7.Util.removeClass(this, MIXED_STYLE_CLASS);
                checkDescendents(this.checked);
                updateAncestors();
            }

            /**
             * (Re)initializes this object.
             */
            this.init = function() {
                // Rebuild arrays for fast lookup.

                this.rowIndices.splice(0, this.rowIndices.length);
                this.parentIds.splice(0, this.parentIds.length);
                this.togglers.splice(0, this.togglers.length);
                this.checkboxes.splice(0, this.checkboxes.length);
                this.childCheckboxes.splice(0, this.childCheckboxes.length);

                var rows = table.rows;
                for (var i = 0; i < rows.length; ++i) {
                    var row = rows[i];

                    this.rowIndices[row.id] = i;

                    // Looks for parent ID.
                    var parentId = null;
                    var tds = l7.Util.getElementsByClassName('parentId', row, 'td');
                    if (tds.length == 1) {
                        parentId = tds[0].innerHTML;
                    }
                    this.parentIds[i] = parentId;

                    // Looks for toggler IMG elements and register event handler.
                    var toggler = null;
                    var imgs = l7.Util.getElementsByClassName('l7_treeTable_toggler', row, 'img');
                    if (imgs.length == 1) {
                        toggler = imgs[0];
                        toggler.l7_treeTable = this;
                        toggler.l7_rowIndex = i;
                        toggler.onclick = toggle;
                        l7.Util.addClass(toggler, 'clickable');
                    }
                    this.togglers[i] = toggler;

                    // Looks for checkbox INPUT elements and register event handler.
                    var checkbox = null;
                    var chks = l7.Util.getElementsByClassName('l7_treeTable_checkbox', row, 'input');
                    if (chks.length == 1) {
                        checkbox = chks[0];
                        checkbox.l7_treeTable = this;
                        checkbox.l7_rowIndex = i;
                        checkbox.onclick = check;

                        if (parentId != null) {
                            var parentIndex = this.rowIndices[parentId];
                            var a = this.childCheckboxes[parentIndex];
                            if (a == null) {
                                this.childCheckboxes[parentIndex] = a = new Array();
                            }
                            a.push(checkbox);
                        }
                    }
                    this.checkboxes[i] = checkbox;
                }
            }

            this.init();
        }

        /**
         * Call this in body.onload. It will initializes all HTML TABLE element with the
         * CSS class 'l7-treeTable'.
         *
         * @static
         */
        l7.TreeTable.initAll = function() {
            var tables = l7.Util.getElementsByClassName('l7-treeTable', null, 'table');
            for (var i = 0; i < tables.length; ++i) {
                var table = tables[i];
                table.l7_treeTable = new l7.TreeTable(table);
            }
        }

    })();
}
