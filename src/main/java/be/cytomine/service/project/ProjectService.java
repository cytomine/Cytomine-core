package be.cytomine.service.project;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.acl.*;
import be.cytomine.domain.command.CommandHistory;
import be.cytomine.domain.command.CommandHistory_;
import be.cytomine.domain.meta.*;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Ontology_;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.ontology.UserAnnotation_;
import be.cytomine.domain.project.*;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.command.CommandHistoryRepository;
import be.cytomine.repository.project.ProjectRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.Task;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.*;
import java.util.*;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Service
@RequiredArgsConstructor
public class ProjectService extends ModelService {

    private final CommandHistoryRepository commandHistoryRepository;

    private final CurrentUserService currentUserService;

    private final ProjectRepository projectRepository;

    private final EntityManager em;

    private final SecurityACLService securityACLService;

    public Optional<Project> read(Long id) {
        Optional<Project> project = projectRepository.findById(id);
        project.ifPresent(value -> securityACLService.check(value, READ));
        return project;
    }

    public List<Project> readMany(Collection<Long> ids) {
        List<Project> projects = projectRepository.findAllById(ids);
        for (Project project : projects) {
            securityACLService.check(project, READ);
        }
        return projects;
    }

    /**
     * List last project opened by user
     * If the user has less than "max" project opened, add last created project to complete list
     */
    public List<LastOpenedProject> listLastOpened(User user, Long offset, Long max) {
        // TODO:
        throw new RuntimeException("TODO");
    }



    List<Project> listByOntology(Ontology ontology) {
        return projectRepository.findAllProjectForUserByOntology(currentUserService.getCurrentUsername(),ontology);
    }

//    List<Software> listBySoftware(Software software) {
//        // TODO:
//        throw new RuntimeException("TODO");
//    }


    List<CommandHistory> lastAction(Project project, int max) {
        securityACLService.check(project, READ);
        return commandHistoryRepository.findAllByProject(project, PageRequest.of(0, max, Sort.by("created").descending()));
    }




    public List<Project> list() {
        ProjectSearchExtension searchExtension = ProjectSearchExtension.builder()
                .withLastActivity(true)
                .withCurrentUserRoles(true)
                .build();

        ProjectSearchParameter searchParameter = ProjectSearchParameter.builder()
                .user(SearchParameterEntry.builder().operation(SearchOperation.equals).value("admin").build())
                .countAnnotations(SearchParameterEntry.builder().operation(SearchOperation.gte).value(1l).build())
                .membersCount(SearchParameterEntry.builder().operation(SearchOperation.gte).value(1l).build())
//                .project(SearchParameterEntry.builder().operation(SearchOperation.equals).value(313l).build())
//                .ontology(SearchParameterEntry.builder().operation(SearchOperation.equals).value(303l).build())
                .build();
        Pageable pageable = PageRequest.of(0, 1, Sort.by("name").ascending());
        return list(searchExtension, searchParameter, pageable);
    }

