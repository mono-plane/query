#!/bin/sh

set -o xtrace

rm -rf modules/system/layers/base/org/wildfly/extension/presto/
rm -f standalone/configuration/standalone-presto.xml
rm -f domain/configuration/presto-domain.xml
rm -f domain/configuration/presto-host.xml