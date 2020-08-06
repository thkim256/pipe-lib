#!/bin/bash
usage()
{
  echo "Usage: build.sh version"
}

if [ -z "$1" ]
then
  usage
  exit 1
fi
VERSION=$1

# workspace
mkdir -p accordion-lib && 
  cp -r `ls | grep -Ev 'build.sh|accordion-lib'` accordion-lib

# git setting
cd accordion-lib
git init . &&
  git add . && 
  git commit -m "Accordion v$VERSION"

# zip
cd ..
zip -r accordion-lib-$VERSION.zip accordion-lib &&
  rm -rf accordion-lib
