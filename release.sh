#!/bin/sh

function getVersion {
        cat << EOF | xmllint --noent --shell pom.xml | grep content | cut -f2 -d=
setns pom=http://maven.apache.org/POM/4.0.0
xpath /pom:project/pom:version/text()
EOF
}

agentcount=`ps aux|grep gpg-agent|wc -l`

current_version=$(getVersion)
major_version=$(expr $current_version : '\(.*\)\..*\..*\-SNAPSHOT')
minor_version=$(expr $current_version : '.*\.\(.*\)\..*\-SNAPSHOT')
bugfix_version=$(expr $current_version : '.*\..*\.\(.*\)-SNAPSHOT')

CURRENT_VERSION=$major_version.$minor_version.$bugfix_version.$(date +"%Y%m%d%H%M")
NEW_VERSION="$major_version.$minor_version.$(expr $bugfix_version + 1)-SNAPSHOT"

echo "Releasing eclipse-settings-maven-plugin version $CURRENT_VERSION
------------------------------------------------------------------------
This script assumes you are running on OS X, it hasn't been tested on
any other operating systems, and you can bet it won't work on Windows...

REQUIREMENTS:

 - a pure JDK 7 environment, JDK 8 or newer won't cut it
 - Maven 3.3.1 (older releases are b0rked, just don't bother)
 - gpg, gpg-agent and pinentry for signing"

export JAVA_HOME=`/usr/libexec/java_home -v1.7`
echo "
Current Java version is: $(java -version 2>&1 | tail -n 2 | head -n 1)
"

printf "Performing: 'mvn clean' "
if ! mvn clean -B;
then
    echo "FAILED..."
fi

mkdir -p target

echo "Releasing $CURRENT_VERSION" > target/release.log

printf "Performing: 'mvn versions:set -DnewVersion=$CURRENT_VERSION' "
if ! mvn versions:set -DnewVersion=$CURRENT_VERSION -B>> target/release.log ;
then
    echo "FAILED...

See target/release.log for more information.

Use the following command to revert your workspace to the original
state:

    git checkout .
"
    exit 1
else
    echo DONE...
    echo "" >> target/release.log
fi

printf "Performing: 'git commit -a -m \"Preparing to release $CURRENT_VERSION\"' "
echo "git commit -a -m \"Preparing to release $CURRENT_VERSION\"" >> target/release.log
if ! git commit -a -m "Preparing to release $CURRENT_VERSION" >> target/release.log ;
then
    echo "FAILED...

See target/release.log for more information.

Use the following command to revert your workspace to the original
state:

    git checkout .
"
    exit 1
else
    echo DONE...
    echo "" >> target/release.log
fi

printf "Performing: 'mvn package' "
echo "mvn package" >> target/release.log
if ! mvn package -B >> target/release.log ;
then
    echo "FAILED...

See target/release.log for more information.

Use the following commands to revert your workspace to the original
state:

    git reset HEAD^ --hard
"
    exit 1
else
    echo DONE...
    echo "" >> target/release.log
fi

printf "Performing: 'git tag $CURRENT_VERSION' "
echo "git tag $CURRENT_VERSION" >> target/release.log
if ! git tag $CURRENT_VERSION >> target/release.log;
then
    echo "FAILED...

See target/release.log for more information.

Use the following commands to revert your workspace to the original
state:

    git reset HEAD^ --hard

Check if the tag was created:

    git tag --list
"
    exit 1
else
    echo DONE...
    echo "" >> target/release.log
fi

printf "Performing: 'mvn versions:set -DnewVersion=$NEW_VERSION' "
echo "mvn versions:set -DnewVersion=$NEW_VERSION" >> target/release.log
if ! mvn versions:set -DnewVersion=$NEW_VERSION -B >> target/release.log ;
then
    echo "FAILED...

See target/release.log for more information.

Use the following commands to revert your workspace to the original
state:

    git reset HEAD^ --hard
    git tag -d $CURRENT_VERSION
"
    exit 1
else
    echo DONE...
    echo "" >> target/release.log
fi

printf "Performing: 'git commit -a -m \"Preparing for new development\"' "
echo "git commit -a -m \"Preparing for new development\"" >> target/release.log
if ! git commit -a -m "Preparing for new development" >> target/release.log ;
then
    echo "FAILED...

See target/release.log for more information.

Use the following commands to revert your workspace to the original
state:

    git reset HEAD^ --hard
    git tag -d $CURRENT_VERSION
"
    exit 1
else
    echo DONE...
    echo "" >> target/release.log
fi

echo "
Done!

All you need to do is check whether the release is OK, and then publish 
the artefacts by issuing the following command:

    git push
    git push --tags

Have fun!
"
