package be.cytomine.score

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Score", description = "")
class Score extends CytomineDomain implements Serializable {

    @RestApiObjectField(description = "The name of the score")
    String name

    static hasMany = [values : ScoreValue]

    static constraints = {
        name(blank: false, unique: true)
    }
    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "id"
        cache true
        values fetch: 'join'
    }

    void checkAlreadyExist() {
        Score.withNewSession {
            if (name) {
                Score scoreAlreadyExist = Score.findByName(name)
                if (scoreAlreadyExist && (scoreAlreadyExist.id != id)) {
                    throw new AlreadyExistException("Score " + name + " already exist!")
                }
            }
        }
    }

    /**
     * Insert JSON data into domain in param
     * @param domain Domain that must be filled
     * @param json JSON containing data
     * @return Domain with json data filled
     */
    static Score insertDataIntoDomain(def json, def domain = new Score()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.name = JSONUtils.getJSONAttrStr(json, 'name')
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['name'] = domain?.name
        returnArray['values'] = domain?.values?.sort {a,b -> a.index <=> b.index}?.collect{ScoreValue.getDataFromDomain(it)}
        if (!returnArray['values']) {
            returnArray['values'] = []
        }
        returnArray['kikou'] = 'lol'
        return returnArray
    }
}
