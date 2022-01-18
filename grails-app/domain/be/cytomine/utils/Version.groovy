package be.cytomine.utils

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

import groovy.util.logging.Log

/**
 * Cytomine version history
 */
@Log
class Version {

    Date deployed
    Integer major
    Integer minor
    Integer patch

    static mapping = {
        version false
        id generator: 'identity', column: 'nid'
    }

    static constraints = {
        major (nullable: true)
        minor (nullable: true)
        patch (nullable: true)
    }

    static Version setCurrentVersion(String semantic) {
        Version actual = getLastVersion()

        Integer major = Integer.parseInt(semantic.split("\\.")[0])
        Integer minor = Integer.parseInt(semantic.split("\\.")[1])
        Integer patch = Integer.parseInt(semantic.split("\\.")[2])
        Version version = new Version(deployed: new Date(), major:major, minor:minor, patch:patch)
        if(major == 0 && minor == 0 && patch == 0) throw new NumberFormatException()

        log.info "Last version was ${actual}. Actual version will be $semantic ($version)"

        if(actual && !isOlderVersion(version)) {
            log.info "version $version don't need to be saved"
            return actual
        } else {
            log.info "New version detected"

            version.save(flush:true,failOnError: true)
            return version
        }
    }

    static boolean isOlderVersion(Version version) {
        Version actual = getLastVersion()
        log.info "Check is older $actual=actual and compared=$version"
        if(actual) {
            return (actual.major < version.major || (actual.major == version.major && actual.minor < version.minor)
                    || (actual.major == version.major && actual.minor == version.minor && actual.patch <= version.patch))
        } else return true
    }

    static Version getLastVersion() {
        def lastInList = Version.list(max:1,sort:"deployed",order:"desc")
        return lastInList.isEmpty()? null : lastInList.get(0)
    }

    String toString() {
        return "version ${major}.${minor}.${patch} (deployed ${deployed})"
    }
}
