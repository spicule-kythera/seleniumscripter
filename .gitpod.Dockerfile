FROM gitpod/workspace-full

RUN sudo docker pull registry.jetbrains.team/p/prj/containers/projector-idea-c

RUN sudo docker run --rm -p 8887:8887 -v /workspace/template-jetbrains-intellij-idea/.jetbrains:/home/projector-user -it registry.jetbrains.team/p/prj/containers/projector-idea-c      

RUN wget https://github.com/mozilla/geckodriver/releases/download/v0.24.0/geckodriver-v0.24.0-linux64.tar.gz tar -xvzf geckodriver* chmod +x geckodriver
