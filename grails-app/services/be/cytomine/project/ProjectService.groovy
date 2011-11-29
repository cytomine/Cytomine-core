package be.cytomine.project

import be.cytomine.Exception.ObjectNotFoundException
import be.cytomine.ModelService
import be.cytomine.ontology.Ontology
import be.cytomine.security.Group
import be.cytomine.security.User
import be.cytomine.security.UserGroup
import org.codehaus.groovy.grails.web.json.JSONObject
import be.cytomine.command.*

class ProjectService extends ModelService {

    static transactional = true
    def ontologyService
    def cytomineService
    def commandService
    def domainService

    boolean saveOnUndoRedoStack = false

    def list() {
        Project.list(sort: "name")
    }

    def list(Ontology ontology) {
        Project.findAllByOntology(ontology)
    }

    def list(User user) {
        user.projects()
    }

    def list(Discipline discipline) {
        project.findAllByDiscipline(discipline)
    }

    def read(def id) {
        Project.read(id)
    }

    def get(def id) {

        Project.get(id)
    }

    def lastAction(Project project, def max) {
        return CommandHistory.findAllByProject(project, [sort: "created", order: "desc", max: max])
    }

    def add(def json) {
        User currentUser = cytomineService.getCurrentUser()
        return executeCommand(new AddCommand(user: currentUser), json)
    }

    def update(def json)  {
        User currentUser = cytomineService.getCurrentUser()
        return executeCommand(new EditCommand(user: currentUser), json)
    }

    def delete(def json)  {
        User currentUser = cytomineService.getCurrentUser()
        return executeCommand(new DeleteCommand(user: currentUser), json)
    }


    /**
     * Restore domain which was previously deleted
     * @param json domain info
     * @param commandType command name (add/delete/...) which execute this method
     * @param printMessage print message or not
     * @return response
     */
    def restore(JSONObject json, String commandType, boolean printMessage) {
        restore(Project.createFromDataWithId(json),commandType,printMessage)
    }
    def restore(Project domain, String commandType, boolean printMessage) {
        //Save new object
        domainService.saveDomain(domain)
        //Build response message
        return responseService.createResponseMessage(domain,[domain.id, domain.name],printMessage,commandType,domain.getCallBack())
    }
    /**
     * Destroy domain which was previously added
     * @param json domain info
     * @param commandType command name (add/delete/...) which execute this method
     * @param printMessage print message or not
     * @return response
     */
    def destroy(JSONObject json, String commandType, boolean printMessage) {
        //Get object to delete
         destroy(Project.get(json.id),commandType,printMessage)
    }
    def destroy(Project domain, String commandType, boolean printMessage) {
        //Build response message
        //Delete all command / command history from project
        CommandHistory.findAllByProject(domain).each { it.delete() }
        Command.findAllByProject(domain).each {
            it
            UndoStackItem.findAllByCommand(it).each { it.delete()}
            RedoStackItem.findAllByCommand(it).each { it.delete()}
            it.delete()
        }
        //Delete group map with project
        Group projectGroup = Group.findByName(domain.name);

        if (projectGroup) {
            projectGroup.name = "TO REMOVE " + domain.id
            log.info "group " + projectGroup + " will be renamed"
            projectGroup.save(flush: true)
        }
        def groups = domain.groups()
        groups.each { group ->
            ProjectGroup.unlink(domain, group)
            //for each group, delete user link
            def users = group.users()
            users.each { user ->
                UserGroup.unlink(user, group)
            }
            //delete group
            group.delete(flush: true)
        }

        def response = responseService.createResponseMessage(domain,[domain.id, domain.name],printMessage,commandType,domain.getCallBack())
        //Delete object
        domainService.deleteDomain(domain)
        return response
    }

    /**
     * Edit domain which was previously edited
     * @param json domain info
     * @param commandType  command name (add/delete/...) which execute this method
     * @param printMessage  print message or not
     * @return response
     */
    def edit(JSONObject json, String commandType, boolean printMessage) {
        //Rebuilt previous state of object that was previoulsy edited
        edit(fillDomainWithData(new Project(),json),commandType,printMessage)
    }
    def edit(Project domain, String commandType, boolean printMessage) {
        //Validate and save domain
        Group group = Group.findByName(domain.name)
        log.info "rename group " + group?.name + "(" + group + ") by " + domain?.name
        if (group) {
            group.name = domain.name
            group.save(flush: true)
        }
        //Build response message
        def response = responseService.createResponseMessage(domain,[domain.id, domain.name],printMessage,commandType,domain.getCallBack())
        //Save update
        domainService.saveDomain(domain)
        return response
    }

    /**
     * Create domain from JSON object
     * @param json JSON with new domain info
     * @return new domain
     */
    Project createFromJSON(def json) {
       return Project.createFromData(json)
    }

    /**
     * Retrieve domain thanks to a JSON object
     * @param json JSON with new domain info
     * @return domain retrieve thanks to json
     */
    def retrieve(JSONObject json) {
        Project project = Project.get(json.id)
        if(!project) throw new ObjectNotFoundException("Project " + json.id + " not found")
        return project
    }

}
