#!/usr/bin/perl

require 5.005;
use strict;


my %QUOTES = (
  ibm => 111.0,
  l7 => 281.0,
  msft => 831,
);


my $input = "";
while (<STDIN>) {
  $input .= $_;
}


my $method = $ENV{REQUEST_METHOD};
if (!($method =~ /post/i)) {
  print "Content-Type: text/html\n\n";
  print <<ENDOFCLIENT;
<HTML><HEAD><TITLE>Warehouse sample</TITLE>
<SCRIPT language=javascript>

function doSubmit() {
  var got = document.xmlform.inputxml.value;
  var loc = document.xmlform.serviceurl.value;
  if (loc.length < 1)
    loc = document.xmlform.serviceurl.value = document.location;

  var xmlhttp;
  try {
     xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
  } catch (e) {
     xmlhttp = new XMLHttpRequest();
  }
  try {
    xmlhttp.open("POST", loc, true);
  } catch (e1) {
    try {
      netscape.security.PrivilegeManager.enablePrivilege("UniversalBrowserRead");
      xmlhttp.open("POST", loc, true);
    } catch (blah) {
      document.xmlform.outputxml.value = "Exception: " + blah + "\\n\\n";
      document.xmlform.outputxml.value += 'Shut down all browser windows; then, add:\\n\\n  user_pref("signed.applets.codebase_principal_support", true);\\n\\nto C:/Documents and Settings/YOUR_USERNAME/Application Data/Mozilla/Firefox/Profiles/default.3cu/prefs.js';
      throw blah;
    }
  }
  xmlhttp.setRequestHeader("Content-Type", "text/xml");
  xmlhttp.onreadystatechange=function() {
    if (xmlhttp.readyState == 4) {
      document.xmlform.outputxml.value = xmlhttp.responseText;
      if (xmlhttp.status != 200) {
        document.xmlform.outputxml.value += "Warning: Got non-200 status of: " + xmlhttp.status + " " + xmlhttp.statusText;
      }
    }
  };
  xmlhttp.send(got);

  return false;
}

</SCRIPT>
</HEAD>
<BODY>
<form name=xmlform method=post action="#">
<p>Service URL: <input type=text size=80 name=serviceurl>

<p>Available messages: 
<ul>
<li>&lt;echo&gt;random crap&lt;/echo&gt;
<li>&lt;getquote&gt;msft&lt;/getquote&gt;<p>
</ul>

<p>Input XML:<br><textarea name=inputxml rows=5 cols=80></textarea>
<p><input type=button value="post" onClick="doSubmit();">
<p>Output XML:<br><textarea name=outputxml rows=10 cols=80 readonly=true></textarea>
</BODY>

ENDOFCLIENT

  exit(0);

}

my $ctype = $ENV{CONTENT_TYPE};
if (!($ctype =~ /text\/xml\b/i)) {
  print "Content-Type: text/html\n\n";
  print "Content must be text/xml\n";
  exit(0);
}


print "Content-Type: text/xml\n\n";

foreach my $env (keys %ENV) {
#  print "$env=$ENV{$env}<br>\n";
}

if ($input =~ /<echo>(.*?)<\/echo>/) {
  print "<echoResponse>$1</echoResponse>\n";
  exit(0);
} elsif ($input =~ /<getquote>(.*?)<\/getquote>/) {
  my $symbol = $1;
  my $quote = $QUOTES{$symbol};
  if ($quote) {
    print "<getquoteResponse>\n  <symbol>$symbol</symbol>\n  <quote>$quote</quote>\n</getquoteResponse>\n";
  } else {
    print "<getquoteError>Unrecognized symbol: $symbol.  Recognized: " . join(" ", keys %QUOTES) .
         "</getquoteError>\n";
  }
} else {
  print "<errorResponse>Unrecognized command</errorResponse>\n";
}

exit(0);
