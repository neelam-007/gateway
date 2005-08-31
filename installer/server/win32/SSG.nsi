;$Id$
;NSIS Modern User Interface version 1.63
;based on Basic Example Script, which was Written by Joost Verburg

!define J2RE "jre1.5.0_02"  ;Name of directory containing JRE
!define J2RE_PATH "C:\${J2RE}"   ;Full path to directory containing JRE (at .nsi compile-time)
!define COMPANY "Layer 7 Technologies"
!define MUI_PRODUCT "SecureSpan Gateway" ;Define your own software name here

; Edit this to set the version number in the build (is auto-edited by build.xml's OFFICIAL-build target)
!define MUI_VERSION "HEAD"

!define BUILD_DIR "..\..\..\build" ;UneasyRooster\build dir, root of jar files and things

!include "MUI.nsh"

;--------------------------------
;Configuration

  ;General
  OutFile "${MUI_PRODUCT} ${MUI_VERSION} Installer.exe"

  ;Folder selection page
  InstallDir "$PROGRAMFILES\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"

  ;Remember install folder
  InstallDirRegKey HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" ""

;--------------------------------
;Modern UI Configuration

  !define MUI_LICENSEPAGE
 ; !define MUI_COMPONENTSPAGE
  !define MUI_DIRECTORYPAGE
  !define MUI_STARTMENUPAGE

  !define MUI_ABORTWARNING

  !define MUI_UNINSTALLER
  !define MUI_UNCONFIRMPAGE

  !define MUI_HEADERBITMAP "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

  ;Remember the Start Menu Folder
  !define MUI_STARTMENUPAGE_REGISTRY_ROOT "HKCU"
  !define MUI_STARTMENUPAGE_REGISTRY_KEY "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Layer 7 SecureSpan Gateway"

  !define TEMP $R0


;--------------------------------
;Languages

  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Copy the SecureSpan Gateway files to the application folder."

;--------------------------------
;Data

  LicenseData "License.txt"

  ReserveFile "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

;--------------------------------
;Installer Sections

Section "SecureSpan Gateway" SecCopyUI

  CreateDirectory "$INSTDIR\logs"
  CreateDirectory "$INSTDIR\bin"
  CreateDirectory "$INSTDIR\etc\conf"
  CreateDirectory "$INSTDIR\etc\keys"
  CreateDirectory "$INSTDIR\var\attachments"

  SetOutPath "$INSTDIR"
  File /r "${BUILD_DIR}\install\ssg\tomcat"
  File /r "C:\jdk1.5.0_02-windows-i586-p-redist"
  Rename "$INSTDIR\jdk1.5.0_02-windows-i586-p-redist" "$INSTDIR\jdk"
  ;File /r "${BUILD_DIR}\install\ssg\jdk_1.5.0_02" this would include the linux jvm

  SetOutPath "$INSTDIR/bin"
  File "${BUILD_DIR}\..\native\win32\uptime\Release\uptime.exe"
  File "${BUILD_DIR}\..\native\win32\process\Release\process.exe"
  File "${BUILD_DIR}\..\native\win32\killproc\Release\killproc.exe"
  File "${BUILD_DIR}\..\etc\ssg.cmd"
  File "${BUILD_DIR}\..\etc\service.cmd"
  File "${BUILD_DIR}\..\etc\SSG.exe"
  File "${BUILD_DIR}\..\etc\ssgruntimedefs.cmd"

  SetOutPath "$INSTDIR/etc"
  File /r "${BUILD_DIR}\install\ssg\etc\conf"
  File /r "${BUILD_DIR}\install\ssg\etc\sql"
  File /r "${BUILD_DIR}\install\ssg\etc\ldapTemplates"

  SetOutPath "$INSTDIR"
  File /r "${BUILD_DIR}\configwizard"

  ;Store install folder
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" "" $INSTDIR

  ; !insertmacro MUI_STARTMENU_WRITE_BEGIN

  ; Create shortcuts
  CreateDirectory "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}"
  CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Uninstall SecureSpan Gateway.lnk" "$INSTDIR\Uninstall.exe"
  CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Configure SecureSpan Gateway.lnk" "$INSTDIR\configwizard\ssgconfig.cmd"

  ; !insertmacro MUI_STARTMENU_WRITE_END

  ;Register with Add/Remove programs
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayName" "${MUI_PRODUCT}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "UninstallString" "$INSTDIR\Uninstall.exe"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "InstallLocation" "$INSTDIR"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "Publisher" "${COMPANY}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "URLInfoAbout" "http://www.layer7tech.com"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "DisplayVersion" "${MUI_VERSION}"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoModify" "1"
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}" "NoRepair" "1"

  ;Create uninstaller
  WriteUninstaller "$INSTDIR\Uninstall.exe"

  ; install the service
  ExecWait '"$INSTDIR\bin\service.cmd" install' $0
  DetailPrint "service.cmd install returned with code $0"

  ; run the gateway configurator
  ExecWait '"$INSTDIR\configwizard\ssgconfig.cmd"' $0
  DetailPrint "configwizard returned with code $0"

  ; ask user if he wants the service to be started now
  MessageBox MB_YESNO "Do you want to start the SecureSpan Gateway service now?" IDNO skipstartservice

  ExecWait 'net start SSG' $0
  DetailPrint "net start SSG returned with code $0"

  skipstartservice:
SectionEnd

;Display the Finish header
;Insert this macro after the sections if you are not using a finish page
!insertmacro MUI_SECTIONS_FINISHHEADER

;--------------------------------
;Descriptions

!insertmacro MUI_FUNCTIONS_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecCopyUI} $(DESC_SecCopyUI)
!insertmacro MUI_FUNCTIONS_DESCRIPTION_END

;--------------------------------
;Uninstaller Section

Section "Uninstall"

  ExecWait 'net stop SSG' $0
  DetailPrint "net stop SSG returned with code $0"

  ExecWait '"$INSTDIR\bin\service.cmd" uninstall' $0
  DetailPrint "service.cmd uninstall returned with code $0"

  RMDir /r "$INSTDIR"

  ; Remove shortcut
  ReadRegStr ${TEMP} "${MUI_STARTMENUPAGE_REGISTRY_ROOT}" "${MUI_STARTMENUPAGE_REGISTRY_KEY}" "${MUI_STARTMENUPAGE_REGISTRY_VALUENAME}"
  StrCmp ${TEMP} "" noshortcuts
  Delete "$SMPROGRAMS\${TEMP}\Uninstall SecureSpan Gateway.lnk"
  Delete "$SMPROGRAMS\${TEMP}\Configure SecureSpan Gateway.lnk"
  RMDir "$SMPROGRAMS\${TEMP}" ; Only if empty, so it won't delete other shortcuts

  noshortcuts:

  DeleteRegKey /ifempty HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}"

  RMDir "$PROGRAMFILES\${COMPANY}"

  ;Display the Finish header
  !insertmacro MUI_UNFINISHHEADER

SectionEnd
