#!/bin/sh
mv deployment/*.war .
mv deployment/*.ear .

war=$(ls *.war 2> /dev/null | wc -l)
ear=$(ls *.ear 2> /dev/null | wc -l)
lib=$(ls deployment/lib/*  2> /dev/null | wc -l)
conf=$(ls deployment/config/* 2> /dev/null | wc -l)

if [ $lib -gt 0  ]
then
  for x in deployment/lib/*.*
  do
    mv $x /lib/
  done
fi

if [ $conf -gt 0  ]
then
  for y in deployment/config/*.*
  do
    mv $y /config/
  done
fi

if [ $war -gt 0 ]
then
  for i in *.war
  do
    #mv $i /deploy/
    fname=${i%.war}
    mkdir /deploy/$fname
    unzip -d /deploy/$fname $i
  done
fi

if [ $ear -gt 0  ]
then
  for j in *.ear
  do
    #mv $j /deploy/
    fname=${i%.ear}
    mkdir /deploy/$fname
    unzip -d /deploy/$fname $j
  done
fi

tail -f /dev/null
