FROM gitpod/workspace-full:latest

USER root

RUN sudo apt install python3 python3-pip -y \
    && sudo apt install python3-cryptography -y \
    && python3 -m pip install -U pip \
    && sudo apt install less libxext6 libxrender1 libxtst6 libfreetype6 libxi6 -y \
    && pip3 install projector-installer --user \
    && source ~/.profile


 



