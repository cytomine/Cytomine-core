package be.cytomine.score

import be.cytomine.CytomineDomain
import be.cytomine.Exception.AlreadyExistException
import be.cytomine.image.ImageInstance
import be.cytomine.score.Score
import be.cytomine.score.ScoreValue
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import org.restapidoc.annotation.RestApiObject
import org.restapidoc.annotation.RestApiObjectField

@RestApiObject(name = "Score", description = "")
class ImageScore extends CytomineDomain implements Serializable {

    @RestApiObjectField(description = "The score value")
    ScoreValue scoreValue

    @RestApiObjectField(description = "The possible value of the score")
    ImageInstance imageInstance

    @RestApiObjectField(description = "The position of the value in the score")
    User user

    static constraints = {
        scoreValue(nullable: false)
        imageInstance(nullable: false)
        user(nullable: false)
    }
    static mapping = {
        id(generator: 'assigned', unique: true)
        sort "id"
        cache true
        scoreValue fetch: 'join'
    }

    void checkAlreadyExist() {
        ImageScore.withNewSession {
            if (imageInstance && user && scoreValue) {
                ImageScore scoreAlreadyExist = ImageScore.findAllByImageInstanceAndUser(imageInstance, user).find{it.scoreValue.score == scoreValue.score}
                if (scoreAlreadyExist && (scoreAlreadyExist.id != id)) {
                    throw new AlreadyExistException("Score $scoreValue already exist in image $image !")
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
    static ImageScore insertDataIntoDomain(def json, def domain = new ImageScore()) {
        domain.id = JSONUtils.getJSONAttrLong(json, 'id', null)
        domain.imageInstance = JSONUtils.getJSONAttrDomain(json, "imageInstance", new ImageInstance(), true)
        domain.scoreValue = JSONUtils.getJSONAttrDomain(json, "scoreValue", new ScoreValue(), true)
        domain.user = JSONUtils.getJSONAttrDomain(json, "user", new User(), true)
        return domain;
    }

    /**
     * Define fields available for JSON response
     * @param domain Domain source for json value
     * @return Map with fields (keys) and their values
     */
    static def getDataFromDomain(def domain) {
        def returnArray = CytomineDomain.getDataFromDomain(domain)
        returnArray['imageInstance'] = domain?.imageInstance?.id
        returnArray['scoreValue'] = domain?.scoreValue?.id
        returnArray['scoreValueName'] = domain?.scoreValue?.value
        returnArray['user'] = domain?.user?.id
        returnArray['score'] = domain?.scoreValue?.score?.id
        return returnArray
    }
}