    public List<Project> list(ProjectSearchExtension searchExtension, ProjectSearchParameter searchParameter, Pageable pageable) {
        Long currentUserId = 58l;
//        CriteriaBuilder cb = em.getCriteriaBuilder();
//        CriteriaQuery<Tuple> cq = cb.createTupleQuery();
//        Root<Project> project = cq.from(Project.class);
//        Join<Project, Ontology> ontologies = project.join(Project_.ontology, JoinType.LEFT);
//        cq.select(cb.tuple(project, cb.count(ontologies)));


//        Long userId = 58l;
//        String username = "admin";
//        Long numberOfUserAnnotation = 1l;
//        Long membersCount = 2l;


        String column = pageable.getSort().stream().findFirst().get().getProperty();
        Sort.Direction direction = pageable.getSort().stream().findFirst().get().getDirection();

        if (column.equals("lastActivity") && !searchExtension.withLastActivity) {
            throw new WrongArgumentException("Cannot sort on lastActivity without argument withLastActivity");
        }
        if (column.equals("membersCount") && !searchExtension.withMembersCount) {
            throw new WrongArgumentException("Cannot sort on membersCount without argument withMembersCount");
        }

        if (searchParameter.members!=null && !searchExtension.withMembersCount) {
            throw new WrongArgumentException("Cannot search on members attributes without argument withMembersCount");
        }





        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Tuple> cq = cb.createTupleQuery();

        Root<Project> project = cq.from(Project.class);
        Join<AclEntry, AclSid> aclSidJoincOUNT = null;
        Join<Project, CommandHistory> commandHistoryJoin = null;
        List<Selection> selects = new ArrayList<>();
        List<Predicate> wheres = new ArrayList<>();
        List<Predicate> havings = new ArrayList<>();

        if(searchParameter.project!=null) {
            wheres.add(buildOperationFromEntry(cb, project, searchParameter.project));
        }

        if(searchParameter.ontology!=null) {
            // TODO: really required?
            Join<Project, Ontology> ontology = project.join(Project_.ontology, JoinType.LEFT);
            wheres.add(buildOperationFromEntry(cb, ontology, searchParameter.ontology));
        }

        if(searchParameter.tags!=null) {
            Root<TagDomainAssocitation> tags = cq.from(TagDomainAssocitation.class);
            Predicate condition = cb.and(cb.equal(project.get(Project_.id), tags.get(TagDomainAssocitation_.DOMAIN_IDENT)));
            wheres.add(condition);
            Join<TagDomainAssocitation, Tag> tag = tags.join(TagDomainAssocitation_.TAG, JoinType.INNER);
            wheres.add(buildOperationFromEntry(cb, tag, searchParameter.tags));
        }

        if(searchParameter.user!=null) {
            Root<AclObjectIdentity> aclObjectIdentity = cq.from(AclObjectIdentity.class);
            Predicate condition = cb.and(cb.equal(project.get(Project_.id), aclObjectIdentity.get(AclObjectIdentity_.objectId)));
            wheres.add(condition);

            Join<AclObjectIdentity, AclEntry> aclEntryJoin = aclObjectIdentity.join(AclObjectIdentity_.ACL_ENTRIES, JoinType.INNER);
            Join<AclEntry, AclSid> aclSidJoin = aclEntryJoin.join(AclEntry_.SID, JoinType.INNER);
            //aclSidJoin.on(cb.equal(aclSidJoin.get(AclSid_.SID), username));
            wheres.add(cb.and(buildOperationFromEntry(cb, aclSidJoin.get(AclSid_.SID), searchParameter.user)));
        }

        if(searchParameter.membersCount!=null) {
            Root<AclObjectIdentity> aclObjectIdentity = cq.from(AclObjectIdentity.class);
            wheres.add(cb.and(cb.equal(project.get(Project_.id), aclObjectIdentity.get(AclObjectIdentity_.objectId))));

            Join<AclObjectIdentity, AclEntry> aclEntryJoin = aclObjectIdentity.join(AclObjectIdentity_.ACL_ENTRIES, JoinType.INNER);
            aclSidJoincOUNT = aclEntryJoin.join(AclEntry_.SID, JoinType.INNER);
            //cq.groupBy(aclSidJoincOUNT);
        }

        Join<Project, UserAnnotation> annotations = project.join(Project_.userAnnotations, JoinType.LEFT);
        cq.groupBy(project);
        cq.where(wheres.toArray(new Predicate[wheres.size()]));

        if (searchParameter.countAnnotations!=null) {
            selects.add(cb.countDistinct(annotations));
            havings.add(buildOperationFromEntry(cb, cb.count(annotations), searchParameter.countAnnotations));
        }
        if(searchParameter.membersCount!=null) {
            selects.add(cb.countDistinct(annotations));
            havings.add(cb.and(buildOperationFromEntry(cb, cb.countDistinct(aclSidJoincOUNT), searchParameter.membersCount)));
        }

        if(searchExtension.withLastActivity) {
            commandHistoryJoin = project.join(Project_.commandHistories, JoinType.LEFT);
            selects.add(cb.greatest(commandHistoryJoin.get(CommandHistory_.created)));
        }

//        if(searchExtension.withDescription) {
//            Join<Description, Project> description = project.join(Description_.domainIdent, JoinType.LEFT);
//
////            Root<Description> descriptionRoot = cq.from(Description.class);
////            wheres.add(cb.and(cb.equal(project.get(Project_.id), descriptionRoot.get(Description_.domainIdent))));
////            selects.add(descriptionRoot.get(Description_.data));
//        }

        Join<Project, AdminProjectView> adminProjectViewJoin = null;
        if(searchExtension.withCurrentUserRoles) {
            adminProjectViewJoin = project.join(Project_.adminProjectViews, JoinType.LEFT);
            wheres.add(cb.and(cb.equal(adminProjectViewJoin.get(AdminProjectView_.key).get(AdminProjectPK_.userId), currentUserId)));
            //selects.add(cb.isNull(adminProjectViewJoin.get(AdminProjectView_.key).get(AdminProjectPK_.id)));
            selects.add(cb.count(adminProjectViewJoin.get(AdminProjectView_.key).get(AdminProjectPK_.id)));

            Join<Project, ProjectRepresentativeUser> projectProjectRepresentativeUserJoin = project.join(Project_.representativeUsers, JoinType.LEFT);
            wheres.add(cb.and(cb.equal(projectProjectRepresentativeUserJoin.get(ProjectRepresentativeUser_.user), currentUserId)));
            //selects.add(cb.isNull(projectProjectRepresentativeUserJoin.get(ProjectRepresentativeUser_.user)));
            selects.add(cb.count(projectProjectRepresentativeUserJoin.get(ProjectRepresentativeUser_.user)));
        }

        if(searchParameter.currentUserRole!=null) {
            if (searchParameter.currentUserRole.value.equals("manager") && searchParameter.currentUserRole.value.equals("contributor")) {
                // do nothing
            } else if (searchParameter.currentUserRole.value.equals("manager")) {
                wheres.add(cb.and(cb.isNotNull(adminProjectViewJoin.get("userId"))));
            } else if (searchParameter.currentUserRole.value.equals("contributor")) {
                wheres.add(cb.and(cb.isNull(adminProjectViewJoin.get("userId"))));
            }
        }


        cq.having(havings.toArray(new Predicate[havings.size()]));

        selects.add(0, project);
        cq.select(cb.tuple(selects.toArray(new Selection[selects.size()])));



        cq.orderBy(direction.equals(Sort.Direction.ASC) ? cb.asc(project.get(column)) : cb.desc(project.get(column)));

        //cq.having(cb.greaterThanOrEqualTo(cb.count(aclSidJoincOUNT), membersCount));
        List<Tuple> result = em.createQuery(cq).getResultList();


        System.out.println("Total = " + result.size());


        List<Map<String, Object>> results = new ArrayList<>(result.size());
        for (Tuple t : result) {
            Map<String, Object> projectRow = new HashMap<>();


            Project o = (Project) t.get(0);
            projectRow.putAll(o.toJsonObject());

            Long cnt = (Long) t.get(1);
            System.out.println("***************** Project " + o.getName() + " *****************");

            System.out.println("Number of users: " + (Long) t.get(2));
            System.out.println("Users = " + securityACLService.getProjectUsers(o));

            System.out.println("umber of annotations = " + cnt);
            System.out.println("Annotations = " + em.createQuery(
                    "select count(userAnnotation.id) "+
                            "from UserAnnotation as userAnnotation where userAnnotation.project.id = " + o.getId()).getResultList().get(0));

            results.add(projectRow);
        }


//        final int start = (int)pageable.getOffset();
//        final int end = Math.min((start + pageable.getPageSize()), results.size());
        final Page<Map<String, Object>> page = createPageFromList(results, pageable);

        System.out.println("***************************************");
        System.out.println("***************** PAGE *****************");
        System.out.println("****************************************");
        for (Map<String, Object> stringObjectMap : page) {
            System.out.println("ROW");
            for (Map.Entry<String, Object> stringObjectEntry : stringObjectMap.entrySet()) {
                System.out.println(stringObjectEntry.getKey() + " = " + stringObjectEntry.getValue());
            }
            System.out.println("****************************************");
        }


//        cq.where(... add some predicates here ...);
//        cq.groupBy(customer.get(Customer_.id));
//        cq.orderBy(cb.desc(cb.count(orders)));
//        List<Tuple> result = em.createQuery(cq).getResultList();
//        for (Tuple t : result) {
//            Customer c = (Customer) t.get(0);
//            Long cnt = (Long) t.get(1);
//            System.out.println("Customer " + c.getName() + " has " + cnt + " orders");
//        }
        return null;
    }

