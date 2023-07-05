#!/bin/bash

# build Docker Image
echo ${{ secrets.QADO_TRIPLESTORE_URL }}
while read line; do

    echo ${{ "secrets." echo ${line::-1} }}
    echo $line
done < service_config/files/template.env