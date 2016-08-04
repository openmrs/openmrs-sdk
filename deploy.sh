#!/usr/bin/env bash

# To make it all work:
# 1) generate personal access token. Go to https://github.com/settings/tokens
# 2) add three environment variables to travis CI:
#     - BINTRAY_USER
#     - BINTRAY_API_KEY - go to https://bintray.com/profile/edit -> API key
#     - GITHUB_API_KEY - the one generated above
# 3) Go to Bintray and create new openmrs-sdk package in maven repo

LAST_COMMIT = `git log --oneline -1 | grep "Releasing $TRAVIS_TAG"`
if [ -z "$LAST_COMMIT" ]
then 
echo "Setting version to $TRAVIS_TAG"
mvn versions:set -DnewVersion=$TRAVIS_TAG
git tag -d $TRAVIS_TAG
git push --quiet "https://$GITHUB_API_KEY@github.com/$TRAVIS_REPO_SLUG.git" :refs/tags/$TRAVIS_TAG > /dev/null 2>&1
git add -u
git commit -m "Releasing $TRAVIS_TAG"
git tag $TRAVIS_TAG
git push --quiet "https://$GITHUB_API_KEY@github.com/$TRAVIS_REPO_SLUG.git" $TRAVIS_TAG > /dev/null 2>&1
else
echo "Releasing version $TRAVIS_TAG"
mvn deploy -Pbintray
fi
