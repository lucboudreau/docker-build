from cogniteev/oracle-java:java7

# make the "en_US.UTF-8" locale so postgres will be utf-8 enabled by default
RUN apt-get update && apt-get install -y locales && rm -rf /var/lib/apt/lists/* \
  && localedef -i en_US -c -f UTF-8 -A /usr/share/locale/locale.alias en_US.UTF-8
  ENV LANG en_US.utf8

RUN apt-get update
RUN apt-get install -y git curl python python-jinja2 libswt-gtk-3-jni libswt-gtk-3-java

RUN adduser --disabled-password --gecos '' buildguy
RUN adduser --disabled-password --gecos '' gitguy

RUN sudo -u buildguy bash -c "cd /home/buildguy && curl http://www.apache.org/dist/maven/maven-3/3.3.3/binaries/apache-maven-3.3.3-bin.tar.gz | tar -xz"
RUN sudo -u buildguy bash -c "cd /home/buildguy && curl https://www.apache.org/dist/ant/binaries/apache-ant-1.9.6-bin.tar.gz | tar -xz"

RUN echo 'export JAVA_HOME=/usr/lib/jvm/java-7-oracle' >> ~buildguy/.profile
RUN echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~buildguy/.profile
RUN echo 'export PATH="/home/buildguy/apache-ant-1.9.6/bin:$PATH"' >> ~buildguy/.profile
RUN echo 'export PATH="/home/buildguy/apache-maven-3.3.3/bin:$PATH"' >> ~buildguy/.profile
RUN echo 'export ANT_OPTS="-XX:MaxPermSize=256m"' >> ~buildguy/.profile
RUN mkdir ~buildguy/.ssh/

ADD scripts /scripts/
RUN chmod a+x /scripts/*.py

ADD settings.xml /home/buildguy/.m2/
RUN mkdir /home/buildguy/aggregate-metrics
RUN mkdir /home/buildguy/.ivy2

RUN chown -R gitguy:gitguy ~gitguy/
RUN chown -R buildguy:buildguy ~buildguy/

ENTRYPOINT ["python", "/scripts/start.py"]
