package be.cytomine.service.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.AbstractSlice;
import be.cytomine.domain.image.UploadedFile;
import be.cytomine.domain.image.UploadedFileStatus;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.security.UserJob;
import be.cytomine.exceptions.WrongArgumentException;
import be.cytomine.repository.image.AbstractImageRepository;
import be.cytomine.repository.image.UploadedFileRepository;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.UrlApi;
import be.cytomine.service.search.UploadedFileSearchParameter;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.*;
import be.cytomine.utils.filters.SQLSearchParameter;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import be.cytomine.utils.filters.SearchParameterProcessed;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.security.acls.domain.BasePermission.*;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class UploadedFileService extends ModelService {

    private CurrentUserService currentUserService;

    private SecurityACLService securityACLService;

    private UploadedFileRepository uploadedFileRepository;

    private AbstractImageRepository abstractImageRepository;


    @Override
    public Class currentDomain() {
        return UploadedFile.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new UploadedFile().buildDomainFromJson(json, getEntityManager());
    }


    public Page<UploadedFile> list(Pageable pageable) {
        securityACLService.checkAdmin(currentUserService.getCurrentUser());
        return uploadedFileRepository.search(null, null, null, null, pageable);
    }

    public Page<UploadedFile> list(SecUser user, Long parentId, Boolean onlyRoot, Pageable pageable) {
        securityACLService.checkIsSameUser(user, currentUserService.getCurrentUser());
        List<Storage> storages = securityACLService.getStorageList(currentUserService.getCurrentUser(), false);
        return uploadedFileRepository.search(user.getId(), parentId, onlyRoot, DomainUtils.extractIds(storages), pageable);
    }

    public List<Map<String, Object>> list(UploadedFileSearchParameter searchParameters, String sortedProperty, String sortDirection) {

        // authorization check is done in the sql request

        searchParameters.findStorage().ifPresent(x -> x.setOperation(SearchOperation.in));
        searchParameters.findUser().ifPresent(x -> x.setOperation(SearchOperation.in));


        List<SearchParameterEntry> validatedSearchParameters = SQLSearchParameter.getDomainAssociatedSearchParameters(UploadedFile.class, searchParameters.toList(), getEntityManager());

        for (SearchParameterEntry validatedSearchParameter : validatedSearchParameters) {
            if (!validatedSearchParameter.getProperty().contains(".")) {
                validatedSearchParameter.setProperty("uf." + validatedSearchParameter.getProperty());
            }
            if(validatedSearchParameter.getValue() instanceof CytomineDomain) {
                validatedSearchParameter.setValue(((CytomineDomain)validatedSearchParameter.getValue()).getId());
                validatedSearchParameter.setProperty(validatedSearchParameter.getProperty()+"_id");
            }
            if(validatedSearchParameter.getValue() instanceof List) {
                if (((List)validatedSearchParameter.getValue()).size()>0 && ((List)validatedSearchParameter.getValue()).get(0) instanceof CytomineDomain) {
                    List<Long> list = new ArrayList<>();
                    for (Object o : ((List) validatedSearchParameter.getValue())) {
                        list.add(((CytomineDomain)o).getId());
                    }
                    validatedSearchParameter.setValue(list);
                    validatedSearchParameter.setProperty(validatedSearchParameter.getProperty()+"_id");
                }

            }
        }

        SearchParameterProcessed sqlSearchConditions = SQLSearchParameter.searchParametersToSQLConstraints(validatedSearchParameters);
        String search = sqlSearchConditions.getData().stream().map(SearchParameterEntry::getSql).collect(Collectors.joining(" AND "));

        String sort = "";
        if (List.of("content_type", "id", "created", "filename", "originalFilename", "size", "status").contains(sortedProperty)) {
            sort = "uf."+ SQLUtils.toSnakeCase(sortedProperty);
        } else if(sortedProperty.equals("globalSize")) {
            sort = "COALESCE(SUM(DISTINCT tree.size),0)+uf.size";
        } else {
            sort = "uf.created ";
        }
        sort = " ORDER BY " + sort;
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC ";

        String request = "SELECT uf.id, " +
                "uf.content_type, " +
                "uf.created, " +
                "uf.filename, " +
                "uf.original_filename, " +
                "uf.size, " +
                "uf.status, " +
                "uf.storage_id, " +
                "uf.user_id, " +
                "CASE WHEN (nlevel(uf.l_tree) > 0) THEN ltree2text(subltree(uf.l_tree, 0, 1)) ELSE NULL END AS root, " +
                "COUNT(DISTINCT tree.id) AS nb_children, " +
                "COALESCE(SUM(DISTINCT tree.size),0)+uf.size AS global_size, " +
                "CASE WHEN (uf.status = " + UploadedFileStatus.CONVERTED.getCode() + " OR uf.status = " + UploadedFileStatus.DEPLOYED.getCode() + ") " +
                "THEN ai.id ELSE NULL END AS image " +
                "FROM uploaded_file uf " +
                "LEFT JOIN (SELECT *  FROM uploaded_file t " +
                "WHERE EXISTS (SELECT 1 FROM acl_sid AS asi LEFT JOIN acl_entry AS ae ON asi.id = ae.sid " +
                "LEFT JOIN acl_object_identity AS aoi ON ae.acl_object_identity = aoi.id " +
                "WHERE aoi.object_id_identity = t.storage_id AND asi.sid = :username) AND t.deleted IS NULL) " +
                "AS tree ON (uf.l_tree @> tree.l_tree AND tree.id != uf.id) " +
                "LEFT JOIN abstract_image AS ai ON ai.uploaded_file_id = uf.id " +
                "LEFT JOIN uploaded_file AS parent ON parent.id = uf.parent_id " +
                "WHERE EXISTS (SELECT 1 FROM acl_sid AS asi " +
                "LEFT JOIN acl_entry AS ae ON asi.id = ae.sid " +
                "LEFT JOIN acl_object_identity AS aoi ON ae.acl_object_identity = aoi.id " +
                "WHERE aoi.object_id_identity = uf.storage_id AND asi.sid = :username) " +
                "AND (uf.parent_id IS NULL OR parent.content_type similar to '%zip%') " +
                "AND uf.content_type NOT similar to '%zip%' " +
                "AND uf.deleted IS NULL " +
                "AND " +
                search +
                " GROUP BY uf.id, ai.id " +
                sort;


        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
        Map<String, Object> mapParams = sqlSearchConditions.getSqlParameters();
        mapParams.put("username", currentUserService.getCurrentUsername());
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<Tuple> resultList = query.getResultList();
        List<Map<String, Object>> results = new ArrayList<>();
        for (Tuple rowResult : resultList) {
            Map<String, Object> result = new HashMap<>();
            for (TupleElement<?> element : rowResult.getElements()) {
                Object value = rowResult.get(element.getAlias());
                if (value instanceof BigInteger) {
                    value = ((BigInteger)value).longValue();
                }
                result.put(element.getAlias(), value);
            }
            results.add(result);
        }
        return results;

    }

//
//    def listHierarchicalTree(User user, Long rootId) {
//        UploadedFile root = read(rootId)
//        if(!root) {
//            throw new ForbiddenException("UploadedFile not found")
//        }
//        securityACLService.checkAtLeastOne(root, READ)
//        String request = "SELECT uf.id, uf.created, uf.original_filename, uf.content_type, " +
//                "uf.l_tree, uf.parent_id as parent, " +
//                "uf.size, uf.status, " +
//                "array_agg(ai.id) as image, array_agg(asl.id) as slices, array_agg(cf.id) as companion_file " +
//                "FROM uploaded_file uf " +
//                "LEFT JOIN abstract_image ai ON ai.uploaded_file_id = uf.id " +
//                "LEFT JOIN abstract_slice asl ON asl.uploaded_file_id = uf.id " +
//                "LEFT JOIN companion_file cf ON cf.uploaded_file_id = uf.id " +
//                "LEFT JOIN acl_object_identity as aoi ON aoi.object_id_identity = uf.storage_id " +
//                "LEFT JOIN acl_entry as ae ON ae.acl_object_identity = aoi.id " +
//                "LEFT JOIN acl_sid as asi ON asi.id = ae.sid " +
//                "WHERE uf.l_tree <@ '" + root.lTree + "'::text::ltree " +
//                "AND asi.sid = :username " +
//                "AND uf.deleted IS NULL " +
//                "GROUP BY uf.id " +
//                "ORDER BY uf.l_tree ASC "
//
//        def data = []
//        def sql = new Sql(dataSource)
//        sql.eachRow(request, [username: user.username]) { resultSet ->
//                def row = SQLUtils.keysToCamelCase(resultSet.toRowResult())
//            row.lTree = row.lTree.value
//            row.image = row.image.array.find { it != null }
//            row.slices = row.slices.array.findAll { it != null } // A same UF can be linked to several slices (virtual stacks)
//            row.companionFile = row.companionFile.array.find { it != null }
//            row.thumbURL =  null
//            if(row.image) {
//                row.thumbURL = UrlApi.getAbstractImageThumbUrl(row.image as Long)
//                row.macroURL = UrlApi.getAssociatedImage(row.image as Long, "macro", row.contentType as String, 256)
//            } else if (row.slices.size() > 0) {
//                row.thumbURL = UrlApi.getAbstractSliceThumbUrl(row.slices[0] as Long)
//            }
//            data << row
//        }
//        sql.close()
//
//        return data
//    }

    public Optional<UploadedFile> find(Long id) {
        Optional<UploadedFile> uploadedFile = uploadedFileRepository.findById(id);
        uploadedFile.ifPresent(file -> securityACLService.check(file.container(),READ));
        return uploadedFile;
    }

    public UploadedFile get(Long id) {
        return find(id).orElse(null);
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        SecUser currentUser = currentUserService.getCurrentUser();
        if(currentUser instanceof UserJob) {
            currentUser = ((UserJob)currentUser).getUser();
        }
        securityACLService.checkUser(currentUser);
        if (!json.isMissing("storage")) {
            securityACLService.check(json.getJSONAttrLong("storage"), Storage.class, WRITE);
        }
        return executeCommand(new AddCommand(currentUser),null, json);

    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return  Response structure (new domain data, old domain data..)
     */
    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(),WRITE);
        if (jsonNewData.get("storage")!=null && !Objects.equals(jsonNewData.getJSONAttrLong("storage"), ((UploadedFile) domain).getStorage().getId())) {
            securityACLService.check(jsonNewData.getJSONAttrLong("storage"), Storage.class, WRITE);
        }
        return executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
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
        securityACLService.checkUser(currentUser);
        securityACLService.check(domain.container(),WRITE);
        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }


    @Override
    public List<String> getStringParamsI18n(CytomineDomain domain) {
        UploadedFile uploadedFile = (UploadedFile)domain;
        return Arrays.asList(String.valueOf(uploadedFile.getId()), uploadedFile.getFilename());
    }


    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        throw new RuntimeException("not yet implemented");
