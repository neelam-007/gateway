=============================================================================
 Gateway Management Client Examples
=============================================================================

Create Service
==============
To create a new service use a command such as:

  gatewayManagementClient.sh localhost create -type service \
  -inFile service_resource.xml -username admin -password password

as the "-outFile" option is not specified the resource will be written to the
console.


Enumerate Services
==================
To enumerate (list) services use a command such as:

  gatewayManagementClient.sh localhost enumerate -type service \
  -username admin -password password


