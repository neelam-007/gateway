#!/bin/bash
#
# Check if the my.cnf file is up to date.
#
# This script is currently limited to checking for full configuration
# lines without any spaces.
#

#
# CONFIG
#
EXIT_CODE_NEEDS_UPDATE="0";
EXIT_CODE_UP_TO_DATE="1";
EXIT_CODE_ERROR="2";
EXIT_VALUE=${EXIT_CODE_ERROR}

MY_CNF_FILE="/etc/my.cnf"

MYCNF_REQUIRE_mysqld='
innodb_flush_log_at_trx_commit=1
innodb_safe_binlog
sync_binlog=1
slave_compressed_protocol=1 
slave-net-timeout=30
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
	eval "${1}=\"\${UPDATED_LINES}\""
}

#
# WORK
#
if [ -w "${MY_CNF_FILE}" ] && [ -f "${MY_CNF_FILE}" ] ; then
  info "Checking configuration file \"${MY_CNF_FILE}\"."  
  
  # find lines by section
  CURRENT_SECTION="";
  SEEN_SECTIONS=""
  while read -r CONFIG_LINE; do
    issection "${CONFIG_LINE}"
    if [ ${?} -eq 0 ] ; then
      CURRENT_SECTION="${CONFIG_LINE//[\[\]]/}"      
      SEEN_SECTIONS="${SEEN_SECTIONS} ${CURRENT_SECTION}"
      info "Checking section \"${CURRENT_SECTION}\"."  
    fi
    if [ ! -z "${CURRENT_SECTION}" ] ; then
    	SECTION_PROPERTY="MYCNF_REQUIRE_${CURRENT_SECTION//\./___}"
		update_section_requirements "${SECTION_PROPERTY}" "${CONFIG_LINE}"   	
    fi
  done < "${MY_CNF_FILE}"
  
  # report missing items by section
  MISSING_ITEMS=0
  for CURRENT_SECTION in ${SEEN_SECTIONS}; do
    SECTION_PROPERTY="MYCNF_REQUIRE_${CURRENT_SECTION//\./___}"
  	REQUIRED_LINES="${!SECTION_PROPERTY}"  
  	for LINE in ${REQUIRED_LINES}; do
  	  MISSING_ITEMS=1
  	  info "Missing configuration for section \"${CURRENT_SECTION}\" line is \"${LINE}\"."
	done
  done

  # error if no sections processed
  if [ ${MISSING_ITEMS} -ne 0 ] || [ -z "${SEEN_SECTIONS}" ] ; then
    EXIT_VALUE=${EXIT_CODE_NEEDS_UPDATE}
  else
    EXIT_VALUE=${EXIT_CODE_UP_TO_DATE}
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
