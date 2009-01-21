/**
 * @module l7
 * @namespace l7
 * @requires l7.css
 * @requires YUI module "container", "event" if using l7.Widget
 * @requires YUI module "animation" if using l7.Resize
 * @requires YUI modules "button", "container", "json" if using l7.Dialog
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
            } else if (typeof startElement == 'string' || startElement instanceof String) {
                startElement = document.getElementById(startElement);
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
         * Searches for an ancestor element with a matching tag name.
         *
         * @param {HTMLElement} startElement    the element whose ancestors are to be searched
         * @param {string} tagName              tag name to search for
         * @return {HTMLElement} the ancestor element found; null if not found
         */
        l7.Util.getAncestorElementByTagName = function(startElement, tagName) {
            var result = null;
            for (var el = startElement.parentNode; el != null; el = el.parentNode) {
                if (el.nodeName.toLowerCase() == tagName.toLowerCase()) {
                    result = el;
                    break;
                }
            }
            return result;
        }

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
         * Returns the property values of passed objects as an array.
         * @param {objects} varargs    any number of objects
         * @return {array} array of property values; may be empty but never null
         */
        l7.Util.getPropertyValues = function(varargs) {
            var result = [];
            for (var i = 0; i < arguments.length; ++i) {
                var argument = arguments[i];
                if (argument instanceof Object) {
                    for (var property in argument) {
                        result.push(argument[property]);
                    }
                }
            }
            return result;
        }

        /**
         * Returns a new object with properties merged from all the passed objects.
         * @param {objects} varargs    any number of objects
         * @return {object}
         */
        l7.Util.mergeProperties = function(varargs) {
            var result = {};
            for (var i = 0; i < arguments.length; ++i) {
                var argument = arguments[i];
                for (var property in argument) {
                    result[property] = argument[property];
                }
            }
            return result;
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
         * @return {array} array of all element objects found; may be empty but never null
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
         * Tests if a string is empty or contains blank characters only.
         * Blank characters include white space, tab, carriage return, linefeed, vertical tab and form feed.
         *
         * @static
         * @param {string} s    the string to test
         * @return {boolean} true if the given string is empty or contains blank characters only
         */
        l7.Util.isBlankOnly = function(s) {
            return s.match(/^\s*$/) != null;
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

        /**
         * Formats an integer into a string with leading zero padded to at least the specified number of digits.
         * E.g., formatInteger(7, 3) results in "007".
         * @static
         * @param {integer} n
         * @param {integer} numDigits
         */
        l7.Util.formatInteger = function(n, numDigits) {
            var result = '' + n;
            for (var i = numDigits - result.length; i > 0; --i) {
                result = '0' + result;
            }
            return result;
        }

        /**
         * Formats a date object into a string.
         * @static
         * @param {Date} date       a JavaScript Date object
         * @param {string} format   "yyyy-MM-dd", "MMM d, yyyy", "MM/dd/yyyy", or "yyyy/MM/dd"
         */
        l7.Util.formatDate = function(date, format) {
            var result = null;
            if (format == 'yyyy-MM-dd') {
                result = date.getFullYear() + '-'
                       + l7.Util.formatInteger(date.getMonth() + 1, 2) + '-'
                       + l7.Util.formatInteger(date.getDate(), 2);
            } else if (format == 'MMM d, yyyy') {
                result = _monthShortNames[date.getMonth()] + ' ' + date.getDate() + ', ' + date.getFullYear();
            } else if (format == 'MM/dd/yyyy') {
                result = l7.Util.formatInteger(date.getMonth() + 1, 2) + '/'
                       + l7.Util.formatInteger(date.getDate(), 2) + '/'
                       + date.getFullYear();
            } else if (format == 'yyyy/MM/dd') {
                result = date.getFullYear() + '/'
                       + l7.Util.formatInteger(date.getMonth() + 1, 2) + '/'
                       + l7.Util.formatInteger(date.getDate(), 2);
            }
            return result;
        }

        /**
         * @private
         * @type array
         */
        var _monthShortNames = [
            'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
        ];

        /**
         * Overrides default month short names for localization purpose.
         * @static
         * @param {array} names     array of strings
         */
        l7.Util.setMonthShortNames = function(names) {
            _monthShortNames = names;
        }

        /**
         * Replace indexed placeholders in a pattern by supplied objects.
         * Placeholders have the form "{i}" where i starts at 0 and matches the
         * position of the passed parameters. For example, "{0}" is replaced
         * by the 2nd parameter.
         * @param {string} pattern      the message format pattern
         * @param {object} varargs      objects for insertion into the pattern
         * @return {object}
         */
        l7.Util.formatMessage = function(pattern /* , varargs ... */) {
            var result = pattern;
            for (var i = 1; i < arguments.length; ++i) {
                var placeholder = '{' + (i - 1) + '}';
                result = result.replace(placeholder, arguments[i]);
            }
            return result;
        }

        /**
         * Tests if an object literal is an l7-style exception object.
         * See http://sarek/mediawiki/index.php?title=Enterprise_Organization_LLD#JSON_format_for_exception.
         *
         * @static
         * @param {object} o    the object literal
         * @return {boolean} true if it is an Layer 7 exception object
         */
        l7.Util.isException = function(o) {
            return o != null && o.exception != undefined && o.exception != null;
        }

        /**
         * Show selected elements by making their "display" style default.
         *
         * @static
         * @param {string} idPattern            regexp pattern of IDs
         * @param {HTMLElement} startElement    element to start searching; null for document body
         * @param {string} tagName              name of tags to restrict search; null for all tags
         */
        l7.Util.showElements = function(idPattern, startElement, tagName) {
            var elements = l7.Util.getElementsById(idPattern, startElement, tagName);
            for (var i in elements) {
                elements[i].style.display = '';
            }
        }

        /**
         * Hide selected elements by making their "display" style "none".
         *
         * @static
         * @param {string} idPattern            regexp pattern of IDs
         * @param {HTMLElement} startElement    element to start searching; null for document body
         * @param {string} tagName              name of tags to restrict search; null for all tags
         */
        l7.Util.hideElements = function(idPattern, startElement, tagName) {
            var elements = l7.Util.getElementsById(idPattern, startElement, tagName);
            for (var i in elements) {
                elements[i].style.display = 'none';
            }
        }
    })();
};

