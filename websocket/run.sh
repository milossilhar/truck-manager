#!/bin/bash

cd ..
mvn clean install
cd websocket
mvn install
mvn cargo:run