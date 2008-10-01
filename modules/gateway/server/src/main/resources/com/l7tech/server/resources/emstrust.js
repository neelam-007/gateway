
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