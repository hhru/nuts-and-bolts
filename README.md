# Nuts-and-Bolts

Nuts-and-Bolts is a set of small Java libraries which are used in [hh.ru](https://hh.ru) to create micro-services.

## Main features

* Extended configuration of Jetty:
    * fail-fast (does not accept new connections if all threads are busy)
    * built-in off-heap server cache
    * built-in monitoring
    * logging filters
    * websocket support
    
* JDBC:
    * using multiple data sources (master, slaves)
    * built-in monitoring of data source, connections and call stack before statement
    * timed-out statements
    * Embedded PostgreSQL for unit-testing
    
* Hibernate:
    * transaction support for multiple data sources
    * extended logging for queries (requestId, controller name)            

* Kafka-integration:
    * consumers/producers configuration

## Dependency management policy
All crucial dependencies in any NaB module have to be managed with [parent pom dependency management](https://github.com/hhru/nuts-and-bolts/blob/master/pom.xml#L49-L55)
to be able to provide these versions into app via pom import and guarantee version consistency with NaB required versions
[How to release a new version](https://github.com/hhru/nuts-and-bolts/wiki/How-to-release-a-new-version)

