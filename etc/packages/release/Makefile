VERSION=3.7.0

package:
	tar -czvf /tmp/layer7-release-${VERSION}.tar.gz etc/issue* etc/*release boot/grub/splash* layer7-release.spec

rpm: 
	rpmbuild --target noarch -tb /tmp/layer7-release-${VERSION}.tar.gz

default:
	package
	rpm


##############################################################################
# OTHER TARGETS
##############################################################################
clean:
	rm -r -f /tmp/layer7-release-${VERSION}.tar.gz 

##############################################################################

