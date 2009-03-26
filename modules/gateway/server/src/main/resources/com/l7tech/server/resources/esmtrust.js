
function readCookie(cookieName) {
    var theCookie = "" + document.cookie;
    var ind = theCookie.indexOf(cookieName);
    if (ind == -1 || cookieName=="")
        return "";
    var ind1 = theCookie.indexOf(';', ind);
    if (ind1 == -1)
        ind1 = theCookie.length;
    return unescape(theCookie.substring(ind+cookieName.length+1, ind1));
}

function copyCookie() {
    if (document.forms["esmtrustform"])
        document.forms["esmtrustform"].elements["returncookie"].value = readCookie("returncookie");
}

function init() {
    if (!document.forms["esmtrustform"])
        return;
    if (document.getElementById('esminfo').value == '') {
        document.getElementById('esminforow').style.display = "none";
    } else if (document.getElementById('esminfo').value == '-') {
        document.getElementById('esminforow').style.display = "none";
        document.getElementById('esmidrow').style.display = "none";
        document.getElementById('esmpemrow').style.display = "none";
        document.getElementById('esmusernamerow').style.display = "none";
    } else {
        document.getElementById('esmidrow').style.display = "none";
        document.getElementById('esmpemrow').style.display = "none";
        document.getElementById('esmusernamerow').style.display = "none";
    }
}
