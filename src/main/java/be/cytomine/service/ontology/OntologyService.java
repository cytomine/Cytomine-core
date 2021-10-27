package be.cytomine.service.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.ValidationError;
import be.cytomine.domain.command.*;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.*;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OntologyService extends ModelService {

    private final OntologyRepository ontologyRepository;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;

    private final ProjectRepository projectRepository;

    private final TermService termService;

    private final PermissionService permissionService;

    private final SecUserService secUserService;

    public Ontology get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<Ontology> find(Long id) {
        Optional<Ontology> optionalOntology = ontologyRepository.findById(id);
        optionalOntology.ifPresent(ontology -> securityACLService.check(ontology,READ));
        return optionalOntology;
    }

    /**
     * List ontology with full tree structure (term, relation,...)
     * Security check is done inside method
     */
    public List<Ontology> list() {
        return securityACLService.getOntologyList(currentUserService.getCurrentUser());
    }

    /**
     * List ontology with just id/name
     * Security check is done inside method
     */
    public List<JsonObject> listLight() {
        List<Ontology> ontologies = list();
        List<JsonObject>  data = new ArrayList<>();
        for (Ontology ontology : ontologies) {
            data.add(JsonObject.of("id", ontology.getId(), "name", ontology.getName()));
        }
        return data;
    }


    @Override
    public CommandResponse add(JsonObject jsonObject) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        jsonObject.put("user", currentUser.getId());
        return executeCommand(new AddCommand(currentUser),null,jsonObject);
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        securityACLService.check(domain,WRITE);
        SecUser currentUser = currentUserService.getCurrentUser();
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(domain,DELETE);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public Class currentDomain() {
        return Ontology.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Ontology().buildDomainFromJson(json, getEntityManager());
    }

    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((Ontology)domain).getName());
    }

    public void determineRightsForUsers(Ontology ontology, List<SecUser> users) {
        for (SecUser user : users) {
            determineRightsForUser(ontology, user);
        }
    }

    public void determineRightsForUser(Ontology ontology, SecUser user) {
        List<Project> projects = projectRepository.findAllByOntology(ontology);
        if(projects.stream().anyMatch(project -> secUserService.listAdmins(project).contains(user))) {
            permissionService.addPermission(ontology, user.getUsername(), BasePermission.ADMINISTRATION);
        } else {
            permissionService.deletePermission(ontology, user.getUsername(), BasePermission.ADMINISTRATION);
        }
        if(projects.stream().anyMatch(project -> secUserService.listUsers(project).contains(user))) {
            permissionService.addPermission(ontology, user.getUsername(), BasePermission.READ);
        } else {
            permissionService.deletePermission(ontology, user.getUsername(), BasePermission.READ);
        }
    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        permissionService.addPermission(domain, currentUserService.getCurrentUsername(), BasePermission.ADMINISTRATION);
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Ontology ontology = (Ontology)domain;
        if(ontology!=null && ontology.getName()!=null) {
            if(ontologyRepository.findByName(ontology.getName()).stream().anyMatch(x -> !Objects.equals(x.getId(), ontology.getId())))  {
                throw new AlreadyExistException("Ontology " + ontology.getName() + " already exist!");
            }
        }
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentProject((Ontology) domain, transaction, task);
        deleteDependentTerm((Ontology) domain, transaction, task);
    }

    private void deleteDependentProject(Ontology ontology, Transaction transaction, Task task) {
        if(!projectRepository.findAllByOntology(ontology).isEmpty()) {
            throw new ConstraintException("Ontology is linked with project. Cannot delete ontology!");
        }
    }

    private void deleteDependentTerm(Ontology ontology, Transaction transaction, Task task) {
        for (Term term : termService.list(ontology)) {
            termService.delete(term, transaction, task,false);
        }

    }



}
