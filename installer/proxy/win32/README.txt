$Id$

How to build the Client Proxy self-installing executable
========================================================

Steps that need to be done once (more or less) to set yourself up to make ClientProxy releases
----------------------------------------------------------------------------------------------
- Be running a Win32 OS of some kind, preferable Windows 2000 or Windows XP or something non-DOS-based
- Download and install the Nullsoft Scriptable Install System (NSIS 2.0) from http://nsis.sourceforge.net/
- Download the J2SE v 1.4.1_03 Windows JRE from http://java.sun.com/j2se/1.4.1/download.html
- Install the JRE to some directory
- Copy the JRE tree into UneasyRooster/installer/proxy/win32/j2re1.4.1_03 (ie, into this directory)
- Delete from the copied JRE all files listed as Optional in the j2re1.4.1_03/README.txt

Steps that need to be done to package a ClientProxy release
-----------------------------------------------------------
- Clean, rebuild, test, and package the UneasyRooster tree to prepare a build dir and ClientProxy.jar file.
- Edit ClientProxy.nsi and ensure that the MUI_VERSION define matches the version you are packaging.
- In Windows Explorer, right-click on the ClientProxy.nsi.
   - To quickly build a zlib-based installer, select "Compile NSI" from the context menu.
   - To take longer and build an optimized bzip2-based isntaller, select "Compile NSI (with bz2)"
- Distribute the resulting file (ie "Layer 7 Client Proxy 0.9b Installer.exe") to end users.



