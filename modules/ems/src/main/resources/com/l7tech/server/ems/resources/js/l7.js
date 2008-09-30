/**
 * @module l7
 * @namespace l7
 * @requires YUI module "animation" if using l7.Resize
 * @requires YUI modules "container", "button" if using l7.Error
 */

// -----------------------------------------------------------------------------
// Creates the l7 global namespace object, if not already created.
// -----------------------------------------------------------------------------
if (typeof l7 == "undefined" || !l7) {
    /**
     * The l7 global namespace object.
     *
     * @class l7
     * @static
     */
    var l7 = {};
}

// -----------------------------------------------------------------------------
// Utilities
// -----------------------------------------------------------------------------
if (!l7.Util) {
    (function(){
        /**
         * Provides various utility functions.
         *
         * @class l7.Util
         */
        l7.Util = {};

        /**
         * Searches for elements with HTML ID matching a given pattern.
         *
         * @static
         * @param {string} idPattern            regexp pattern of IDs
         * @param {HTMLElement} startElement    element to start searching; null for document body
         * @param {string} tagName              name of tags to restrict search; null for all tags
         * @return {array} an array of elements found; may be empty but never null
         */
        l7.Util.getElementsById = function(idPattern, startElement, tagName) {
            var result = new Array();

            if (startElement == null) {
                startElement = document.body;
            }
            if (tagName == null) {
                tagName = '*';
            }
            tagName = tagName.toLowerCase();

            var elements = startElement.getElementsByTagName(tagName);

            if (idPattern.substring(0, 1) != '^') idPattern = '^' + idPattern;
            if (idPattern.substring(idPattern.length - 1, idPattern.length) != '$') idPattern = idPattern + '$';
            var regexp = new RegExp(idPattern);
            for(var i = 0, j = 0; i < elements.length; ++i) {
                if (regexp.test(elements[i].id)) {
                    result[j] = elements[i];
                    ++j;
                }
            }

            return result;
        };

        /**
         * Searches for elements with a matching class name. This class name may
         * be one of multiple class names assigned to an element.
         *
         * @namespace l7.Util
         * @static
         * @param {string} className            class name
         * @param {HTMLElement} startElement    element to start searching; null for document body
         * @param {string} tagName              name of tags to restrict search; null for all tags
         * @return {array} an array of elements found; may be empty but never null
         */
        l7.Util.getElementsByClassName = function(className, startElement, tagName) {
            var result = new Array();

            if (startElement == null) {
                startElement = document.body;
            }
            if (tagName == null) {
                tagName = '*';
            }
            tagName = tagName.toLowerCase();

            var elements = startElement.getElementsByTagName(tagName);

            var regexp = new RegExp('(^|\\s)' + className + '(\\s|$)');
            for(var i = 0, j = 0; i < elements.length; i++) {
                if (regexp.test(elements[i].className)) {
                    result[j] = elements[i];
                    ++j;
                }
            }

            return result;
        };

        /**
         * Returns the parent folder of a given path, include the trailing slash.
         *
         * @static
         * @param {string} path      a path
         * @return {string}
         */
        l7.Util.getParent = function(path) {
            return path.substring(0, path.lastIndexOf('/') + 1);
        };

        /**
         * Returns the file name portion of a given path, i.e., the last component.
         *
         * @static
         * @param {string} path      a path
         * @return {string}
         */
        l7.Util.getFileName = function(path) {
            return path.substring(path.lastIndexOf('/') + 1);
        };

        /**
         * Sets the source path of an IMG element.
         *
         * @static
         * @param {HTMLElement} img     an IMG element
         * @param {string} src          the source path
         */
        l7.Util.setImage = function(img, src) {
            img.src = src;
        };

        /**
         * Sets the class of an element. This will become the only class of this element.
         *
         * @static
         * @param {HTMLElement} element     the HTML element
         * @param {string} className        the class name
         */
        l7.Util.setClass = function(element, className) {
            element.className = className;
        };

        /**
         * Adds a class to an element, if not already added.
         *
         * @static
         * @param {HTMLElement} element     the HTML element
         * @param {string} className        the class name
         * @return {boolean} true if added; false if already added
         * @see removeClass
         */
        l7.Util.addClass = function(element, className) {
            var regexp = new RegExp('(^|\\s)' + className + '(\\s|$)');
            var classes = element.className;
            if (!regexp.test(classes)) {
                if (classes.length == 0) {
                    element.className = className;
                } else {
                    element.className += ' ' + className;
                }
                return true;
            } else {
                return false;
            }
        };

        /**
         * Removes a class to an element, if there.
         * Return true if removed.
         *
         * @static
         * @param {HTMLElement} element     the HTML element
         * @param {string} className        the class name
         * @return {boolean} true if removed; false if not there
         * @see addClass
         */
        l7.Util.removeClass = function(element, className) {
            var regexp = new RegExp('(^|\\s+)' + className + '(\\s+|$)', 'g');
            var classes = element.className;
            if (regexp.test(classes)) {
                element.className = classes.replace(regexp, ' ').replace(/^\s+/, '').replace(/\s+$/, '');
                return true;
            } else {
                return false;
            }
        };

        /**
         * Tests if an element is assigned a class.
         *
         * @static
         * @param {HTMLElement} element     the HTML element
         * @param {string} className        the class name
         * @return {boolean} true if element has the given class
         */
        l7.Util.hasClass = function(element, className) {
            var regexp = new RegExp('(^|\\s+)' + className + '(\\s+|$)');
            return regexp.test(element.className);
        };

        /**
         * Finds all the classes of an element that matches a given pattern.
         *
         * @static
         * @param {HTMLElement} element     the HTML element
         * @param {string} pattern          the regular expression pattern; must include ^ and $ if exact match desired
         * @return {array} class names found; may be empty but never null
         */
        l7.Util.findClasses = function(element, pattern) {
            var result = new Array();
            var classes = element.className.split(/\s+/);
            var regexp = new RegExp(pattern);
            for (var i = 0; i < classes.length; ++i) {
                var clazz = classes[i];
                if (regexp.test(clazz)) {
                    result.push(clazz);
                }
            }
            return result;
        }

        /**
         * Finds the first class of an element that matches a given pattern.
         *
         * @static
         * @param {HTMLElement} element     the HTML element
         * @param {string} pattern          the regular expression pattern; must include ^ and $ if exact match desired
         * @return class name if found; null otherwise
         */
        l7.Util.findClass = function(element, pattern) {
            var result = null;
            var classes = element.className.split(/\s+/);
            var regexp = new RegExp(pattern);
            for (var i = 0; i < classes.length; ++i) {
                var clazz = classes[i];
                if (regexp.test(clazz)) {
                    result = clazz;
                    break;
                }
            }
            return result;
        }

        /**
         * Returns a string with all special character properly esacped for use as HTML text.
         *
         * @static
         * @param {string} s         the text to escape
         * @return {string} a string with all special character properly esacped
         */
        l7.Util.escapeAsText = function(s) {
            s.replace('<', '&lt;');
            s.replace('>', '&gt;');
            s.replace('&', '&amp;');
            return s;
        };

        /**
         * Tests if an array contains an object.
         *
         * @static
         * @param {array} array     the array
         * @param {object} obj      the object
         * @return {boolean}
         */
        l7.Util.arrayContains = function(array, obj) {
            for (var i = 0; i < array.length; ++i) {
                if (array[i] == obj) {
                    return true;
                }
            }
            return false;
        };

        /**
         * Removes matching element(s) from an array.
         *
         * @static
         * @param {array} array     the array
         * @param {object} element  the element to match using ==
         */
        l7.Util.removeArrayElement = function(array, element) {
            for (var i = array.length - 1; i >=0; --i) {
                if (array[i] == element) {
                    array.splice(i, 1);
                }
            }
        };

        /**
         * Returns true if a string (all of it) is the text form of an integer (excluding floating point number).
         *
         * @static
         * @param {string} s    the string to test
         * @return {boolean}
         */
        l7.Util.isIntString = function(s) {
            var n = parseInt(s, 10);
            return s.indexOf('.') == -1 && n - s == 0;
        }

        /**
         * Returns true if a string (all of it) is the text form of a floating point (including integer).
         *
         * @static
         * @param {string} s    the string to test
         * @return {boolean}
         */
        l7.Util.isFloatString = function(s) {
            var x = parseFloat(s);
            return x - s == 0;
        }
    })();
};

