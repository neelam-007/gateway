;$Id$
;NSIS Modern User Interface version 1.63
;based on Basic Example Script, which was Written by Joost Verburg

!define J2RE "j2re1.4.2"  ;Name of directory containing JRE
!define J2RE_PATH "C:\${J2RE}"   ;Full path to directory containing JRE (at .nsi compile-time)
!define COMPANY "Layer 7 Technologies"
!define MUI_PRODUCT "SecureSpan Agent" ;Define your own software name here

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
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Layer 7 SecureSpan Agent"

  !define TEMP $R0


;--------------------------------
;Languages

  !insertmacro MUI_LANGUAGE "English"

;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Copy the SecureSpan Agent files to the application folder."

;--------------------------------
;Data

  LicenseData "License.txt"

  ReserveFile "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

;--------------------------------
;Installer Sections

Section "SecureSpan Agent" SecCopyUI

  ;ADD YOUR OWN STUFF HERE!

  SetOutPath "$INSTDIR"
  File "${BUILD_DIR}\..\native\win32\systray4j.dll"
  File "SecureSpan Agent.exe"
  File "SecureSpan Agent.ini"
  File "SecureSpan Agent.bat"
  File "SecureSpan Agent in Text Mode.bat"
  File "${BUILD_DIR}\ClientProxy.jar"
  File /r "${BUILD_DIR}\lib"
  File /r "${J2RE_PATH}"

  ;Store install folder
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" "" $INSTDIR

  !insertmacro MUI_STARTMENU_WRITE_BEGIN

    ;Create shortcuts
    CreateDirectory "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Start SecureSpan Agent.lnk" "$INSTDIR\SecureSpan Agent.exe" parameters "$INSTDIR\SecureSpan Agent.exe" 0
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Start SecureSpan Agent in Troubleshooting Mode.lnk" "$INSTDIR\SecureSpan Agent.bat" parameters "$INSTDIR\SecureSpan Agent.exe" 1
    ;CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Start SecureSpan Agent in Text Mode.lnk" "$INSTDIR\SecureSpan Agent in Text Mode.bat" parameters "$INSTDIR\SecureSpan Agent.exe" 2
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Uninstall SecureSpan Agent.lnk" "$INSTDIR\Uninstall.exe"

  !insertmacro MUI_STARTMENU_WRITE_END

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

  ;ADD YOUR OWN STUFF HERE!

  Delete "$INSTDIR\SecureSpan Agent.exe"
  Delete "$INSTDIR\SecureSpan Agent.ini"
  Delete "$INSTDIR\SecureSpan Agent.bat"
  Delete "$INSTDIR\SecureSpan Agent in Text Mode.bat"
  Delete "$INSTDIR\ClientProxy.jar"
  Delete "$INSTDIR\systray4j.dll"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\${J2RE}"
  Delete "$INSTDIR\Uninstall.exe"

  ;Remove shortcut
  ReadRegStr ${TEMP} "${MUI_STARTMENUPAGE_REGISTRY_ROOT}" "${MUI_STARTMENUPAGE_REGISTRY_KEY}" "${MUI_STARTMENUPAGE_REGISTRY_VALUENAME}"

  StrCmp ${TEMP} "" noshortcuts

    Delete "$SMPROGRAMS\${TEMP}\Start SecureSpan Agent.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Start SecureSpan Agent in Troubleshooting Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Start SecureSpan Agent in Text Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Uninstall SecureSpan Agent.lnk"
    RMDir "$SMPROGRAMS\${TEMP}" ;Only if empty, so it won't delete other shortcuts

  noshortcuts:

  RMDir "$INSTDIR"
  RMDir "$PROGRAMFILES\${COMPANY}"

  DeleteRegKey /ifempty HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  DeleteRegKey HKLM "Software\Microsoft\Windows\CurrentVersion\Uninstall\${MUI_PRODUCT} ${MUI_VERSION}"

  ;Display the Finish header
  !insertmacro MUI_UNFINISHHEADER

SectionEnd