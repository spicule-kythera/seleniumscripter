FROM jetbrains/projector-idea-c
RUN sudo mkdir -p .jetbrains; sudo touch .jetbrains/.gitkeep; sudo chown -R 1000:1000 .jetbrains
RUN sudo chown -R 1000:gitpod .
RUN sudo chmod g+w -R .
RUN sudo docker run --rm -p 8887:8887 -v /workspace/seleniumscripter/.jetbrains:/home/projector-user -it registry.jetbrains.team/p/prj/containers/projector-idea-c



