[![Build Status](https://travis-ci.org/hhru/nuts-and-bolts.svg?branch=master)](https://travis-ci.org/hhru/nuts-and-bolts) [![codecov](https://codecov.io/gh/hhru/nuts-and-bolts/branch/master/graph/badge.svg)](https://codecov.io/gh/hhru/nuts-and-bolts)

# Nuts-and-Bolts

> Attention: **We provide no gurantee for support, backward compatibility and licence policy stability for the project in the future. We do not 
recommend to use it in projects, which are not related to HeadHunter. Use at your own risk**

> Внимание: **Мы не гарантируем поддержку, обратную совместимость или неизменность лицензионной политики для этого проекта в будущем. Мы не 
рекомендуем испозовать его в проектах, не связанных с HeadHunter. Используйте на свой страх и риск**  

Nuts-and-Bolts is a small Java application framework which is used at [hh.ru](https://hh.ru) to create micro-services.

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

```java
package com.example;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import ru.hh.nab.core.Launcher;

public class ExampleMain extends Launcher {

  public static void main(String[] args) {
    doMain(new AnnotationConfigApplicationContext(ExampleConfig.class));
  }
}
```

[Full example](https://github.com/hhru/nuts-and-bolts/tree/master/nab-example)

[How to release new version](https://github.com/hhru/nuts-and-bolts/wiki/How-to-release-new-version)
