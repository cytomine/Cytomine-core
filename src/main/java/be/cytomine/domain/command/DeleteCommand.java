package be.cytomine.domain.command;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.service.ModelService;
import be.cytomine.utils.CommandResponse;
import lombok.Data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Data
@Entity
@DiscriminatorValue("be.cytomine.command.DeleteCommand")
public class DeleteCommand extends Command {

    @Transient
    Object backup;

    /**
     * Add project link in command
     */
    boolean linkProject = true;

    /**
     * Process an Add operation for this command
     * @return Message
     */
    public CommandResponse execute(ModelService service) {
        //Retrieve domain to delete it
        CytomineDomain oldDomain = domain;
        //Init command info
        // TODO
//        CytomineDomain container = oldDomain?.container()
//        if(container && container instanceof Project && linkProject) {
//            super.setProject(container)
//        }
        CommandResponse response = service.destroy(oldDomain, printMessage);
        fillCommandInfoJSON(backup.toString(), response.getData().get("message").toString());
        return response;
    }

}
