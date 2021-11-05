FROM gitpod/workspace-full

FROM jetbrains/projector-idea-c

RUN docker run --rm -p 8887:8887 -it registry.jetbrains.team/p/prj/containers/projector-idea-c      

RUN wget https://github.com/mozilla/geckodriver/releases/download/v0.24.0/geckodriver-v0.24.0-linux64.tar.gz tar -xvzf geckodriver* chmod +x geckodriver
