# build a docker container using the ssg RPM and with the latest version of CentOS 6 as a base
FROM centos:centos6
MAINTAINER Heinrich Butow <heinrich.butow@ca.com>

# the list of ports that we will expose between containers
# this should only contain the ports that gateway nodes need to communicate with each other
# container ports are made available on the host via e.g. "docker run -p 8080:8080 -p 8443:8443"
#EXPOSE 2124 8080 8443 9443

# who the RUN commands listed below should run as
# if commands need to run as someone else, add an additional USER stanza just above the command you add
USER root

# inject the ssg and ssg-appliance RPMs into the filesystem of the container
# the RPM is expected to be in the same directory as this Dockerfile
RUN yum install -y sudo perl passwd mysql util-linux-ng wget tar
ADD ssg-*.noarch.rpm /root/
ADD ssg-appliance*.x86_64.rpm /root/
RUN rpm -ivh /root/ssg-*.noarch.rpm /root/ssg-appliance-*.x86_64.rpm
RUN rm /root/ssg-*.rpm

# WARNING: this is a hack to get around the process controller not starting (trying to do something like "su gateway -c /bin/echo hello" fails)
RUN rm -f /etc/security/limits.d/*

# WARNING: this is probably a hack
# it seems we need this so that node.properties is successfully created when provisioning
RUN chmod 770 /opt/SecureSpan/Gateway/node/default/etc/conf/

# some RPMs that are useful during development
RUN yum install -y which telnet nc strace

# include envconsul in the build to support setting environment variable from Consul
RUN wget -O - https://github.com/hashicorp/envconsul/releases/download/v0.5.0/envconsul_0.5.0_linux_amd64.tar.gz | tar xzf - -O envconsul_0.5.0_linux_amd64/envconsul > /usr/local/bin/envconsul && chmod 755 /usr/local/bin/envconsul

# include etcdctl in the build to support setting environment variables from etcd
RUN wget -O - https://github.com/coreos/etcd/releases/download/v2.2.2/etcd-v2.2.2-linux-amd64.tar.gz | tar xzf - -O etcd-v2.2.2-linux-amd64/etcdctl > /usr/local/bin/etcdctl && chmod 755 /usr/local/bin/etcdctl

# set the timezone
RUN echo "ZONE=\"America/Vancouver\"" > /etc/sysconfig/clock
RUN ln -sf /usr/share/zoneinfo/America/Vancouver /etc/localtime

# add the provisioning script
ADD gateway-provision-and-run.sh /root/
RUN chmod 700 /root/gateway-provision-and-run.sh

# if you need to add more changes, put them at the end
# future builds take less time that way (because of docker creating intermediate images)

# the command to run when starting the container
CMD /root/gateway-provision-and-run.sh
