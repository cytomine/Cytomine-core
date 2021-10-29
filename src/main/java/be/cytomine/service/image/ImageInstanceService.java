package be.cytomine.service.image;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.*;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.dto.SliceCoordinate;
import be.cytomine.dto.SliceCoordinates;
import be.cytomine.exceptions.*;
import be.cytomine.repository.image.*;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.PermissionService;
import be.cytomine.service.command.TransactionService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.CommandResponse;
import be.cytomine.utils.JsonObject;
import be.cytomine.utils.SQLUtils;
import be.cytomine.utils.Task;
import be.cytomine.utils.filters.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.criteria.Join;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.join;
import static org.springframework.security.acls.domain.BasePermission.READ;
import static org.springframework.security.acls.domain.BasePermission.WRITE;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ImageInstanceService extends ModelService {

    private EntityManager entityManager;

    private CurrentUserService currentUserService;

    private CurrentRoleService currentRoleService;

    private SecurityACLService securityACLService;

    private PermissionService permissionService;

    private ImageInstanceRepository ImageInstanceRepository;

    private ImageInstanceRepository imageInstanceRepository;

    private TransactionService transactionService;

    private ImageInstanceService imageInstanceService;

    private CompanionFileRepository companionFileRepository;

    private AbstractSliceRepository abstractSliceRepository;

    private AbstractSliceService abstractSliceService;

    private CompanionFileService companionFileService;

    private AttachedFileService attachedFileService;

    private AttachedFileRepository attachedFileRepository;

    private AttachedFileService attachedFileService;

    private NestedImageInstanceRepository nestedImageInstanceRepository;

    private AbstractImageService abstractImageService;

    private SliceInstanceRepository sliceInstanceRepository;


    @Override
    public Class currentDomain() {
        return ImageInstance.class;
    }


    public Optional<ImageInstance> find(Long id) {
        Optional<ImageInstance> ImageInstance = ImageInstanceRepository.findById(id);
        ImageInstance.ifPresent(image -> securityACLService.check(image.container(),READ));
        return ImageInstance;
    }

    public ImageInstance get(Long id) {
        return find(id).orElse(null);
    }

    public List<Long> getAllImageId(Project project) {
        securityACLService.check(project, READ);
        return imageInstanceRepository.getAllImageId(project);
    }

    private List<SearchParameterEntry> getDomainAssociatedSearchParameters(List<SearchParameterEntry> searchParameters, boolean blinded) {

        for (SearchParameterEntry parameter : searchParameters){
            if(parameter.getProperty().equals("name") || parameter.getProperty().equals("instanceFilename")){
                parameter.setProperty(blinded ? "blindedName" : "instanceFilename");
            }
            if(parameter.getProperty().equals("numberOfJobAnnotations")) {
                parameter.setProperty("countImageJobAnnotations");
            }
            if(parameter.getProperty().equals("numberOfReviewedAnnotations")) {
                parameter.setProperty("countImageReviewedAnnotations");
            }
            if(parameter.getProperty().equals("numberOfAnnotations")) {
                parameter.setProperty("countImageAnnotations");
            }
        }


        List<SearchParameterEntry> validParameters = SQLSearchParameter.getDomainAssociatedSearchParameters(ImageInstance.class, searchParameters, getEntityManager());

        String abstractImageAlias = "ai";
        String imageInstanceAlias = "ii";
        validParameters.addAll(
                SQLSearchParameter.getDomainAssociatedSearchParameters(AbstractImage.class, searchParameters, getEntityManager()).stream().map(
                        x -> new SearchParameterEntry(abstractImageAlias+"."+x.getProperty(), x.getOperation(), x.getValue())).collect(Collectors.toList()));
        validParameters.addAll(
                SQLSearchParameter.getDomainAssociatedSearchParameters(UploadedFile.class, searchParameters, getEntityManager()).stream().map(
                        x -> new SearchParameterEntry("mime."+x.getProperty(), x.getOperation(), x.getValue())).collect(Collectors.toList()));

        for (SearchParameterEntry parameter : searchParameters){
            String property;
            switch(parameter.getProperty()) {
                case "tag" :
                    property = "tda.tag_id";
                    parameter.setValue(SQLSearchParameter.convertSearchParameter(Long.class, parameter.getValue(), entityManager));
                    validParameters.add(new SearchParameterEntry(property, parameter.getOperation(), parameter.getValue()));
                    break;
                default:
                    continue;
            }
        }

        if(searchParameters.size() > 0){
            log.debug("The following search parameters have not been validated: "+searchParameters);
        }

        validParameters.stream().filter(x -> x.getProperty().equals("baseImage")).forEach(searchParameterEntry -> {
            searchParameterEntry.setProperty("base_image_id");
            searchParameterEntry.setValue(((CytomineDomain)searchParameterEntry.getValue()).getId());
        });
        return validParameters;
    }


    public Page<Map<String, Object>> list(User user, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long max, Long offset) {
        securityACLService.checkIsSameUser(user, currentUserService.getCurrentUser());

        String imageInstanceAlias = "ui";
        String abstractImageAlias = "ai";


        if (sortColumn==null) {
            sortColumn = "created";
        }
        if (sortDirection==null) {
            sortDirection = "asc";
        }

        // TODO: move this in controller
        if (sortColumn.equals("numberOfAnnotations")) {
            sortColumn = "countImageAnnotations";
        }
        if (sortColumn.equals("numberOfJobAnnotations")) {
            sortColumn = "countImageJobAnnotations";
        }
        if (sortColumn.equals("numberOfReviewedAnnotations")) {
            sortColumn = "countImageReviewedAnnotations";
        }
        if (sortColumn.equals("name")) {
            sortColumn = "instanceFilename";
        }

        String sortedProperty = ReflectionUtils.findField(ImageInstance.class, sortColumn)!=null ? imageInstanceAlias + "." + sortColumn : null;
        if (sortedProperty==null) sortedProperty = ReflectionUtils.findField(AbstractImage.class, sortColumn)!=null ? abstractImageAlias + "." + sortColumn : null;
        if (sortedProperty==null) throw new CytomineMethodNotYetImplementedException("ImageInstance list sorted by " + sortColumn + " is not implemented");
        sortedProperty = SQLSearchParameter.fieldNameToSQL(sortedProperty);

        List<SearchParameterEntry> validatedSearchParameters = getDomainAssociatedSearchParameters(searchParameters, false);

        validatedSearchParameters.stream().filter(x -> !x.getProperty().contains(".")).forEach(searchParameterEntry -> {
            searchParameterEntry.setProperty(imageInstanceAlias + "." + searchParameterEntry.getProperty());
        });

        validatedSearchParameters.stream().filter(x -> x.getProperty().equals("ui.instanceFilename")).forEach(searchParameterEntry -> {
            searchParameterEntry.setProperty("name");
        });


        final String finalSortedProperty = sortedProperty;
        boolean joinAI = validatedSearchParameters.stream().anyMatch(x -> x.getProperty().contains(abstractImageAlias+".") || finalSortedProperty.contains(abstractImageAlias + "."));

        SearchParameterProcessed sqlSearchConditions = SQLSearchParameter.searchParametersToSQLConstraints(validatedSearchParameters);
        SearchParameterEntry nameSearch = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().equals("name")).findFirst().orElse(null);



        String imageInstanceCondition = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith(imageInstanceAlias + ".")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String abstractImageCondition = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith(abstractImageAlias + ".")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String tagsCondition = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith("tda.")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));

        String select, from, where, search, sort;
        String request;

        select = "SELECT distinct " + imageInstanceAlias + ".* ";
        from = "FROM user_image "+ imageInstanceAlias;
        where = "WHERE user_image_id = " + user.getId();
        search = "";

        if (!imageInstanceCondition.isBlank()) {
            search += " AND ";
            search += imageInstanceCondition;
        }
        if (!abstractImageCondition.isBlank()) {
            search += " AND ";
            search += abstractImageCondition;
        }
        if(!tagsCondition.isBlank()){
            from += "LEFT OUTER JOIN tag_domain_association tda ON ui.id = tda.domain_ident AND tda.domain_class_name = 'be.cytomine.image.ImageInstance' "; //TODO: class name will change!
            search +=" AND ";
            search += tagsCondition;
        }

        if (nameSearch!=null) {
            String operation = "";
            if (nameSearch.getOperation() == SearchOperation.ilike) {
                operation = "ILIKE";
            } else if (nameSearch.getOperation() == SearchOperation.like) {
                operation = "LIKE";
            } else if (nameSearch.getOperation() == SearchOperation.equals) {
                operation = "==";
            }
            search += "AND ( (NOT project_blind AND " + imageInstanceAlias + ".instance_filename " + operation + " :name) " +
                    "  OR (project_blind AND NOT user_project_manager AND base_image_id::text " + operation + " :name) " +
                    "  OR (project_blind AND user_project_manager AND (base_image_id::text " + operation + ":name " +
                    "  OR "+ imageInstanceAlias + ".instance_filename " + operation + ":name)))";
        }

        if (search.contains(imageInstanceAlias + ".instance_filename") || sortedProperty.contains(imageInstanceAlias + ".instance_filename")) {
            joinAI = true;
            search = search.replaceAll(imageInstanceAlias + "\\.instance_filename", "COALESCE("+ imageInstanceAlias + ".instance_filename, "+ abstractImageAlias + ".original_filename)");
        }


        if (sortedProperty.contains(imageInstanceAlias + ".instance_filename")) {
            joinAI = true;
            sortedProperty = sortedProperty.replaceAll(imageInstanceAlias + "\\.instance_filename", "COALESCE(" + imageInstanceAlias + ".instance_filename, " + abstractImageAlias + ".original_filename)");
            select += ", COALESCE(" + imageInstanceAlias + ".instance_filename, " +abstractImageAlias + ".original_filename) ";
        }

        sort = " ORDER BY " + sortedProperty;
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC ";


        if (joinAI) {
            select += ", " + abstractImageAlias + ".* ";
            from += "JOIN abstract_image " + abstractImageAlias +" ON "  + abstractImageAlias + ".id = " + imageInstanceAlias + ".base_image_id ";
        }

        request = select + from + where + search + sort;
        if (max > 0) {
            request += " LIMIT " + max;
        }
        if (offset > 0) {
            request += " OFFSET " + offset;
        }

        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
        Map<String, Object> mapParams = sqlSearchConditions.getSqlParameters();
        if(nameSearch!=null){
            mapParams.put("name", nameSearch.getValue());
        }
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
            result.computeIfPresent("created", (k, v) -> ((Date)v).getTime());
            result.computeIfPresent("updated", (k, v) -> ((Date)v).getTime());
            result.computeIfPresent("reviewStart", (k, v) -> ((Date)v).getTime());
            result.computeIfPresent("reviewStop", (k, v) -> ((Date)v).getTime());

            result.put("reviewUser", result.get("reviewUserId"));
            result.put("baseImage", result.get("baseImageId"));
            result.put("project", result.get("projectId"));

            JsonObject object = ImageInstance.getDataFromDomain(new ImageInstance().buildDomainFromJson(result, entityManager));
            object.put("projectBlind", result.get("projectBlind"));
            object.put("projectName", result.get("projectName"));
            results.add(result);
        }

        request = "SELECT COUNT(DISTINCT " + imageInstanceAlias + ".id) " + from + where + search;
        query = getEntityManager().createNativeQuery(request, Long.class);
        int count = query.getFirstResult();
        Page<Map<String, Object>> page = new PageImpl<>(results, PageRequest.of(offset.intValue(), max.intValue()), count);
        return page;

    }

    public Page<Map<String, Object>> list(Project project, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long max, Long offset, boolean light) {
        securityACLService.check(project, READ);

        String imageInstanceAlias = "ii";
        String abstractImageAlias = "ai";
        String mimeAlias = "mime";

        if (sortColumn==null) {
            sortColumn = "created";
        }
        if (sortDirection==null) {
            sortDirection = "asc";
        }

        if (sortColumn.equals("numberOfAnnotations")) {
            sortColumn = "countImageAnnotations";
        }
        if (sortColumn.equals("numberOfJobAnnotations")) {
            sortColumn = "countImageJobAnnotations";
        }
        if (sortColumn.equals("numberOfReviewedAnnotations")) {
            sortColumn = "countImageReviewedAnnotations";
        }


        String sortedProperty = ReflectionUtils.findField(ImageInstance.class, sortColumn)!=null ? imageInstanceAlias + "." + sortColumn : null;
        if(sortColumn.equals("blindedName")) {
            sortColumn = "id";
        }

        if (sortedProperty==null) sortedProperty = ReflectionUtils.findField(AbstractImage.class, sortColumn)!=null ? abstractImageAlias + "." + sortColumn : null;
        if(sortedProperty==null) sortedProperty = ReflectionUtils.findField(UploadedFile.class, sortColumn)!=null ? mimeAlias + "." + sortColumn : null;
        if (sortedProperty==null) throw new CytomineMethodNotYetImplementedException("ImageInstance list sorted by " + sortColumn + " is not implemented");
        sortedProperty = SQLSearchParameter.fieldNameToSQL(sortedProperty);

        List<SearchParameterEntry> validatedSearchParameters = getDomainAssociatedSearchParameters(searchParameters, project.getBlindMode());

        validatedSearchParameters.stream().filter(x -> !x.getProperty().contains(".")).forEach(searchParameterEntry -> {
            searchParameterEntry.setProperty(imageInstanceAlias + "." + searchParameterEntry.getProperty());
        });

        SearchParameterEntry blindedNameSearch = null;
        boolean manager = false;
        for (SearchParameterEntry parameter : searchParameters){
            if(parameter.getProperty().equals("blindedName")){
                parameter.setProperty("ai.id");
                blindedNameSearch = parameter;
                break;
            }
        }

        final String finalSortedProperty = sortedProperty;
        boolean joinAI = validatedSearchParameters.stream().anyMatch(x -> x.getProperty().contains(abstractImageAlias+".") || finalSortedProperty.contains(abstractImageAlias + "."));
        boolean joinMime = validatedSearchParameters.stream().anyMatch(x -> x.getProperty().contains(mimeAlias+".") || finalSortedProperty.contains(mimeAlias + "."));

        SearchParameterProcessed sqlSearchConditions = SQLSearchParameter.searchParametersToSQLConstraints(validatedSearchParameters);

        String imageInstanceCondition = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith(imageInstanceAlias + ".")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String abstractImageCondition = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith(abstractImageAlias + ".")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String mimeCondition = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith(mimeAlias + ".")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));
        String tagsCondition = sqlSearchConditions.getData().stream().filter(x -> x.getProperty().startsWith("tda.")).map(x -> x.getSql()).collect(Collectors.joining(" AND "));


        if(blindedNameSearch!=null) {
            joinAI = true;
            blindedNameSearch = (SearchParameterEntry)blindedNameSearch.getValue();

            try{
                securityACLService.checkIsAdminContainer(project, currentUserService.getCurrentUser());
                manager = true;
            } catch(ForbiddenException e){}
        }

        String select, from, where, search, sort;
        String request;

        select = "SELECT distinct " + imageInstanceAlias + ".* ";
        from = "FROM image_instance " + imageInstanceAlias;
        where = "WHERE "  + imageInstanceAlias + ".project_id = " + project.getId() + " AND imageInstanceAlias.parent_id IS NULL";
        search = "";

        if (!imageInstanceCondition.isBlank()) {
            search += " AND ";
            search += imageInstanceCondition;
        }
        if (!abstractImageCondition.isBlank()) {
            search += " AND ";
            search += abstractImageCondition;
        }
        if (!mimeCondition.isBlank()) {
            search += " AND ";
            search += mimeCondition;
        }
        if(!tagsCondition.isBlank()){
            from += "LEFT OUTER JOIN tag_domain_association tda ON ii.id = tda.domain_ident AND tda.domain_class_name = 'be.cytomine.image.ImageInstance' "; //TODO: package will change
            search +=" AND ";
            search += tagsCondition;
        }

        if(blindedNameSearch!=null && manager) {
            search +=" AND ";
            search += "("+abstractImageAlias+".id::text ILIKE :name OR  "+ imageInstanceAlias + ".instance_filename ILIKE :name ) ";
        } else if(blindedNameSearch!=null){
            search +=" AND ";
            search += abstractImageAlias+".id::text ILIKE :name ";
        }

        if (search.contains(imageInstanceAlias + ".instance_filename") || sortedProperty.contains(imageInstanceAlias + ".instance_filename")) {
            joinAI = true;
            search = search.replaceAll("imageInstanceAlias" + "\\.instance_filename", "COALESCE(" + imageInstanceAlias + ".instance_filename, " + abstractImageAlias + ".original_filename)");
        }

        if (sortedProperty.contains(imageInstanceAlias + ".instance_filename")) {
            joinAI = true;
            sortedProperty = sortedProperty.replaceAll(imageInstanceAlias + "\\.instance_filename", "COALESCE(" + imageInstanceAlias + ".instance_filename, " + abstractImageAlias + ".original_filename)");
            select += ", COALESCE(" + imageInstanceAlias + ".instance_filename, " + abstractImageAlias + ".original_filename) ";
        }

        sort = " ORDER BY " + sortedProperty;
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC ";

        if (joinAI) {
            select += ", " + abstractImageAlias + ".* ";
            from += "JOIN abstract_image "+ abstractImageAlias + " ON " + abstractImageAlias + ".id = " + imageInstanceAlias + ".base_image_id ";
        }
        if (joinMime) {
            select += ", " + mimeAlias + ".* ";
            from += "JOIN uploaded_file  " + mimeAlias + " ON " + mimeAlias + ".id = " + abstractImageAlias + ".uploaded_file_id ";
        }

        request = select + from + where + search + sort;
        if (max > 0) {
            request += " LIMIT " + max;
        }
        if (offset > 0) {
            request += " OFFSET " + offset;
        }

        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
        Map<String, Object> mapParams = sqlSearchConditions.getSqlParameters();
        if(blindedNameSearch!=null){
            mapParams.put("name", blindedNameSearch.getValue());
        }
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
            result.computeIfPresent("created", (k, v) -> ((Date)v).getTime());
            result.computeIfPresent("updated", (k, v) -> ((Date)v).getTime());
            result.computeIfPresent("reviewStart", (k, v) -> ((Date)v).getTime());
            result.computeIfPresent("reviewStop", (k, v) -> ((Date)v).getTime());

            result.put("reviewUser", result.get("reviewUserId"));
            result.put("baseImage", result.get("baseImageId"));
            result.put("project", result.get("projectId"));
            result.put("user", result.get("userId"));

            JsonObject object = ImageInstance.getDataFromDomain(new ImageInstance().buildDomainFromJson(result, entityManager)); //TODO: select N+1
            object.put("numberOfAnnotations", result.get("countImageAnnotations"));
            object.put("numberOfJobAnnotations", result.get("countImageJobAnnotations"));
            object.put("numberOfReviewedAnnotations", result.get("countImageReviewedAnnotations"));
            object.put("projectBlind", result.get("projectBlind"));
            object.put("projectName", result.get("projectName"));
            results.add(result);
        }

        request = "SELECT COUNT(DISTINCT " + imageInstanceAlias + ".id) " + from + where + search;
        query = getEntityManager().createNativeQuery(request, Long.class);
        int count = query.getFirstResult();

        if(light) {
            List<Map<String,Object>> lightResult = new ArrayList<>();
            for (Map<String, Object> result : results) {
                lightResult.add(JsonObject.of("id", result.get("id"), "instanceFilename", result.get("instanceFilename"), "blindedName", result.get("blindedName")));
            }
            results = lightResult;
        }

        Page<Map<String, Object>> page = new PageImpl<>(results, PageRequest.of(offset.intValue(), max.intValue()), count);
        return page;

    }




    public List<Map<String, Object>> listLight(User user) {
        securityACLService.checkIsSameUser(user,currentUserService.getCurrentUser());
        boolean isAdmin = currentRoleService.isAdminByNow(user);
        String request = "select * from user_image where user_image_id = :id order by instance_filename";
        Query query = getEntityManager().createNativeQuery(request, Tuple.class);
        query.setParameter("id", user.getId());
        List<Tuple> resultList = query.getResultList();

        List<Map<String, Object>> results = new ArrayList<>();
        for (Tuple tuple : resultList) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("id" , tuple.get("id"));
            line.put("projectName" , tuple.get("project_name"));
            line.put("project" , tuple.get("project_id"));
            if (tuple.get("project_blind")!=null && (boolean) tuple.get("project_blind")) {
                line.put("blindedName" , tuple.get("base_image_id"));
            }
            if ((tuple.get("project_blind")==null && !((boolean) tuple.get("project_blind"))) || isAdmin || (boolean)tuple.get("user_project_manager")) {
                line.put("instanceFilename" , tuple.get("instance_filename")!=null ? tuple.get("instance_filename") : tuple.get("original_filename"));
            }
            results.add(line);
        }
        return results;
    }


    public List<Map<String, Object>> listLight(Project project) {
        securityACLService.check(project, READ);
        List<Map<String, Object>> data = new ArrayList<>();
        for (ImageInstance imageInstance : imageInstanceRepository.findAllByProject(project)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", imageInstance.getId());
            row.put("instanceFilename", imageInstance.getInstanceFilename());
            data.add(row);
        }
        return data;

    }

    //TODO:
