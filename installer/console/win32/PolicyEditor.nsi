;$Id$
;NSIS Modern User Interface version 1.63
;based on Basic Example Script, which was Written by Joost Verburg

!define J2RE_PARENT "C:\"  ; Directory which contains j2re1.4.1_03 subdirectory
!define COMPANY "Layer 7 Technologies"
!define MUI_PRODUCT "Policy Editor" ;Define your own software name here
!define MUI_VERSION "0.9b" ;Define your own software version here
!define BUILD_DIR "..\..\..\build" ;UneasyRooster\build dir, root of jar files and things

!include "MUI.nsh"

;--------------------------------
;Configuration

  ;General
  OutFile "Layer 7 ${MUI_PRODUCT} ${MUI_VERSION} Installer.exe"

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
  !define MUI_STARTMENUPAGE_REGISTRY_VALUENAME "Layer 7 ${MUI_PRODUCT}"

  !define TEMP $R0


;--------------------------------
;Languages
 
  !insertmacro MUI_LANGUAGE "English"
  
;--------------------------------
;Language Strings

  ;Description
  LangString DESC_SecCopyUI ${LANG_ENGLISH} "Copy the Policy Editor files to the application folder."

;--------------------------------
;Data
  
  LicenseData "License.txt"

  ReserveFile "${NSISDIR}\Contrib\Icons\modern-header 2.bmp"

;--------------------------------
;Installer Sections

Section "Policy Editor" SecCopyUI

  ;ADD YOUR OWN STUFF HERE!

  SetOutPath "$INSTDIR"
  File "Policy Editor.exe"
  File "Policy Editor Debug.bat"
  File "${BUILD_DIR}\PolicyEditor.jar"
  File /r "${BUILD_DIR}\lib"
  File /r "${J2RE_PARENT}\j2re1.4.1_03"
  
  ;Store install folder
  WriteRegStr HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}" "" $INSTDIR

  !insertmacro MUI_STARTMENU_WRITE_BEGIN
    
    ;Create shortcuts
    CreateDirectory "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Start Policy Editor.lnk" "$INSTDIR\Policy Editor.exe"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Start Policy Editor in Troubleshooting Mode.lnk" "$INSTDIR\Policy Editor Debug.bat"
    CreateShortCut "$SMPROGRAMS\${MUI_STARTMENUPAGE_VARIABLE}\Uninstall Policy Editor.lnk" "$INSTDIR\Uninstall.exe"
  
  !insertmacro MUI_STARTMENU_WRITE_END
  
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

  Delete "$INSTDIR\Policy Editor Debug.bat"
  Delete "$INSTDIR\Policy Editor.exe"
  Delete "$INSTDIR\PolicyEditor.jar"
  RMDir /r "$INSTDIR\lib"
  RMDir /r "$INSTDIR\j2re1.4.1_03"
  Delete "$INSTDIR\Uninstall.exe"

  ;Remove shortcut
  ReadRegStr ${TEMP} "${MUI_STARTMENUPAGE_REGISTRY_ROOT}" "${MUI_STARTMENUPAGE_REGISTRY_KEY}" "${MUI_STARTMENUPAGE_REGISTRY_VALUENAME}"

  StrCmp ${TEMP} "" noshortcuts
  
    Delete "$SMPROGRAMS\${TEMP}\Start Policy Editor.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Start Policy Editor in Troubleshooting Mode.lnk"
    Delete "$SMPROGRAMS\${TEMP}\Uninstall Policy Editor.lnk"
    RMDir "$SMPROGRAMS\${TEMP}" ;Only if empty, so it won't delete other shortcuts
    
  noshortcuts:

  RMDir "$INSTDIR"
  RMDir "$PROGRAMFILES\${COMPANY}"

  DeleteRegKey /ifempty HKCU "Software\${COMPANY}\${MUI_PRODUCT} ${MUI_VERSION}"
  
  ;Display the Finish header
  !insertmacro MUI_UNFINISHHEADER

SectionEnd
