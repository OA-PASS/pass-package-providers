#
# Produces an image with the J10P package provider and dependencies in /lib.
#
# Contents of /lib must be copied to <WORKDIR>/BOOT-INF/lib, and a Deposit Services runtime configuration
# (typically repositories.json) must be provided in order to make a viable image.
#
# See provider-integration/src/main/docker/Dockerfile
#
FROM ${docker.image.deposit-services-core.name}
RUN mkdir /provider-with-dependencies
ADD target/jscholarship-package-provider-${project.version}.jar /provider-with-dependencies
ADD target/dependencies/ /provider-with-dependencies