//    def listLastOpened(User user, Long offset = null, Long max = null) {
//        securityACLService.checkIsSameUser(user, cytomineService.currentUser)
//        def data = []
//
//        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
//        db.persistentImageConsultation.aggregate(
//                [$match: [user: user.id]],
//                [$group: [_id: '$image', "date": [$max: '$created']]],
//                [$sort: [date: -1]],
//                [$limit: (max == null ? 5 : max)]
//        ).results().each {
//            try {
//                ImageInstance image = read(it['_id'])
//
//                data << [id              : it['_id'],
//                        date            : it['date'],
//                        thumb           : UrlApi.getImageInstanceThumbUrl(image.id),
//                        instanceFilename: image.blindInstanceFilename,
//                        project         : image.project.id
//                ]
//            } catch (CytomineException e) {
//                //if user has data but has no access to picture,  ImageInstance.read will throw a forbiddenException
//            }
//        }
//        data = data.sort { -it.date.getTime() }
//        return data
//    }

        // TODO:
//    def listTree(Project project, Long max  = 0, Long offset = 0) {
//        securityACLService.check(project, READ)
//
//        def children = []
//        def images = list(project, null, null, [], max, offset)
//        images.data.each { image ->
//                children << [id: image.id, key: image.id, title: image.instanceFilename, isFolder: false, children: []]
//        }
//        def tree = [:]
//        tree.isFolder = true
//        tree.hideCheckbox = true
//        tree.name = project.getName()
//        tree.title = project.getName()
//        tree.key = project.getId()
//        tree.id = project.getId()
//        tree.children = children
//        tree.size = images.total
//        return tree
//    }



    //TODO:
