package be.cytomine.social

import be.cytomine.image.ImageInstance
import be.cytomine.image.SliceInstance
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional
import org.joda.time.DateTime
import org.springframework.web.context.request.RequestContextHolder

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

@Transactional
class UserPositionService extends ModelService {

    def securityACLService
    def dataSource
    def mongo
    def noSQLCollectionService

    def add(def json){

        SecUser user = cytomineService.getCurrentUser()
        SliceInstance slice = null
        ImageInstance image = null

        def sliceId = JSONUtils.getJSONAttrLong(json, "slice", 0)
        def imageId = JSONUtils.getJSONAttrLong(json, "image", 0)
        if (sliceId) {
            slice = SliceInstance.read(sliceId)
            image = slice.image
        }
        else if (imageId) {
            image = ImageInstance.read(imageId)
            slice = image.referenceSlice
        }

        def position = new LastUserPosition()
        position.user = user
        position.image = image
        position.slice = slice
        position.project = image.project
        def polygon = [
                [JSONUtils.getJSONAttrDouble(json,"topLeftX",-1),JSONUtils.getJSONAttrDouble(json,"topLeftY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"topRightX",-1),JSONUtils.getJSONAttrDouble(json,"topRightY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"bottomRightX",-1),JSONUtils.getJSONAttrDouble(json,"bottomRightY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"bottomLeftX",-1),JSONUtils.getJSONAttrDouble(json,"bottomLeftY",-1)]
        ]
        position.location = polygon
        position.zoom = JSONUtils.getJSONAttrInteger(json,"zoom",0)
        position.rotation = JSONUtils.getJSONAttrDouble(json,"rotation",0)
        position.broadcast = JSONUtils.getJSONAttrBoolean(json, "broadcast", false)
        position.created = new Date()
        position.updated = position.created
        position.imageName = image.getBlindInstanceFilename()
        position.insert(flush:true, failOnError : true) //don't use save (stateless collection)

        position = new PersistentUserPosition()
        position.user = user
        position.image = image
        position.slice = slice
        position.project = image.project
        polygon = [
                [JSONUtils.getJSONAttrDouble(json,"topLeftX",-1),JSONUtils.getJSONAttrDouble(json,"topLeftY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"topRightX",-1),JSONUtils.getJSONAttrDouble(json,"topRightY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"bottomRightX",-1),JSONUtils.getJSONAttrDouble(json,"bottomRightY",-1)],
                [JSONUtils.getJSONAttrDouble(json,"bottomLeftX",-1),JSONUtils.getJSONAttrDouble(json,"bottomLeftY",-1)]
        ]
        position.location = polygon
        position.zoom = JSONUtils.getJSONAttrInteger(json,"zoom",0)
        position.rotation = JSONUtils.getJSONAttrDouble(json,"rotation",0)
        position.broadcast = JSONUtils.getJSONAttrBoolean(json, "broadcast", false)
        position.session = RequestContextHolder.currentRequestAttributes().getSessionId()
        position.created = new Date()
        position.updated = position.created
        position.imageName = image.getBlindInstanceFilename()
        position.insert(flush:true, failOnError : true) //don't use save (stateless collection)

        return position
    }

    def lastPositionByUser(ImageInstance image, SecUser user, boolean broadcast, SliceInstance slice = null) {
        securityACLService.check(image,READ)
        def userPositions = LastUserPosition.createCriteria().list(sort: "created", order: "desc", max: 1) {
            eq("user", user)
            eq("image", image)

            if (slice) {
                eq("slice", slice)
            }

            if(broadcast) {
                eq("broadcast", true)
            }
        }
        def result = (userPositions.size() > 0) ? userPositions[0] : [:]
        return result
    }

    def listOnlineUsersByImage(ImageInstance image, boolean broadcast, SliceInstance slice = null) {
        securityACLService.check(image,READ)
        DateTime thirtySecondsAgo = new DateTime().minusSeconds(30)

        def match = [image: image.id, created: [$gte: thirtySecondsAgo.toDate()]]
        if(broadcast) {
            match = [$and: [match, [broadcast: true]]]
        }

        if (slice) {
            match = [$and: [match, [slice: slice.id]]]
        }

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def userPositions = db.lastUserPosition.aggregate(
                [$match: match],
                [$project: [user: '$user']],
                [$group : [_id : '$user']]
        );

        def result= userPositions.results().collect{it["_id"]}
        return ["users": result]
    }

    def list(ImageInstance image, User user, SliceInstance slice, Long afterThan = null, Long beforeThan = null, Long max = 0, Long offset = 0){
        securityACLService.check(image,WRITE)
        return PersistentUserPosition.createCriteria().list(max : max, offset : offset, sort: "created", order: "asc") {
            if(user) eq("user", user)
            eq("image", image)
            if (slice) eq("slice", slice)
            if(afterThan) gte("created", new Date(afterThan))
            if(beforeThan) lte("created", new Date(beforeThan))
        }
    }

    def summarize(ImageInstance image, User user, SliceInstance slice, Long afterThan = null, Long beforeThan = null){
        securityACLService.check(image,WRITE)

        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
        def userPositions

        def match = [[image: image.id]];
        if(afterThan) match << [created: [$gte: new Date(afterThan)]]
        if(beforeThan) match << [created: [$lt: new Date(beforeThan)]]
        if(user) match << [user:user.id]
        if (slice) match << [slice: slice.id]

        if(afterThan || beforeThan || user || slice) {
            match = [$and : match]
        } else {
            match = [image: image.id]
        }

        userPositions = db.persistentUserPosition.aggregate(
                [$match: match],
                [$group : [_id : [location : '$location', zoom : '$zoom', rotation : '$rotation'],
                           frequency : [$sum : 1], image : [$first: '$image']]]
        );

        def results = []
        userPositions.results().each{
            results << [location : it["_id"].location, zoom : it["_id"].zoom, rotation: it["_id"].rotation,
                        frequency : it.frequency, image : it.image]
        }
        return results
    }
}
