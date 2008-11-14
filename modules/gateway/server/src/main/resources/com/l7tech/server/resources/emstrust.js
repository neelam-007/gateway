
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
    document.forms["emstrustform"].elements["returncookie"].value = readCookie("returncookie");
}

function init() {
    if ( document.getElementById('emsinfo').value == '' ) {
        document.getElementById('emsinforow').style.visibility="collapse";
        document.getElementById('emsuserdescrow').style.visibility="collapse";
    } else {
        document.getElementById('emsidrow').style.visibility="collapse";
        document.getElementById('emspemrow').style.visibility="collapse";
        document.getElementById('emsusernamerow').style.visibility="collapse";
    }

    if ( document.getElementById('returnurl').value == '' ) {
        document.getElementById('cancel').style.visibility="collapse";
    }
}