//    def listExtended(Project project, String sortColumn, String sortDirection, def searchParameters, Long max  = 0, Long offset = 0, def extended) {
//
//        def data = []
//        def images = list(project, sortColumn, sortDirection, searchParameters, max, offset)
//
//        //get last activity grouped by images
//        def user = cytomineService.currentUser
//
//        def db = mongo.getDB(noSQLCollectionService.getDatabaseName())
//        def result = db.persistentImageConsultation.aggregate(
//                [$match: [user: user.id]],
//                [$sort: [created: -1]],
//                [$group: [_id: '$image', created: [$max: '$created'], user: [$first: '$user']]],
//                [$sort: [_id: 1]]
//        )
//
//        def consultations = result.results().collect {
//            [imageId: it['_id'], lastActivity: it['created'], user: it['user']]
//        }
//
//        // we sorted to apply binary search instead of a simple "find" method. => performance
//        def binSearchI = { aList, property, target ->
//                def a = aList
//                def offSet = 0
//        while (!a.empty) {
//            def n = a.size()
//            def m = n.intdiv(2)
//            if (a[m]."$property" > target) {
//                a = a[0..<m]
//            } else if (a[m]."$property" < target) {
//                a = a[(m + 1)..<n]
//                offSet += m + 1
//            } else {
//                return (offSet + m)
//            }
//        }
//        return -1
//        }
//
//        images.data.each { image ->
//                def index
//            def line = image
//            if(extended.withLastActivity) {
//                index = binSearchI(consultations, "imageId", image.id)
//                if (index >= 0) {
//                    line.putAt("lastActivity", consultations[index].lastActivity)
//                } else {
//                    line.putAt("lastActivity", null)
//                }
//            }
//            data << line
//        }
//        images.data = data
//        return images
//    }
//
//
//
//
//
//
//
//
//




    public SliceInstance getReferenceSlice(Long id) {
        ImageInstance image = find(id).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        AbstractSlice abstractSlice = abstractImageService.getReferenceSlice(image.getBaseImage());
        return sliceInstanceRepository.findByBaseSliceAndImage(abstractSlice, image).orElse(null);
    }

    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.check(json.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkisNotReadOnly(json.getJSONAttrLong("project"), Project.class);

        json.put("user", currentUser.getId());


        Optional<ImageInstance> alreadyExist =
                imageInstanceRepository.findByProjectIdAndBaseImageId(json.getJSONAttrLong("project"), json.getJSONAttrLong("baseImage"));

        if (alreadyExist.isPresent()) {

        } else {

        }


        return executeCommand(new AddCommand(currentUser),null, json);

    }


    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    def add(def json) {


        def project = Project.read(json.project)
        def baseImage = AbstractImage.read(json.baseImage)
        def alreadyExist = ImageInstance.findByProjectAndBaseImage(project, baseImage)

        if (alreadyExist && alreadyExist.checkDeleted()) {
            //Image was previously deleted, restore it
            def jsonNewData = JSON.parse(alreadyExist.encodeAsJSON())
            jsonNewData.deleted = null
            Command c = new EditCommand(user: currentUser)
            return executeCommand(c, alreadyExist, jsonNewData)
        }
        else {
            Command c = new AddCommand(user: currentUser)
            return executeCommand(c, null, json)
        }
    }

    def afterAdd(ImageInstance domain, def response) {
        def abstractSlices = AbstractSlice.findAllByImage(domain.baseImage)
        abstractSlices.each {
            new SliceInstance(baseSlice: it, image: domain, project: domain.project).save()
        }
    }

    def beforeDelete(ImageInstance domain) {
        SliceInstance.findAllByImage(domain).each { it.delete() }
    }

    /**
     * Update this domain with new data from json
     * @param domain Domain to update
     * @param jsonNewData New domain datas
     * @return Response structure (new domain data, old domain data..)
     */
    def update(ImageInstance domain, def jsonNewData) {
        securityACLService.check(domain.container(), READ)
        securityACLService.check(jsonNewData.project, Project, READ)
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), domain.user)
        securityACLService.checkisNotReadOnly(domain.container())
        securityACLService.checkisNotReadOnly(jsonNewData.project, Project)
        def attributes = JSON.parse(domain.encodeAsJSON())
        SecUser currentUser = cytomineService.getCurrentUser()
        Command c = new EditCommand(user: currentUser)

        def res = executeCommand(c, domain, jsonNewData)
        ImageInstance imageInstance = res.object

        Double resolutionX = JSONUtils.getJSONAttrDouble(attributes, "physicalSizeX", null)
        Double resolutionY = JSONUtils.getJSONAttrDouble(attributes, "physicalSizeY", null)

        boolean resolutionUpdated = (resolutionX != imageInstance.physicalSizeX) || (resolutionY != imageInstance.physicalSizeY)

        if (resolutionUpdated) {
            def annotations
            annotations = UserAnnotation.findAllByImage(imageInstance)
            annotations.each {
                def json = JSON.parse(it.encodeAsJSON())
                userAnnotationService.update(it, json)
            }

            annotations = AlgoAnnotation.findAllByImage(imageInstance)
            annotations.each {
                def json = JSON.parse(it.encodeAsJSON())
                algoAnnotationService.update(it, json)
            }

            annotations = ReviewedAnnotation.findAllByImage(imageInstance)
            annotations.each {
                def json = JSON.parse(it.encodeAsJSON())
                reviewedAnnotationService.update(it, json)
            }
        }
        return res
    }

    /**
     * Delete this domain
     * @param domain Domain to delete
     * @param transaction Transaction link with this command
     * @param task Task for this command
     * @param printMessage Flag if client will print or not confirm message
     * @return Response structure (code, old domain,..)
     */
    def delete(ImageInstance domain, Transaction transaction = null, Task task = null, boolean printMessage = true) {
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), domain.user)
        SecUser currentUser = cytomineService.getCurrentUser()
