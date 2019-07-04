package be.cytomine.social

import be.cytomine.AnnotationDomain
import be.cytomine.image.ImageInstance
import be.cytomine.project.Project
import be.cytomine.security.SecUser
import be.cytomine.utils.JSONUtils
import be.cytomine.utils.ModelService
import grails.transaction.Transactional

import static org.springframework.security.acls.domain.BasePermission.READ

@Transactional
class AnnotationActionService extends ModelService {

    def securityACLService

    def add(def json){

        AnnotationDomain annotation = AnnotationDomain.getAnnotationDomain(JSONUtils.getJSONAttrLong(json,"annotationIdent",0))

        securityACLService.check(annotation,READ)
        ImageInstance image = annotation.image
        SecUser user = cytomineService.getCurrentUser()
        AnnotationAction action = new AnnotationAction()
        action.user = user
        action.image = image
        action.project = image.project
        action.action = JSONUtils.getJSONAttrStr(json,"action",true)
        action.created = new Date()
        action.annotationClassName = annotation.class.getName()
        action.annotationIdent = annotation.id
        action.annotationCreator = annotation.user
        action.insert(flush:true) //don't use save (stateless collection)

        return action
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
