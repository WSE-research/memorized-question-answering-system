#!/bin/bash

ls -all

cd ../

# build Docker Image
if ! mvn clean package -DskipTests;
then
  exit 1
fi