//        Command c = new DeleteCommand(user: currentUser, transaction: transaction)
//        return executeCommand(c, domain, null)

        //We don't delete domain, we juste change a flag
        def jsonNewData = JSON.parse(domain.encodeAsJSON())
        jsonNewData.deleted = new Date().time
        Command c = new EditCommand(user: currentUser)
        c.delete = true
        return executeCommand(c,domain,jsonNewData)
    }

    def deleteDependentAlgoAnnotation(ImageInstance image, Transaction transaction, Task task = null) {
        AlgoAnnotation.findAllByImage(image).each {
            algoAnnotationService.delete(it, transaction)
        }
    }

    def deleteDependentReviewedAnnotation(ImageInstance image, Transaction transaction, Task task = null) {
        ReviewedAnnotation.findAllByImage(image).each {
            reviewedAnnotationService.delete(it, transaction, null, false)
        }
    }

    def deleteDependentUserAnnotation(ImageInstance image, Transaction transaction, Task task = null) {
        UserAnnotation.findAllByImage(image).each {
            userAnnotationService.delete(it, transaction, null, false)
        }
    }

    def deleteDependentAnnotationAction(ImageInstance image, Transaction transaction, Task task = null) {
        AnnotationAction.findAllByImage(image).each {
            it.delete()
        }
    }

    def deleteDependentLastUserPosition(ImageInstance image, Transaction transaction, Task task = null) {
        LastUserPosition.findAllByImage(image).each {
            it.delete()
        }
    }

    def deleteDependentPersistentUserPosition(ImageInstance image, Transaction transaction, Task task = null) {
        PersistentUserPosition.findAllByImage(image).each {
            it.delete()
        }
    }

    def deleteDependentPersistentImageConsultation(ImageInstance image, Transaction transaction, Task task = null) {
        PersistentImageConsultation.findAllByImage(image.id).each {
            it.delete()
        }
    }

    def deleteDependentProperty(ImageInstance image, Transaction transaction, Task task = null) {
        Property.findAllByDomainIdent(image.id).each {
            propertyService.delete(it, transaction, null, false)
        }

    }

    def deleteDependentNestedImageInstance(ImageInstance image, Transaction transaction, Task task = null) {
        NestedImageInstance.findAllByParent(image).each {
            it.delete(flush: true)
        }
    }

    def sliceInstanceService
    def deleteDependentSliceInstance(ImageInstance image, Transaction transaction, Task task = null) {
        SliceInstance.findAllByImage(image).each {
            sliceInstanceService.delete(it, transaction, task)
        }
    }

    def trackService
    def deleteDependentTrack(ImageInstance image, Transaction transaction, Task task = null) {
        Track.findAllByImage(image).each {
            trackService.delete(it, transaction, task)
        }
    }

    def getStringParamsI18n(def domain) {
        return [domain.id, domain.blindInstanceFilename, domain.project.name]
    }

    private def getDomainAssociatedSearchParameters(ArrayList searchParameters, boolean blinded) {

        for (def parameter : searchParameters){
            if(parameter.field.equals("name") || parameter.field.equals("instanceFilename")){
                parameter.field = blinded ? "blindedName" : "instanceFilename"
            }
            if(parameter.field.equals("numberOfJobAnnotations")) parameter.field = "countImageJobAnnotations"
            if(parameter.field.equals("numberOfReviewedAnnotations")) parameter.field = "countImageReviewedAnnotations"
            if(parameter.field.equals("numberOfAnnotations")) parameter.field = "countImageAnnotations"
        }


        def validParameters = getDomainAssociatedSearchParameters(ImageInstance, searchParameters)

        String abstractImageAlias = "ai"
        String imageInstanceAlias = "ii"
        validParameters.addAll(getDomainAssociatedSearchParameters(AbstractImage, searchParameters).collect {[operator:it.operator, property:abstractImageAlias+"."+it.property, value:it.value]})
        validParameters.addAll(getDomainAssociatedSearchParameters(UploadedFile, searchParameters).collect {[operator:it.operator, property:"mime."+it.property, value:it.value]})

        loop:for (def parameter : searchParameters){
            String property
            switch(parameter.field) {
                case "tag" :
                    property = "tda.tag_id"
                    parameter.values = convertSearchParameter(Long.class, parameter.values)
                    break
                default:
                    continue loop
            }
            validParameters << [operator: parameter.operator, property: property, value: parameter.values]
        }

        if(searchParameters.size() > 0){
            log.debug "The following search parameters have not been validated: "+searchParameters
        }

        validParameters.findAll { it.property.equals("baseImage") }.each {
            it.property = "base_image_id"
            it.value = it.value.id
        }
        return validParameters
    }























    public SecUser findImageUploaded(Long ImageInstanceId) {
        ImageInstance ImageInstance = find(ImageInstanceId).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", ImageInstanceId));
        return Optional.ofNullable(ImageInstance.getUploadedFile()).map(UploadedFile::getUser).orElse(null);
    }

    /**
     * Check if some instances of this image exists and are still active
     */
    public boolean isImageInstanceUsed(Long ImageInstanceId) {
        ImageInstance domain = find(ImageInstanceId).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", ImageInstanceId));
        boolean usedByImageInstance = imageInstanceRepository.findAllByBaseImage(domain).size() != 0;
        boolean usedByNestedFile = companionFileRepository.findAllByImage(domain).size() != 0;

        return usedByImageInstance || usedByNestedFile;
    }

    /**
     * Returns the list of all the unused abstract images
     */
    public List<ImageInstance> listUnused() {
        return list().getContent().stream().filter(x -> !isImageInstanceUsed(x.getId())).collect(Collectors.toList());
    }

    public Page<ImageInstance> list() {
        return list(null, List.of(), Pageable.unpaged());
    }

    public Page<ImageInstance> list(Project project, List<SearchParameterEntry> searchParameters, Pageable pageable) {
        List<SearchParameterEntry> validSearchParameters = SQLSearchParameter.getDomainAssociatedSearchParameters(ImageInstance.class, searchParameters, getEntityManager());

        Specification<ImageInstance> specification = SpecificationBuilder.getSpecificationFromFilters(validSearchParameters);

        if (!currentRoleService.isAdminByNow(currentUserService.getCurrentUser())) {
            List<Storage> storages = securityACLService.getStorageList(currentUserService.getCurrentUser(), false);
            Specification<ImageInstance> filterStorages = (root, query, criteriaBuilder) -> {
                    Join<ImageInstance, UploadedFile> uploadedFileJoin = root.join(ImageInstance_.uploadedFile);
                    return criteriaBuilder.in(uploadedFileJoin.get("storage")).value(storages);
            };
            specification = specification.and(filterStorages);
        }
        Page<ImageInstance> images = ImageInstanceRepository.findAll(specification, pageable);

        if (project != null) {
            //TODO: could be possible to include in specification (using join)
            if (pageable.isPaged()) {
                throw new WrongArgumentException("Pagination not supported with 'project' parameter");
            } else {
                HashSet<Long> ids = new HashSet<>(ImageInstanceRepository.findAllIdsByProject(project));
                images = new PageImpl<>(images.getContent().stream().filter(x -> ids.contains(x.getId())).collect(Collectors.toList()));

            }
        }

        return images;
    }



    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ImageInstance().buildDomainFromJson(json, getEntityManager());
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }


    /**
     * Add the new domain with JSON data
     * @param json New domain data
     * @return Response structure (created domain data,..)
     */
    public CommandResponse add(JsonObject json) {
        transactionService.start();
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);

        if (!json.isMissing("uploadedFile")) {
            //TODO: ???
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
        securityACLService.check(domain.container(),WRITE);

        JsonObject versionBeforeUpdate = domain.toJsonObject();

        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);
        ImageInstance ImageInstance = (ImageInstance)commandResponse.getObject();

        Integer magnification = versionBeforeUpdate.getJSONAttrInteger("magnification",null);
        Double physicalSizeX = versionBeforeUpdate.getJSONAttrDouble("physicalSizeX",null);

        boolean magnificationUpdated = !Objects.equals(magnification, ImageInstance.getMagnification());
        boolean physicalSizeXUpdated = !Objects.equals(physicalSizeX, ImageInstance.getPhysicalSizeX());

        List<ImageInstance> images = new ArrayList<>();
        if(physicalSizeXUpdated && magnificationUpdated ) {
            if(physicalSizeX!= null && magnification!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> physicalSizeX.equals(x.getPhysicalSizeX()) && magnification.equals(x.getMagnification())));
            } else if(physicalSizeX!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> physicalSizeX.equals(x.getPhysicalSizeX()) && x.getMagnification()==null));
            } else if(magnification!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> magnification.equals(x.getMagnification()) && x.getPhysicalSizeX()==null));
            } else {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> x.getMagnification()==null && x.getPhysicalSizeX()==null));
            }
            for (ImageInstance image : images) {
                JsonObject json = image.toJsonObject();
                json.put("physicalSizeX", ImageInstance.getPhysicalSizeX());
                json.put("magnification", ImageInstance.getMagnification());
                imageInstanceService.update(image, json);
            }
        } else if (physicalSizeXUpdated) {
            if(physicalSizeX!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> physicalSizeX.equals(x.getPhysicalSizeX()) ));
            } else {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> x.getPhysicalSizeX()==null));
            }

            for (ImageInstance image : images) {
                JsonObject json = image.toJsonObject();
                json.put("physicalSizeX", ImageInstance.getPhysicalSizeX());
                imageInstanceService.update(image, json);
            }
        }
        if(magnificationUpdated) {
            if(magnification!= null) {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> magnification.equals(x.getMagnification()) ));
            } else {
                images.addAll(imageInstanceRepository.findAllByBaseImage(ImageInstance, x -> x.getMagnification()==null));
            }

            for (ImageInstance image : images) {
                JsonObject json = image.toJsonObject();
                json.put("magnification", ImageInstance.getMagnification());
                imageInstanceService.update(image, json);
            }
        }
        return commandResponse;
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

        if (!isImageInstanceUsed(domain.getId())) {
            Command c = new DeleteCommand(currentUser, transaction);
            return executeCommand(c,domain, null);
        } else {
            List<ImageInstance> instances = imageInstanceRepository.findAllByBaseImage((ImageInstance) domain);
            throw new ForbiddenException("Abstract Image has instances in active projects : " +
                    instances.stream().map(x -> x.getProject().getName()).collect(Collectors.joining(",")) +
                    " with the following names : " +
                    instances.stream().map(x -> x.getInstanceFilename()).distinct().collect(Collectors.joining(",")),
                    Map.of("projectNames", instances.stream().map(x -> x.getProject().getName()).collect(Collectors.toList()), "imageNames", instances.stream().map(x -> x.getInstanceFilename()).distinct().collect(Collectors.toList())));
        }


    }

    private boolean hasProfile(ImageInstance image) {
        return companionFileRepository.countByImageAndType(image, "HDF5")>0;
    }

    private SliceCoordinates getSliceCoordinates(ImageInstance image) {
        List<AbstractSlice> slices = abstractSliceRepository.findAllByImage(image);
        SliceCoordinates sliceCoordinates = new SliceCoordinates(
                slices.stream().map(AbstractSlice::getChannel).distinct().sorted().collect(Collectors.toList()),
                slices.stream().map(AbstractSlice::getZStack).distinct().sorted().collect(Collectors.toList()),
                slices.stream().map(AbstractSlice::getTime).distinct().sorted().collect(Collectors.toList()),
            );
        return sliceCoordinates;
    }


    private SliceCoordinate getReferenceSliceCoordinate(ImageInstance image) {
        SliceCoordinates sliceCoordinates = getSliceCoordinates(image);
        SliceCoordinate referenceSliceCoordinates = new SliceCoordinate(
                sliceCoordinates.getChannels().get((int) Math.floor(sliceCoordinates.getChannels().size()/2)),
                sliceCoordinates.getZStacks().get((int) Math.floor(sliceCoordinates.getZStacks().size()/2)),
                sliceCoordinates.getTimes().get((int) Math.floor(sliceCoordinates.getTimes().size()/2))
                );
        return referenceSliceCoordinates;
    }

    private AbstractSlice getReferenceSlice(ImageInstance ImageInstance) {
        SliceCoordinate sliceCoordinate = getReferenceSliceCoordinate(ImageInstance);
        return abstractSliceRepository.findByImageAndChannelAndZStackAndTime(ImageInstance, sliceCoordinate.getChannel(), sliceCoordinate.getZStack(), sliceCoordinate.getTime())
                .orElseThrow(() -> new ObjectNotFoundException("AbstractSlice", "image:" + ImageInstance.getId() + "," + sliceCoordinate.getChannel()+ ":" + sliceCoordinate.getZStack() + ":" + sliceCoordinate.getTime()));
    }

    /**
     * Get all image servers for an image id
     */
