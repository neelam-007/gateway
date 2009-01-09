/**
 * @module entityTreeTable
 * @namespace l7.EntityTreeTable
 * @requires l7.js
 * @requires YUI module "connection"
 * @requires YUI module "dom"
 * @requires YUI module "event"
 * @requires YUI module "json"
 * @requires YUI module "logger"
 * @requires YUI module "menu"
 */

if (!l7.EntityTreeTable) {
    (function() {

        // --------------------------------------------------------------------------------
        // Local constants
        // --------------------------------------------------------------------------------

        /** CSS style class for highlighting the table row of a selected entity. */
        var HIGHLIGHTED_BACKGROUND_CSS = 'selected';

        /** File name portion of toggler image in expanded state. */
        var TOGGLER_EXPANDED_IMG_NAME = 'minus.png';

        /** File name portion of toggler image in collapsed state. */
        var TOGGLER_COLLAPSED_IMG_NAME = 'plus.png';

        /** File name portion of image for no toggling. */
        var TOGGLER_NONE_IMG_NAME = 'dot.png';

        var TRISTATE_CHECKBOX_UNCHECKED = 0;
        var TRISTATE_CHECKBOX_CHECKED = 1;
        var TRISTATE_CHECKBOX_MIXED = 2;

        /** CSS style class that when applied to a checked checkbox, symbolizes a tri-state checkbox with mixed state. */
        var TRISTATE_CHECKBOX_MIXED_CSS = 'mixedStateCheckbox';

        /** Values of l7.EntityTreeTable.COLUMN has this prefix if it is a column for monitored property. */
        var MONITORING_COLUMN_PREFIX = 'monitoring-';

        var MONITORING_COLUMN_PREFIX_REGEXP = new RegExp('^' + MONITORING_COLUMN_PREFIX);

        // --------------------------------------------------------------------------------
        // Local methods
        // --------------------------------------------------------------------------------

        /**
         * @param {HTMLImageElement} img
         * @return {boolean}
         */
        function isToggler(img) {
            var imgName = l7.Util.getFileName(img.src);
            return imgName == TOGGLER_EXPANDED_IMG_NAME ||
                   imgName == TOGGLER_COLLAPSED_IMG_NAME ||
                   imgName == TOGGLER_NONE_IMG_NAME;
        }
        /**
         * @param {string} columnId     an l7.EntityTreeTable.COLUMN enum value
         * @return {boolean} true if a column for monitored property
         */
        function isMonitoringColumn(columnId) {
            return MONITORING_COLUMN_PREFIX_REGEXP.test(columnId);
        }

        /**
         * @param {string} columnId     an l7.EntityTreeTable.COLUMN enum value
         * @return {string} the property name portion of columnId; null if not a column for monitored property
         */
        function getMonitoredPropertyNameInColumn(columnId) {
            if (columnId.search(MONITORING_COLUMN_PREFIX) == 0) {
                return columnId.substr(MONITORING_COLUMN_PREFIX.length);
            } else {
                return null;
            }
        }

        // --------------------------------------------------------------------------------
        // Class definition
        // --------------------------------------------------------------------------------

        /**
         * Constructs the rows of a table body using given entity data.
         *
         * Configurations are passed in as an object literal with properties:
         *   localizedStrings {object} {optional) - object literal with properties to override those of l7.EntityTreeTable.DEFAULT_LOCALIZED_STRINGS
         *   columns {array} - array of l7.EntityTreeTable.COLUMN values, from left to right
         *   highlightableEntities {array} - array of l7.EntityTreeTable.ENTITY values; specifies which entity types can be highlighted; undefined or null means none
         *   onEntityHighlighted {function(previous, entity)} - callback method upon an entity (of a type specified in highlightableEntities) highlighted; arguments are the previously highlighted entity object literal (null if none) and the newly highlighted entity object literal
         *   entitiesWithTristateCheckbox {array} (optional) - array of l7.EntityTreeTable.ENTITY values that should get tri-state multi-selection checkbox
         *   entitiesWithRadioButton {array} (optional) - array of l7.EntityTreeTable.ENTITY values that should get mono-selection radio button
         *   entitiesWithZoomIcon {array} - array of l7.EntityTreeTable.ENTITY values that should have zoom icon; optional if columns contains l7.EntityTreeTable.COLUMN.ZOOM; default is all types
         *   onClickTrustDialogButton {function(event, entity)} - required if columns contains l7.EntityTreeTable.COLUMN.TRUST_STATUS
         *   onClickAccessDialogButton {function(event, entity)} - required if columns contains l7.EntityTreeTable.COLUMN.ACCESS_STATUS
         *   onClickEntityTristateCheckbox {function(event, entity)} - callback method upon click of an entity tri-state checkbox; optional if entitiesWithTristateCheckbox array is used
         *   onClickEntityRadioButton {function(event, entity)} - callback method upon click of an entity radio button; optional if entitiesWithRadioButton array is used
         *   onClickDashboardCheckbox {function(event, entity)} - callback method upon state change of Dashboard checkboxes; required if columns contains l7.EntityTreeTable.COLUMN.DASHBOARD
         *   onClickZoomIcon {function(event, entity)} - callback method upon click of an entity zoom icon; required if columns contain l7.EntityTreeTable.COLUMN.ZOOM
         *   onClickMonitoringPropertyDialogButton {function(event, params)} - callback method upon clicking a monitoring property dialog button; required if columns contains l7.EntityTreeTable.COLUMN.MONITORING_*
         *
         * Entity data are passed in as an array of entity object literals, with these properties (as applicable):
         *   id {string} (always required)
         *   parentId {string} (always required) - null if no parent, i.e., this is root
         *   type {string} (always required) - an l7.EntityTreeTable.ENTITY value
         *   name {string} (always required)
         *   ancestors {array} - array of ancestor's names, ordered from topmost to immediate parent
         *   rbacCUD {boolean} (always required) - applicable to enterprise folders, SSG Clusters, service folders, published services or policy fragments;
         *                       whether the current user has RBAC permission to create/update/delete an entity;
         *                       for an enterprise folder, this means rename, move, delete or add children
         *                       (Exception: root folder cannot be moved or deleted by anyone);
         *                       for an SSG Cluster, this means rename, move or delete
         *   version {string} - applicable to an SSG Cluster/Node or a service policy
         *   onlineStatus {string} - an l7.EntityTreeTable.SSG_CLUSTER_ONLINE_STATE value for an SSG Cluster; an l7.EntityTreeTable.SSG_NODE_ONLINE_STATE for an SSG Node
         *   trustStatus {boolean} - true if trust has been established
         *   accessStatus {boolean} - applicable to SSG Clusters or SSG Nodes; true if access account has been set for an SSG Cluster; true if access role has been granted for an SSG Node
         *   sslHostName {string} - applicable to SSG Clusters
         *   adminPort {string} - applicable to SSG Clusters
         *   ipAddress {string} - applicable to SSG Clusters or SSG Nodes
         *   dbHosts {array} - applicable to SSG Clusters; array of database host names
         *   selfHostName {string} - applicable to SSG Nodes
         *   monitoredProperties {object} - applicable to SSG Clusters or SSG Nodes;
         *                                  for an SSG Cluster the possible properties are l7.EntityTreeTable.SSG_CLUSTER_MONITORING_PROPERTY values;
         *                                  for an SSG Node the possible properties are l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY values;
         *                                  their property values are object literals with 3 properties:
         *                                    monitored {boolean} (required) -
         *                                    value {string} (optional) - in displayable format
         *                                    critical {boolean} (optional) - default is false
         *
         * @public
         * @constructor
         * @param {HTMLTableSectionElement} tbody   the table TBODY element to populate
         * @param {array} entities                  entity data for populating the table rows
         * @param {string} imgFolder                folder location of image icons
         * @param {object} config                   object literal of configuration values
         * @return {l7.EntityTreeTable} an l7.EntityTreeTable instance or null if error
         */
        l7.EntityTreeTable = function(tbody, entities, imgFolder, config) {
            if (tbody == null || tbody.nodeName == undefined || tbody.nodeName.toLowerCase() != 'tbody') {
                YAHOO.log('Cannot instantiate EntityTreeTable: tbody must be an HTMLTableSectionElement', 'error', 'l7.EntityTreeTable constructor');
                return;
            }
            if (!(entities instanceof Array)) {
                YAHOO.log('Cannot instantiate EntityTreeTable: entities must be an array', 'error', 'l7.EntityTreeTable constructor');
                return;
            }
            if (typeof imgFolder != 'string') {
                YAHOO.log('Cannot instantiate EntityTreeTable: imgFolder must be a string', 'error', 'l7.EntityTreeTable constructor');
                return;
            }
            if (typeof config != 'object') {
                YAHOO.log('Cannot instantiate EntityTreeTable: config must be an object', 'error', 'l7.EntityTreeTable constructor');
                return;
            }

            // --------------------------------------------------------------------------------
            // Private instance variables
            // --------------------------------------------------------------------------------

            /**
             * The assoicated TABLE element.
             * @private
             * @type HTMLTableSectionElement
             */
            this._tbody = tbody;

            /**
             * @private
             */
            this._entities = entities;

            /**
             * @private
             * @type object
             */
            this._imgFolder = imgFolder;

            /**
             * @private
             * @type object
             */
            this._config = config;

            /**
             * Map of entities.
             * Map key is entity ID.
             * Map value is the entity object.
             * @private
             * @type object
             */
            this._entitiesById = {};

            /**
             * Name for all entity radio INPUT buttons; randomly generated to ensure uniqueness.
             * @private
             * @type string
             */
            this._radioButtonsName = (((1+Math.random())*0x100000000)|0).toString(16).substring(1);

            /**
             * The entity object literal currently highlighted.
             * @private
             * @type object
             */
            this._highlightedEntity = null;

            /**
             * @private
             * @type object
             */
            this._localizedStrings = {};

            // --------------------------------------------------------------------------------
            // Local methods
            // --------------------------------------------------------------------------------

            /**
             * Onclick event handler for the table body.
             * @param {MouseEvent} event    the click event
             * Expected execution scope is the l7.EntityTreeTable instance.
             */
            function onClickTBody(event) {
                var target = YAHOO.util.Event.getTarget(event);

                // Proceed only if click target is not a checkbox, radio button or clickable image.
                var nodeName = target.nodeName.toLowerCase();
                if (nodeName == 'input') return;
                if (nodeName == 'img' && (l7.Util.hasClass(target, 'clickableImg') || l7.Util.hasClass(target, 'clickable'))) return;

                var tr = YAHOO.util.Dom.getAncestorByTagName(target, 'tr');
                var entity = tr._entity;
                if (l7.Util.arrayContains(this._config.highlightableEntities, entity.type)) {
                    this.highlightEntity(entity, true);
                }
            };

            /**
             * Onclick event handler for a toggler IMG element.
             * @param {MouseEvent} event    the click event
             * @param {object} params       parameters passed;
             *                              where params.entities is array of entity object literal,
             *                              and params.entityId is the ID of the entity being clicked
             * Expected execution scope is the toggler HTMLImageElement.
             */
            function onEntityTogglerClicked(event, params) {
                var entities = params.entities;
                var entityId = params.entityId;

                /**
                 * @param {HTMLImageElement} toggler    the toggler IMG element
                 * @return {boolean} true if the give toggler IMG element is in expanded state
                 */
                function isExpanded(toggler) {
                    return l7.Util.getFileName(toggler.src) == TOGGLER_EXPANDED_IMG_NAME;
                }

                /**
                 * Sets the state of the give toggler IMG element.
                 * @param {HTMLImageElement} toggler    the toggler IMG element
                 * @param {boolean} expanded            true for expanded state; false for collapsed state
                 */
                function setExpanded(toggler, expanded) {
                    toggler.src = l7.Util.getParent(toggler.src)
                                + (expanded ? TOGGLER_EXPANDED_IMG_NAME : TOGGLER_COLLAPSED_IMG_NAME);
                }

                /**
                 * Hides all descendents of this row.
                 */
                function hideDescendents() {
                    // We cannot use recursion because browsers cannot cope with that.
                    // Instead, we use a single-pass algorithm that take advantage of the fact that
                    // the entities array is ordered with children following their parent.

                    // First, find the array index position of the entity being clicked.
                    for (var i = 0; i < entities.length; ++i) {
                        if (entities[i].id == entityId) {
                            // Found. Starting from the position after that, iterates through its descendents.
                            var idsToHide = [ entityId ]; // IDs of entities whose children need to be hidden.
                            for (++i; i < entities.length; ++i) {
                                var entity = entities[i];
                                if (l7.Util.arrayContains(idsToHide, entity.parentId)) {
                                    entity._tr.style.display = 'none';
                                    if (entity._children.length > 0) {
                                        // This entity itself is a parent. Remember to hide its children.
                                        idsToHide.push(entity.id);
                                    }
                                }
                            }
                            break; // All done.
                        }
                    }
                }

                /**
                 * Unhide descendents of this row; subject to the state of its parent toggler.
                 */
                function unhideDescendents() {
                    // Single-pass algorithm.
                    // First, find the array index position of the entity being clicked.
                    for (var i = 0; i < entities.length; ++i) {
                        if (entities[i].id == entityId) {
                            // Found. Starting from the position after that, iterates through its descendents.
                            var idsToUnhide = [ entityId ]; // IDs of entities whose children need to be unhidden.
                            for (++i; i < entities.length; ++i) {
                                var entity = entities[i];
                                if (l7.Util.arrayContains(idsToUnhide, entity.parentId)) {
                                    entity._tr.style.display = '';
                                    if (entity._children.length > 0) {
                                        // This entity itself is a parent.
                                        // If its toggler is in expanded state, then its children needs to be unhidden too.
                                        if (isExpanded(entity._toggler)) {
                                            idsToUnhide.push(entity.id);
                                        }
                                    }
                                }
                            }
                            break; // All done.
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
             * Onclick event handler for a checkbox INPUT element.
             * @param {MouseEvent} event    the click event
             * @param {object} params       parameters passed;
             *                              where params.entities is array of entity object literals,
             *                              and params.entityId is the ID of the entity clicked,
             *                              and params.callback is the user-supplied call back method
             * Expected execution scope is the checkbox HTMLInputElement.
             */
            function onEntityCheckboxClicked(event, params) {
                var entities = params.entities;
                var entityId = params.entityId;
                var callback = params.callback;

                function getState(entity) {
                    return entity._tristateCheckboxState;
                }

                /**
                 * Sets the state of tri-state checkbox of an entity.
                 * @param {object} entity       the entity object literal
                 * @param {int} state           TRISTATE_CHECKBOX_UNCHECKED, TRISTATE_CHECKBOX_CHECKED or TRISTATE_CHECKBOX_MIXED
                 */
                function setState(entity, state) {
                    var checkbox = entity._tristateCheckbox;
                    if (checkbox) {
                        if (state == TRISTATE_CHECKBOX_UNCHECKED) {
                            checkbox.checked = false;
                            l7.Util.removeClass(checkbox, TRISTATE_CHECKBOX_MIXED_CSS);
                        } else if (state == TRISTATE_CHECKBOX_CHECKED) {
                            checkbox.checked = true;
                            l7.Util.removeClass(checkbox, TRISTATE_CHECKBOX_MIXED_CSS);
                        } else if (state == TRISTATE_CHECKBOX_MIXED) {
                            checkbox.checked = true;
                            l7.Util.addClass(checkbox, TRISTATE_CHECKBOX_MIXED_CSS);
                        }
                    }
                    entity._tristateCheckboxState = state;
                }

                /**
                 * Sets the state of tri-state checkbox of an entity according to the states of its children.
                 * @param {object} entity       the entity object literal
                 */
                function setStateAccordingToChildren(entity) {
                    // Counts the states of its children.
                    var numUnchecked = 0;
                    var numChecked = 0;
                    for (var j in entity._children) {
                        var state = getState(entity._children[j]);
                        if (state == TRISTATE_CHECKBOX_UNCHECKED) {
                            ++numUnchecked;
                        } else if (state == TRISTATE_CHECKBOX_CHECKED) {
                            ++numChecked;
                        }
                    }

                    if (numUnchecked == entity._children.length) {
                        // All children unchecked.
                        setState(entity, TRISTATE_CHECKBOX_UNCHECKED);
                    } else if (numChecked == entity._children.length) {
                        // All children checked.
                        setState(entity, TRISTATE_CHECKBOX_CHECKED);
                    } else {
                        // Some children checked, some unchecked, some mixed.
                        setState(entity, TRISTATE_CHECKBOX_MIXED);
                    }
                }

                /**
                 * Cascades down the checkbox state to the descendents of this entity.
                 * @param {boolean} checked   true or false
                 */
                function checkDescendents(checked) {
                    // Single-pass algorithm.
                    // Starting from the index position after the entity being clicked, iterates through its descendents.
                    var idsToCheck = [ entityId ]; // IDs of entities whose children need to be hidden.
                    for (var i = index + 1; i < entities.length; ++i) {
                        var entity = entities[i];
                        if (l7.Util.arrayContains(idsToCheck, entity.parentId)) {
                            if (entity.rbacCUD) {
                                setState(entity, checked ? TRISTATE_CHECKBOX_CHECKED : TRISTATE_CHECKBOX_UNCHECKED);
                            }
                            if (entity._children.length > 0) {
                                // This entity itself is a parent. Remember to check its children.
                                idsToCheck.push(entity.id);
                            }
                        }
                    }
                }


                /**
                 * Bubbles up the checkbox state to the ancestors of this entity.
                 */
                function updateAncestors() {
                    // Single-pass algorithm.
                    // Starting from the index position before the entity being clicked, iterates through its ancestors.
                    var parentId = entities[index].parentId;
                    for (var i = index - 1; i >= 0; --i) {
                        if (entities[i].id == parentId) {
                            var entity = entities[i];
                            setStateAccordingToChildren(entity);
                            parentId = entity.parentId;
                        }
                    }
                }

                // First, find the array index position of the entity being clicked.
                var index;
                for (var i = 0; i < entities.length; ++i) {
                    if (entities[i].id == entityId) {
                        index = i;
                        break;
                    }
                }
                if (index == null) {
                    YAHOO.log('Entity ID (' + entityId + ') not found in entities array.', 'error', 'l7.EntityTreeTable onEntityCheckboxClicked');
                    return;
                }

                setState(entities[index], this.checked ? TRISTATE_CHECKBOX_CHECKED : TRISTATE_CHECKBOX_UNCHECKED);
                checkDescendents(this.checked);
                updateAncestors();

                if (callback != undefined && callback != null) {
                    callback(event, entities[index]);
                }
            }

            // --------------------------------------------------------------------------------
            // Private instance methods
            // --------------------------------------------------------------------------------

            /**
             * Finds the index position of a column in the table.
             * @private
             * @param {string} column   an l7.EntityTreeTable.COLUMN value
             * @return column index if found; null if not column is not in table
             */
            this._getColumnIndex = function(column) {
                var columns = this._config.columns;
                for (var i in columns) {
                    if (column == columns[i]) {
                        return i;
                    }
                }
                return null;
            }

            /**
             * @private
             * @param {object} strings  map of localized strings
             */
            this._setLocalizedStrings = function(strings) {
                var i;
                // Initializes with default values.
                for (i in l7.EntityTreeTable.DEFAULT_LOCALIZED_STRINGS) {
                    this._localizedStrings[i] = l7.EntityTreeTable.DEFAULT_LOCALIZED_STRINGS[i];
                }
                // Then customizes with passed values.
                for (i in strings) {
                     this._localizedStrings[i] = strings[i];
                }
            }

            /**
             * Initializes a TD in the name column for an entity.
             *
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initNameCell = function(td, entity) {
                td.className = 'name';  // CSS style class

                // Starts with indentation proportional to tree level.
                for (var i = 0; i < entity._treeLevel; ++i) {
                    var indent = document.createElement('img');
                    indent.src = this._imgFolder + '/spacer16.png';
                    td.appendChild(indent);
                }

                // Followed by a toggler IMG (+|-|.) to expand/collapse children.
                var toggler = document.createElement('img');
                if (entity._children.length == 0) {
                    toggler.src = this._imgFolder + '/' + TOGGLER_NONE_IMG_NAME;
                } else {
                    toggler.src = this._imgFolder + '/' + TOGGLER_EXPANDED_IMG_NAME;
                    toggler.className = 'clickable';
                    YAHOO.util.Event.addListener(toggler, 'click', onEntityTogglerClicked, {entities : this._entities, entityId : entity.id});
                }
                td.appendChild(toggler);
                entity._toggler = toggler;

                // Followed by a tri-state checkbox, if specified.
                if (this._config.entitiesWithTristateCheckbox != undefined && this._config.entitiesWithTristateCheckbox != null) {
                    if (l7.Util.arrayContains(this._config.entitiesWithTristateCheckbox, entity.type)) {
                        var checkbox = document.createElement('input');
                        checkbox.type = 'checkbox';
                        checkbox.disabled = !entity.rbacCUD;
                        YAHOO.util.Event.addListener(checkbox, 'click', onEntityCheckboxClicked, {entities : this._entities, entityId : entity.id, callback : this._config.onClickEntityTristateCheckbox});
                        td.appendChild(checkbox);
                        entity._tristateCheckbox = checkbox;
                        entity._tristateCheckboxState = TRISTATE_CHECKBOX_UNCHECKED;
                    }
                }

                // Or a radio button, if specified.
                if (this._config.entitiesWithRadioButton != undefined && this._config.entitiesWithRadioButton != null) {
                    if (l7.Util.arrayContains(this._config.entitiesWithRadioButton, entity.type)) {
                            var radioButton = l7.Widget.createInputRadio(this._radioButtonsName);
                            radioButton.disabled = !entity.rbacCUD;
                            if (this._config.onClickEntityRadioButton != undefined && this._config.onClickEntityRadioButton != null) {
                                YAHOO.util.Event.addListener(radioButton, 'click', this._config.onClickEntityRadioButton, entity);
                            }
                            td.appendChild(radioButton);
                            entity._radioButton = radioButton;
                    }
                }

                // Followed by an entity icon.
                var icon = document.createElement('img');
                if (entity.type == l7.EntityTreeTable.ENTITY.ENTERPRISE_FOLDER) {
                    icon.src = this._imgFolder + '/folder.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                    icon.src = this._imgFolder + '/SSGCluster.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_NODE) {
                    icon.src = this._imgFolder + '/SSGNode.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SERVICE_FOLDER) {
                    icon.src = this._imgFolder + '/folder.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.PUBLISHED_SERVICE) {
                    icon.src = this._imgFolder + '/publishedService.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.PUBLISHED_SERVICE_ALIAS) {
                    icon.src = this._imgFolder + '/publishedServiceAlias.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.OPERATION) {
                    icon.src = this._imgFolder + '/operation.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.POLICY_FRAGMENT) {
                    icon.src = this._imgFolder + '/policyFragment.png';
                } else if (entity.type == l7.EntityTreeTable.ENTITY.POLICY_FRAGMENT_ALIAS) {
                    icon.src = this._imgFolder + '/policyFragmentAlias.png';
                } else {
                    YAHOO.log('Using blank entity icon because entity type is unrecognized: ' + entity.type, 'warning', 'l7.EntityTreeTable.initNameTD');
                    icon.src = this._imgFolder + '/spacer16.png';
                }
                td.appendChild(icon);
                entity._icon = icon;

                // Finally the entity name.
                // Valuable Lesson: Don't do this by
                //     td.innerHTML += entity.name
                // That will nuke event handlers of all preceding elements.
                var name = document.createElement('span');
                if (entity.type == l7.EntityTreeTable.ENTITY.PUBLISHED_SERVICE_ALIAS ||
                    entity.type == l7.EntityTreeTable.ENTITY.POLICY_FRAGMENT_ALIAS) {
                    name.style.fontStyle = 'italic';
                }
                name.innerHTML = entity.name;
                td.appendChild(name);
                entity._nameSpan = name;
            }

            /**
             * Initializes a TD in the online status column for an entity.
             *
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initOnlineStatusCell = function(td, entity) {
                var icon = document.createElement('img');
                entity._onlineStatusIcon = icon;
                this.setEntityOnlineStatus(entity.id, entity.onlineStatus);
                td.appendChild(icon);
            }

            /**
             * Initializes a TD in the trust status column for an entity.
             *
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initTrustStatusCell = function(td, entity) {
                var icon = document.createElement('img');
                entity._trustStatusIcon = icon;
                this.setEntityTrustStatus(entity.id, entity.trustStatus);
                td.appendChild(icon);
            }

            /**
             * Initializes a TD in the access status column for an entity.
             *
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initAccessStatusCell = function(td, entity) {
                var icon = document.createElement('img');
                entity._accessStatusIcon = icon;
                this.setEntityAccessStatus(entity.id, entity.trustStatus, entity.accessStatus);
                td.appendChild(icon);
            }

            /**
             * Initializes a TD in the dashboard column for an SSG Cluster.
             *
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initDashboardCell = function(td, entity) {
                td.className = 'dashboard'; // CSS style class
                // Only SSG Clusters can have dashboard.
                if (entity.type == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                    var checkbox = document.createElement('input');
                    checkbox.type = 'checkbox';
                    checkbox.title = this._localizedStrings.DASHBOARD_CHECKBOX_TOOLTIP;
                    if (this._config.onClickDashboardCheckbox != undefined) {
                        YAHOO.util.Event.addListener(checkbox, 'click', this._config.onClickDashboardCheckbox, entity);
                    }
                    checkbox.disabled = !entity.trustStatus || !entity.accessStatus;
                    td.appendChild(checkbox);
                    entity._dashboardCheckbox = checkbox;
                }
            }

            /**
             * Initializes a TD in the type column for an entity.
             *
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initTypeCell = function(td, entity) {
                if (entity.type == l7.EntityTreeTable.ENTITY.ENTERPRISE_FOLDER) {
                    td.innerHTML = this._localizedStrings.ENTERPRISE_FOLDER;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                    td.innerHTML = this._localizedStrings.SSG_CLUSTER;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_NODE) {
                    td.innerHTML = this._localizedStrings.SSG_NODE;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SERVICE_FOLDER) {
                    td.innerHTML = this._localizedStrings.SERVICE_FOLDER;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.PUBLISHED_SERVICE) {
                    td.innerHTML = this._localizedStrings.PUBLISHED_SERVICE;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.PUBLISHED_SERVICE_ALIAS) {
                    td.innerHTML = this._localizedStrings.PUBLISHED_SERVICE_ALIAS;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.OPERATION) {
                    td.innerHTML = this._localizedStrings.OPERATION;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.POLICY_FRAGMENT) {
                    td.innerHTML = this._localizedStrings.POLICY_FRAGMENT;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.POLICY_FRAGMENT_ALIAS) {
                    td.innerHTML = this._localizedStrings.POLICY_FRAGMENT_ALIAS;
                }
            }

            /**
             * Initializes a TD in the version column for an entity.
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initVersionCell = function(td, entity) {
                if (entity.version != undefined) {
                    td.className = l7.Util.isIntString(entity.version) ? 'right' : 'left';
                    td.innerHTML = entity.version;
                }
            }

            /**
             * Initializes a TD in the details column for an entity.
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initDetailsCell = function(td, entity) {
                td.className = 'details'; // CSS style class.
                if (entity.type == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                    td.innerHTML = '<span class="hasTooltip" title="' + this._localizedStrings.SSL_HOST_NAME + '">' + entity.sslHostName + '</span>'
                                 + ':<span class="hasTooltip" title="' + this._localizedStrings.ADMINISTRATIVE_PORT_NUMBER + '">' + entity.adminPort + '</span>';
                    if (entity.ipAddress) {
                        td.innerHTML += ' (<span class="hasTooltip" title="' + this._localizedStrings.IP_ADDRESS + '">' + entity.ipAddress + '</span>)';
                    }
                    if (entity.dbHosts) {
                        td.innerHTML += ' [<span class="hasTooltip" title="' + this._localizedStrings.DATABASE_HOSTS + '">' + entity.dbHosts.join(',') + '</span>]';
                    }
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_NODE) {
                    td.innerHTML = '<span class="hasTooltip" title="' + this._localizedStrings.SELF_HOST_NAME + '">' + entity.selfHostName + '</span>'
                                 + ' (<span class="hasTooltip" title="' + this._localizedStrings.IP_ADDRESS + '">' + entity.ipAddress + '</span>)';
                } else {
                    // No details for other entity types.
                }
            }

            /**
             * Initializes a TD in the zoom column for an entity.
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             */
            this._initZoomCell = function(td, entity) {
                td.className = 'center'; // CSS style class.
                if (this._config.entitiesWithZoomIcon == undefined ||
                    l7.Util.arrayContains(this._config.entitiesWithZoomIcon, entity.type)) {
                    var zoomIcon = document.createElement('img');
                    zoomIcon.src = this._imgFolder + '/zoom.png';
                    if (entity.rbacCUD) {
                        zoomIcon.className = 'clickable'; // CSS style class.
                        zoomIcon.title = this._localizedStrings.ZOOM_ICON_TOOLTIP;
                        if (this._config.onClickZoomIcon != undefined) {
                            YAHOO.util.Event.addListener(zoomIcon, 'click', this._config.onClickZoomIcon, entity);
                        }
                    } else {
                        zoomIcon.className = 'disabled'; // CSS style class.
                    }
                    td.appendChild(zoomIcon);
                    entity._zoomIcon = zoomIcon;
                }
            }

            /**
             * Initializes a TD in a monitored property column for an entity.
             * @private
             * @param {HTMLTableCellElement} td
             * @param {object} entity
             * @param {string} column
             */
            this._initMonitoredPropertyCell = function(td, entity, column) {

                /**
                 * @param {string} propertyName
                 * @param {string} entityType       an l7.EntityTreeTable.ENTITY enum value
                 * @return {boolean}
                 */
                function isMonitoringPropertyApplicableToEntity(propertyName, entityType) {
                    if (entityType == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                        return l7.Util.hasPropertyValue(l7.EntityTreeTable.SSG_CLUSTER_MONITORING_PROPERTY, propertyName);
                    } else if (entityType == l7.EntityTreeTable.ENTITY.SSG_NODE) {
                        return l7.Util.hasPropertyValue(l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY, propertyName);
                    } else {
                        return false;
                    }
                }

                var propertyName = getMonitoredPropertyNameInColumn(column);
                if (isMonitoringPropertyApplicableToEntity(propertyName, entity.type)) {
                    // We need a DIV element inside the TD element in order to have the
                    // dialog button float in front of the property value.
                    var div = document.createElement('div');
                    div.className = 'monProp'; // CSS style class.
                    div.style.position = 'relative';
                    td.appendChild(div);

                    // The dialog button is a ghost icon - it is initially invisible until mouse over the table cell.
                    var dialogButton = document.createElement('img');
                    dialogButton.className = 'ghostDialogIcon clickableImg';
                    dialogButton.style.display = 'none';
                    dialogButton.src = this._imgFolder + '/ghostDialogIcon.png';
                    dialogButton.title = this._localizedStrings.MONITORED_PROPERTY_BUTTON_TOOLTIP;
                    if (this._config.onClickMonitoringPropertyDialogButton != undefined) {
                        YAHOO.util.Event.addListener(dialogButton, 'click', this._config.onClickMonitoringPropertyDialogButton, { entity : entity, propertyName : propertyName });
                    }
                    YAHOO.util.Event.addListener(td, 'mouseover', function(event, dialogButton) { dialogButton.style.display = ''; }, dialogButton);
                    YAHOO.util.Event.addListener(td, 'mouseout', function(event, dialogButton) { dialogButton.style.display = 'none'; }, dialogButton);
                    div.appendChild(dialogButton);

                    var span = document.createElement('span');
                    div.appendChild(span);

                    if (entity._monitoredPropertySpans == undefined) {
                        entity._monitoredPropertySpans = {};
                    }
                    entity._monitoredPropertySpans[propertyName] = span;
                    if (entity._monitoredPropertyTds == undefined) {
                        entity._monitoredPropertyTds = {};
                    }
                    entity._monitoredPropertyTds[propertyName] = td;

                    if (entity.monitoredProperties != undefined && entity.monitoredProperties[propertyName] != undefined) { // Optional data on initial load.
                        var property = entity.monitoredProperties[propertyName];
                        this.setMonitoredProperty(entity.id, propertyName, property);
                    }
                } else {
                    td.className = 'notApplicable'; // CSS style class.
                }
            }

            /**
             * @private
             */
            this._init = function() {
                this._setLocalizedStrings(this._config.localizedStrings);
                this.setHighlightableEntities(this._config.highlightableEntities);
                this.load(this._entities);
            }

            // --------------------------------------------------------------------------------
            // Public instance methods
            // --------------------------------------------------------------------------------

            /**
             * Returns the configuration object literal passed into the constructor.
             * @public
             * @return {object} the configuration object literal
             */
            this.getConfig = function() {
                return this._config;
            }

            this.expandAll = function() {
                // TODO
            }

            /**
             * Hide all rows except those entities at tree level zero.
             * @public
             */
            this.collapseAll = function() {
                var root;
                var rootCount = 0;
                for (var i = 0; i < this._entities.length; ++i) {
                    var entity = this._entities[i];
                    if (entity._children.length > 0) {
                        var toggler = entity._toggler;
                        toggler.src = l7.Util.getParent(toggler.src) + TOGGLER_COLLAPSED_IMG_NAME;
                    }
                    if (entity._treeLevel > 0) {
                        entity._tr.style.display = 'none';
                    } else {
                        rootCount = rootCount + 1;
                        root = entity;
                    }
                }

                if ( rootCount == 1 ) {
                    toggler = root._toggler;
                    toggler.src = l7.Util.getParent(toggler.src) + TOGGLER_EXPANDED_IMG_NAME;
                    for (i = 0; i < root._children.length; ++i) {
                        entity = root._children[i];
                        entity._tr.style.display = '';
                    }
                }
            }

            /**
             * Changes the name of an entity.
             * @public
             * @param {string} entityId
             * @param {string} name
             * @return {boolean} true if successfully applied; false if unchanged because of error
             */
            this.setEntityName = function(entityId, name) {
                var entity = this._entitiesById[entityId];
                var span = entity._nameSpan;
                if (span != undefined) {
                    span.innerHTML = name;
                    return true;
                }
                return false;
            }

            /**
             * @public
             * @return {array} the current entity list; as an array of entity object literals
             */
            this.getEntities = function() {
                return this._entities;
            }

            /**
             * @public
             * @param {string} id   entity ID
             * @return {object} the entity object literal; undefined if not found
             */
            this.getEntityById = function(entityId) {
                return this._entitiesById[entityId];
            }

            /**
             * @public
             * @param {object} entity
             * @return {object} the parent entity object literal
             */
            this.getParentEntity = function(entity) {
                return l7.Util.findFirstArrayElementByProperty(this._entities, 'id', entity.parentId);
            }

            /**
             * Helper function for context menu to find the entity associated with a context menu target.
             * @public
             * @param {HTMLElement} target      object returned by YAHOO.widget.ContextMenu.contextEventTarget
             * @return {object} object literal of the entity found
             */
            this.findEntityFromContextEventTarget = function(target) {
                var tr = target.nodeName.toLowerCase() == 'tr' ? target : YAHOO.util.Dom.getAncestorByTagName(target, 'tr');
                return tr._entity;
            }

            /**
             * Specifies which entity types can be highlighted by clicking.
             * @public
             * @param {array} types
             */
            this.setHighlightableEntities = function(types) {
                if (types != undefined && types != null && types.length != undefined && types.length > 0) {
                    this._config.highlightableEntities = types;
                    YAHOO.util.Event.addListener(this._tbody, 'click', onClickTBody, null, this);
                } else {
                    YAHOO.util.Event.removeListener('click', onClickTBody);
                }
            }

            /**
             * Selects an entity with highlighting, and invoke any callback method defined in
             * the constructor config parameter.
             * @public
             * @param {string|object} arg   an entity ID or an entity object literal
             * @param {boolean} doCallback  true to invoke the onEntityHighlighted method passed as the config parameter in the constructor
             */
            this.highlightEntity = function(arg, doCallback) {
                var entity;
                if (typeof arg == 'string') {
                    entity = this._entitiesById[arg];
                } else {
                    entity = arg;
                }

                var previous = this._highlightedEntity;

                // Clear highlighting of previous selected TR, if any.
                if (previous != null) l7.Util.removeClass(previous._tr, HIGHLIGHTED_BACKGROUND_CSS);

                // Highlight the new selected TR.
                if (entity != null) l7.Util.addClass(entity._tr, HIGHLIGHTED_BACKGROUND_CSS);

                this._highlightedEntity = entity;
                if (doCallback && this._config.onEntityHighlighted != null) this._config.onEntityHighlighted(previous, entity);
            }

            /**
             * @public
             * @return {object} object literal of the entity highlighted
             */
            this.getHighlightedEntity = function() {
                return this._highlightedEntity;
            }

            /**
             * Sets/Changes the entity types that should have tri-state multi-selection checkbox.
             * @public
             * @param {array} entityTypes   array of l7.EntityTreeTable.ENTITY values
             */
            this.setEntitiesWithTristateCheckbox = function(entityTypes) {
                this._config.entitiesWithTristateCheckbox = entityTypes;
                this.load(this._entities);
            }

            /**
             * Sets/Changes the entity types that should have radio button.
             * @public
             * @param {array} entityTypes   array of l7.EntityTreeTable.ENTITY values
             */
            this.setEntitiesWithRadioButton = function(entityTypes) {
                this._config.entitiesWithRadioButton = entityTypes;
                this.load(this._entities);
            }

            /**
             * @public
             * @return {array} array of entity IDs whose dashboard checkbox is selected; may be empty but never null
             */
            this.getEntitiesWithDashboardSelected = function() {
                var result = [];
                for (var i in this._entities) {
                    var entity = this._entities[i];
                    if (entity._dashboardCheckbox && entity._dashboardCheckbox.checked) {
                        result.push(entity.id);
                    }
                }
                return result;
            }

            /**
             * Sets the state of the dashboard checkbox of one SSG Cluster entity.
             * This will not trigger the onClickDashboardCheckbox callback.
             *
             * @public
             * @param {string} id           the entity ID
             * @param {boolean} checked     true if checkbox should be checked; false otherwise
             */
            this.setDashboardCheckBoxState = function(id, checked) {
                var entity = this._entitiesById[id];
                if (entity && entity._dashboardCheckbox) {
                    entity._dashboardCheckbox.checked = checked;
                }
            }

            /**
             * @public
             * @param {array} types     array of entity types (l7.EntityTreeTable.ENTITY values) to filter; null for no filter
             * @return {array} array of entity object literals whose checkbox is selected; may be empty but never null
             */
            this.getEntitiesWithTristateCheckboxSelected = function(types) {
                var result = [];
                for (var i in this._entities) {
                    var entity = this._entities[i];
                    if (entity._tristateCheckbox && entity._tristateCheckbox.checked) {
                        if (types == undefined ||
                            types == null ||
                            l7.Util.arrayContains(types, entity.type)) {
                            result.push(entity);
                        }
                    }
                }
                return result;
            }

            /**
             * @public
             * @param {string} entityId     entity ID
             * @return {boolean} true if selected
             */
            this.isEntityCheckboxSelected = function(entityId) {
                var entity = this._entitiesById[entityId];
                return entity && entity._tristateCheckbox && entity._tristateCheckbox.checked;
            }

            /**
             * @public
             * @return {object} entity object literal whose radio button is selected; null if none selected
             */
            this.getEntityWithRadioButtonSelected = function() {
                for (var i in this._entities) {
                    var entity = this._entities[i];
                    if (entity._radioButton && entity._radioButton.checked) {
                        return entity;
                    }
                }
                return null;
            }

            /**
             * @public
             * @param {string} entityId     entity ID
             * @return {boolean} true if selected; false if not selected or entity does not have radio button or no entity with given ID
             */
            this.isEntityRadioButtonSelected = function(entityId) {
                var entity = this._entitiesById[entityId];
                return entity && entity._radioButton && entity._radioButton.checked;
            }

            /**
             * Changes the online status of an entity.
             * @public
             * @param {string} entityId     ID of an entity
             * @param {strung} state        an SSG_CLUSTER_ONLINE_STATE value for an SSG Cluster,
             *                              an SSG_NODE_ONLINE_STATE value for an SSG Node
             * @return {boolean} true if successfully applied; false if unchanged because of error
             */
            this.setEntityOnlineStatus = function(entityId, state) {
                var entity = this._entitiesById[entityId];
                var icon = entity._onlineStatusIcon;
                if (state == undefined || state == null) {
                    icon.src = this._imgFolder + '/spacer.png';
                    icon.title = null;
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                    if (state == l7.EntityTreeTable.SSG_CLUSTER_ONLINE_STATE.UP) {
                        icon.src = this._imgFolder + '/SSGClusterStateUp.png';
                        icon.title = this._localizedStrings.SSG_CLUSTER_STATE_UP;
                    } else if (state == l7.EntityTreeTable.SSG_CLUSTER_ONLINE_STATE.PARTIAL) {
                        icon.src = this._imgFolder + '/SSGClusterStatePartial.png';
                        icon.title = this._localizedStrings.SSG_CLUSTER_STATE_PARTIAL;
                    } else if (state == l7.EntityTreeTable.SSG_CLUSTER_ONLINE_STATE.DOWN) {
                        icon.src = this._imgFolder + '/SSGClusterStateDown.png';
                        icon.title = this._localizedStrings.SSG_CLUSTER_STATE_DOWN;
                    } else {
                        YAHOO.log('Unrecognized online status for entity type=\"' + entity.type + '\": ' + state, 'warning', 'l7.EntityTreeTable.setEntityOnlineStatus');
                        return false;
                    }
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_NODE) {
                    if (state == l7.EntityTreeTable.SSG_NODE_ONLINE_STATE.ON) {
                        icon.src = this._imgFolder + '/SSGNodeStateOn.png';
                        icon.title = this._localizedStrings.SSG_NODE_STATE_ON;
                    } else if (state == l7.EntityTreeTable.SSG_NODE_ONLINE_STATE.OFF) {
                        icon.src = this._imgFolder + '/SSGNodeStateOff.png';
                        icon.title = this._localizedStrings.SSG_NODE_STATE_OFF;
                    } else if (state == l7.EntityTreeTable.SSG_NODE_ONLINE_STATE.DOWN) {
                        icon.src = this._imgFolder + '/SSGNodeStateDown.png';
                        icon.title = this._localizedStrings.SSG_NODE_STATE_DOWN;
                    } else if (state == l7.EntityTreeTable.SSG_NODE_ONLINE_STATE.OFFLINE) {
                        icon.src = this._imgFolder + '/SSGNodeStateOffline.png';
                        icon.title = this._localizedStrings.SSG_NODE_STATE_OFFLINE;
                    } else {
                        YAHOO.log('Unrecognized online status for entity type \"' + entity.type + '\": ' + state, 'warning', 'l7.EntityTreeTable.setEntityOnlineStatus');
                        return false;
                    }
                } else {
                    // No online status for other entity types.
                    return false;
                }
                return true;
            };

            /**
             * Sets the trust status of an entity.
             * @public
             * @param {string} entityId     ID of an entity
             * @param {boolean} trusted     true if trust has been established
             * @return {boolean} true if successfully applied; false if error, e.g., entity with given ID not found
             */
            this.setEntityTrustStatus = function(entityId, trusted) {
                var entity = this._entitiesById[entityId];
                var icon = entity._trustStatusIcon;
                if (entity.type == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                    if (trusted == undefined || trusted == null) {
                        icon.src = this._imgFolder + '/spacer.png';
                        icon.title = null;
                    } else if (trusted) {
                        icon.src = this._imgFolder + '/trust.png';
                        icon.title = this._localizedStrings.TRUST_ESTABLISHED;
                    } else {
                        icon.src = this._imgFolder + '/noTrust.png';
                        icon.title = this._localizedStrings.TRUST_NOT_ESTABLISHED;
                    }
                    if (!trusted && this._config.onClickTrustDialogButton != undefined) {
                        icon.className = 'clickableImg';
                        icon.title = this._localizedStrings.TRUST_TO_BE_ESTABLISHED;
                        YAHOO.util.Event.addListener(icon, 'click', this._config.onClickTrustDialogButton, entity);
                    }
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_NODE) {
                    if (trusted == undefined || trusted == null) {
                        icon.src = this._imgFolder + '/spacer.png';
                        icon.title = null;
                    } else if (trusted) {
                        icon.src = this._imgFolder + '/trust.png';
                        icon.title = this._localizedStrings.TRUST_ESTABLISHED;
                    } else {
                        icon.src = this._imgFolder + '/noTrust.png';
                        icon.title = this._localizedStrings.TRUST_NOT_ESTABLISHED;
                    }
                } else {
                    // No trust status for other entity types.
                    icon.src = this._imgFolder + '/spacer.png';
                    icon.title = null;
                    return false;
                }
                return true;
            };

            /**
             * Sets the access status of an entity.
             * @public
             * @param {string} entityId     ID of an entity
             * @param {boolean} trusted     true if trust has been established
             * @param {boolean} state       true if access account has been configured for an SSG Cluster,
             *                              true if access role has been granted for an SSG Node
             * @return {boolean} true if successfully applied; false if unchanged because of error
             */
            this.setEntityAccessStatus = function(entityId, trusted, state) {
                var entity = this._entitiesById[entityId];
                var icon = entity._accessStatusIcon;
                if (entity.type == l7.EntityTreeTable.ENTITY.SSG_CLUSTER) {
                    if (state == undefined || state == null) {
                        icon.src = this._imgFolder + '/spacer.png';
                        icon.title = null;
                    } else if (state) {
                        icon.src = this._imgFolder + '/accessAccount.png';
                        if (trusted) {
                            icon.title = this._localizedStrings.ACCESS_ACCOUNT_IS_SET;
                        } else {
                            icon.title = this._localizedStrings.ACCESS_ACCOUNT_CANNOT_CHANGE;
                        }
                    } else {
                        icon.src = this._imgFolder + '/noAccessAccount.png';
                        if (trusted) {
                            icon.title = this._localizedStrings.ACCESS_ACCOUNT_CAN_SET;
                        } else {
                            icon.title = this._localizedStrings.ACCESS_ACCOUNT_CANNOT_SET;
                        }
                    }
                    if (trusted && this._config.onClickAccessDialogButton != undefined) {
                        icon.className = 'clickableImg';
                        YAHOO.util.Event.addListener(icon, 'click', this._config.onClickAccessDialogButton, entity);
                    }
                } else if (entity.type == l7.EntityTreeTable.ENTITY.SSG_NODE) {
                    if (state == undefined || state == null) {
                        icon.src = this._imgFolder + '/spacer.png';
                        icon.title = null;
                    } else if (state) {
                        icon.src = this._imgFolder + '/accessGranted.png';
                        icon.title = this._localizedStrings.ACCESS_GRANTED;
                    } else {
                        icon.src = this._imgFolder + '/accessNotGranted.png';
                        icon.title = this._localizedStrings.ACCESS_NOT_GRANTED;
                    }
                } else {
                    // No access status for other entity types.
                    icon.src = this._imgFolder + '/spacer.png';
                    icon.title = null;
                    return false;
                }
                return true;
            };

            /**
             * Changes the value of a monitored property of an entity.
             * @public
             * @param {string} entityId         ID of an entity
             * @param {string} propertyName     name of monitored property;
             *                                  an l7.EntityTreeTable.SSG_CLUSTER_MONITORING_PROPERTY or
             *                                  l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY enum value;
             *                                  must be applicable to the entity's type
             * @param {object} property         the property object literal
             * @return {boolean} true if successfully applied; false if unchanged because of error
             */
            this.setMonitoredProperty = function(entityId, propertyName, property) {
                var entity = this._entitiesById[entityId];
                var td = entity._monitoredPropertyTds[propertyName];
                var span = entity._monitoredPropertySpans[propertyName];
                if (!td || !span) {
                    YAHOO.log('Unable to set value of monitored property \"' + propertyName + '\" for entity \"' + entity.name + '\": ' + (td ? 'span' : 'td') + ' element not found', 'warning', 'l7.EntityTreeTable.setMonitoredProperty');
                    return false;
                }
                if (property.monitored) {
                    if (property.value != undefined && property.value != null) {
                        span.innerHTML = property.value;
                    } else {
                        span.innerHTML = '<img src="' + this._imgFolder + '/busy16.gif' + '"/>';
                    }
                    if (property.critical) {
                        td.className = 'right critical'; // CSS style class.
                    } else {
                        td.className = 'right'; // CSS style class.
                    }
                } else {
                    td.className = 'right notMonitored'; // CSS style class.
                    span.innerHTML = '&nbsp;'; // Needed to keep dialog icon vertically centered.
                }
                return true;
            }

            /**
             * Remove all data and table body rows.
             * @public
             */
            this.clear = function() {
                while (this._tbody.rows.length > 0) {
                    this._tbody.deleteRow(-1);
                }

                this._entities = [];
                this._entitiesById = {};
                this._highlightedEntity = null;
            }

            /**
             * Re-initializes the table body rows using the given entity data.
             *
             * @public
             * @param {array} entities     array of entity object literals
             */
            this.load = function(entities) {
                // Clears existing data and table body rows.
                this.clear();
                this._entities = entities;

                var i, j, entity;

                for (i in entities) {
                    entity = entities[i];
                    this._entitiesById[entity.id] = entity;
                }

                // Pre-computes some values for each entity and stashes them back into the entity itself.
                for (i in entities) {
                    entity = entities[i];
                    // Children.
                    entity._children = l7.Util.findArrayElementsByProperty(entities, 'parentId', entity.id);

                    // Tree level (root is 0).
                    var parent = l7.Util.findFirstArrayElementByProperty(entities, 'id', entity.parentId);
                    if (parent == null) {
                        entity._treeLevel = 0;
                    } else {
                        entity._treeLevel = parent._treeLevel + 1;
                    }
                }

                // Creates one table body row per entity.
                var tr, td;
                for (i in entities) {
                    entity = entities[i];

                    tr = this._tbody.insertRow(-1);
                    tr._entity = entity;
                    entity._tr = tr;
                    entity._tds = {};

                    if (this._config.columns != undefined) {
                        for (j in this._config.columns) {
                            var column = this._config.columns[j];
                            td = tr.insertCell(-1);
                            entity._tds[column] = td;

                            if (column == l7.EntityTreeTable.COLUMN.NAME) {
                                this._initNameCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.ONLINE_STATUS) {
                                this._initOnlineStatusCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.TRUST_STATUS) {
                                this._initTrustStatusCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.ACCESS_STATUS) {
                                this._initAccessStatusCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.DASHBOARD) {
                                this._initDashboardCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.TYPE) {
                                this._initTypeCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.VERSION) {
                                this._initVersionCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.DETAILS) {
                                this._initDetailsCell(td, entity);
                            } else if (column == l7.EntityTreeTable.COLUMN.ZOOM) {
                                this._initZoomCell(td, entity);
                            } else if (isMonitoringColumn(column)) {
                                this._initMonitoredPropertyCell(td, entity, column);
                            } else {
                                YAHOO.log('Cannot populate cell because column is unrecognized: ' + column, 'warning', 'l7.EntityTreeTable.load()');
                            }
                        }
                    }
                }
            }

            /**
             * Displays the data error message (from configuration property localizedStrings.ENTITY_DATA_ERROR).
             * Caller is responsible for calling clear() beforehand.
             * @public
             */
            this.displayDataError = function(message) {
                var tr = this._tbody.insertRow(-1);
                var td = tr.insertCell(-1);
                td.colSpan = this._config.columns.length;
                if (message) {
                    td.innerHTML = message;
                } else {
                    td.innerHTML = this._localizedStrings.ENTITY_DATA_ERROR;
                }
            }

            // --------------------------------------------------------------------------------
            // Constructor statements
            // --------------------------------------------------------------------------------

            this._init();
        }

        // --------------------------------------------------------------------------------
        // Public constants
        // --------------------------------------------------------------------------------

        /**
         * Enum of monitoring property names for SSG Cluster entities.
         * @public
         * @final
         */
        l7.EntityTreeTable.SSG_CLUSTER_MONITORING_PROPERTY = {
            AUDIT_SIZE  : 'auditSize'
        }

        /**
         * Enum of monitoring property names for SSG Node entities.
         * @public
         * @final
         */
        l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY = {
            LOG_SIZE    : 'logSize',
            DISK_USED   : 'diskUsed',
            DISK_FREE   : 'diskFree',
            RAID_STATUS : 'raidStatus',
            CPU_TEMP    : 'cpuTemp',
            CPU_USAGE   : 'cpuUsage',
            CLOCK_DRIFT : 'clockDrift'
        }

        /**
         * Enum of all available columns.
         * @public
         * @final
         */
        l7.EntityTreeTable.COLUMN = {
            NAME                   : 'name',
            ONLINE_STATUS          : 'onlineStatus',
            TRUST_STATUS           : 'trustStatus',
            ACCESS_STATUS          : 'accessStatus',
            TYPE                   : 'type',
            VERSION                : 'version',
            DASHBOARD              : 'dashboard',
            DETAILS                : 'details',
            ZOOM                   : 'zoom',
            MONITORING_AUDIT_SIZE  : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_CLUSTER_MONITORING_PROPERTY.AUDIT_SIZE,
            MONITORING_LOG_SIZE    : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY.LOG_SIZE,
            MONITORING_DISK_USED   : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY.DISK_USED,
            MONITORING_DISK_FREE   : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY.DISK_FREE,
            MONITORING_RAID_STATUS : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY.RAID_STATUS,
            MONITORING_CPU_TEMP    : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY.CPU_TEMP,
            MONITORING_CPU_USAGE   : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY.CPU_USAGE,
            MONITORING_CLOCK_DRIFT : MONITORING_COLUMN_PREFIX + l7.EntityTreeTable.SSG_NODE_MONITORING_PROPERTY.CLOCK_DRIFT
        };

        /**
         * Enum of entity types.
         * @public
         * @final
         */
        l7.EntityTreeTable.ENTITY = {
            ENTERPRISE_FOLDER       : 'enterpriseFolder',
            SSG_CLUSTER             : 'ssgCluster',
            SSG_NODE                : 'ssgNode',
            SERVICE_FOLDER          : 'serviceFolder',
            PUBLISHED_SERVICE       : 'publishedService',
            PUBLISHED_SERVICE_ALIAS : 'publishedServiceAlias',
            OPERATION               : 'operation',
            POLICY_FRAGMENT         : 'policyFragment',
            POLICY_FRAGMENT_ALIAS   : 'policyFragmentAlias'
        };

        /**
         * Enum of SSG Cluster online states.
         * @public
         * @final
         */
        l7.EntityTreeTable.SSG_CLUSTER_ONLINE_STATE = {
            UP      : 'up',
            PARTIAL : 'partial',
            DOWN    : 'down'
        };

        /**
         * Enum of SSG Node online states.
         * @public
         * @final
         */
        l7.EntityTreeTable.SSG_NODE_ONLINE_STATE = {
            ON      : 'on',
            OFF     : 'off',
            DOWN    : 'down',
            OFFLINE : 'offline'
        };

        /**
         * Enum of monitoring property states. Note that the corresponding CSS classes are identical.
         * @public
         * @final
         */
        l7.EntityTreeTable.MONITORING_PROPERTY_STATE = {
            NOT_APPLICABLE : 'notApplicable',
            NOT_MONITORED  : 'notMonitored',
            CRITICAL       : 'critical'
        };

        /**
         * Default localized strings. To be overridden individually by config.localizedStrings
         * passed into constructor.
         * @public
         * @final
         */
        l7.EntityTreeTable.DEFAULT_LOCALIZED_STRINGS = {
            ENTITY_DATA_ERROR : 'Data error.',
            SSG_CLUSTER_STATE_UP : 'Up',
            SSG_CLUSTER_STATE_PARTIAL : 'Partial',
            SSG_CLUSTER_STATE_DOWN : 'Down',
            SSG_NODE_STATE_ON : 'On',
            SSG_NODE_STATE_OFF : 'Off',
            SSG_NODE_STATE_DOWN : 'Down',
            SSG_NODE_STATE_OFFLINE : 'Offline',
            TRUST_ESTABLISHED : 'Trust has been established',
            TRUST_NOT_ESTABLISHED : 'Trust has not been established',
            TRUST_TO_BE_ESTABLISHED : 'Trust has not been established. Click to establish.',
            ACCESS_ACCOUNT_IS_SET : 'Access account is set. Click to change access account.',
            ACCESS_ACCOUNT_CAN_SET : 'No access account. Click to enter access account.',
            ACCESS_ACCOUNT_CANNOT_CHANGE : 'Access account is set. Trust must be established before changing access account.',
            ACCESS_ACCOUNT_CANNOT_SET : 'No access account. Trust must be established before entering access account.',
            ACCESS_GRANTED : 'Access granted',
            ACCESS_NOT_GRANTED : 'Access not granted',
            DASHBOARD_CHECKBOX_TOOLTIP : 'Show/Hide dashboard for this SSG Cluster',
            MONITORED_PROPERTY_BUTTON_TOOLTIP : 'Click to configure this monitoring property',
            ENTERPRISE_FOLDER : 'folder',
            SSG_CLUSTER : 'SSG Cluster',
            SSG_NODE : 'SSG Node',
            SERVICE_FOLDER : 'folder',
            PUBLISHED_SERVICE : 'published service',
            PUBLISHED_SERVICE_ALIAS : 'published service alias',
            OPERATION : 'operation',
            POLICY_FRAGMENT : 'policy fragment',
            POLICY_FRAGMENT_ALIAS : 'policy fragment alias',
            SSL_HOST_NAME : 'SSL host name',
            ADMINISTRATIVE_PORT_NUMBER : 'administrative port number',
            IP_ADDRESS : 'IP address',
            DATABASE_HOSTS : 'database host(s)',
            SELF_HOST_NAME : 'self host name',
            ZOOM_ICON_TOOLTIP : 'Show details'
        };

    })();
}
