#!/bin/sh

VERSION=$1
SED_SCRIPT="s|<version>.*</version><!-- VERSION -->|<version>${VERSION}</version><!-- VERSION -->|g"
POMS="pom.xml nuts-and-bolts/pom.xml example-app/pom.xml"

for p in $POMS; do
  sed -e "$SED_SCRIPT" $p > ${p}.tmp
  mv ${p}.tmp $p
done
