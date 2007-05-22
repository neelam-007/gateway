#!/bin/bash
#Swiped from /etc/init.d/functions on a FC6 machine.

# Log that something succeeded
success() {
  echo_success
  return 0
}

# Log that something failed
failure() {
  echo_failure
  local rc=$?
  return $rc
}

echo_success() {
  echo "[  OK  ]"
  return 0
}

echo_failure() {
  echo -n "[FAILED]"
  return 1
}
