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

/** Hash map for link address of each tab. */
var tabUrl = new Array();

/**
 * @param url   destination URL
 */
function loadPage(url) {
    window.location = url;
}

function setImage(imgElem, src) {
    imgElem.src = src;
}

function setClass(elem, className) {
    elem.className = className;
}

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
 * Handles mouse click on an "off" tab.
 */
function tabClick() {
    var tabName = this.id.substring(5);
    <!-- PROTOTYPE ONLY BEGIN -->
    loadPage(tabUrl[tabName]);
    <!-- PROTOTYPE ONLY EHD -->
    //loadPage(tabName);
}

/**
 * Initializes an "off" tab.
 *
 * @param tabName   the predefined tab name
 * @param url       link address
 */
function initTab(tabName, url) {
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

    tabUrl[tabName] = url;
}


var tippyExpanded = new Image();
tippyExpanded.src = '../images/tippyExpanded.png';
var tippyCollapsed = new Image();
tippyCollapsed.src = '../images/tippyCollapsed.png';

function toggleTippy(tippy, targetId) {
    var target = document.getElementById(targetId);
    if (tippy.src == tippyExpanded.src) {
        target.style.display = 'none';
        tippy.src = tippyCollapsed.src;
    } else {
        target.style.display = 'block';
        tippy.src = tippyExpanded.src;
    }
}

/**
 * Searches for elements with ID matching a given pattern.
 *
 * @param idPattern     regexp pattern of IDs
 * @param node          node to start searching; null for document body
 * @param tag           name of tags to restrict search; null for all tags
 * @return array of elements found; may be empty but never null
 */
function getElementsById(idPattern, startNode, tag) {
	var result = new Array();

	if (startNode == null) {
        // Defaults to body if not set.
        startNode = document.body;
	}

	if (tag == null) {
        // Search all tags if not set.
        tag = '*';
	}
	tag = tag.toLowerCase();
	var elements = startNode.getElementsByTagName(tag);

    if (idPattern.substring(0, 1) != '^') idPattern = '^' + idPattern;
    if (idPattern.substring(idPattern.length - 1, idPattern.length) != '$') idPattern = idPattern + '$';
    var pattern = new RegExp(idPattern);
    for(var i = 0, j = 0; i < elements.length; i++) {
        if (pattern.test(elements[i].id)) {
            result[j] = elements[i];
            j++;
        }
    }

	return result;
}

/**
 * Expand all tippies with ID matching a given pattern.
 *
 * @param idPattern     regexp patter of IDs
 */
function expandTippies(idPattern) {
    var tippies = getElementsById(idPattern);
    for (var i in tippies) {
        if (tippies[i].src == tippyCollapsed.src) {
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
        if (tippies[i].src == tippyExpanded.src) {
            tippies[i].onclick();
        }
    }
}
