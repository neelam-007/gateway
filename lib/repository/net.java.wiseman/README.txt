Wiseman patch info
==================
The wiseman patch adds support for customization of HTTP request handling.

The patch also fixes an issue with handling formatted response messages (if
not patched an error occurs if the first child of the SOAP Body is not an 
element).

To build the library with the patch do the following:

1) cvs -d :pserver:guest@cvs.dev.java.net:/cvs co -R -r wiseman_1_0_final \
   -d wiseman_1_0_final wiseman 
2) cd wiseman_1_0_final
3) patch -p1 < ../wiseman.patch
4)  ... comment out test and deployment targets ...
5) ant build

The patch was created using:

  diff -rNU 3 --exclude=CVS wiseman_1_0_final wiseman > wiseman.patch
