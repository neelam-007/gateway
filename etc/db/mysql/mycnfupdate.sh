#!/bin/bash
#
# Script to edit my.cnf file.
# 
# Line / section based update script for my.cnf files.
# Only supports update of configuration lines without spaces.
#

#
# CONFIG
#
EXIT_CODE_SUCCESS="0";
EXIT_CODE_ERROR="1";
EXIT_VALUE=${EXIT_CODE_ERROR}

MY_CNF_FILE="/etc/my.cnf"


ADD_LINES_mysqld='
innodb_flush_log_at_trx_commit=1
innodb_safe_binlog
sync_binlog=1
slave_compressed_protocol=1 
slave-net-timeout=30
'

DELETE_LINES_mysqld='
innodb_flush_log_at_trx_commit=2
'

#
# FUNCTIONS
#
info() {
	output "INFO: ${1}"
}

error() {
	output "ERROR: ${1}"
}

output() {
	TXT="${1}"
	echo "${TXT}"
}

issection() {
	TXT="${1}"
	if [ "[${TXT//[\[\]]/}]" == "${TXT}" ] ; then
	  return 0
	else
	  return 1
	fi
}

update_section_requirements() {
	REQUIRED_LINES="${!1}"
    CHECK_LINE="${2}"
	UPDATED_LINES=""
	for LINE in ${REQUIRED_LINES}; do
	  if [ "${LINE}" != "${CHECK_LINE}" ] ; then
	    UPDATED_LINES="${UPDATED_LINES} ${LINE}" 
	  fi
	done
	eval ${1}=\"\${UPDATED_LINES}\"
}

isdelete() {
	DELETE_LINES="${!1}"
    CHECK_LINE="${2}"
	for LINE in ${DELETE_LINES}; do
	  if [ "${LINE}" == "${CHECK_LINE}" ] ; then
	    return 0
	  fi
	done
    return 1
}

write_section_additions() {
	ADD_LINES="${!1}"
    FILE="${2}"
	for LINE in ${ADD_LINES}; do
	  info "Adding configuration line \"${LINE}\"."
	  echo "${LINE}" >> "${FILE}"
	done
	if [ ! -z "${ADD_LINES}" ] ; then
      echo "" >> "${FILE}"
    fi
    return ${?}
}

#
# WORK
#
if [ -w "${MY_CNF_FILE}" ] && [ -f "${MY_CNF_FILE}" ] ; then
  info "Updating configuration file \"${MY_CNF_FILE}\"."  
  
  # create temp file
  TEMP_MYCNF="$(mktemp -t \"my.cnf.XXXXXXXXXX\")"
  if [ ${?} -ne 0 ] ; then
    error "Could not create temporary file.";
    EXIT_VALUE=${EXIT_CODE_ERROR}	      
  else
    # find lines by section
    CURRENT_SECTION="";
    while read -r CONFIG_LINE; do
      issection "${CONFIG_LINE}"
      if [ ${?} -eq 0 ] ; then
        if [ ! -z "CURRENT_SECTION" ] ; then
          SECTION_ADD_PROPERTY="ADD_LINES_${CURRENT_SECTION//\./___}"
          write_section_additions "${SECTION_ADD_PROPERTY}" "${TEMP_MYCNF}"
        fi
        CURRENT_SECTION="${CONFIG_LINE//[\[\]]/}"      
        info "Checking section \"${CURRENT_SECTION}\"."
      fi
      if [ ! -z "${CURRENT_SECTION}" ] ; then
    	SECTION_ADD_PROPERTY="ADD_LINES_${CURRENT_SECTION//\./___}"
		update_section_requirements "${SECTION_ADD_PROPERTY}" "${CONFIG_LINE}"   	

    	SECTION_DELETE_PROPERTY="DELETE_LINES_${CURRENT_SECTION//\./___}"
        isdelete "${SECTION_DELETE_PROPERTY}" "${CONFIG_LINE}" 
        if [ ${?} -eq 0 ] ; then
          info "Deleting configuration line \"${CONFIG_LINE}\"."
        else
          echo "${CONFIG_LINE}" >> "${TEMP_MYCNF}"
        fi
      else
        echo "${CONFIG_LINE}" >> "${TEMP_MYCNF}"
      fi
    done < "${MY_CNF_FILE}"  

    # write final section
    if [ ! -z "CURRENT_SECTION" ] ; then
      SECTION_ADD_PROPERTY="ADD_LINES_${CURRENT_SECTION//\./___}"
      write_section_additions "${SECTION_ADD_PROPERTY}" "${TEMP_MYCNF}"
      if [ ${?} -eq 0 ] ; then
        EXIT_VALUE=${EXIT_CODE_SUCCESS}
      fi
    fi
    
    # copy new file
    if [ ${EXIT_VALUE} -eq 0 ] ; then
      mv -f "${TEMP_MYCNF}" "${MY_CNF_FILE}"
      chmod 644 "${MY_CNF_FILE}"
    fi
  fi
  
else
  error "Cannot find/write configuration file \"${MY_CNF_FILE}\".";
  EXIT_VALUE=${EXIT_CODE_ERROR}	
fi

#
# EXIT
#
info "Exiting with code ${EXIT_VALUE}"
exit ${EXIT_VALUE}
