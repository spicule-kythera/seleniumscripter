FROM gitpod/workspace-full:latest

USER root

RUN apt-get update \
    && apt-get install -y openjdk-8-jdk \
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
    && apt-get clean && rm -rf /var/cache/apt/* && rm -rf /var/lib/apt/lists/* && rm -rf /tmp/*

FROM jetbrains/projector-idea-c

USER root
docker pull 
RUN docker pull jetbrains/projector-idea-c \
    && docker run --rm -p 8887:8887 -v /workspace/seleniumscripter/.jetbrains:/home/projector-user -it registry.jetbrains.team/projector-idea-c

 



