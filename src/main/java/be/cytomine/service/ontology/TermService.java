package be.cytomine.service.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Relation;
import be.cytomine.domain.ontology.RelationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.AlreadyExistException;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.ontology.RelationRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TermService extends ModelService {

    private final TermRepository termRepository;

    private final RelationRepository relationRepository;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;

    private final RelationTermService relationTermService;

    @Override
    public Class currentDomain() {
        return Term.class;
    }

    /**
     * List all term, Only for admin
     */
    public List<Term> list() {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return termRepository.findAll();
    }

    public Term get(Long id) {
        return find(id).orElse(null);
    }

    public Optional<Term> find(Long id) {
        Optional<Term> optionalTerm = termRepository.findById(id);
        optionalTerm.ifPresent(term -> securityACLService.check(term.container(),READ));
        return optionalTerm;
    }

    public List<Term> list(Ontology ontology) {
        securityACLService.check(ontology.container(),READ);
        return termRepository.findAllByOntology(ontology);
    }

    public List<Term> list(Project project) {
        securityACLService.check(project,READ);
        return termRepository.findAllByOntology(project.getOntology());
    }

    public List<Long> getAllTermId(Project project) {
        securityACLService.check(project.container(), READ);
        if (project.getOntology() == null) {
            return List.of();
        } else {
            return termRepository.listAllIds(project.getOntology());
        }
    }

    /**
     * Add the new domain with JSON data
     * @param jsonObject New domain data
     * @return Response structure (created domain data,..)
     */
    @Override
    public CommandResponse add(JsonObject jsonObject) {
        if (jsonObject.isMissing("ontology")) {
            throw new WrongArgumentException("Ontology is mandatory for term creation");
        }
        securityACLService.check(jsonObject.getJSONAttrLong("ontology"), Ontology.class ,WRITE);
        SecUser currentUser = currentUserService.getCurrentUser();
        return executeCommand(new AddCommand(currentUser),null,jsonObject);
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData) {
        securityACLService.check(domain.container(),WRITE);
        SecUser currentUser = currentUserService.getCurrentUser();
        return executeCommand(new EditCommand(currentUser), domain,jsonNewData);
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
        securityACLService.check(domain.container(),DELETE);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }


    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new Term().buildDomainFromJson(json, getEntityManager());
    }

    public void checkDoNotAlreadyExist(CytomineDomain domain){
        Term term = (Term)domain;
        if(term!=null && term.getName()!=null) {
            if(termRepository.findByNameAndOntology(term.getName(), term.getOntology()).stream().anyMatch(x -> !Objects.equals(x.getId(), term.getId())))  {
                throw new AlreadyExistException("Term " + term.getName() + " already exist in this ontology!");
            }
        }
    }

    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        Term term = (Term)domain;
        return Arrays.asList(String.valueOf(term.getId()), term.getName());
    }

    @Override
    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentRelationTerm((Term)domain, transaction, task);
    }

    public void deleteDependentRelationTerm(Term term, Transaction transaction, Task task) {
        for (RelationTerm relationTerm : relationTermService.list(term)) {
            relationTermService.delete(relationTerm, transaction, null, false);
        }
    }

//    def deleteDependentAlgoAnnotationTerm(Term term, Transaction transaction, Task task = null) {
//        def nbreAlgoAnnotation = AlgoAnnotationTerm.createCriteria().count {
//            isNull("deleted")
//            or {
//                eq("term", term)
//                eq("expectedTerm", term)
//            }
//        }
//
//        if (nbreAlgoAnnotation>0) {
//            throw new ConstraintException("Term is still linked with ${nbreAlgoAnnotation} annotations created by job. Cannot delete term!")
//        }
//    }
//
//    def deleteDependentAnnotationTerm(Term term, Transaction transaction, Task task = null) {
//        def nbreUserAnnotation = AnnotationTerm.countByTermAndDeletedIsNull(term)
//
//        if (nbreUserAnnotation>0) {
//            throw new ConstraintException("Term is still linked with ${nbreUserAnnotation} annotations created by user. Cannot delete term!")
//        }
//    }


//    def deleteDependentHasManyReviewedAnnotation(Term term, Transaction transaction, Task task = null) {
//        def criteria = ReviewedAnnotation.createCriteria()
//        def results = criteria.list {
//            terms {
//                inList("id", term.id)
//            }
//        }
//
//        if(!results.isEmpty()) {
//            throw new ConstraintException("Term is linked with ${results.size()} validate annotations. Cannot delete term!")
//        }
//    }
//
//    def deleteDependentHasManyAnnotationFilter(Term term, Transaction transaction, Task task = null) {
//        def criteria = AnnotationFilter.createCriteria()
//        def results = criteria.list {
//            users {
//                inList("id", term.id)
//            }
//        }
//        results.each {
//            it.removeFromTerms(term)
//            it.save()
//        }
//    }
//

}
