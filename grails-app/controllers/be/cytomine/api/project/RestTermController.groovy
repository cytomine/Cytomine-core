package be.cytomine.api.project

import be.cytomine.project.Term
import grails.converters.XML
import grails.converters.JSON
import be.cytomine.project.Annotation
import be.cytomine.project.AnnotationTerm
import be.cytomine.security.User
import be.cytomine.command.Command
import be.cytomine.command.term.AddTermCommand
import be.cytomine.command.UndoStack
import be.cytomine.command.term.EditTermCommand
import be.cytomine.command.term.DeleteTermCommand

class RestTermController {

  def springSecurityService
  def transactionService

  def list = {

    log.info "List:"+ params.id
    def data = [:]

    if(params.id == null) {
      data.term = Term.list()
    } else
    {
      if(Annotation.exists(params.id))
        data.term = Annotation.get(params.id).terms()
      else {
        response.status = 404
        render contentType: "application/xml", {
          errors {
            message("Term not found with id: " + params.idterm)
          }
        }
      }
    }
    withFormat {
      json { render data as JSON }
      xml { render data as XML}
    }
  }

  def show = {
    log.info "Show:"+ params.id
    if(params.id && Term.exists(params.id)) {
      def data = [:]
      data.term = Term.findById(params.id)
      withFormat {
        json { render data as JSON }
        xml { render data as XML }
      }
    } else {
      response.status = 404
      render contentType: "application/xml", {
        errors {
          message("Term not found with id: " + params.idterm)
        }
      }
    }
  }

  def add = {
    log.info "Add"
    User currentUser = User.get(springSecurityService.principal.id)
    log.info "User:" + currentUser.username + " request:" + request.JSON.toString()

    Command addTermCommand = new AddTermCommand(postData : request.JSON.toString())

    def result = addTermCommand.execute()

    if (result.status == 201) {
      addTermCommand.save()
      new UndoStack(command : addTermCommand, user: currentUser,transactionInProgress:  currentUser.transactionInProgress).save()
    }

    response.status = result.status
    log.debug "result.status="+result.status+" result.data=" + result.data
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }

  def update = {
    log.info "Update"
    User currentUser = User.get(springSecurityService.principal.id)
    log.info "User:" + currentUser.username + " request:" + request.JSON.toString()

    def result
    if((String)params.id!=(String)request.JSON.term.id) {
      log.error "Term id from URL and from data are different:"+ params.id + " vs " +  request.JSON.term.id
      result = [data : [term : null , errors : ["Term id from URL and from data are different:"+ params.id + " vs " +  request.JSON.term.id ]], status : 400]
    }
    else
    {

      Command editTermCommand = new EditTermCommand(postData : request.JSON.toString())
      result = editTermCommand.execute()

      if (result.status == 200) {
        editTermCommand.save()
        new UndoStack(command : editTermCommand, user: currentUser, transactionInProgress:  currentUser.transactionInProgress).save()
      }
    }

    response.status = result.status
    log.debug "result.status="+result.status+" result.data=" + result.data
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }

  def delete =  {
    log.info "Delete"
    User currentUser = User.get(springSecurityService.principal.id)
    log.info "User:" + currentUser.username + " params.id=" + params.id
    def postData = ([id : params.id]) as JSON
    def result = null

    Command deleteTermCommand = new DeleteTermCommand(postData : postData.toString())

    result = deleteTermCommand.execute()
    if (result.status == 204) {
      deleteTermCommand.save()
      new UndoStack(command : deleteTermCommand, user: currentUser, transactionInProgress:  currentUser.transactionInProgress).save()
    }
    response.status = result.status
    withFormat {
      json { render result.data as JSON }
      xml { render result.data as XML }
    }
  }


}
