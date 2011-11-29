package be.cytomine.command

import grails.converters.JSON

/**
 * Created by IntelliJ IDEA.
 * User: lrollus
 * Date: 14/04/11
 * Time: 13:43
 * To change this template use File | Settings | File Templates.
 */
class DeleteCommand extends Command {

    static String commandNameUndo = "Add"
    static String commandNameRedo = "Delete"

    protected createMessage(def updatedTerm, def params) {
        responseService.createMessage(updatedTerm, params, "Delete")
    }

    protected def restore(def service,def json) {
        return service.restore(json,commandNameUndo,printMessage)
    }

    protected def destroy(def service,def json) {
        return service.destroy(json,commandNameRedo,printMessage)
    }



    def undo() {
        initService()
        return service.restore(JSON.parse(data),commandNameUndo,printMessage)
    }

    def redo() {
        initService()
        return service.destroy(JSON.parse(data),commandNameRedo,printMessage)
    }


    def execute()  {
        initService()
        //Create new domain
        def oldDomain = service.retrieve(json)
        def backup = oldDomain.encodeAsJSON()
        //Init command info
        super.initCurrentCommantProject(oldDomain?.projectDomain())

        def response = service.destroy(oldDomain, "Delete", printMessage)
        fillCommandInfoJSON(backup, response.message)
        return response
    }
}
