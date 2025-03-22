FROM ubuntu:24.04
RUN apt-get update \
   && apt-get install -y gnupg \
	 && apt-get install -y wget
RUN wget -qO - https://www.mongodb.org/static/pgp/server-7.0.asc | apt-key add -
RUN echo "deb [ arch=amd64,arm64 ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-7.0.list
RUN apt-get update \
   && apt-get install -y mongodb-org
RUN apt-get update \
   && apt-get install -y vim
