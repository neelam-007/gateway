
mlyons@layer7tech.com note:

one-jar-boot-0.96.1.jar is the latest CVS of one-jar as of Mon, Feb-16-2009, but with ezhikov's patch from
here applied:

    [ 2589830 ] Speed and memory footprint improvement -> No More OOM
    http://sourceforge.net/tracker/index.php?func=detail&aid=2589830&group_id=111153&atid=658459

The patch is to JarFileClassLoader.java, and causes it to make fewer unnecessary copies of various possibly-huge
byte arrays.
