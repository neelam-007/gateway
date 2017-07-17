# UneasyRooster
Repository for core Gateway code

# Build Status
[![Build Status](http://apim-teamcity.l7tech.com:8111/app/rest/builds/buildType:(id:ApiGateway_IntegrationTrunk_01Source_UneasyRooster_Default)/statusIcon)](https://apim-teamcity.l7tech.com:8443/project.html?projectId=ApiGateway_IntegrationTrunk_01Source_UneasyRooster)

# Contributing
Contributing to the Gateway is done through Pull Requests. In order to submit changes create a pull request targeting the `develop` branch.

## Pull Request Validations
The pull request title must begin with the referenced Rally issue. If there is no referenced rally issue the title should begin with `NORALLY`.
For example:
* DE304951: Inconsistent behavior of the Assertion "Request Message size"
* NORALLY test pr build, do not merge 

The [UneasyRooster PR Validation](https://apim-teamcity.l7tech.com:8443/viewType.html?buildTypeId=ApiGateway_Utilities_PullRequestValidation_UneasyRoosterPrValidation)
build is used to verify that any pull request targeting develop has it's referenced rally issue in an engineering completed state before the pull request can be closed. 
Engineering completed state means the following:
* Defects must have the defect state `Closed`
* User Stories must have the schedule state `Accepted` or `Released`
* Features must have the state `Done` or `Shipped`

## Rerun a Pull Request Validation
These are instructions if you need to rerun a pull request validation after a Rally Issue has been closed.

1) Go to the [UneasyRooster PR Validation](https://apim-teamcity.l7tech.com:8443/viewType.html?buildTypeId=ApiGateway_Utilities_PullRequestValidation_UneasyRoosterPrValidation) build.
2) Click the `...` button beside the `run` button.
3) Select the pull request number in the `Build branch` dropdown
4) Click `Run Build`

## Validation Script Repository
For more information see the [Gateway-GitHub-Utils](https://github-isl-01.ca.com/APIM-Gateway/Gateway-GitHub-Utils) repository that contains the Pull Request validation script. 