FROM registry.jetbrains.team/p/prj/containers/projector-idea-u

USER root

RUN apt update && apt install -y wget curl openjdk-1.8-jdk && \
wget https://downloads.lightbend.com/scala/2.12.15/scala-2.12.15.deb && \
dpkg -i scala-2.12.15.deb && curl -O https://dl.google.com/go/go1.12.7.linux-amd64.tar.gz && \
tar xvf go1.12.7.linux-amd64.tar.gz && chown -R root:root ./go && mv go /usr/local

USER projector-user