// -----------------------------------------------------------------------------
// Connection
// -----------------------------------------------------------------------------
if (!l7.Connection) {
    (function(){
        l7.Connection = {};

        /**
         * Sends a synchronous request via the XMLHttpRequest object.
         * The instantiated XMLHttpRequest object is returned for caller to
         * check its status and parse its responseText.
         *
         * @static
         * @param {string} method       HTTP method; 'GET' or 'POST'
         * @param {string} uri          resource path (include any parameters if method is 'GET')
         * @param {string} postData     POST body, if method is 'POST'
         * @param {boolean} isJSON      used for setting the content type; true for 'application/json', false for 'application/x-www-form-urlencoded'
         * @return {object} the XMLHttpRequest object; null if fail to instantiate XMLHttpRequest object
         */
        l7.Connection.syncRequest = function(method, uri, postData, isJSON) {
            var MSXML_PROGIDS = [
                'Microsoft.XMLHTTP',
                'MSXML2.XMLHTTP.3.0',
                'MSXML2.XMLHTTP'
            ];

            var xhr;
            try {
                // For non-IE browsers.
                xhr = new XMLHttpRequest();
            } catch (e) {
                for (var i = 0; i < MSXML_PROGIDS.length; ++i) {
                    try {
                        // For IE.
                        xhr = new ActiveXObject(MSXML_PROGIDS[i]);
                        break;
                    } catch (e2) {
                    }
                }
            }

            if (!xhr) {
                return null;
            }

            xhr.open(method, uri, false);

            if (method.toUpperCase() === 'POST') {
                if (isJSON) {
                    xhr.setRequestHeader('Content-Type', 'application/json; charset=UTF-8');
                } else {
                    xhr.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded; charset=UTF-8');
                }
            }

            xhr.send(postData || '');
            return xhr;
        }
    })();
}

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

        /**
         * Make a selection in a drop down list.
         * @param {HTMLSelectElement} dropDown  the drop down list
         * @param {string} value                the value to select
         * @return {boolean} true if value exists; false if not
         */
        l7.Widget.selectDropDownByValue = function(dropDown, value) {
            for (var i = 0; i < dropDown.options.length; ++i) {
                if (dropDown.options[i].value == value) {
                    dropDown.selectedIndex = i;
                    return true;
                }
            }
            return false;
        }

        /**
         * Initializes a drop down with selection for both 24- and 12-hour clock.
         * The resulting selection values are integer 0 to 23.
         * @param {HTMLSelectElement} dropDown
         */
        l7.Widget.initHourlyDropDown = function(dropDown) {
            var optGroup = document.createElement('optgroup');
            optGroup.label = '00 - 23';
            for (var hr = 0; hr < 24; ++hr) {
                var option = document.createElement('option');
                option.value = hr;
                option.innerHTML = (hr < 10 ? '0' : '') + hr + ':00';
                optGroup.appendChild(option);
            }
            dropDown.appendChild(optGroup);

            optGroup = document.createElement('optgroup');
            optGroup.label = 'AM/PM';
            for (hr = 0; hr < 24; ++hr) {
                var h = hr > 12 ? (hr - 12) : (hr == 0 ? 12 : hr);
                var ampm = hr < 12 ? 'AM' : 'PM';
                option = document.createElement('option');
                option.value = hr;
                option.innerHTML = h + ':00 ' + ampm;
                optGroup.appendChild(option);
            }
            dropDown.appendChild(optGroup);
        }

        /**
         * Turns a plain text input element into a date picker by popping up a
         * calendar dialog whenever in focus.
         * On return, the YAHOO.widget.Calendar object can be retrieved by calling
         * textBox.getCalendar(). This is useful for localization or changing min/max dates (see YUI doc).
         * Upon date change, the onkeyup event handler of textBox will be called.
         * @static
         * @param {HTMLInputElement} textBox    INPUT element of type="text"
         * @param {string} format               "yyyy-MM-dd", "MMM d, yyyy", "MM/dd/yyyy", or "yyyy/MM/dd"
         * @param {string|Date|null} minDate    can also be set later by calling textBox.getCalendar().cfg.setProperty('mindate', aDate)
         * @param {string|Date|null} maxDate    can also be set later by calling textBox.getCalendar().cfg.setProperty('maxdate', aDate)
         * @param {string|Date|null} selected   can also be set later by calling textBox.getCalendar().cfg.setProperty('selected', aDate)
         * @param {string|null} title           dialog title HTML; null will default to "Choose a date:"
         */
        l7.Widget.initCalendarTextBox = function(textBox, format, minDate, maxDate, selected, title) {
            var calendarDiv = document.createElement('div');
            calendarDiv.style.border = 'none';
            calendarDiv.style.cssFloat = 'none';    // Override: calender is float:left normally.
            var calendar = new YAHOO.widget.Calendar(calendarDiv, {
                iframe           : false,   // Because container has IFRAME support.
                hide_blank_weeks : true,
                mindate          : minDate,
                maxdate          : maxDate,
                selected         : selected
            });
            calendar.render();

            var dialogHeadDiv = document.createElement('div');
            dialogHeadDiv.className = 'hd';
            dialogHeadDiv.innerHTML = title == null ? 'Choose a date:' : title;

            var dialogBodyDiv = document.createElement('div');
            dialogBodyDiv.className = 'bd';
            dialogBodyDiv.style.padding = '0';
            dialogBodyDiv.appendChild(calendarDiv);

            var dialogDiv = document.createElement('div');
            dialogDiv.appendChild(dialogHeadDiv);
            dialogDiv.appendChild(dialogBodyDiv);
            document.body.appendChild(dialogDiv);
            var dialog = new YAHOO.widget.Dialog(dialogDiv, {
                context   : [textBox, 'tl', 'bl'],
                draggable : false,
                close     : true
            });
            dialog.render();
            dialog.hide();  // Using dialog.hide() instead of visible:false is a workaround for an IE6/7 container known issue with border-collapse:collapse.

            calendar.renderEvent.subscribe(function(event) {
                // Tell Dialog it's contents have changed, Currently used by container for IE6/Safari2 to sync underlay size.
                dialog.fireEvent('changeContent');
            });
            calendar.selectEvent.subscribe(function(event) {
                dialog.hide();
                var date = calendar.getSelectedDates()[0];
                textBox.value = l7.Util.formatDate(date, format);
                textBox.onkeyup();
            });

            var mouseOverDialog = false;
            YAHOO.util.Event.addListener(dialogDiv, 'mouseover', function() {
                mouseOverDialog = true;
            });
            YAHOO.util.Event.addListener(dialogDiv, 'mouseout', function() {
                mouseOverDialog = false;
            });

            YAHOO.util.Event.addListener(textBox, 'focus', function() {
                calendar.render();          // mindate and maxdate may have changed
                dialog.align('tl', 'bl');   // Why is this necessary?
                dialog.show();
            });
            YAHOO.util.Event.addListener(textBox, 'blur', function() {
                if (!mouseOverDialog) dialog.hide();
            });

            textBox.readOnly = true;
            textBox.getCalendar = function() { return calendar; }
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
                            handler   : function() {
                                this.hide();
                                this.setBody('');       // This is to prevent the hidden dialog from
                                this.moveTo(0, 0);      //     taking up phantom space.
                            },
                            isDefault : true
                        }
                    ],
                    close       : true,
                    draggable   : true,
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
            l7.Dialog._errorDialog.center();
            l7.Dialog._errorDialog.show();
        }

        /**
         * Displays a simple info dialog.
         *
         * @param {string} header   localized header text; defaults to 'Info' if null
         * @param {html} body       HTML content
         * @param {string} okText   localized text label for the OK button; defaults to 'OK' if null
         * @requires YAHOO.widget.Dialog
         * @requires YAHOO.widget.Button
         */
        l7.Dialog.showInfoDialog = function(header, body, okText) {
            if (l7.Dialog._infoDialog == undefined) {
                /**
                 * @private
                 */
                l7.Dialog._infoDialog = new YAHOO.widget.Dialog('l7_Dialog_infoDialog', {
                    buttons     : [
                        {
                            text      : 'OK',
                            handler   : function() {
                                this.hide();
                                this.setBody('');       // This is to prevent the hidden dialog from
                                this.moveTo(0, 0);      //     taking up phantom space.
                            },
                            isDefault : true
                        }
                    ],
                    close       : true,
                    draggable   : true,
                    icon        : YAHOO.widget.SimpleDialog.ICON_INFO,
                    modal       : true,
                    visible     : false,
                    zindex      : 999
                });
            }
            l7.Dialog._infoDialog.setHeader(header == null ? 'Info' : header);
            l7.Dialog._infoDialog.setBody(body);
            l7.Dialog._infoDialog.render(document.body);
            l7.Dialog._infoDialog.getButtons()[0].set('label', okText == null ? 'OK' : okText);
            l7.Dialog._infoDialog.center();
            l7.Dialog._infoDialog.show();
        }

        /**
         * Displays the given l7-style exception object literal in a simple error dialog.
         *
         * @public
         * @param {object} o        an l7-style exception object literal
         * @param {string} header   localized header text; defaults to 'Error' if null
         * @param {html} beginBody  beginning HTML content
         * @param {string} okText   localized text label for the OK button; defaults to 'OK' if null
         */
        l7.Dialog.showExceptionDialog = function(o, header, beginBody, okText) {
            var divId = 'l7_Dialog_exceptionDiv';
            var tippyId = divId + '_tippy';
            var stackTraceTrIdPrefix = 'l7_Dialog_stackTrace_';
            var body = beginBody
                     + '<div style="margin-top: 10px;">' + l7.Util.escapeAsText(o.localizedMessage == null ? o.message : o.localizedMessage) + '</div>'
                     + '<div class="tippy" style="margin: 10px 0 4px 0; width: 600px;">'
                     +     '<img id="' + tippyId + '" class="tippy" src="../images/tippyCollapsed.png" alt="" onclick="l7.Tippy.toggleTippy(this, \'' + divId + '\')" />'
                     +     '<span class="clickable" onclick="l7.Tippy.toggleTippy(\'' + tippyId + '\', \'' + divId + '\')">Details</span>'
                     + '</div>'
                     + '<div id="' + divId + '" style="display: none; margin-left: 17px">'
                     +     '<table class="spaced" style="border: 1px solid #000000; margin: 3px 0 3px 0; width: 583px;">'
                     +         '<tr>'
                     +             '<th colspan="2" style="background-color: #000000; color: #ffffff;">'
                     +                 '<span class="clickable" onclick="l7.Util.showElements(\'' + stackTraceTrIdPrefix + '.+\');">Show</span>'
                     +                 ' / '
                     +                 '<span class="clickable" onclick="l7.Util.hideElements(\'' + stackTraceTrIdPrefix + '.+\');">Hide</span>'
                     +                 ' Stack Trace'
                     +             '</th>'
                     +         '</tr>';
            for (var e = o, i = 0; e != null || e != undefined; e = e.cause, ++i) {
                if (e !== o) body += '<tr><th colspan="2" style="background-color: #d0d0d0;">Caused By:</th></tr>';
                body += '<tr><th class="top">Exception:</th><td width="100%">' + l7.Util.escapeAsText(e.exception) + '</td></tr>';
                if (e.message) {
                    body += '<tr><th class="top">Message:</th><td class="wrap">' + l7.Util.escapeAsText(e.message) + '</td></tr>';
                }
                if (e.localizedMessage && e.localizedMessage != e.message) {
                    body += '<tr><th class="top">Localized Message:</th><td class="wrap">' + l7.Util.escapeAsText(e.localizedMessage) + '</td></tr>';
                }
                var stackTraceTrId = stackTraceTrIdPrefix + i;
                body += '<tr id="' + stackTraceTrId + '" style="display: none;"><th class="top">Stack Trace:</th><td>';
                if (e.stackTrace) {
                    for (var j in e.stackTrace) {
                        body += '<div>' + l7.Util.escapeAsText(e.stackTrace[j]) + '</div>';
                    }
                } else {
                    body += '(none)';
                }
                body += '</td></tr>';
            }
            body +=     '</table>'
                  + '</div>';
            l7.Dialog.showErrorDialog(header, body, okText);
        }

        /**
         * Displays a simple error dialog if the given object is an l7-style exception object.
         *
         * @param {object} o        the object literal to test
         * @param {string} header   localized header text; defaults to 'Error' if null
         * @param {html} beginBody  beginning HTML content
         * @param {string} okText   localized text label for the OK button; defaults to 'OK' if null
         * @return {boolean} true if it was an exception
         */
        l7.Dialog.showExceptionDialogIfException = function(o, header, beginBody, okText) {
            var result = l7.Util.isException(o);
            if (result) {
                l7.Dialog.showExceptionDialog(o, header, beginBody, okText);
            }
            return result;
        }

        /**
         * Parses JSON text and displays a simple error dialog if the JSON is malformed
         * or if it is parsed into a l7-style exception object.
         *
         * @public
         * @param {string} s                 the text to parse as JSON
         * @param {string} errHeader         header text to use as header of error dialog
         * @param {string} htmlIfException   HTML to display at the top of the error dialog if the JSON
         *                                   text is parsed into a l7-style exception
         * @param {string} htmlIfBadJSON     HTML to display in error dialog if the JSON text is malformed
         * @param {string} errOk             text to label the button in error dialog
         * @return {string|object} the input string if it contains white spaces only;
         *                         or the resulting object literal if parsed successfully
         *                         which can be a l7-style exception object;
         *                         or the input string if it cannot be parsed as JSON
         */
        l7.Dialog.parseJSON = function(s, errHeader, htmlIfException, htmlIfBadJSON, errOk) {
            if (s.search(/\S/) == -1) { // white spaces only
                return s;
            } else {
                try {
                    var o = YAHOO.lang.JSON.parse(s);
                    if (l7.Util.isException(o)) {
                        l7.Dialog.showExceptionDialog(o, errHeader, htmlIfException, errOk);
                    }
                    return o;
                } catch (e) {
                    l7.Dialog.showErrorDialog(errHeader, htmlIfBadJSON + '<br/><br/>' + e, errOk);
                    return s;
                }
            }
        }
    })();
}
