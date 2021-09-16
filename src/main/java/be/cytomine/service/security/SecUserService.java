package be.cytomine.service.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.Command;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.EditCommand;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.*;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.ModelService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SecUserService extends ModelService {

    private final SecUserRepository secUserRepository;

    public Optional<SecUser> read(Long id) {
        return secUserRepository.findById(id);
    }

    public Optional<SecUser> read(String id) {
        try {
            return read(Long.valueOf(id));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }

    }

    public Optional<SecUser> findByUsername(String username) {
        return secUserRepository.findByUsernameLikeIgnoreCase(username);
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        synchronized (this.getClass()) {
            SecUser currentUser = null; //cytomineService.getCurrentUser(); //TODO
//            securityACLService.checkUser(currentUser)
            if (!json.containsKey("user")) {
                json.put("user", null); //TODO
                json.put("origin", "ADMINISTRATOR");
            }
            return executeCommand(new AddCommand(currentUser), null, json);
        }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData) {
        //SecUser currentUser = cytomineService.getCurrentUser()
        //securityACLService.checkIsCreator(user,currentUser)
        return executeCommand(new EditCommand(),domain, jsonNewData);
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    public CommandResponse delete(CytomineDomain domain, Task task, boolean printMessage) { //TODO: Transaction transaction,
        //SecUser currentUser = cytomineService.getCurrentUser() //TODO
//        if(domain.algo()) {
//            Job job = ((UserJob)domain).job
//            securityACLService.check(job?.container(),READ)
//            securityACLService.checkFullOrRestrictedForOwner(job, ((UserJob)domain).user)
//        } else {
//            securityACLService.checkAdmin(currentUser)
//            securityACLService.checkIsNotSameUser(domain,currentUser)
//        }
        Command c = new DeleteCommand(); //TODO : ,transaction:transaction
        return executeCommand(c,domain,null);
    }

    @Override
    public Class currentDomain() {
        return SecUser.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new User().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        SecUser secUser = (SecUser)domain;
        return Arrays.asList(String.valueOf(secUser.getId()), secUser.getUsername());
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        SecUser user = (SecUser)domain;
        Optional<SecUser> userWithSameUsername = secUserRepository.findByUsernameLikeIgnoreCase(user.getUsername());
        if (userWithSameUsername.isPresent() && !Objects.equals(userWithSameUsername.get().getId(), user.getId())) {
            throw new AlreadyExistException("User "+user.getUsername() + " already exist!");
        }
    }


    public Optional<SecUser> readCurrentUser() {
        return secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get());
    }

    public SecUser getCurrentUser() {
        return secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get()).orElseThrow(() -> new ServerException("Cannot read current user"));
    }
}
