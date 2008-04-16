#!/bin/bash

#removes the password expiry time from the gateway account.

days=$(getent shadow gateway | awk -F ':' '{print $5}')
if [ "$days" = "60" ] ; then
	chage -M 99999 gateway
	chage -m 0 gateway
	echo "gateway password expiry removed"
else
	echo "the gateway password is not set to expire."
	echo "no changes needed"
fi
