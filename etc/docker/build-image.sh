#!/bin/bash

# this is the name of the image that build.sh produces
IMAGE_NAME='teamcity/ssg'

# Docker Registry settings
REGISTRY_HOST='apim-dr.l7tech.com'
REGISTRY_USER='tinder'
REGISTRY_PASS='7layer'
REGISTRY_EMAIL='noreply@l7tech.com'

removeExistingImages() {
	echo "Removing existing Docker images from the build agent"
	echo "Existing images:"
	docker images
	if [ $(docker images -q | wc -l) -eq 0 ]; then
		echo "Done removing existing Docker images from the build agent (no images found)"
		return
	fi
	docker images -q | uniq | xargs docker rmi -f 
	if [ $(docker images -q | wc -l) -gt 0 ]; then
		echo "Failed to remove the existing Docker images from the build agent"
		exit 1
	fi
	echo "Done removing existing Docker images from the build agent"
}

buildTheImage() {
	echo "Building the image"
	pushd ../.. &> /dev/null
	./build.sh makedocker docker
	popd &> /dev/null
	echo "Done building the image"
}

exportTheImage() {
	echo "Exporting the Docker image"
	
	IMAGE_TAG=`docker images | grep "^$IMAGE_NAME[[:space:]]\+" | awk '{ print $2 }'`
	IMAGE_ID=`docker images | grep "^$IMAGE_NAME[[:space:]]\+" | awk '{ print $3 }'`
	echo "DEBUG: IMAGE_NAME=\"$IMAGE_NAME\""
	echo "DEBUG: IMAGE_TAG=\"$IMAGE_TAG\""
	echo "DEBUG: IMAGE_ID=\"$IMAGE_ID\""
	if [ "$IMAGE_NAME" == "" ] || [ "$IMAGE_TAG" == "" ] || [ "$IMAGE_ID" == "" ]; then
		echo "ERROR: some of the image values are unset"
		exit 1
	fi
	
	FIXED_IMAGE_NAME=`echo "$IMAGE_NAME" | tr '/' '-'`
	echo "DEBUG: FIXED_IMAGE_NAME=\"$FIXED_IMAGE_NAME\""
	TARBALL_PATH="./$FIXED_IMAGE_NAME-$IMAGE_TAG.tar.gz"
	echo "DEBUG: TARBALL_PATH=\"$TARBALL_PATH\""
	if [ "$TARBALL_PATH" == "" ]; then
		echo "ERROR: could not generate a proper tarball path"
		exit 1
	fi
	docker save "$IMAGE_NAME:$IMAGE_TAG" | pigz - > "$TARBALL_PATH"
	if [ $? -ne 0 ]; then
		echo "ERROR: exporting the Docker image failed"
		exit 1
	fi
	echo "Done exporting the Docker image"
}

squashTheImage() {
	echo "Squashing the image"
	cat "$TARBALL_PATH" | gunzip - | sudo /home/hbutow/bin/docker-squash -verbose -t "$REGISTRY_HOST/$IMAGE_NAME:$IMAGE_TAG" | pigz - > "./squashed.tar.gz"
	if [ $? -ne 0 ]; then
		echo "ERROR: failed to squash the image"
		exit 1
	fi
	echo "Loading the squashed tarball"
	cat "./squashed.tar.gz" | gunzip - | docker load
	if [ $? -ne 0 ]; then
		echo "ERROR: failed to load the squashed image"
		exit 1
	fi
	echo "Removing the squashed tarball"
	rm "./squashed.tar.gz" &> /dev/null
	if [ $? -ne 0 ]; then
		echo "ERROR: failed to remove the squashed tarball"
		exit 1
	fi
	echo "Saving the squashed image"
	# this may seem redundant when we already have squashed.tar.gz above but this is a workaround for a bug in docker-squash
	# where it doesn't include the base image layer in the tarball it produces
	docker save "$REGISTRY_HOST/$IMAGE_NAME:$IMAGE_TAG" | pigz - > "$TARBALL_PATH"
	if [ $? -ne 0 ]; then
		echo "ERROR: failed to save the squashed image"
		exit 1
	fi
	echo "Done squashing the image"
}

pushTheImage() {
	echo "Pushing the image to the Docker Registry"
	docker login -u "$REGISTRY_USER" -p "$REGISTRY_PASS" -e "$REGISTRY_EMAIL" "$REGISTRY_HOST"
	if [ $? -ne 0 ]; then
		echo "ERROR: could not login to Docker Registry"
		exit 1
	fi
	docker push "$REGISTRY_HOST/$IMAGE_NAME:$IMAGE_TAG"
	if [ $? -ne 0 ]; then
		echo "ERROR: push to private registry failed"
		exit 1
	fi
	echo "Done pushing the image to the Docker Registry"
}


removeExistingImages
buildTheImage
exportTheImage
squashTheImage
pushTheImage
removeExistingImages

