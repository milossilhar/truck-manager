#!/bin/bash

docker run -p 8080:8080 -v "$(pwd)"/war:'/usr/local/tomcat/webapps' tomcat
