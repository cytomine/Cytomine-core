/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

dataSource {
    pooled = true

    /**
     * (String) The fully qualified Java class name of the JDBC driver to be used.
     * The driver has to be accessible from the same classloader as tomcat-jdbc.jar
     */
    driverClassName = "org.postgresql.Driver"

    /**
     * (String) The connection username to be passed to our JDBC driver to establish a connection.
     */
    username = "docker"
    password = "docker"

    dialect = org.hibernate.spatial.dialect.postgis.PostgisDialect

    properties {
        /**
         * (boolean) Register the pool with JMX or not. The default value is true.
         */
        jmxEnabled = true

        /**
         * (int)The initial number of connections that are created when the pool is started. Default value is 10
         */
        initialSize = 50

        /**
         * (int) The minimum number of established connections that should be kept in the pool at all times.
         * The connection pool can shrink below this number if validation queries fail.
         * Default value is derived from initialSize:10
         */
        minIdle = 50

        /**
         * (int) The maximum number of active connections that can be allocated from this pool at the same time.
         * The default value is 100
         */
        maxActive = 500

        /**
         * (int) The maximum number of connections that should be kept in the pool at all times.
         * Default value is maxActive:100 Idle connections are checked periodically (if enabled)
         */
        maxIdle = 500

        /**
         * (int) The maximum number of milliseconds that the pool will wait (when there are no available connections)
         * for a connection to be returned before throwing an exception. Default value is 30000 (30 seconds)
         */
        maxWait = 30000

        /**
         * (long) Time in milliseconds to keep this connection. This attribute works both when returning connection
         * and when borrowing connection.
         * When a connection is borrowed from the pool, the pool will check to see if the now - time-when-connected > maxAge
         * has been reached , and if so, it reconnects before borrow it.
         * When a connection is returned to the pool, the pool will check to see if the now - time-when-connected > maxAge
         * has been reached, and if so, it closes the connection rather than returning it to the pool.
         * The default value is 0, which implies that connections will be left open and no age check will be done upon
         * borrowing from the pool and returning the connection to the pool.
         */
        maxAge = 5 * 60000

        /**
         * (int) The number of milliseconds to sleep between runs of the idle connection validation/cleaner thread.
         * This value should not be set under 1 second.
         * It dictates how often we check for idle, abandoned connections, and how often we validate idle connections.
         * The default value is 5000 (5 seconds).
         */
        timeBetweenEvictionRunsMillis = 5000

        /**
         * (int) The minimum amount of time an object may sit idle in the pool before it is eligible for eviction.
         * The default value is 60000 (60 seconds).
         */
        minEvictableIdleTimeMillis = 60000
    }
}
hibernate {
//  cache.use_second_level_cache = true
//  cache.use_query_cache = true
//    cache.use_second_level_cache = false
//    cache.use_query_cache = false   // Changed to false to be enable the distributed cache
//    cache.provider_class = 'net.sf.ehcache.hibernate.SingletonEhCacheProvider'

    //CLUSTER
//    cache.provider_class = 'net.sf.ehcache.hibernate.EhCacheProvider'
//    cache.provider_class = 'net.sf.ehcache.hibernate.SingletonEhCacheProvider'
    // hibernate.cache.region.factory_class = 'net.sf.ehcache.hibernate.SingletonEhCacheRegionFactory'
    cache.use_second_level_cache = true
    cache.use_query_cache = false
    //cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    singleSession = true // configure OSIV singleSession mode
}
// environment specific settings
environments {
    scratch {
        dataSource {
            dbCreate = "update"
            url = "jdbc:postgresql://localhost:5432/cytomineempty"
            password = "postgres"
        }
    }
    development {
        dataSource {
            dbCreate = "update"
            url = "jdbc:postgresql://localhost:5432/docker"
            username = "docker"
            password = "docker"
        }
    }
    test {
        dataSource {
            //loggingSql = true
            dbCreate = "create"
            url = "jdbc:postgresql://localhost:5433/docker"
            username = "docker"
            password = "docker"
        }
    }
    production {
        dataSource {
            dbCreate = "update"
            url = "jdbc:postgresql://postgresql:5432/docker"
            username='docker'
            password='docker'
        }
    }
    perf {
        dataSource {
            //loggingSql = true
            dbCreate = "update"
            url = "jdbc:postgresql://localhost:5433/cytomineperf"
            password = "postgres"
        }
    }
    testrun {
        dataSource {
            //loggingSql = true
            dbCreate = "create"
            url = "jdbc:postgresql://localhost:5432/cytominetestrun"
            password = "postgres"
        }
    }
}
grails {
    mongo {
        host = "localhost"
        port = 27017
        databaseName = "cytomine"
        options {
            connectionsPerHost = 10 // The maximum number of connections allowed per host
            threadsAllowedToBlockForConnectionMultiplier = 5 // so it*connectionsPerHost threads can wait for a connection
        }
    }
}

environments {
    test {
        grails {
            mongo {
                port = 27018
            }
        }
    }
}
/*
environments {
    test {
        grails {
            mongo {
                databaseName = "cytominetest"
            }
        }
    }
} */
