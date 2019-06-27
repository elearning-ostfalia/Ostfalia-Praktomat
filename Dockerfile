# FROM python:2.7.9
# FROM debian:jessie
# we need ubuntu in order to install Oracle Java 
# (I did not succedd with python:2.7.9 which is probabely based on debian)
FROM ubuntu:xenial

MAINTAINER Ostfalia

ENV PYTHONUNBUFFERED 1

# for praktomat itself
RUN apt-get update && apt-get install -y locales && locale-gen de_DE.UTF-8

# change locale to somethin UTF-8 
# RUN apt-get install -y locales && locale-gen de_DE.UTF-8 
ENV LANG de_DE.UTF-8
ENV LC_ALL de_DE.UTF-8

RUN apt-get update && apt-get install -y swig libxml2-dev libxslt1-dev python2.7 python-dev python-pip libpq-dev locales wget cron


# Java:
# install OpenJDK (only needed if you want to run Java Compiler checker)
# install checkstyle (only needed if you want to run Checkstyle checker)
RUN apt-get update && apt-get install -y default-jdk checkstyle


# && apt-get autoremove -y


 
RUN mkdir /praktomat
WORKDIR /praktomat
ADD requirements.txt /praktomat/
RUN pip install --upgrade pip 
RUN pip install -r requirements.txt --ignore-installed --force-reinstall --upgrade --no-cache-dir && pip install --upgrade chardet && pip install gunicorn[eventlet]
# gunicorn is used for async processing


ADD . /praktomat

RUN mkdir -p /praktomat/upload


# COPY src/ src/
# COPY extra extra/
# COPY media media/

# remove staticfiles, otherwise we get problems with collectstatic later on
RUN pip uninstall staticfiles 


# clean packages
RUN apt-get clean
RUN rm -rf /var/cache/apt/archives/* /var/lib/apt/lists/*

# create cron job for deleting temporary files
COPY cron.conf /etc/cron.d/praktomat-cron
#RUN chmod 0644 /etc/cron.d/praktomat-cron
RUN crontab /etc/cron.d/praktomat-cron

# run entrypoint.sh
ENTRYPOINT ["/praktomat/src/entrypoint.sh"]


