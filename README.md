[![Build Status](https://travis-ci.org/hhru/nuts-and-bolts.svg?branch=master)](https://travis-ci.org/hhru/nuts-and-bolts) 
[![codecov](https://codecov.io/gh/hhru/nuts-and-bolts/branch/master/graph/badge.svg)](https://codecov.io/gh/hhru/nuts-and-bolts)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=ru.hh.nab%3Anuts-and-bolts-parent&metric=alert_status)](https://sonarcloud.io/dashboard?id=ru.hh.nab%3Anuts-and-bolts-parent)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=ru.hh.nab%3Anuts-and-bolts-parent&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=ru.hh.nab%3Anuts-and-bolts-parent)

# Nuts-and-Bolts

Nuts-and-Bolts is a small Java application framework which is used at [hh.ru](https://hh.ru) to create micro-services.

> Please read [Disclaimer](https://github.com/hhru/nuts-and-bolts/wiki/Disclaimer) before using this framework.
# Versioning policy
`[major].[minor].[micro]`
* major version part - changes on dramatic technological platform changes
* minor version part - changes require a few lines of additional code to make it work
* micro version part - changes do not require additional code to make it work

update 4.17.1 -> 4.17.3 - should not require any additional code  
update 4.17.10 -> 4.18.0 - should require some additional code
> Absence of the code required for new mior version may not break an application compeletely. But it may cause partial degradation. Please, pay attention to check the app is working properly on such updates
## Main features

* Extended configuration of Jetty:
    * fail-fast (does not accept new connections if all threads are busy)
    * graceful shutdown (waits for current requests to be completed before shutdown)
    * built-in off-heap server cache
    * built-in monitoring
    * logging filters      
    
* JDBC:
    * using multiple data sources (master, slaves)
    * built-in monitoring of data source, connections and call stack before statement
    * timed-out statements
    * Embedded PostgreSQL for unit-testing
    
* Hibernate:
    * transaction support for multiple data sources
    * extended logging for queries (requestId, controller name)            

## Getting started
[Full example](https://github.com/hhru/nuts-and-bolts/tree/master/nab-example)

[How to release a new version](https://github.com/hhru/nuts-and-bolts/wiki/How-to-release-a-new-version)
