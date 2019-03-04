#!/bin/bash

cd ..
mvn clean install
cd websocket
cp ./target/websocket.war ./war/
