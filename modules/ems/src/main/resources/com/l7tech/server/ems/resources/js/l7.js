/**
 * @module l7
 * @namespace l7
 * @requires YUI module "animation" if using l7.Resize
 * @requires YUI modules "button", "container" if using l7.Dialog
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
         * Creates a set from an array. The resulting set can be used to determine
         * existence of a value in the orginal array by testing for set[x] != undefined.
         * @param {array} array     array of strings
         * @return {object} the resulting set
         */
        l7.Util.arrayToSet = function(array) {
            var result = {};
            for (var i in array) {
                result[array[i]] = array[i];
            }
            return result;
        }

        /**
         * Creates a map from an array, using a property of each element as its key.
         * The resulting map can be used to extract elements efficiently by its key, e.g.,
         * result[keyValue].
         * @param {array} array     array of strings
         * @param {string} key      name of the property in each element to use as the key value
         * @return {object} the resulting map
         */
        l7.Util.arrayToMap = function(array, keyName) {
            var result = {};
            var keyValue;
            for (var i in array) {
                keyValue = array[i][keyName];
                if (keyValue != undefined) {
                    result[keyValue] = array[i];
                }
            }
        }

        /**
         * Tests if an array contains an object. For repeated usage on the same array,
         * it it better to create a set using l7.Util.arrayToSet(array).
         *
         * @static
         * @param {array} array     the array to test
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
         * Tests if an object has a property with the specific value.
         * @param {object} obj
         * @param {any} value
         * @return {boolean}
         */
        l7.Util.hasPropertyValue = function(obj, value) {
            for (var i in obj) {
                if (obj[i] == value) {
                    return true;
                }
            }
            return false;
        }

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
         * Finds all elments in a given array which has a given property value.
         * @param {array} array         the array to search
         * @param {string} propName     the property name to look up
         * @param {any} propValue       the property value to match
         * @return {array} array of all element objects found
         */
        l7.Util.findArrayElementsByProperty = function(array, propName, propValue) {
            var result = [];
            for (var i in array) {
                if (array[i][propName] == propValue) {
                    result.push(array[i]);
                }
            }
            return result;
        }

        /**
         * Finds the first elments in a given array which has a given property value.
         * @param {array} array         the array to search
         * @param {string} propName     the property name to look up
         * @param {any} propValue       the property value to match
         * @return {object} the element object found; null if no match
         */
        l7.Util.findFirstArrayElementByProperty = function(array, propName, propValue) {
            for (var i in array) {
                if (array[i][propName] == propValue) {
                    return array[i];
                }
            }
            return null;
        }

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

    /**
     * Tests if an object literal is an Layer 7 exception object
     *
     * @param {object} o    the object literal
     * @return {boolean} true if it is an Layer 7 exception object
     */
    l7.Util.isException = function(o) {
        return o != null && o.exception != undefined && o.exception != null;
    }
};