// -----------------------------------------------------------------------------
// Tab bar
// -----------------------------------------------------------------------------
if (!l7.TabBar) {
    (function(){
        /**
         * Provides support functions for tab bar.
         *
         * @class l7.TabBar
         * @static
         */
        l7.TabBar = {};

        // Precaches rollover images.
        if (document.images) {
            new Image().src = '../images/tabLTover.png';
            new Image().src = '../images/tabCTover.png';
            new Image().src = '../images/tabRTover.png';
            new Image().src = '../images/tabLMover.png';
            new Image().src = '../images/tabRMover.png';
            new Image().src = '../images/tabLBover.png';
            new Image().src = '../images/tabCBover.png';
            new Image().src = '../images/tabRBover.png';
        }

        // Local hash maps for fast lookup of the 9 table cells that make up a tab.
        // L = Left, C = Center, R = Right
        // T = Top, M = Middle, B = Bottom
        var tabLTcell = [];
        var tabCTcell = [];
        var tabRTcell = [];
        var tabLMcell = [];
        var tabCMcell = [];
        var tabRMcell = [];
        var tabLBcell = [];
        var tabCBcell = [];
        var tabRBcell = [];

        /**
         * Handles mouseover event on the center TD element in an "off" tab.
         */
        function onTabOver() {
            var tabName = this.id.substring(5);
            l7.Util.setImage(tabLTcell[tabName].firstChild, '../images/tabLTover.png');
            l7.Util.setClass(tabCTcell[tabName], 'tabCTover');
            l7.Util.setImage(tabRTcell[tabName].firstChild, '../images/tabRTover.png');
            l7.Util.setClass(tabLMcell[tabName], 'tabLMover');
            l7.Util.setClass(tabCMcell[tabName], 'tabCMover');
            l7.Util.setClass(tabRMcell[tabName], 'tabRMover');
            l7.Util.setImage(tabLBcell[tabName].firstChild, '../images/tabLBover.png');
            l7.Util.setClass(tabCBcell[tabName], 'tabCBover');
            l7.Util.setImage(tabRBcell[tabName].firstChild, '../images/tabRBover.png');
        };

        /**
         * Handles mouseout event on the center TD element in an "off" tab.
         */
        function onTabOut() {
            var tabName = this.id.substring(5);
            l7.Util.setImage(tabLTcell[tabName].firstChild, '../images/tabLT.png');
            l7.Util.setClass(tabCTcell[tabName], 'tabCT');
            l7.Util.setImage(tabRTcell[tabName].firstChild, '../images/tabRT.png');
            l7.Util.setClass(tabLMcell[tabName], 'tabLM');
            l7.Util.setClass(tabCMcell[tabName], 'tabCM');
            l7.Util.setClass(tabRMcell[tabName], 'tabRM');
            l7.Util.setImage(tabLBcell[tabName].firstChild, '../images/tabLBoff.png');
            l7.Util.setClass(tabCBcell[tabName], 'tabCBoff');
            l7.Util.setImage(tabRBcell[tabName].firstChild, '../images/tabRBoff.png');
        };

        /**
         * Initializes an "off" tab.
         *
         * @namespace l7.TabBar
         * @static
         * @param {string} tabName   the predefined tab name
         */
        l7.TabBar.initTab = function(tabName) {
            var id = 'tabCM' + tabName;
            var td = document.getElementById(id);
            td.onmouseover = onTabOver;
            td.onmouseout = onTabOut;

            // Populates hash map for fast lookup of the 9 table cells that make up a tab.
            tabLTcell[tabName] = document.getElementById('tabLT' + tabName);
            tabCTcell[tabName] = document.getElementById('tabCT' + tabName);
            tabRTcell[tabName] = document.getElementById('tabRT' + tabName);
            tabLMcell[tabName] = document.getElementById('tabLM' + tabName);
            tabCMcell[tabName] = document.getElementById('tabCM' + tabName);
            tabRMcell[tabName] = document.getElementById('tabRM' + tabName);
            tabLBcell[tabName] = document.getElementById('tabLB' + tabName);
            tabCBcell[tabName] = document.getElementById('tabCB' + tabName);
            tabRBcell[tabName] = document.getElementById('tabRB' + tabName);
        };
    })();
}

