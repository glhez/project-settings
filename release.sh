#!/bin/bash

function getVersion {
        cat << EOF | xmllint --noent --shell pom.xml | grep content | cut -f2 -d=
setns pom=http://maven.apache.org/POM/4.0.0
xpath /pom:project/pom:version/text()
EOF
}

agentcount=`ps aux|grep gpg-agent|wc -l`

current_version=$(getVersion)
#major_version=$(expr $current_version : '\(.*\)\..*\..*\-SNAPSHOT')
major_version=$(expr $current_version : '\(.*\)\..*\-SNAPSHOT')
#minor_version=$(expr $current_version : '.*\.\(.*\)\..*\-SNAPSHOT')
minor_version=$(expr $current_version : '.*\.\(.*\)\-SNAPSHOT')
#bugfix_version=$(expr $current_version : '.*\..*\.\(.*\)-SNAPSHOT')

#CURRENT_VERSION=$major_version.$minor_version.$bugfix_version
#NEW_VERSION="$major_version.$minor_version.$(expr $bugfix_version + 1)-SNAPSHOT"
CURRENT_VERSION=$major_version.$minor_version
NEW_VERSION="$major_version.$(expr $minor_version + 1)-SNAPSHOT"

echo "Releasing eclipse-settings-maven-plugin version $CURRENT_VERSION
------------------------------------------------------------------------
This script assumes you are running on OS X, it hasn't been tested on
any other operating systems, and you can bet it won't work on Windows...

REQUIREMENTS:

 - a pure JDK 7 environment, JDK 8 or newer won't cut it
 - Maven 3.3.1 (older releases are b0rked, just don't bother)
 - gpg, gpg-agent and pinentry for signing
 - xmllint (usually in a libxml-utils package)
 "

#export JAVA_HOME=`/usr/libexec/java_home -v1.7`
echo "
Current Java version is: $(java -version 2>&1 | tail -n 2 | head -n 1)
"

printf "Performing: 'mvn clean' "
if ! mvn clean -B;
then
    echo "FAILED..."
fi

mkdir -p ~/eclipse-settings-plugin

echo "Releasing $CURRENT_VERSION" > ~/eclipse-settings-plugin/release.log

printf "Performing: 'mvn release:prepare -DreleaseVersion=$CURRENT_VERSION -DdevelopmentVersion=$NEW_VERSION'"
echo "Performing: 'mvn release:prepare -DreleaseVersion=$CURRENT_VERSION -DdevelopmentVersion=$NEW_VERSION'" >> ~/eclipse-settings-plugin/release.log
output="$(mvn release:prepare -DreleaseVersion=$CURRENT_VERSION -DdevelopmentVersion=$NEW_VERSION -B>> ~/eclipse-settings-plugin/release.log)"
if [[ $? -ne 0 ]]
then
    echo "FAILED...

See ~/eclipse-settings-plugin/release.log for more information.

Use the following command to revert your workspace to the original
state:

    git reset HEAD^ --hard
"
    exit 1
else
    echo DONE...
    echo "" >> ~/eclipse-settings-plugin/release.log
fi

printf "Performing: 'mvn release:perform' "
echo "mvn release:perform" >> ~/eclipse-settings-plugin/release.log
output="$(mvn release:perform -B >> ~/eclipse-settings-plugin/release.log)"
if [[ $? -ne 0 ]]
then
    echo "FAILED...

See ~/eclipse-settings-plugin/release.log for more information.

Use the following commands to revert your workspace to the original
state:

    git reset HEAD^ --hard
    git tag -d $CURRENT_VERSION
"
    exit 1
else
    echo DONE...
    echo "" >> ~/eclipse-settings-plugin/release.log
fi

echo "
Done!

All you need to do is check whether the release is OK, and then publish 
the artefacts by issuing the following command:

    git push
    git push --tags

Have fun!
"
