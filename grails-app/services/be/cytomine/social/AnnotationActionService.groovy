package be.cytomine.social

import be.cytomine.AnnotationDomain
import be.cytomine.image.ImageInstance
import be.cytomine.image.SliceInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.security.User
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

@Transactional
class AnnotationActionService extends ModelService {

    def securityACLService
    def dataSource
    def mongo

    def add(def json){

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(JSONUtils.getJSONAttrLong(json,"annotationIdent",0))

        securityACLService.check(annotation,READ)
        ImageInstance image = annotation.image
        SecUser user = cytomineService.getCurrentUser()

        AnnotationAction action = new AnnotationAction()
        action.user = user
        action.image = image
        action.slice = annotation.slice
        action.project = image.project
        action.created = new Date()
        action.action = JSONUtils.getJSONAttrStr(json,"action",true)
        action.annotationClassName = annotation.class.getName()
        action.annotationIdent = annotation.id
        action.annotationCreator = annotation.user
        action.insert(flush:true) //don't use save (stateless collection)

        return action
    }

    def list(SliceInstance slice, User user, Long afterThan = null, Long beforeThan = null){
        securityACLService.check(slice,WRITE)
        return AnnotationAction.createCriteria().list(sort: "created", order: "asc") {
            if(user) eq("user", user)
            eq("slice", slice)
            if(afterThan) gte("created", new Date(afterThan))
            if(beforeThan) lte("created", new Date(beforeThan))
        }
    }

    def list(ImageInstance image, User user, Long afterThan = null, Long beforeThan = null) {
        securityACLService.check(image,WRITE)
        return AnnotationAction.createCriteria().list(sort: "created", order: "asc") {
            if (user) eq("user", user)
            eq("image", image)
            if (afterThan) gte("created", new Date(afterThan))
            if (beforeThan) lte("created", new Date(beforeThan))
        }
    }

    def countByProject(Project project, Long startDate = null, Long endDate = null, String type = null) {
        def result = AnnotationAction.createCriteria().get {
            eq("project", project)
            if(startDate) {
                gt("created", new Date(startDate))
            }
            if(endDate) {
                lt("created", new Date(endDate))
            }
            if(type) {
                eq("action", type)
            }
            projections {
                rowCount()
            }
        }

        return [total: result]
    }
}