//    @Deprecated
//    List<String> imageServers(Long ImageInstanceId) {
//        ImageInstance image = find(ImageInstanceId).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", ImageInstanceId));
//        AbstractSlice slice = getReferenceSlice();
//        return [imageServersURLs : [slice?.uploadedFile?.imageServer?.url + "/slice/tile?zoomify=" + slice?.path]]
//    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((ImageInstance)domain).getOriginalFilename());
    }

    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        deleteDependentAbstractSlice((ImageInstance)domain, transaction, task);
        deleteDependentImageInstance((ImageInstance)domain, transaction, task);
        deleteDependentCompanionFile((ImageInstance)domain, transaction, task);
        deleteDependentAttachedFile((ImageInstance)domain, transaction, task);
        deleteDependentNestedImageInstance((ImageInstance)domain, transaction, task);
    }


    private void  deleteDependentAbstractSlice(ImageInstance ai, Transaction transaction, Task task) {
        List<AbstractSlice> slices = abstractSliceRepository.findAllByImage(ai);
        for (AbstractSlice slice : slices) {
            abstractSliceService.delete(slice, transaction, task);
        }

    }

    private void deleteDependentImageInstance(ImageInstance ai, Transaction transaction,Task task) {
        List<ImageInstance> images = imageInstanceRepository.findAllByBaseImage(ai);
        if(!images.isEmpty()) {
            throw new ConstraintException("This image $ai cannot be deleted as it has already been insert " +
                    "in projects " + images.stream().map(x -> x.getProject().getName()).collect(Collectors.joining(",")));
        }
    }


    private void deleteDependentCompanionFile (ImageInstance ai, Transaction transaction, Task task) {
        List<CompanionFile> companionFiles = companionFileRepository.findAllByImage(ai);
        for (CompanionFile companionFile : companionFiles) {
            companionFileService.delete(companionFile, transaction, task);
        }
    }

    private void deleteDependentAttachedFile(ImageInstance ai, Transaction transaction,Task task)  {
        List<AttachedFile> attachedFiles = attachedFileRepository.findAllByImage(ai);
        for (AttachedFile attachedFile : attachedFiles) {
            attachedFileService.delete(attachedFile, transaction, task);
        }
    }

    private void  deleteDependentNestedImageInstance(ImageInstance ai, Transaction transaction,Task task) {
        List<NestedImageInstance> nestedImageInstances = nestedImageInstanceRepository.findAllByBaseImage(ai);
        for(NestedImageInstance nestedImageInstance : nestedImageInstances) {
            nestedImageInstanceRepository.delete(nestedImageInstance);
        }
    }
}