    static <T> Page<T> createPageFromList(List<T> list, Pageable pageable) {
        if (list == null) {
            throw new IllegalArgumentException("To create a Page, the list mustn't be null!");
        }

        int startOfPage = pageable.getPageNumber() * pageable.getPageSize();
        if (startOfPage > list.size()) {
            return new PageImpl<>(new ArrayList<>(), pageable, 0);
        }

        int endOfPage = Math.min(startOfPage + pageable.getPageSize(), list.size());
        return new PageImpl<>(list.subList(startOfPage, endOfPage), pageable, list.size());
    }


    private Predicate buildOperationFromEntry(CriteriaBuilder build, Expression x, SearchParameterEntry searchParameterEntry) {
        if (searchParameterEntry.operation.equals(SearchOperation.equals)) {
            return build.equal(x, searchParameterEntry.value);
        } else if (searchParameterEntry.operation.equals(SearchOperation.gte)) {
            return build.greaterThanOrEqualTo(x, (Long)searchParameterEntry.value);
        }
        throw new RuntimeException(searchParameterEntry.operation + " not supported...");
    }


    @Override
    public CommandResponse add(JsonObject jsonObject) {
        return null;
    }

    @Override
    public Class currentDomain() {
        return Project.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return null;
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData) {
        return null;
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Task task, boolean printMessage) {
        return null;
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }


