#!/bin/bash
mvn clean package
docker build -t ioswarm/oers --rm .
