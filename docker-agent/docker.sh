#!/bin/bash

docker build -t build-buddy-7 -f Dockerfile7 --build-arg buildguyUID=`id | sed 's/.*uid=\([[:digit:]]\+\).*/\1/g'` .
docker build -t build-buddy-8 -f Dockerfile8 --build-arg buildguyUID=`id | sed 's/.*uid=\([[:digit:]]\+\).*/\1/g'` .
