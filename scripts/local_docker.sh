#!/usr/bin/env bash

echo $PWD
eval $(minikube docker-env)
docker build -t testframework:latest .