    // working code for ontologies

//    CriteriaBuilder cb = em.getCriteriaBuilder();
//    CriteriaQuery<Tuple> cq = cb.createTupleQuery();
//    Root<Ontology> ontology = cq.from(Ontology.class);
//    Join<Ontology, Project> projects = ontology.join(Ontology_.projects, JoinType.LEFT);
//        cq.select(cb.tuple(ontology, cb.count(projects)));
//        cq.groupBy(ontology);
//        cq.having(cb.greaterThanOrEqualTo(cb.count(projects), 1L));
//    List<Tuple> result = em.createQuery(cq).getResultList();
//        for (Tuple t : result) {
//        Ontology o = (Ontology) t.get(0);
//        Long cnt = (Long) t.get(1);
//        System.out.println("Ontology " + o.getName() + " has " + cnt + " projects");
//    }

}

@Data
@Builder
class ProjectSearchParameter {

    SearchParameterEntry user;

    SearchParameterEntry membersCount;

    SearchParameterEntry countImages;

    SearchParameterEntry countAnnotations;

    SearchParameterEntry countJobAnnotations;

    SearchParameterEntry countReviewedAnnotations;

    SearchParameterEntry ontology;

    SearchParameterEntry project;

    SearchParameterEntry tags;

    SearchParameterEntry members;

    SearchParameterEntry currentUserRole;

}

@Data
@Builder
class SearchParameterEntry {

    Object value;

    SearchOperation operation;

}

enum SearchOperation {
    equals, nequals, like, ilike, lte, gte, in

}

@Data
@Builder
class ProjectSearchExtension {

    boolean withMembersCount;

    boolean withLastActivity;

//    Boolean withDescription;

    boolean withCurrentUserRoles;
}

@Data
@AllArgsConstructor
class LastOpenedProject {
    Long id;
    Date date;
    boolean opened;
}