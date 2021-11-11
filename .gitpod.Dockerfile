FROM gitpod/workspace-full:latest

USER root

RUN apt install python3 python3-pip -y \
    && apt install python3-cryptography -y \
    && python3 -m pip install -U pip \
    && apt install less libxext6 libxrender1 libxtst6 libfreetype6 libxi6 -y \
    && pip3 install projector-installer --user \
    && source ~/.profile

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

 