// -----------------------------------------------------------------------------
// Widget
// -----------------------------------------------------------------------------
if (!l7.Widget) {
    (function(){
        l7.Widget = {};

        /**
         * Creates a input radio button element; accounting for browser differences.
         * @param name
         * @return {HTMLInputElement} the created radio button
         */
        l7.Widget.createInputRadio = function(name) {
            var result;
            try{
                // Works in IE7. Throws in FF3, Opera 9, Safari 3.
                result = document.createElement('<input type="radio" name="' + name + '" />');
            } catch (err) {
                // Works in FF3, Opera 9, Safari 3. Doesn't work in IE7.
                result = document.createElement('input');
            }
            result.type = 'radio';
            result.name = name;
            return result;
        }
    })();
}

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
// Dialog
// -----------------------------------------------------------------------------
if (!l7.Dialog) {
    (function(){
        /**
         * Provides simple error dialog.
         *
         * @class l7.Dialog
         * @static
         */
        l7.Dialog = {};

        /**
         * Shows the wait dialog.
         *
         * @public
         * @static
         * @param {string} header   text for dialog header
         */
        l7.Dialog.showWaitDialog = function(header) {
            if (l7.Dialog._waitDialog == undefined) {
                /**
                 * @private
                 */
                l7.Dialog._waitDialog = new YAHOO.widget.Panel('l7_Dialog_waitDialog', {
                    close       : false,
                    draggable   : false,
                    fixedcenter : true,
                    modal       : true,
                    visible     : false,
                    zindex      : 999
                });
                l7.Dialog._waitDialog.setBody('<div class="center"><img src="../images/busy32.gif" /></div>');
            }
            l7.Dialog._waitDialog.setHeader(header);
            l7.Dialog._waitDialog.render(document.body);
            l7.Dialog._waitDialog.show();
        }

        /**
         * Hides the wait dialog.
         *
         * @public
         * @static
         */
        l7.Dialog.hideWaitDialog = function() {
            l7.Dialog._waitDialog.hide();
        }

        /**
         * Displays a simple error dialog.
         *
         * @param {string} header   localized header text; defaults to 'Error' if null
         * @param {html} body       HTML content
         * @param {string} okText   localized text label for the OK button; defaults to 'OK' if null
         * @requires YAHOO.widget.Dialog
         * @requires YAHOO.widget.Button
         */
        l7.Dialog.showErrorDialog = function(header, body, okText) {
            if (l7.Dialog._errorDialog == undefined) {
                /**
                 * @private
                 */
                l7.Dialog._errorDialog = new YAHOO.widget.Dialog('l7_Dialog_errorDialog', {
                    buttons     : [
                        {
                            text      : 'OK',
                            handler   : function() { this.hide(); },
                            isDefault : true
                        }
                    ],
                    close       : true,
                    draggable   : false,
                    fixedcenter : true,
                    icon        : YAHOO.widget.SimpleDialog.ICON_WARN,
                    modal       : true,
                    visible     : false,
                    zindex      : 999
                });
            }
            l7.Dialog._errorDialog.setHeader(header == null ? 'Error' : header);
            l7.Dialog._errorDialog.setBody(body);
            l7.Dialog._errorDialog.render(document.body);
            l7.Dialog._errorDialog.getButtons()[0].set('label', okText == null ? 'OK' : okText);
            l7.Dialog._errorDialog.show();
        }

        /**
         * Displays the given l7-style exception object literal in a simple error dialog.
         *
         * @public
         * @param {object} ex       an l7-style exception object literal
         * @param {string} header   localized header text; defaults to 'Error' if null
         * @param {html} beginBody  beginning HTML content
         * @param {string} okText   localized text label for the OK button; defaults to 'OK' if null
         */
        l7.Dialog.showExceptionDialog = function(exception, header, beginBody, okText) {
            var body = beginBody;
            for (var e = exception; e != null || e != undefined; e = e.cause) {
                if (e !== exception) body += '<div>Caused By:</div>';
                body += '<table class="spaced" style="border: 1px solid #000000; margin: 6px 0 6px 0; width: 50em;">';
                body += '<tr><th>Exception:</th><td>' + l7.Util.escapeAsText(e.exception) + '</td></tr>';
                if (e.message) {
                    body += '<tr><th>Message:</th><td class="wrap">' + l7.Util.escapeAsText(e.message) + '</td></tr>';
                }
                if (e.localizedMessage) {
                    body += '<tr><th>Localized Message:</th><td class="wrap">' + l7.Util.escapeAsText(e.localizedMessage) + '</td></tr>';
                }
                body += '</table>';
            }
            body += '</div>';
            l7.Dialog.showErrorDialog(header, body, okText);
        }

        /**
         * Displays a simple error dialog if the given object is an l7-style exception object.
         *
         * @param {object} o        the object literal to test
         * @param {string} header   localized header text; defaults to 'Error' if null
         * @param {html} beginBody  beginning HTML content
         * @param {string} okText   localized text label for the OK button; defaults to 'OK' if null
         * @return {boolean} true if it was an exception.
         */
        l7.Dialog.showExceptionDialogIfException = function(o, header, beginBody, okText) {
            var result = l7.Util.isException(o);
            if (result) {
                l7.Dialog.showExceptionDialog(o, header, beginBody, okText);
            }
            return result;
        }

        l7.Dialog.showExceptionDialogIfJSONException = function(s, header, beginBodyIfException, bodyIfJSONParseException, okText) {
            if (s.search(/\S/) == -1) {
                return null;
            } else {
                try {
                    var o = YAHOO.lang.JSON.parse(s);
                    var isException = l7.Util.isException(o);
                    if (isException) {
                        l7.Dialog.showExceptionDialog(o, header, beginBodyIfException, okText);
                        return true;
                    } else {
                        return o;
                    }
                    var isException = l7.Dialog.showExceptionDialogIfException(o, header, beginBodyIfException, okText);
                    return o;
                } catch (e) {
                    l7.Dialog.showErrorDialog(header, bodyIfJSONParseException + ': ' + e, okText);
                    return false;
                }
            }
        }
    })();
}
