#!/bin/sh

set -o xtrace

rm -rf modules/system/layers/base/org/wildfly/extension/monoplane/query/
rm -f standalone/configuration/standalone-query.xml
rm -f domain/configuration/query-domain.xml
rm -f domain/configuration/query-host.xml