#!/bin/sh

VERSION=$1
SED_SCRIPT="s|<version>.*</version><!-- VERSION -->|<version>${VERSION}</version><!-- VERSION -->|g"
POMS="pom.xml internal-service-nab/pom.xml"

sed -i -e "$SED_SCRIPT" -- $POMS
