#!/bin/bash

echo -e "\033[0;32mDeploying updates to GitHub...\033[0m"

git add .

git commit -m "Update test.test pipe : `date`"

git push -u origin master
