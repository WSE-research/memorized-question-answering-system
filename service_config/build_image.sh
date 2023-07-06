#!/bin/bash

# replace secrets
if [ -z "$QADO_TRIPLESTORE_URL" ]
then
  echo "QADO_TRIPLESTORE_URL is not set. Check your secrets."
  exit
else
  sed -i "s/SECRETS_QADO_TRIPLESTORE_URL/$QADO_TRIPLESTORE_URL/g" ./service_config/files/.env
fi

if [ -z "$QADO_TRIPLESTORE_DATABASE" ]
then
  echo "QADO_TRIPLESTORE_DATABASE is not set. Check your secrets."
  exit
else
  sed -i "s/SECRETS_QADO_TRIPLESTORE_DATABASE/$QADO_TRIPLESTORE_DATABASE/g" ./service_config/files/.env
fi

if [ -z "$QADO_TRIPLESTORE_USERNAME" ]
then
  echo "QADO_TRIPLESTORE_USERNAME is not set. Check your secrets."
  exit
else
  sed -i "s/SECRETS_QADO_TRIPLESTORE_USERNAME/$QADO_TRIPLESTORE_USERNAME/g" ./service_config/files/.env
fi

if [ -z "$QADO_TRIPLESTORE_PASSWORD" ]
then
  echo "QADO_TRIPLESTORE_PASSWORD is not set. Check your secrets."
  exit
else
  sed -i "s/SECRETS_QADO_TRIPLESTORE_PASSWORD/$QADO_TRIPLESTORE_PASSWORD/g" ./service_config/files/.env
fi


# build Docker Image
if ! mvn clean package -DskipTests;
then
  exit 1
fi