// -----------------------------------------------------------------------------
// Tippy
// -----------------------------------------------------------------------------
if (!l7.Tippy) {
    (function(){
        /**
         * Provides support functions for expandable/collipsible tippies.
         *
         * @class l7.Tippy
         * @static
         */
        l7.Tippy = {};

        /** Base name of image file representing a tippy in expanded state. */
        var EXPANDED_TIPPY_IMG_NAME = 'tippyExpanded.png';

        /** Base name of image file representing a tippy in collapsed state. */
        var COLLAPSED_TIPPY_IMG_NAME = 'tippyCollapsed.png';

        /**
         * Tests if the given IMG element is the expanded tippy image.
         *
         * @static
         * @param {object} img   an IMG element
         * @return {boolean} true if the given IMG element is the expanded tippy image
         */
        l7.Tippy.isTippyExpanded = function(img) {
            return l7.Util.getFileName(img.src) == EXPANDED_TIPPY_IMG_NAME;
        };

        /**
         * Tests if the given IMG element is the collapsed tippy image.
         *
         * @static
         * @param {object} img   an IMG element
         * @return {boolean} true if the given IMG element is the collapsed tippy image
         */
        l7.Tippy.isTippyCollapsed = function(img) {
            return l7.Util.getFileName(img.src) == COLLAPSED_TIPPY_IMG_NAME;
        };

        /**
         * Toggles the state of a tippy, and expand/collapse the target element correspondingly.
         *
         * @static
         * @param {string, object}      HTML ID of tippy IMG element, or a tippy IMG element
         * @param {string} targetId     HTML ID of target element to expand/collapse
         */
        l7.Tippy.toggleTippy = function(tippy, targetId) {
            if (typeof tippy == 'string') {
                tippy = document.getElementById(tippy);
            }
            var target = document.getElementById(targetId);
            if (l7.Tippy.isTippyExpanded(tippy)) {
                target.style.display = 'none';
                tippy.src = l7.Util.getParent(tippy.src) + COLLAPSED_TIPPY_IMG_NAME;
            } else {
                target.style.display = '';
                tippy.src = l7.Util.getParent(tippy.src) + EXPANDED_TIPPY_IMG_NAME;
            }
        };

        /**
         * Expands all tippies (i.e., IMG elements) with HTML ID matching a given pattern.
         *
         * @static
         * @param {string} idPattern     regexp pattern of tippies (i.e., IMG elements) IDs
         */
        l7.Tippy.expandTippies = function(idPattern) {
            var tippies = l7.Util.getElementsById(idPattern);
            for (var i in tippies) {
                if (l7.Util.getFileName(tippies[i].src) == COLLAPSED_TIPPY_IMG_NAME) {
                    tippies[i].onclick();
                }
            }
        };

        /**
         * Collapses all tippies (i.e., IMG elements) with HTML ID matching a given pattern.
         *
         * @static
         * @param {string} idPattern     regexp patter of tippies (i.e., IMG elements) IDs
         */
        l7.Tippy.collapseTippies = function(idPattern) {
            var tippies = l7.Util.getElementsById(idPattern);
            for (var i in tippies) {
                if (l7.Util.getFileName(tippies[i].src) == EXPANDED_TIPPY_IMG_NAME) {
                    tippies[i].onclick();
                }
            }
        };
    })();
}

