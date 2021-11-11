FROM gitpod/workspace-full:latest

USER root

RUN apt-get update && \
    apt-get install -y curl \
    wget \
    openjdk-8-jdk

RUN sudo docker-up
    && docker pull registry.jetbrains.team/p/prj/containers/projector-idea-c \
    && run -d -p 8887:8887 registry.jetbrains.team/p/prj/containers/projector-idea-c \
    && run --rm -p 8887:8887 -v /workspace/seleniumscripter.jetbrains:/home/projector-user -it registry.jetbrains.team/p/prj/containers/projector-idea-c 

RUN apt-get update \
    && apt-get install  \
    && bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh && sdk install java 8.312.07.1-amzn && sdk install java 11.0.13.8.1-amzn" \
    && apt install -y unzip xvfb libxi6 libgconf-2-4 \
    && curl -sS -o - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add \
    && bash -c "echo 'deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main' >> /etc/apt/sources.list.d/google-chrome.list" \
    && apt -y update \
    && apt -y install google-chrome-stable \
    && wget https://chromedriver.storage.googleapis.com/94.0.4606.61/chromedriver_linux64.zip \
    && unzip chromedriver_linux64.zip \
    && mv chromedriver /usr/bin/chromedriver \
    && chown root:root /usr/bin/chromedriver \
    && chmod +x /usr/bin/chromedriver \
    && mv /usr/bin/chromedriver /workspace/seleniumscripter \
    && chown root:root /workspace/seleniumscripter \
    && chmod +x /workspace/seleniumscripter \
    && apt-get clean && rm -rf /var/cache/apt/* && rm -rf /var/lib/apt/lists/* && rm -rf /tmp/*

 



