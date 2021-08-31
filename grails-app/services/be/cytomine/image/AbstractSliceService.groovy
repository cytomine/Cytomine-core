package be.cytomine.image

import be.cytomine.Exception.ConstraintException
import be.cytomine.command.AddCommand
import be.cytomine.command.Command
import be.cytomine.command.DeleteCommand
import be.cytomine.command.EditCommand
import be.cytomine.command.Transaction
import be.cytomine.security.SecUser
import be.cytomine.utils.ModelService
import be.cytomine.utils.Task

import static org.springframework.security.acls.domain.BasePermission.READ
import static org.springframework.security.acls.domain.BasePermission.WRITE

class AbstractSliceService extends ModelService {

    static transactional = true
    
    def cytomineService
    def securityACLService

    def currentDomain() {
        return AbstractSlice
    }

    def read(def id) {
        AbstractSlice slice = AbstractSlice.read(id)
        if (slice) {
            securityACLService.checkAtLeastOne(slice, READ)
        }
        slice
    }

    def read(AbstractImage image, double c, double z, double t) {
        AbstractSlice slice = AbstractSlice.findByImageAndChannelAndZStackAndTime(image, c, z, t)
        if (slice) {
            securityACLService.checkAtLeastOne(slice, READ)
        }
        slice
    }

    def list(AbstractImage image) {
        securityACLService.check(image, READ)
        AbstractSlice.findAllByImage(image)
    }

    def list(UploadedFile uploadedFile) {
        securityACLService.check(uploadedFile, READ)
        AbstractSlice.findAllByUploadedFile(uploadedFile)
    }

    def add(def json) {
        SecUser currentUser = cytomineService.getCurrentUser()
        securityACLService.checkUser(currentUser)

        Command c = new AddCommand(user: currentUser)
        executeCommand(c, null, json)
    }

    def update(AbstractSlice slice, def json) {
        securityACLService.checkAtLeastOne(slice, WRITE)
        SecUser currentUser = cytomineService.getCurrentUser()

        Command c = new EditCommand(user: currentUser)
        executeCommand(c, slice, json)
    }

    def delete(AbstractSlice slice, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.checkAtLeastOne(slice, READ)
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
        executeCommand(c, slice, null)
    }

    def deleteDependentSliceInstance(AbstractSlice slice, Transaction transaction,Task task=null) {
        def images = SliceInstance.findAllByBaseSlice(slice);
        if(!images.isEmpty()) {
            throw new ConstraintException("This slice $slice cannot be deleted as it has already been insert " +
                    "in projects " + images.collect{it.project.name})
        }
    }

    def getUploaderOfImage(def id){
        AbstractSlice slice = read(id)
        return slice?.uploadedFile?.user
    }

    @Override
    def getStringParamsI18n(Object domain) {
        return [domain.id, domain.channel, domain.zStack, domain.time]
    }
}