// -----------------------------------------------------------------------------
// Resize
// -----------------------------------------------------------------------------
if (!l7.Resize) {
    (function(){
        /**
         * Provides support functions for resizing elements.
         *
         * @class l7.Resize
         * @static
         */
        l7.Resize = {};

        /**
         * Changes the height of elements.
         *
         * @static
         * @param {array} ids     array of element IDs
         * @param {int} delta     height increment in pixels (positive to make taller, negative to make shorter)
         * @param {int} min       minimum element height
         * @param {int} max       maximum element height
         * @requires YAHOO.util.Easing
         */
        l7.Resize.changeHeight = function(ids, delta, min, max) {
            for (var i in ids) {
                var element = document.getElementById(ids[i]);
                var h = element.offsetHeight + delta;
                if (h < min) h = min;
                if (h > max) h = max;
                new YAHOO.util.Anim(ids[i], {height: {to: h, unit: 'px'}}, 0.1, YAHOO.util.Easing.easeOut).animate();
            }
        }
    })();
}

// -----------------------------------------------------------------------------
// Error
// -----------------------------------------------------------------------------
if (!l7.Error) {
    (function(){
        /**
         * Provides simple error dialog.
         *
         * @class l7.Error
         * @static
         */
        l7.Error = {};

        /**
         * Displays a simple error dialog.
         *
         * @param {string} header   localized header text; defaults to 'Error' if null
         * @param {string} width    width; defaults to '30em' if null
         * @param {html} body       HTML content
         * @param {string} okText   localized text label for the OK button
         * @requires YAHOO.widget.SimpleDialog
         * @requires YAHOO.widget.Button
         */
        l7.Error.showDialog = function(header, width, body, okText) {
            var errDlg = new YAHOO.widget.SimpleDialog('errDlg', {
                buttons     : [
                    {
                        text      : okText == null ? 'OK' : okText,
                        handler   : function() { this.hide() },
                        isDefault : true
                    }
                ],
                close       : true,
                draggable   : false,
                fixedcenter : true,
                icon        : YAHOO.widget.SimpleDialog.ICON_WARN,
                modal       : true,
                width       : width == null ? '30em' : width,
                visible     : false
            });
            errDlg.setHeader(header == null ? 'Error' : header);
            errDlg.setBody(body);
            errDlg.render(document.body);
            errDlg.show();
        }
    })();
}
