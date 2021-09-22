package be.cytomine.score

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Score", description = "")
class ScoreValue extends CytomineDomain implements Serializable, Comparable {

    @RestApiObjectField(description = "The score")
    Score score

    @RestApiObjectField(description = "The possible value of the score")
    String value

    @RestApiObjectField(description = "The position of the value in the score")
    int index = 1;

    static constraints = {
        score(nullable: true)
        value(blank: false)
    }
    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "index"
        cache true
    }

    void checkAlreadyExist() {
        ScoreValue.withNewSession {
            if (value && score) {
                ScoreValue scoreAlreadyExist = ScoreValue.findByValueAndScore(value, score)
                if (scoreAlreadyExist && (scoreAlreadyExist.id != id)) {
                    throw new AlreadyExistException("Score value " + value + " already exist in this score!")
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
    static ScoreValue insertDataIntoDomain(def json, def domain = new ScoreValue()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.value = JSONUtils.getJSONAttrStr(json, 'value')
        domain.score = JSONUtils.getJSONAttrDomain(json, "score", new Score(), true)
        domain.index = JSONUtils.getJSONAttrInteger(json, 'index', 99)
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['value'] = domain?.value
        returnArray['score'] = domain?.score?.id
        returnArray['index'] = domain?.index
        return returnArray
    }

    int compareTo(obj) {
        index.compareTo(obj.index)
    }
}