//        deleteDependentAbstractSlice(domain, transaction, task);
//        deleteDependentAbstractImage(domain, transaction, task);

    }

//
//
//    private void deleteDependentAbstractImage(CytomineDomain domain, Transaction transaction,Task task) {
//        log.info("deleteDependentAbstractImage");
//
//        abstractImageRepository.findAllByUploadedFile((UploadedFile) domain)
//
////
////                .each {
////            abstractImageService.delete(it, transaction, task, false, true)
////        }
//    }

//    def abstractSliceService
//
//    def deleteDependentAbstractSlice(CytomineDomain domain, Transaction transaction, Task task = null) {
//        log.info "deleteDependentAbstractSlice"
//        AbstractSlice.findAllByUploadedFile(uploadedFile).each {
//            abstractSliceService.delete(it, transaction, task, false, true)
//        }
//    }
//
//    def companionFileService
//    def deleteDependentCompanionFile(UploadedFile uploadedFile, Transaction transaction, Task task = null) {
//        CompanionFile.findAllByUploadedFile(uploadedFile).each {
//            companionFileService.delete(it, transaction, task, false)
//        }
//    }
//
//    def deleteDependentUploadedFile(UploadedFile uploadedFile, Transaction transaction,Task task=null) {
//        taskService.updateTask(task,task? "Delete ${UploadedFile.countByParent(uploadedFile)} uploadedFile parents":"")
//
//        // Update all children so that their parent is the grandfather
//        UploadedFile.findAllByParent(uploadedFile).each { child ->
//                child.parent = uploadedFile.parent
//            this.update(child, JSON.parse(child.encodeAsJSON()), transaction)
//        }
//
//        String currentTree = uploadedFile?.lTree ?: ""
//        String request = "UPDATE uploaded_file SET l_tree = '' WHERE id= "+uploadedFile.id+";\n"
//
//        String parentTree = (uploadedFile?.parent?.lTree)?:""
//        if (!parentTree.isEmpty()) {
//            request += "UPDATE uploaded_file " +
//                    "SET l_tree = '" +parentTree +"' || subpath(l_tree, nlevel('" +currentTree +"')) " +
//                    "WHERE l_tree <@ '" +currentTree +"';"
//        }
//
//        def sql = new Sql(dataSource)
//        sql.execute(request)
//        sql.close()
//    }
}
