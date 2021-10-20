package be.cytomine.service.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
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

import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Service
@RequiredArgsConstructor
public class SecUserService extends ModelService {

    private final SecUserRepository secUserRepository;

    private final SecurityACLService securityACLService;

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

    public List<SecUser> listAdmins(Project project) {
        return listAdmins(project, true);
    }

    public List<SecUser> listAdmins(Project project, boolean checkPermission) {
        if (checkPermission) {
            securityACLService.check(project,READ);
        }
        return secUserRepository.findAllAdminsByProjectId(project.getId());
    }

    public List<SecUser> listUsers(Project project) {
        return listUsers(project, false, true);
    }

    public List<SecUser> listUsers(Project project, boolean showUserJob, boolean checkPermission) {
        if (checkPermission) {
            securityACLService.check(project,READ);
        }
        List<SecUser> users = secUserRepository.findAllUsersByProjectId(project.getId());

        if(showUserJob) {
            //TODO:: should be optim (see method head comment)
//            List<Job> allJobs = Job.findAllByProject(project, [sort: 'created', order: 'desc'])
//
//            allJobs.each { job ->
//                    def userJob = UserJob.findByJob(job);
//                if (userJob) {
//                    userJob.username = job.software.name + " " + job.created
//                    users << userJob
//                }
//            }
            throw new RuntimeException("Not yet implemented (showUserJob)");
        }
        return users;
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
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        //SecUser currentUser = cytomineService.getCurrentUser() //TODO
//        if(domain.algo()) {
//            Job job = ((UserJob)domain).job
//            securityACLService.check(job?.container(),READ)
//            securityACLService.checkFullOrRestrictedForOwner(job, ((UserJob)domain).user)
//        } else {
//            securityACLService.checkAdmin(currentUser)
//            securityACLService.checkIsNotSameUser(domain,currentUser)
//        }
        Command c = new DeleteCommand(getCurrentUser(), transaction);
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
