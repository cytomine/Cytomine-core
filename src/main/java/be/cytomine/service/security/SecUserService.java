package be.cytomine.service.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.dto.AuthInformation;
import be.cytomine.exceptions.*;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.search.ProjectSearchExtension;
import be.cytomine.service.search.UserSearchExtension;
import be.cytomine.utils.*;
import be.cytomine.utils.filters.SearchParameterEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.READ;

@Service
@RequiredArgsConstructor
public class SecUserService extends ModelService {

    private final SecUserRepository secUserRepository;

    private final UserRepository userRepository;

    private final SecurityACLService securityACLService;

    private final CurrentUserService currentUserService;

    private final CurrentRoleService currentRoleService;

    public Optional<SecUser> find(Long id) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return secUserRepository.findById(id);
    }

    public Optional<SecUser> find(String id) {
        try {
            return find(Long.valueOf(id));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public SecUser get(Long id) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return find(id).orElse(null);
    }

    public Optional<SecUser> findByUsername(String username) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return secUserRepository.findByUsernameLikeIgnoreCase(username);
    }

    public Optional<User> findByEmail(String email) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return userRepository.findByEmailLikeIgnoreCase(email);
    }


    public Optional<SecUser> findByPublicKey(String publicKey) {
        securityACLService.checkGuest(currentUserService.getCurrentUser());
        return secUserRepository.findByPublicKey(publicKey);
    }


    public AuthInformation getAuth(SecUser user) {
        AuthInformation authInformation = new AuthInformation();
        authInformation.setAdmin(currentRoleService.isAdmin(user));
        authInformation.setUser(!authInformation.getAdmin() && currentRoleService.isUser(user));
        authInformation.setGuest(!authInformation.getAdmin() && !authInformation.getUser() && currentRoleService.isGuest(user));

        authInformation.setAdminByNow(currentRoleService.isAdminByNow(user));
        authInformation.setUserByNow(!authInformation.getAdminByNow() && currentRoleService.isUserByNow(user));
        authInformation.setAdminByNow(!authInformation.getAdminByNow() && !authInformation.getUserByNow() && currentRoleService.isAdminByNow(user));

        return authInformation;
    }


    public Optional<SecUser> findCurrentUser() {
        return secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get());
    }

    public SecUser getCurrentUser() {
        return secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get()).orElseThrow(() -> new ServerException("Cannot read current user"));
    }

    public Page<Map<String, Object>> list(UserSearchExtension userSearchExtension, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long max, Long offset) {
        //TODO: test me! (never been executed)

        securityACLService.checkGuest(currentUserService.getCurrentUser());

        if(sortColumn==null) {
            sortColumn = "username";
        }
        if(sortDirection==null) {
            sortDirection = "asc";
        }
        if (sortColumn.equals("role") && !userSearchExtension.isWithRoles()) {
            throw new WrongArgumentException("Cannot sort on user role without argument withRoles");
        }

        if(ReflectionUtils.findField(User.class, sortColumn)==null && !(List.of("role", "fullName").contains(sortColumn))) {
            throw new CytomineMethodNotYetImplementedException("User list sorted by " + sortColumn + "is not implemented");
        }

        Optional<SearchParameterEntry> multiSearch = searchParameters.stream().filter(x -> x.getProperty().equals("fullName")).findFirst();

        String select = "SELECT u.* ";
        String from = "FROM sec_user u ";
        String where ="WHERE job_id IS NULL ";
        String search = "";
        String groupBy = "";
        String sort;

        Map<String, Object> mapParams = new HashMap<>();

        if(multiSearch.isPresent()) {
            String value = ((String) multiSearch.get().getValue()).toLowerCase();
            value = "%$value%";
            where += " and (u.firstname ILIKE :name OR u.lastname ILIKE :name OR u.email ILIKE :name OR u.username ILIKE :name) ";
            mapParams.put("name", value);
        }
        if(userSearchExtension.isWithRoles()){
            select += ", MAX(x.order_number) as role ";
            from +=", sec_user_sec_role sur, sec_role r " +
                    "JOIN (VALUES ('ROLE_GUEST', 1), ('ROLE_USER' ,2), ('ROLE_ADMIN', 3), ('ROLE_SUPER_ADMIN', 4)) as x(value, order_number) ON r.authority = x.value ";
            where += "and u.id = sur.sec_user_id and sur.sec_role_id = r.id ";
            groupBy = "GROUP BY u.id ";
        }

        if(sortColumn.equals("role")) {
            sort = "ORDER BY " + sortColumn + " " + "sortDirection, u.id ASC ";
        } else if(sortColumn.equals("fullName")){
            sort = "ORDER BY u.firstname " + sortDirection + ", u.id ";
        } else if(!sortColumn.equals("id")){ //avoid random sort when multiple values of the
            sort = "ORDER BY u." +sortColumn + " " + sortDirection + ", u.id ";
        } else sort = "ORDER BY u." + sortColumn + " " + sortDirection +" ";

        String request = select + from + where + search + groupBy + sort;

        if (max > 0) {
            request += " LIMIT " + max;
        }
        if (offset > 0) {
            request += " OFFSET " + offset;
        }

        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
//        Map<String, Object> mapParams = sqlSearchConditions.getSqlParameters();
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<Tuple> resultList = query.getResultList();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Tuple rowResult : resultList) {
            JsonObject result = new JsonObject();
            for (TupleElement<?> element : rowResult.getElements()) {
                Object value = rowResult.get(element.getAlias());
                if (value instanceof BigInteger) {
                    value = ((BigInteger)value).longValue();
                }
                String alias = SQLUtils.toCamelCase(element.getAlias());
                result.put(alias, value);
            }

            // TODO?????
            // I mock methods and fields to pass through getDataFromDomain of SecUser
//            map["class"] = User.class
//            map.getMetaClass().algo = { return false }
//            map["language"] = User.Language.valueOf(map["language"])

            JsonObject object = User.getDataFromDomain(new User().buildDomainFromJson(result, getEntityManager()));
            if(userSearchExtension.isWithRoles()){
                String role = "UNDEFINED";
                switch ((Integer)result.get("role")){
                    case 1:
                        role = "ROLE_GUEST";
                        break;
                    case 2:
                        role = "ROLE_USER";
                        break;
                    case 3:
                        role = "ROLE_ADMIN";
                        break;
                    case 4:
                        role = "ROLE_SUPER_ADMIN";
                        break;
                }
                object.put("role", role);
            }
            results.add(object);
        }
        request = "SELECT COUNT(DISTINCT U.id) " + from + where + search;
        query = getEntityManager().createNativeQuery(request);
        long count = ((BigInteger)query.getResultList().get(0)).longValue();
        Page<Map<String, Object>> page = new PageImpl<>(results, PageUtils.buildPage(offset, max), count);
        return page;


    }


    public Page<Map<String, Object>> listUsersExtendedByProject(Project project, UserSearchExtension userSearchExtension, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long max, Long offset) {

        if(ReflectionUtils.findField(User.class, sortColumn)==null && !(List.of("projectRole", "fullName", "lastImageName","lastConnection","frequency").contains(sortColumn))) {
            throw new CytomineMethodNotYetImplementedException("User list sorted by " + sortColumn + "is not implemented");
        }
        if (sortColumn.equals("lastImageName") && !userSearchExtension.isWithLastImage()) {
            throw new WrongArgumentException("Cannot sort on lastImageName without argument withLastImage");
        }
        if (sortColumn.equals("lastConnection") && !userSearchExtension.isWithLastConnection()) {
            throw new WrongArgumentException("Cannot sort on lastConnection without argument withLastConnection");
        }
        if (sortColumn.equals("frequency") && !userSearchExtension.isWithNumberConnections()) {
            throw new WrongArgumentException("Cannot sort on frequency without argument withNumberConnections");
        }

        if (userSearchExtension==null || userSearchExtension.noExtension()) {
            return listUsersByProject(project, searchParameters, sortColumn, sortDirection, max, offset);
        }

        throw new CytomineMethodNotYetImplementedException("the rest of the method still needs to be implemented");

    }

    public Page<Map<String, Object>> listUsersByProject(Project project, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long max, Long offset) {
        securityACLService.check(project,READ);
        // migration from grails: parameter boolean withProjectRole is always true
        Optional<SearchParameterEntry> onlineUserSearch = searchParameters.stream().filter(x -> x.getProperty().equals("status") && x.getValue().equals("online")).findFirst();
        Optional<SearchParameterEntry> multiSearch = searchParameters.stream().filter(x -> x.getProperty().equals("fullName")).findFirst();
        Optional<SearchParameterEntry> projectRoleSearch = searchParameters.stream().filter(x -> x.getProperty().equals("projectRole")).findFirst();

        String select = "select distinct secUser ";
        String from  = "from ProjectRepresentativeUser r right outer join r.user secUser ON (r.project.id = " + project.getId() + "), " +
                    "AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid ";
        String where = "where aclObjectId.objectId = "+project.getId()+" " +
                "and aclEntry.aclObjectIdentity = aclObjectId.id " +
                "and aclEntry.sid = aclSid.id " +
                "and aclSid.sid = secUser.username " +
                "and secUser.class = 'be.cytomine.security.User' ";
        String groupBy = "";
        String order = "";
        String having = "";

        if(multiSearch.isPresent()) {
            String value = ((String) multiSearch.get().getValue()).toLowerCase();
            where += " and (lower(secUser.firstname) like '%$value%' or lower(secUser.lastname) like '%"+value+"%' or lower(secUser.email) like '%"+value+"%') ";
        }
        if(onlineUserSearch.isPresent()) {
            // TODO implement me! :-)
            throw new CytomineMethodNotYetImplementedException("migration wip");
//            def onlineUsers = getAllOnlineUserIds(project)
//            if(onlineUsers.isEmpty())
//                return [data: [], total: 0, offset: 0, perPage: 0, totalPages: 0]
//
//            where += " and secUser.id in ("+getAllOnlineUserIds(project).join(",")+") "
        }


        if (projectRoleSearch.isPresent()) {
            List<String> roles = (projectRoleSearch.get().getValue() instanceof String) ? List.of((String)projectRoleSearch.get().getValue()) : (List<String>)projectRoleSearch.get().getValue();
            having += " HAVING MAX(CASE WHEN r.id IS NOT NULL THEN 'representative' " +
                    "WHEN aclEntry.mask = 16 THEN 'manager' " +
                    "ELSE 'contributor' END) IN (" + roles.stream().map(x -> "'"+x+"'").collect(Collectors.joining(",")) + ")";
        }

        //works because 'contributor' < 'manager' < 'representative'
        select += ", MAX( CASE WHEN r.id IS NOT NULL THEN 'representative'\n" +
                "     WHEN aclEntry.mask = 16 THEN 'manager'\n" +
                "     ELSE 'contributor'\n" +
                " END) as role ";
        groupBy = "GROUP BY secUser.id ";

        if(sortColumn.equals("projectRole")){
            sortColumn = "role";
        } else if(sortColumn.equals("fullName")){
            sortColumn = "secUser.firstname";
        } else {
            sortColumn = "secUser." + sortColumn;
        }
        order = "order by " + sortColumn + " " +  sortDirection;

        String request = select + from + where + groupBy + having + order;

        if (max > 0) {
            request += " LIMIT " + max;
        }
        if (offset > 0) {
            request += " OFFSET " + offset;
        }


        Query query = getEntityManager().createQuery(request, Object[].class);
        List<Map<String, Object>> results = new ArrayList<>();
        List<Object[]> resultList = query.getResultList();
        for (Object[] row : resultList) {
            JsonObject jsonObject = ((User)row[0]).toJsonObject();
            jsonObject.put("role", (String)row[1]);
            results.add(jsonObject);
        }



//        List<Map<String, Object>> results = new ArrayList<>();
//        for (Tuple rowResult : resultList) {
//            JsonObject result = new JsonObject();
//            for (TupleElement<?> element : rowResult.getElements()) {
//                Object value = rowResult.get(element.getAlias());
//                if (value instanceof BigInteger) {
//                    value = ((BigInteger)value).longValue();
//                }
//                String alias = SQLUtils.toCamelCase(element.getAlias());
//                result.put(alias, value);
//            }
//            JsonObject object = Project.getDataFromDomain(new User().buildDomainFromJson(result, getEntityManager()));
//            object.put("role", result.get("role"));
//            results.add(object);
//        }
        request = "SELECT COUNT(DISTINCT secUser) " + from + where;
        query = getEntityManager().createQuery(request);
        long count = ((Long)query.getResultList().get(0));
        Page<Map<String, Object>> page = new PageImpl<>(results, PageUtils.buildPage(offset, max), count);
        return page;


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
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        //SecUser currentUser = cytomineService.getCurrentUser()
        //securityACLService.checkIsCreator(user,currentUser)
        return executeCommand(new EditCommand(null, null),domain, jsonNewData);
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


}
