$Id$

How to build the Policy Editor self-installing executable
=========================================================

Steps that need to be done once (more or less) to set yourself up to make Policy Editor releases
------------------------------------------------------------------------------------------------
- Be running a Win32 OS of some kind, preferable Windows 2000 or Windows XP or something non-DOS-based
- Download and install the Nullsoft Scriptable Install System (NSIS 2.0) from http://nsis.sourceforge.net/
- Download the J2SE v 1.4.1_03 Windows JRE from http://java.sun.com/j2se/1.4.1/download.html
- Install the JRE to some directory, ie C:\
- Delete from the copied JRE all files listed as Optional in the j2re1.4.1_03/README.txt
- Edit PolicyEditor.nsi and change the J2RE_PARENT define to the directory holding the j2re1.4.1_03 subdir.

Steps that need to be done to package a Policy Editor release
-------------------------------------------------------------
- Clean, rebuild, test, and package the UneasyRooster tree to prepare a build dir and PolicyEditor.jar file.
- Edit PolicyEditor.nsi and ensure that the MUI_VERSION define matches the version you are packaging.
- In Windows Explorer, right-click on the PolicyEditor.nsi.
   - To quickly build a zlib-based installer, select "Compile NSI" from the context menu.
   - To take longer and build an optimized bzip2-based isntaller, select "Compile NSI (with bz2)"
- Distribute the resulting file (ie "Layer 7 Policy Editor 0.9b Installer.exe") to end users.

