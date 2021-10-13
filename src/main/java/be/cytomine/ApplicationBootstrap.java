package be.cytomine;

import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.AdminProjectView;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.repository.ontology.OntologyRepository;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.project.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
class ApplicationBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    private final SecUserRepository secUserRepository;

    private final AclRepository aclRepository;

    private final StorageRepository storageRepository;

    private final OntologyRepository ontologyRepository;

    private final ProjectService projectService;

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("ApplicationListener#onApplicationEvent()");
        log.info("ONTOLOGIES");
        for (Ontology ontology : ontologyRepository.findAll().stream().filter(x-> x.getId()==11867l).collect(Collectors.toSet())) {

            log.info("ontology = " + ontology.getName());

            for (Map<String, Object> stringObjectMap : ontology.tree()) {
                log.info("      term = " + stringObjectMap);
            }
            log.info("****************************************");
            log.info("****************************************");
            log.info("****************************************");
            log.info("****************************************");
            log.info("****************************************");
            for (Term term : ontology.getTerms()) {

                    log.info("      ontology = " + ontology.getId());
                    log.info("      term = " + term);
                    log.info("          children = " + term.children());
                    log.info("          parent = " + term.parent().orElse(null));
            }

        }


//        List<AdminProjectView> admins = entityManager.createQuery("SELECT v FROM AdminProjectView v", AdminProjectView.class).getResultList();
//        for (AdminProjectView admin : admins) {
//            log.info("AdminProjectView = " + admin.getId() + " vs " + admin.getUserId());
//        }
//
//
//        log.info("PROJECTS");
//        projectService.list();

//        for (SecUser user : secUserRepository.findAll()) {
//            log.info("User " + user.humanUsername());
//        }
//
//        for (Storage storage : storageRepository.findAll()) {
//            log.info("Storage " + storage.getName() + " for user " + (storage.getUser()!=null ? storage.getUser().getUsername() : null));
//        }
//
//        log.info(aclRepository.listMaskForUsers(90L, "admin").toString());
    }

}