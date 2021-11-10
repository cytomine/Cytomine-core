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
import be.cytomine.utils.*;
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

    private ImageInstanceRepository ImageInstanceRepository;

    private ImageInstanceRepository imageInstanceRepository;

    private AbstractSliceRepository abstractSliceRepository;

    private SliceInstanceService sliceInstanceService;

    private NestedImageInstanceRepository nestedImageInstanceRepository;

    private SliceInstanceRepository sliceInstanceRepository;

    private SliceCoordinatesService sliceCoordinatesService;


    @Override
    public Class currentDomain() {
        return ImageInstance.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ImageInstance().buildDomainFromJson(json, getEntityManager());
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
        return imageInstanceRepository.getAllImageId(project.getId());
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
        from = "FROM user_image "+ imageInstanceAlias + " ";
        where = "WHERE user_image_id = " + user.getId() + " ";
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
        from = "FROM image_instance " + imageInstanceAlias + " ";
        where = "WHERE "  + imageInstanceAlias + ".project_id = " + project.getId() + " AND " + imageInstanceAlias +".parent_id IS NULL ";
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

        Page<Map<String, Object>> page = new PageImpl<>(results, PageUtils.buildPage(offset, max), count);
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
        AbstractSlice abstractSlice = sliceCoordinatesService.getReferenceSlice(image.getBaseImage());
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
        securityACLService.checkIsNotReadOnly(json.getJSONAttrLong("project"), Project.class);

        json.put("user", currentUser.getId());
        return executeCommand(new AddCommand(currentUser),null, json);

    }

    protected void afterAdd(CytomineDomain domain, CommandResponse response) {
        List<AbstractSlice> abstractSlices = abstractSliceRepository.findAllByImage(((ImageInstance)domain).getBaseImage());
        for (AbstractSlice abstractSlice : abstractSlices) {
            SliceInstance sliceInstance = new SliceInstance();
            sliceInstance.setBaseSlice(abstractSlice);
            sliceInstance.setImage((ImageInstance)domain);
            sliceInstance.setProject(((ImageInstance)domain).getProject());
            sliceInstanceRepository.save(sliceInstance);
        }

    }

    protected void beforeDelete(CytomineDomain domain, CommandResponse response) {
        List<SliceInstance> sliceInstances = sliceInstanceRepository.findAllByImage((ImageInstance)domain);
        sliceInstanceRepository.deleteAll(sliceInstances);

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
        securityACLService.check(domain.container(), READ);
        securityACLService.check(jsonNewData.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), ((ImageInstance)domain).getUser());
        securityACLService.checkIsNotReadOnly(domain.container());
        securityACLService.checkIsNotReadOnly(jsonNewData.getJSONAttrLong("project"), Project.class);

        JsonObject attributes = domain.toJsonObject();
        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);

        ImageInstance imageInstance = (ImageInstance)commandResponse.getObject();

        Double resolutionX = attributes.getJSONAttrDouble("physicalSizeX", null);
        Double resolutionY = attributes.getJSONAttrDouble("physicalSizeY", null);

        boolean resolutionUpdated = (!Objects.equals(resolutionX, imageInstance.getPhysicalSizeX())) || (!Objects.equals(resolutionY, imageInstance.getPhysicalSizeY()));

        if (resolutionUpdated) {
            // TODO: update annotations:
//            def annotations
//            annotations = UserAnnotation.findAllByImage(imageInstance)
//            annotations.each {
//                def json = JSON.parse(it.encodeAsJSON())
//                userAnnotationService.update(it, json)
//            }
//
//            annotations = AlgoAnnotation.findAllByImage(imageInstance)
//            annotations.each {
//                def json = JSON.parse(it.encodeAsJSON())
//                algoAnnotationService.update(it, json)
//            }
//
//            annotations = ReviewedAnnotation.findAllByImage(imageInstance)
//            annotations.each {
//                def json = JSON.parse(it.encodeAsJSON())
//                reviewedAnnotationService.update(it, json)
//            }

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
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), ((ImageInstance)domain).getUser());

        Command c = new DeleteCommand(currentUser, transaction);
        return executeCommand(c,domain, null);
    }

    @Override
    public void deleteDependencies(CytomineDomain domain, Transaction transaction, Task task) {
        ImageInstance imageInstance = (ImageInstance)domain;
        deleteDependentAlgoAnnotation(imageInstance, transaction, task);
        deleteDependentReviewedAnnotation(imageInstance, transaction, task);
        deleteDependentUserAnnotation(imageInstance, transaction, task);
        deleteDependentAnnotationAction(imageInstance, transaction, task);
        deleteDependentLastUserPosition(imageInstance, transaction, task);
        deleteDependentPersistentUserPosition(imageInstance, transaction, task);
        deleteDependentPersistentImageConsultation(imageInstance, transaction, task);
        deleteDependentProperty(imageInstance, transaction, task);
        deleteDependentNestedImageInstance(imageInstance, transaction, task);
        deleteDependentSliceInstance(imageInstance, transaction, task);
        deleteDependentTrack(imageInstance, transaction, task);
    }

    private void deleteDependentAlgoAnnotation(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        AlgoAnnotation.findAllByImage(image).each {
//            algoAnnotationService.delete(it, transaction)
//        }
    }

    private void deleteDependentReviewedAnnotation(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        ReviewedAnnotation.findAllByImage(image).each {
//            reviewedAnnotationService.delete(it, transaction)
//        }
    }

    private void deleteDependentUserAnnotation(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        UserAnnotation.findAllByImage(image).each {
//            userAnnotationService.delete(it, transaction)
//        }
    }



    private void deleteDependentAnnotationAction(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        AnnotationAction.findAllByImage(image).each {
//            it.delete()
//        }
    }

    private void deleteDependentLastUserPosition(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        LastUserPosition.findAllByImage(image).each {
//            it.delete()
//        }
    }

    private void deleteDependentPersistentUserPosition(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        PersistentUserPosition.findAllByImage(image).each {
//            it.delete()
//        }
    }

    private void deleteDependentPersistentImageConsultation(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        PersistentImageConsultation.findAllByImage(image).each {
//            it.delete()
//        }
    }

    private void deleteDependentProperty(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        Property.findAllByDomainIdent(image.id).each {
//            propertyService.delete(it, transaction, null, false)
//        }
    }


    private void deleteDependentNestedImageInstance(ImageInstance image, Transaction transaction, Task task) {
        List<NestedImageInstance> nestedImageInstances = nestedImageInstanceRepository.findAllByParent(image);
        for (NestedImageInstance nestedImageInstance : nestedImageInstances) {
            nestedImageInstanceRepository.delete(nestedImageInstance);
        }
    }



    private void deleteDependentSliceInstance(ImageInstance image, Transaction transaction, Task task) {
        List<SliceInstance> slices = sliceInstanceRepository.findAllByImage(image);
        for (SliceInstance slice : slices) {
            sliceInstanceService.delete(slice, transaction, task, false);
        }
    }

    private void deleteDependentTrack(ImageInstance image, Transaction transaction, Task task) {
        //TODO:
//        Track.findAllByImage(image).each {
//            trackService.delete(it, transaction, task)
//        }
    }

    @Override
    public List<Object> getStringParamsI18n(CytomineDomain domain) {
        return List.of(domain.getId(), ((ImageInstance)domain).getBlindInstanceFilename(), ((ImageInstance)domain).getProject().getName());
    }


    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {
        // TODO: with new session?
        Optional<ImageInstance> imageAlreadyExist = imageInstanceRepository.findByProjectAndBaseImage(((ImageInstance)domain).getProject(), ((ImageInstance)domain).getBaseImage());
        if (imageAlreadyExist.isPresent() && (!Objects.equals(imageAlreadyExist.get().getId(), domain.getId()))) {
            throw new AlreadyExistException("Image " + ((ImageInstance)domain).getBaseImage().getOriginalFilename() + " already map with project " + ((ImageInstance)domain).getProject().getName());
        }
    }
}
