// -----------------------------------------------------------------------------
// Utility functions
// -----------------------------------------------------------------------------

/**
 * Searches for elements with ID matching a given pattern.
 * Returns an array of elements found; may be empty but never null.
 *
 * @param idPattern     regexp pattern of IDs
 * @param node          node to start searching; null for document body
 * @param tagName       name of tags to restrict search; null for all tags
 * @return array
 */
function getElementsById(idPattern, startNode, tagName) {
	var result = new Array();

	if (startNode == null) {
        startNode = document.body;
	}
	if (tagName == null) {
        tagName = '*';
	}
	tagName = tagName.toLowerCase();

	var elements = startNode.getElementsByTagName(tagName);

    if (idPattern.substring(0, 1) != '^') idPattern = '^' + idPattern;
    if (idPattern.substring(idPattern.length - 1, idPattern.length) != '$') idPattern = idPattern + '$';
    var pattern = new RegExp(idPattern);
    for(var i = 0, j = 0; i < elements.length; ++i) {
        if (pattern.test(elements[i].id)) {
            result[j] = elements[i];
            ++j;
        }
    }

	return result;
}

/**
 * Searches for elements with a matching class name. This class name may
 * be one of multiple class names assigned to an element.
 * Returns an array of elements found; may be empty but never null
 *
 * @param className     class name
 * @param startNode     node to start searching; null for document body
 * @param tagName       name of tags to restrict search; null for all tags
 * @return array
 */
function getElementsByClassName(className, startNode, tagName) {
	var result = new Array();

	if (startNode == null) {
		startNode = document.body;
	}
	if (tagName == null) {
		tagName = '*';
	}
	tagName = tagName.toLowerCase();

    var elements = startNode.getElementsByTagName(tagName);

    var pattern = new RegExp('(^|\\s)' + className + '(\\s|$)');
    for(var i = 0, j = 0; i < elements.length; i++) {
        if (pattern.test(elements[i].className)) {
            result[j] = elements[i];
            ++j;
        }
    }

	return result;
}

/**
 * Returns the parent folder of a given path, include the trailing slash.
 *
 * @param path      a path
 * @return string
 */
function getParent(path) {
	return path.substring(0, path.lastIndexOf('/') + 1);
}

/**
 * Returns the file name portion of a given path, i.e., the last component.
 *
 * @param path      a path
 * @return string
 */
function getFileName(path) {
	return path.substring(path.lastIndexOf('/') + 1);
}

/**
 * Sets the source path of an IMG element.
 *
 * @param img   an IMG element
 * @param src   the source path
 */
function setImage(img, src) {
    img.src = src;
}

/**
 * Sets the class of an element. This will become the only class of this element.
 *
 * @param element       the HTML element
 * @param className     the class name
 */
function setClass(element, className) {
    element.className = className;
}

/**
 * Adds a class to an element, if not already there.
 * Returns true if added.
 *
 * @param element       the HTML element
 * @param className     the class name
 * @return boolean
 */
function addClass(element, className) {
    var pattern = new RegExp('(^|\\s)' + className + '(\\s|$)');
    var classes = element.className;
    if (!pattern.test(classes)) {
        if (classes.length == 0) {
            element.className = className;
        } else {
            element.className += ' ' + className;
        }
        return true;
    } else {
        return false;
    }
}

/**
 * Removes a class to an element, if there.
 * Return true if removed.
 *
 * @param element       the HTML element
 * @param className     the class name
 * @return boolean
 */
function removeClass(element, className) {
    var pattern = new RegExp('(^|\\s+)' + className + '(\\s+|$)', 'g');
    var classes = element.className;
    if (pattern.test(classes)) {
        element.className = classes.replace(pattern, ' ').replace(/^\s+/, '').replace(/\s+$/, '');
        return true;
    } else {
        return false;
    }
}

/**
 * Tests if an element is assigned a class.
 *
 * @param element       the HTML element
 * @param className     the class name
 * @return boolean
 */
function hasClass(element, className) {
    var pattern = new RegExp('(^|\\s)' + className + '(\\s|$)');
    return pattern.test(element.className);
}

/**
 * Returns a string with all special character properly esacped for use as HTML text.
 *
 * @param s         the text to escape
 * @return string
 */
function escapeAsText(s) {
    s.replace('<', '&lt;');
    s.replace('>', '&gt;');
    s.replace('&', '&amp;');
    return s;
}

/**
 * Tests if an array contains an object.
 *
 * @param array     the array
 * @param obj       the object
 */
function arrayContains(array, obj) {
    for (var i = 0; i < array.length; ++i) {
        if (array[i] == obj) {
            return true;
        }
    }
    return false;
}

/**
 * Removes matching element(s) from an array.
 *
 * @param array     the array
 * @param element   the element to match using ==
 */
function removeArrayElement(array, element) {
    for (var i = array.length - 1; i >=0; --i) {
        if (array[i] == element) {
            array.splice(i, 1);
        }
    }
}

// -----------------------------------------------------------------------------
// Tab bar
// -----------------------------------------------------------------------------

