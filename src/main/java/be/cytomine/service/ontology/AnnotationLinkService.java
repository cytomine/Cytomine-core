package be.cytomine.service.ontology;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.DeleteCommand;
import be.cytomine.domain.command.EditCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.ontology.AnnotationDomain;
import be.cytomine.domain.ontology.AnnotationGroup;
import be.cytomine.domain.ontology.AnnotationLink;
import be.cytomine.domain.security.SecUser;
import be.cytomine.repository.ontology.AnnotationLinkRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
public class AnnotationLinkService extends ModelService {

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AnnotationLinkRepository annotationLinkRepository;

    @Override
    public Class currentDomain() {
        return AnnotationLink.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new AnnotationLink().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((AnnotationLink) domain).getAnnotationGroup().getId());
    }

    public Optional<AnnotationLink> find(Long id) {
        Optional<AnnotationLink> annotationLink = annotationLinkRepository.findById(id);
        annotationLink.ifPresent(group -> securityACLService.check(group.container(), READ));
        return annotationLink;
    }

    public Optional<AnnotationLink> find(AnnotationGroup group, AnnotationDomain annotation) {
        Optional<AnnotationLink> annotationLink = annotationLinkRepository.findByAnnotationIdAndAnnotationGroup(annotation.getId(), group);
        annotationLink.ifPresent(link -> securityACLService.check(link.container(), READ));
        return annotationLink;
    }

    public AnnotationLink get(Long id) {
        return find(id).orElse(null);
    }

    public AnnotationLink get(AnnotationGroup group, AnnotationDomain annotation) {
        return find(group, annotation).orElse(null);
    }

    public List<AnnotationLink> list(AnnotationGroup group) {
        securityACLService.check(group, READ);
        return annotationLinkRepository.findAllByAnnotationGroup(group);
    }

    public CommandResponse add(JsonObject json) {
        transactionService.start();
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        return executeCommand(new AddCommand(currentUser), null, json);
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(), WRITE);

        return executeCommand(new DeleteCommand(currentUser, transaction), domain, null);
    }
}
