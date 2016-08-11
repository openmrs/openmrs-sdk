#!/usr/bin/env bash

# To make it all work:
# 1) generate personal access token. Go to https://github.com/settings/tokens
# 2) add three environment variables to travis CI:
#     - BINTRAY_USER
#     - BINTRAY_API_KEY - go to https://bintray.com/profile/edit -> API key
#     - GITHUB_API_KEY - the one generated above
# 3) Go to Bintray and create new openmrs-sdk package in maven repo

if [[ "$TRAVIS_TAG" == *_* ]]
then 
VERSIONS=(${TRAVIS_TAG//_/ })
NEW_VERSION=${VERSIONS[0]}
NEW_DEV_VERSION=${VERSIONS[1]}
echo "Setting version to $NEW_VERSION"
mvn versions:set -DnewVersion=$NEW_VERSION
git add -u
git commit -m "Releasing $NEW_VERSION"
git tag $NEW_VERSION
git push --quiet "https://$GITHUB_API_KEY@github.com/$TRAVIS_REPO_SLUG.git" $NEW_VERSION > /dev/null 2>&1
echo "Setting development version to $NEW_DEV_VERSION"
mvn versions:set -DnewVersion=$NEW_DEV_VERSION
git add -u
git commit -m "Increasing development version to $NEW_DEV_VERSION"
git push --quiet "https://$GITHUB_API_KEY@github.com/$TRAVIS_REPO_SLUG.git" HEAD:master > /dev/null 2>&1
else
echo "Releasing version $TRAVIS_TAG"
mvn deploy -Pbintray -DskipTests
fi