// Precache rollover images.
if (document.images) {
    new Image().src = '../images/tabLTover.png';
    new Image().src = '../images/tabCTover.png';
    new Image().src = '../images/tabRTover.png';
    new Image().src = '../images/tabLMover.png';
    new Image().src = '../images/tabCMover.png';
    new Image().src = '../images/tabRMover.png';
    new Image().src = '../images/tabLBover.png';
    new Image().src = '../images/tabCBover.png';
    new Image().src = '../images/tabRBover.png';
}

// Hash map for fast lookup of the 9 table cells that make up a tab.
// L = Left, C = Center, R = Right
// T = Top, M = Middle, B = Bottom
var tabLTcell = new Array();
var tabCTcell = new Array();
var tabRTcell = new Array();
var tabLMcell = new Array();
var tabCMcell = new Array();
var tabRMcell = new Array();
var tabLBcell = new Array();
var tabCBcell = new Array();
var tabRBcell = new Array();

/**
 * Handles mouseover on an "off" tab.
 */
function tabOver() {
    var tabName = this.id.substring(5);
    setImage(tabLTcell[tabName].firstChild, '../images/tabLTover.png');
    setClass(tabCTcell[tabName], 'tabCTover');
    setImage(tabRTcell[tabName].firstChild, '../images/tabRTover.png');
    setClass(tabLMcell[tabName], 'tabLMover');
    setClass(tabCMcell[tabName], 'tabCMover');
    setClass(tabRMcell[tabName], 'tabRMover');
    setImage(tabLBcell[tabName].firstChild, '../images/tabLBover.png');
    setClass(tabCBcell[tabName], 'tabCBover');
    setImage(tabRBcell[tabName].firstChild, '../images/tabRBover.png');
}

/**
 * Handles mouseout on an "off" tab.
 */
function tabOut() {
    var tabName = this.id.substring(5);
    setImage(tabLTcell[tabName].firstChild, '../images/tabLT.png');
    setClass(tabCTcell[tabName], 'tabCT');
    setImage(tabRTcell[tabName].firstChild, '../images/tabRT.png');
    setClass(tabLMcell[tabName], 'tabLM');
    setClass(tabCMcell[tabName], 'tabCM');
    setClass(tabRMcell[tabName], 'tabRM');
    setImage(tabLBcell[tabName].firstChild, '../images/tabLBoff.png');
    setClass(tabCBcell[tabName], 'tabCBoff');
    setImage(tabRBcell[tabName].firstChild, '../images/tabRBoff.png');
}

/**
 * Initializes an "off" tab.
 *
 * @param tabName   the predefined tab name
 * @param url       link address
 */
function initTab(tabName) {
    var id = 'tabCM' + tabName;
    var td = document.getElementById(id);
    td.onmouseover = tabOver;
    td.onmouseout = tabOut;

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
}

// -----------------------------------------------------------------------------
// Tippy
// -----------------------------------------------------------------------------

var EXPANDED_TIPPY_IMG_NAME = 'tippyExpanded.png';
var COLLAPSED_TIPPY_IMG_NAME = 'tippyCollapsed.png';

function toggleTippy(tippy, targetId) {
    if (typeof tippy == 'string') {
        tippy = document.getElementById(tippy);
    }
    var target = document.getElementById(targetId);
    if (getFileName(tippy.src) == EXPANDED_TIPPY_IMG_NAME) {
        target.style.display = 'none';
        tippy.src = getParent(tippy.src) + COLLAPSED_TIPPY_IMG_NAME;
    } else {
        target.style.display = '';
        tippy.src = getParent(tippy.src) + EXPANDED_TIPPY_IMG_NAME;
    }
}

/**
 * Expand all tippies with ID matching a given pattern.
 *
 * @param idPattern     regexp patter of IDs
 */
function expandTippies(idPattern) {
    var tippies = getElementsById(idPattern);
    for (var i in tippies) {
        if (getFileName(tippies[i].src) == COLLAPSED_TIPPY_IMG_NAME) {
            tippies[i].onclick();
        }
    }
}

/**
 * Collapses all tippies with ID matching a given pattern.
 *
 * @param idPattern     regexp patter of IDs
 */
function collapseTippies(idPattern) {
    var tippies = getElementsById(idPattern);
    for (var i in tippies) {
        if (getFileName(tippies[i].src) == EXPANDED_TIPPY_IMG_NAME) {
            tippies[i].onclick();
        }
    }
}

// -----------------------------------------------------------------------------
// Resize
// -----------------------------------------------------------------------------

/**
 * Changes the height of elements.
 *
 * @param ids       array of element IDs
 * @param delta     height increment in pixels (positive to make taller, negative to make shorter)
 * @param min       minimum element height
 * @param max       maximum element height
 */
function changeHeight(ids, delta, min, max) {
    for (var i in ids) {
        var element = document.getElementById(ids[i]);
        var h = element.offsetHeight + delta;
        if (h < min) h = min;
        if (h > max) h = max;
        new YAHOO.util.Anim(ids[i], {height: {to: h, unit: 'px'}}, 0.1, YAHOO.util.Easing.easeOut).animate();
    }
}