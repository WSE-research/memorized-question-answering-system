#!/bin/bash

# replace secrets
if [ -n "$STARDOG_TRIPLESTORE_DATABASE" ] && [ -n "$STARDOG_TRIPLESTORE_USERNAME" ] && [ -n "$STARDOG_TRIPLESTORE_PASSWORD" ]
then
  echo "Stardog will be used."
  sed -i "s/SECRETS_STARDOG_TRIPLESTORE_DATABASE/$STARDOG_TRIPLESTORE_DATABASE/g" ./service_config/files/.env
  sed -i "s/SECRETS_STARDOG_TRIPLESTORE_USERNAME/$STARDOG_TRIPLESTORE_USERNAME/g" ./service_config/files/.env
  sed -i "s/SECRETS_STARDOG_TRIPLESTORE_PASSWORD/$STARDOG_TRIPLESTORE_PASSWORD/g" ./service_config/files/.env
else
  echo "STARDOG_TRIPLESTORE_DATABASE, STARDOG_TRIPLESTORE_USERNAME or STARDOG_TRIPLESTORE_PASSWORD is not set. Stardog will not be used."
fi

if [ -n "$VIRTUOSO_TRIPLESTORE_GRAPH" ] && [ -n "$VIRTUOSO_TRIPLESTORE_USERNAME" ] && [ -n "$VIRTUOSO_TRIPLESTORE_PASSWORD" ]
then
  echo "Virtuoso will be used."
  sed -i "s/SECRETS_VIRTUOSO_TRIPLESTORE_GRAPH/$VIRTUOSO_TRIPLESTORE_GRAPH/g" ./service_config/files/.env
  sed -i "s/SECRETS_VIRTUOSO_TRIPLESTORE_USERNAME/$VIRTUOSO_TRIPLESTORE_USERNAME/g" ./service_config/files/.env
  sed -i "s/SECRETS_VIRTUOSO_TRIPLESTORE_PASSWORD/$VIRTUOSO_TRIPLESTORE_PASSWORD/g" ./service_config/files/.env
else
  echo "VIRTUOSO_TRIPLESTORE_GRAPH, VIRTUOSO_TRIPLESTORE_USERNAME or VIRTUOSO_TRIPLESTORE_PASSWORD is not set. Virtuoso will not be used."
fi


# build Docker Image
if ! mvn clean package -DskipTests;
then
  exit 1
fi