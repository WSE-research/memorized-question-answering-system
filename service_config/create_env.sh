#!/bin/bash

# build Docker Image
while read line; do
    echo ${line::-1}
    echo $line
done < service_config/files/template.env