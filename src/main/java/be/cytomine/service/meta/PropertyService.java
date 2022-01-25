package be.cytomine.service.meta;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.meta.AttachedFileRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class PropertyService extends ModelService {

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private CurrentUserService currentUserService;

    @Override
    public CommandResponse add(JsonObject jsonObject) {
        return add(jsonObject, null);
    }

    public CommandResponse add(JsonObject jsonObject, Transaction transaction) {

        CytomineDomain domain = null;
        try {
            domain = (CytomineDomain)getEntityManager()
                    .find(Class.forName(jsonObject.getJSONAttrStr("domainClassName")), jsonObject.getJSONAttrLong("domainIdent"));
        } catch (ClassNotFoundException ignored) {
        }
       if (domain==null) {
           throw new WrongArgumentException("Property has no associated domain:"+ jsonObject.toJsonString());
       }

        if (!domain.getClass().getName().contains("AbstractImage")) {
            securityACLService.check(domain.container(),READ);
            if (domain.userDomainCreator()!=null) {
                securityACLService.checkFullOrRestrictedForOwner(domain, domain.userDomainCreator());
            } else {
                securityACLService.checkIsNotReadOnly(domain);
            }
        }

        SecUser currentUser = currentUserService.getCurrentUser();
        Command command = new AddCommand(currentUser,transaction);
        return executeCommand(command,null, jsonObject);
    }


    public CommandResponse addProperty(String domainClassName, Long domainIdent, String key, String value, SecUser user, Transaction transaction) {
        JsonObject jsonObject = JsonObject.of(
                "domainClassName", domainClassName,
                "domainIdent", domainIdent,
                "key", key,
                "value", value
        );
        return executeCommand(new AddCommand(user, transaction), null, jsonObject);
    }

    @Override
    public Class currentDomain() {
        return Property.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return null;
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        return null;
    }


    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain.container(),READ);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((AttachedFile)domain).getDomainClassName());
    }
}
