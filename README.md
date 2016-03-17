Build-Buddy
===========
Have you ever anxiously hovered over the the merge button of a pull request pondering its impact on unit test success, code coverage, build team impact, checkstyles, etc? :white_check_mark:

Do you feel like you're wasting time reviewing the trivial things instead of verifying the change has a sound approach and is implemented well? :white_check_mark:

Do you like seeing green status updates before signing off on things? :white_check_mark:

Well then you're going to like build-buddy! :smile:

Your build-buddy will fetch the base branch of a pull request, merge in the changes, run your build concurrently on both base and head inside 2 separate docker containers, comment on the PR with analysis, and update the status all while you focus on more important things.

To get started, run

```
gradle clean build
```

at the top level.

The assembly should now be all packed up for you in assembly/build/distributions.

Docker
------
Build buddy depends on docker containers build-buddy-7 and build-buddy-8 being available.  For build-buddy-7 to work properly, you'll need to compile with jdk 1.7.

The Dockerfiles are in docker-agent

In there (after building the project) just run:

```
docker -t build-buddy-7 -f Dockerfile7 .
docker -t build-buddy-8 -f Dockerfile8 .
```
