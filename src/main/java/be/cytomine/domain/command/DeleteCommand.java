package be.cytomine.domain.command;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.service.ModelService;
import be.cytomine.utils.CommandResponse;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;

@Getter
@Setter
@Entity
@DiscriminatorValue("be.cytomine.command.DeleteCommand")
public class DeleteCommand extends Command {



    public DeleteCommand(SecUser currentUser, Transaction transaction) {
        this.user = currentUser;
        this.transaction = transaction;
    }

    public DeleteCommand() {}

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
        CytomineDomain container = oldDomain.container();
        if(container!=null && container instanceof Project && linkProject) {
            super.setProject((Project)container);
        }
        CommandResponse response = service.destroy(oldDomain, printMessage);
        fillCommandInfoJSON(backup.toString(), response.getData().get("message").toString());
        return response;
    }
}
