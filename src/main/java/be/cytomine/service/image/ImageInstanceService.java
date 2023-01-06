package be.cytomine.service.image;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.*;
import be.cytomine.domain.image.*;
import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import be.cytomine.domain.meta.TagDomainAssociation;
import be.cytomine.domain.ontology.*;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.*;
import be.cytomine.repository.image.AbstractSliceRepository;
import be.cytomine.repository.image.ImageInstanceRepository;
import be.cytomine.repository.image.NestedImageInstanceRepository;
import be.cytomine.repository.image.SliceInstanceRepository;
import be.cytomine.repository.meta.AttachedFileRepository;
import be.cytomine.repository.meta.DescriptionRepository;
import be.cytomine.repository.meta.PropertyRepository;
import be.cytomine.repository.meta.TagDomainAssociationRepository;
import be.cytomine.repository.ontology.*;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.repository.security.UserRepository;
import be.cytomine.repositorynosql.social.AnnotationActionRepository;
import be.cytomine.repositorynosql.social.LastUserPositionRepository;
import be.cytomine.repositorynosql.social.PersistentImageConsultationRepository;
import be.cytomine.repositorynosql.social.PersistentUserPositionRepository;
import be.cytomine.service.CurrentRoleService;
import be.cytomine.service.CurrentUserService;
import be.cytomine.service.ModelService;
import be.cytomine.service.dto.ImageInstanceBounds;
import be.cytomine.service.meta.AttachedFileService;
import be.cytomine.service.meta.DescriptionService;
import be.cytomine.service.meta.PropertyService;
import be.cytomine.service.meta.TagDomainAssociationService;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.ontology.*;
import be.cytomine.service.search.ImageSearchExtension;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.*;
import be.cytomine.utils.filters.SQLSearchParameter;
import be.cytomine.utils.filters.SearchOperation;
import be.cytomine.utils.filters.SearchParameterEntry;
import be.cytomine.utils.filters.SearchParameterProcessed;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.service.social.ImageConsultationService.DATABASE_NAME;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Sorts.ascending;
import static com.mongodb.client.model.Sorts.descending;
import static org.springframework.security.acls.domain.BasePermission.READ;

@Slf4j
@Service
@Transactional
public class ImageInstanceService extends ModelService {

    private static List<String> ABSTRACT_IMAGE_COLUMNS_FOR_SEARCH = List.of("width", "height");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CurrentUserService currentUserService;

    @Autowired
    private CurrentRoleService currentRoleService;

    @Autowired
    private SecurityACLService securityACLService;

    @Autowired
    private ImageInstanceRepository imageInstanceRepository;

    @Autowired
    private AbstractSliceRepository abstractSliceRepository;

    @Autowired
    private SliceInstanceService sliceInstanceService;

    @Autowired
    private NestedImageInstanceRepository nestedImageInstanceRepository;

    @Autowired
    private SliceInstanceRepository sliceInstanceRepository;

    @Autowired
    private SliceCoordinatesService sliceCoordinatesService;

    @Autowired
    private UserAnnotationRepository userAnnotationRepository;

    @Autowired
    private AlgoAnnotationRepository algoAnnotationRepository;

    @Autowired
    private ReviewedAnnotationRepository reviewedAnnotationRepository;

    @Autowired
    private UserAnnotationService userAnnotationService;

    @Autowired
    private ReviewedAnnotationService reviewedAnnotationService;

    @Autowired
    private AnnotationActionRepository annotationActionRepository;

    @Autowired
    private PersistentUserPositionRepository persistentUserPositionRepository;

    @Autowired
    private LastUserPositionRepository lastUserPositionRepository;

    @Autowired
    private PersistentImageConsultationRepository persistentImageConsultationRepository;

    @Autowired
    PropertyService propertyService;

    @Autowired
    AnnotationTrackRepository annotationTrackRepository;

    @Autowired
    TrackService trackService;

    @Autowired
    MongoClient mongoClient;

    @Autowired
    DescriptionRepository descriptionRepository;

    @Autowired
    DescriptionService descriptionService;

    @Autowired
    PropertyRepository propertyRepository;

    @Autowired
    TagDomainAssociationRepository tagDomainAssociationRepository;

    @Autowired
    TagDomainAssociationService tagDomainAssociationService;

    @Autowired
    AttachedFileRepository attachedFileRepository;

    @Autowired
    AttachedFileService attachedFileService;

    @Autowired
    SecUserRepository secUserRepository;

    @Autowired
    UserRepository userRepository;


    @Autowired
    AnnotationTermRepository annotationTermRepository;

    @Autowired
    AnnotationTermService annotationTermService;


    private AlgoAnnotationService algoAnnotationService;


    @Autowired
    public void setAlgoAnnotationService(AlgoAnnotationService algoAnnotationService) {
        this.algoAnnotationService = algoAnnotationService;
    }

    @Override
    public Class currentDomain() {
        return ImageInstance.class;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return new ImageInstance().buildDomainFromJson(json, getEntityManager());
    }


    public Optional<ImageInstance> find(Long id) {
        Optional<ImageInstance> ImageInstance = imageInstanceRepository.findById(id);
        ImageInstance.ifPresent(image -> securityACLService.check(image.container(),READ));
        return ImageInstance;
    }

    public ImageInstance get(Long id) {
        return find(id).orElse(null);
    }


    public Optional<ImageInstance> next(ImageInstance imageInstance) {
        return imageInstanceRepository.findTopByProjectAndCreatedLessThanOrderByCreatedDesc(imageInstance.getProject(), imageInstance.getCreated());
    }

    public Optional<ImageInstance> previous(ImageInstance imageInstance) {
        return imageInstanceRepository.findTopByProjectAndCreatedGreaterThanOrderByCreatedAsc(imageInstance.getProject(), imageInstance.getCreated());
    }

    public List<ImageInstance> listByProject(Project project) {
        securityACLService.check(project, READ);
        return imageInstanceRepository.findAllByProject(project);
    }

    public List<Long> getAllImageId(Project project) {
        securityACLService.check(project, READ);
        return imageInstanceRepository.getAllImageId(project.getId());
    }

    public ImageInstanceBounds computeBounds(Project project) {
        securityACLService.check(project, READ);
        ImageInstanceBounds imageInstanceBounds = new ImageInstanceBounds();
        imageInstanceRepository.findAllWithBaseImageUploadedFileByProject(project).forEach(imageInstanceBounds::submit);
        return imageInstanceBounds;
    }

    private List<SearchParameterEntry> getDomainAssociatedSearchParameters(List<SearchParameterEntry> searchParameters, boolean blinded) {
        for (SearchParameterEntry parameter : searchParameters){
            log.debug(parameter.toString());
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
            log.debug(parameter.toString());
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

    public Page<Map<String, Object>> list(SecUser user, List<SearchParameterEntry> searchParameters) {
        return list(user, searchParameters, "created", "desc", 0L, 0L);
    }

    public Page<Map<String, Object>> list(SecUser user, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long max, Long offset) {
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
            from += "LEFT OUTER JOIN tag_domain_association tda ON ui.id = tda.domain_ident AND tda.domain_class_name = 'be.cytomine.domain.image.ImageInstance' ";
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
                    "  OR (project_blind AND NOT user_project_manager AND CAST(base_image_id as text) " + operation + " :name) " +
                    "  OR (project_blind AND user_project_manager AND CAST(base_image_id as text) " + operation + " :name " +
                    "  OR "+ imageInstanceAlias + ".instance_filename " + operation + " :name))";
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
            select += ", " + ABSTRACT_IMAGE_COLUMNS_FOR_SEARCH.stream().map(x -> abstractImageAlias + "." + x).collect(Collectors.joining(",")) + " ";
            from += "JOIN abstract_image " + abstractImageAlias +" ON "  + abstractImageAlias + ".id = " + imageInstanceAlias + ".base_image_id ";
        }

        request = select + from + where + search + sort;
        if (max > 0) {
            request += " LIMIT " + max;
        }
        if (offset > 0) {
            request += " OFFSET " + offset;
        }

        Session session = entityManager.unwrap(Session.class);
        NativeQuery query = session.createNativeQuery(request, Tuple.class);
        Map<String, Object> mapParams = sqlSearchConditions.getSqlParameters();
        if(nameSearch!=null){
            mapParams.put("name", nameSearch.getValue());
        }
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        List<Tuple> resultList = query.list();
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
            // TODO: select N + 1 => see projectService (eagerOntology to load domain directly without fetching database)
            JsonObject object = ImageInstance.getDataFromDomain(new ImageInstance().buildDomainFromJson(result, entityManager));
            object.put("projectBlind", result.get("projectBlind"));
            object.put("projectName", result.get("projectName"));
            results.add(result);
        }

        request = "SELECT COUNT(DISTINCT " + imageInstanceAlias + ".id) " + from + where + search;
        query = session.createNativeQuery(request);
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        long count = ((BigInteger)query.getResultList().get(0)).longValue();
        Page<Map<String, Object>> page = PageUtils.buildPageFromPageResults(results, max, offset, count);
        return page;

    }

    public Page<Map<String, Object>> list(Project project, List<SearchParameterEntry> searchParameters) {
        return list(project, searchParameters, "created", "desc", 0L, 0L, false);
    }

    public Page<Map<String, Object>> list(Project project, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long offset, Long max,  boolean light) {
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
                parameter.setProperty("ii.baseImageId");
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
            blindedNameSearch = ((SearchParameterEntry)blindedNameSearch);

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
            from += "LEFT OUTER JOIN tag_domain_association tda ON ii.id = tda.domain_ident AND tda.domain_class_name = 'be.cytomine.domain.image.ImageInstance' ";
            search +=" AND ";
            search += tagsCondition;
        }

        if(blindedNameSearch!=null && manager) {
            search +=" AND ";
            search += "CAST ("+imageInstanceAlias+".base_image_id AS text) ILIKE :name OR  "+ imageInstanceAlias + ".instance_filename ILIKE :name ) ";
        } else if(blindedNameSearch!=null){
            search +=" AND ";
            search += "CAST ( "+imageInstanceAlias+".base_image_id AS text) ILIKE :name ";
        }

        if (search.contains(imageInstanceAlias + ".instance_filename") || sortedProperty.contains(imageInstanceAlias + ".instance_filename")) {
            joinAI = true;
            search = search.replaceAll(imageInstanceAlias + "\\.instance_filename", "COALESCE(" + imageInstanceAlias + ".instance_filename, " + abstractImageAlias + ".original_filename)");
        }

        if (sortedProperty.contains(imageInstanceAlias + ".instance_filename")) {
            joinAI = true;
            sortedProperty = sortedProperty.replaceAll(imageInstanceAlias + "\\.instance_filename", "COALESCE(" + imageInstanceAlias + ".instance_filename, " + abstractImageAlias + ".original_filename)");
            select += ", COALESCE(" + imageInstanceAlias + ".instance_filename, " + abstractImageAlias + ".original_filename) ";
        }

        sort = " ORDER BY " + sortedProperty;
        sort += (sortDirection.equals("desc")) ? " DESC " : " ASC ";

        if (joinAI) {
            select += ", " + ABSTRACT_IMAGE_COLUMNS_FOR_SEARCH.stream().map(x -> abstractImageAlias + "." + x).collect(Collectors.joining(",")) + " ";
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
        log.debug(request);
        log.debug(mapParams.toString());
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
            results.add(object);
        }

        request = "SELECT COUNT(DISTINCT " + imageInstanceAlias + ".id) " + from + where + search;
        query = getEntityManager().createNativeQuery(request);
        for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
            query.setParameter(entry.getKey(), entry.getValue());
        }
        long count = ((BigInteger)query.getResultList().get(0)).longValue();

        if(light) {
            List<Map<String,Object>> lightResult = new ArrayList<>();
            for (Map<String, Object> result : results) {
                lightResult.add(JsonObject.of("id", result.get("id"), "instanceFilename", result.get("instanceFilename"), "blindedName", result.get("blindedName")));
            }
            results = lightResult;
        }
        Page<Map<String, Object>> page = PageUtils.buildPageFromPageResults(results, max, offset, count);
        return page;

    }




    public List<Map<String, Object>> listLight(SecUser user) {
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
            row.put("instanceFilename", imageInstance.getBlindInstanceFilename());
            data.add(row);
        }
        return data;

    }

    public JsonObject listTree(Project project, Long offset, Long max) {
        securityACLService.check(project, READ);
        List<JsonObject> children = new ArrayList<>();
        Page<Map<String, Object>> images = list(project, new ArrayList<>(), null, null, offset, max, false);
        for (Map<String, Object> image : images.getContent()) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.put("id", image.get("id"));
            jsonObject.put("key", image.get("id"));
            jsonObject.put("title", image.get("instanceFilename"));
            jsonObject.put("isFolder", false);
            jsonObject.put("children", new ArrayList<>());
            children.add(jsonObject);
        }

        JsonObject tree =new JsonObject();
        tree.put("isFolder", true);
        tree.put("hideCheckbox", true);
        tree.put("name", project.getName());
        tree.put("title", project.getName());
        tree.put("key", project.getId());
        tree.put("id", project.getId());
        tree.put("children", children);
        tree.put("size", images.getTotalElements());
        return tree;
    }

    public Page<Map<String, Object>> listExtended(Project project, ImageSearchExtension extension, List<SearchParameterEntry> searchParameters, String sortColumn, String sortDirection, Long offset, Long max) {

        Page<Map<String, Object>> images = list(project, searchParameters, sortColumn, sortDirection, max, offset, false);

        List<Bson> requests = new ArrayList<>();
        requests.add(match(eq("user", currentUserService.getCurrentUser().getId())));
        requests.add(sort(descending("created")));
        requests.add(group("$image", Accumulators.max("created", "$created"), Accumulators.first("user", "$user")));
        requests.add(sort(ascending("_id")));

        MongoCollection<Document> persistentImageConsultation = mongoClient.getDatabase(DATABASE_NAME).getCollection("persistentImageConsultation");

        List<Document> results = persistentImageConsultation.aggregate(requests)
                .into(new ArrayList<>());
        List<Date> consultations = results.stream().map(x -> (Date)x.get("created"))
                .collect(Collectors.toList());
        List<Long> ids = results.stream().map(x -> x.getLong("_id")).collect(Collectors.toList());

        for (Map<String, Object> item : images.getContent()) {
            if (extension.isWithLastActivity()) {
                int index = Collections.binarySearch(ids, (Long)item.get("id"));
                if (index >= 0) {
                    item.put("lastActivity", consultations.get(index));
                }
            }
        }
        return images;
    }

    public SliceInstance getReferenceSlice(Long id) {
        ImageInstance image = find(id).orElseThrow(() -> new ObjectNotFoundException("ImageInstance", id));
        return getReferenceSlice(image);
    }

    public SliceInstance getReferenceSlice(ImageInstance imageInstance) {
        AbstractSlice abstractSlice = sliceCoordinatesService.getReferenceSlice(imageInstance.getBaseImage());
        return sliceInstanceRepository.findByBaseSliceAndImage(abstractSlice, imageInstance).orElse(null);
    }


        /**
         * Add the new domain with JSON data
         * @param json New domain data
         * @return Response structure (created domain data,..)
         */
    public CommandResponse add(JsonObject json) {
        if(json.isMissing("baseImage")) throw new WrongArgumentException("abstract image not set");
        if(json.isMissing("project")) throw new WrongArgumentException("project not set");
        SecUser currentUser = currentUserService.getCurrentUser();
        securityACLService.checkUser(currentUser);
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
        securityACLService.checkUser(currentUser);
        securityACLService.check(jsonNewData.getJSONAttrLong("project"), Project.class, READ);
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), ((ImageInstance)domain).getUser());
        securityACLService.checkIsNotReadOnly(domain.container());
        securityACLService.checkIsNotReadOnly(jsonNewData.getJSONAttrLong("project"), Project.class);

        jsonNewData.putIfAbsent("user", ((ImageInstance)domain).getUser().getId());

        JsonObject attributes = domain.toJsonObject();
        CommandResponse commandResponse = executeCommand(new EditCommand(currentUser, transaction), domain,jsonNewData);

        ImageInstance imageInstance = (ImageInstance)commandResponse.getObject();

        Double resolutionX = attributes.getJSONAttrDouble("physicalSizeX", null);
        Double resolutionY = attributes.getJSONAttrDouble("physicalSizeY", null);

        boolean resolutionUpdated = (!Objects.equals(resolutionX, imageInstance.getPhysicalSizeX())) || (!Objects.equals(resolutionY, imageInstance.getPhysicalSizeY()));

        if (resolutionUpdated) {
            for (AlgoAnnotation algoAnnotation : algoAnnotationRepository.findAllByImage(imageInstance)) {
                algoAnnotationService.update(algoAnnotation, algoAnnotation.toJsonObject());
            }
            for (ReviewedAnnotation reviewedAnnotation : reviewedAnnotationRepository.findAllByImage(imageInstance)) {
                reviewedAnnotationService.update(reviewedAnnotation, reviewedAnnotation.toJsonObject());
            }
            for (UserAnnotation userAnnotation : userAnnotationRepository.findAllByImage(imageInstance)) {
                userAnnotationService.update(userAnnotation, userAnnotation.toJsonObject());
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
        securityACLService.check(domain.container(), READ);
        securityACLService.checkFullOrRestrictedForOwner(domain.container(), ((ImageInstance)domain).getUser());

        Project project = ((ImageInstance) domain).getProject();
        if (Lock.getInstance().lockProject(project)) {
            try {
                log.debug("Delete image " + domain.getId());
                Command c = new DeleteCommand(currentUser, transaction);
                return executeCommand(c,domain, null);
            } finally {
                Lock.getInstance().unlockProject(project);
            }
        } else {
            throw new ServerException("Cannot acquire lock for project " + project.getId()  + " , tryLock return false");
        }
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
        deleteDependentMetadata(imageInstance, transaction, task);
        deleteDependentDescription(imageInstance, transaction, task);
        deleteDependentAttachedFile(imageInstance, transaction, task);
        deleteDependentTagDomainAssociation(imageInstance, transaction, task);
        deleteDependentNestedImageInstance(imageInstance, transaction, task);
        deleteDependentSliceInstance(imageInstance, transaction, task);
        deleteDependentTrack(imageInstance, transaction, task);
    }

    private void deleteDependentAlgoAnnotation(ImageInstance image, Transaction transaction, Task task) {
        for (AlgoAnnotation algoAnnotation : algoAnnotationRepository.findAllByImage(image)) {
            algoAnnotationService.delete(algoAnnotation, transaction, task, false);
        }
    }

    private void deleteDependentReviewedAnnotation(ImageInstance image, Transaction transaction, Task task) {
        for (ReviewedAnnotation reviewedAnnotation : reviewedAnnotationRepository.findAllByImage(image)) {
            reviewedAnnotationService.delete(reviewedAnnotation, transaction, task, false);
        }
    }

    private void deleteDependentUserAnnotation(ImageInstance image, Transaction transaction, Task task) {
        for (UserAnnotation userAnnotation : userAnnotationRepository.findAllByImage(image)) {
            log.debug("Delete userAnnotation : " + userAnnotation.getUser());
            userAnnotationService.delete(userAnnotation, transaction, task, false);
        }
    }



    private void deleteDependentAnnotationAction(ImageInstance image, Transaction transaction, Task task) {
        annotationActionRepository.deleteAllByImage(image.getId());
    }

    private void deleteDependentLastUserPosition(ImageInstance image, Transaction transaction, Task task) {
        lastUserPositionRepository.deleteAllByImage(image.getId());
    }

    private void deleteDependentPersistentUserPosition(ImageInstance image, Transaction transaction, Task task) {
        persistentUserPositionRepository.deleteAllByImage(image.getId());
    }

    private void deleteDependentPersistentImageConsultation(ImageInstance image, Transaction transaction, Task task) {
        persistentImageConsultationRepository.deleteAllByImage(image.getId());
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
        for (Track track : trackService.list(image)) {
            trackService.delete(track, transaction, task, false);
        }
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

    public void startReview(ImageInstance imageInstance) {
        securityACLService.checkFullOrRestrictedForOwner(imageInstance,imageInstance.getUser());
        imageInstance.setReviewStart(new Date());
        imageInstance.setReviewUser(currentUserService.getCurrentUser());
        if (imageInstance.getReviewUser()!=null && imageInstance.getReviewUser().isAlgo()) {
            throw new WrongArgumentException("The review user " + imageInstance.getReviewUser() + " is not a real user (a userjob)");
        }
        saveDomain(imageInstance);
    }

    public void stopReview(ImageInstance imageInstance, boolean cancelReview) {
        if (imageInstance.getReviewStart() == null || imageInstance.getReviewUser() == null) {
            throw new WrongArgumentException("Image is not in review mode: image.reviewStart="+imageInstance.getReviewStart()+" and image.reviewUser=" + imageInstance.getReviewUser());
        }
        if (!currentUserService.getCurrentUser().getId().equals(imageInstance.getReviewUser().getId())) {
            throw new WrongArgumentException("Review can only be validate or stop by "+imageInstance.getReviewUser().getUsername());
        }

        if (cancelReview) {
            if (imageInstance.getReviewStop()==null) {
                //cancel reviewing
                imageInstance.setReviewStart(null);
                imageInstance.setReviewUser(null);
            }
            imageInstance.setReviewStop(null);
        } else {
            imageInstance.setReviewStop(new Date());
        }
        saveDomain(imageInstance);
    }

    public CommandResponse cloneImage(ImageInstance imageinstance, Project project, Boolean areImageMetadataCopied, Boolean areAnnotationsCopied, Boolean areAnnotationsMetadataCopied, List<Long> usersArray, Boolean areAllLayersCopiedOnCurrentUserLayer, Boolean areMissingLayersCopiedOnCurrentUserLayer, List<Map<String, Object>> annotationsTranfertMap) throws ClassNotFoundException {

        log.debug("Clone image {} in project {}", imageinstance.getId(), project.getId());
        log.debug("Copy metadata ? {}", areImageMetadataCopied);
        log.debug("Copy annotation ? {}", areAnnotationsCopied);
        log.debug("Copy annotation metadata ? {}", areAnnotationsMetadataCopied);
        log.debug("Copy transfert map ? {}", annotationsTranfertMap);

        securityACLService.checkFullOrRestrictedForOwner(project, currentUserService.getCurrentUser());
        securityACLService.checkIsAdminContainer(project, currentUserService.getCurrentUser());
        securityACLService.checkIsAdminContainer(imageinstance.getProject(),currentUserService.getCurrentUser());

        CommandResponse result = new CommandResponse();

        //if imageInstance exist, copy metadata on it
        Optional<ImageInstance> alreadyExistingImage = imageInstanceRepository.findByProjectAndBaseImage(project, imageinstance.getBaseImage());
        if (alreadyExistingImage.isPresent()) {
            result.setStatus(200);
            result.setData(JsonObject.of("imageinstance", imageinstance.toJsonObject()));
        } else {
            JsonObject json = imageinstance.toJsonObject();
            json.put("project", project.getId());
            json.remove("id");
            json.remove("updated");
            result = add(json);
        }

        ImageInstance copiedImage = get(Long.parseLong(((Map<String,Object>)result.getData().get("imageinstance")).get("id")+""));

        //step 2 : fetch & create the description, properties
        if(areImageMetadataCopied){
            this.copyMetadata(imageinstance, copiedImage);
        }

        //step 3 & 4 : copyAnnotation and metadata
        if(areAnnotationsCopied){
            if(usersArray==null || usersArray.isEmpty()){
                usersArray = List.of(currentUserService.getCurrentUser().getId());
            }

            List<User> users = userRepository.findAllById(usersArray);
            log.debug("Clone annotations for {} users", users.size());
            List<UserAnnotation> annotations = userAnnotationRepository.findAllByImageAndUserIn(imageinstance, users);
            log.debug("Clone {} annotations", annotations.size());
            for (UserAnnotation annotation : annotations) {
                Long destinationId = annotationsTranfertMap.stream().filter(x -> Long.valueOf(x.get("source") + "").equals(annotation.getUser().getId())).map(x -> Long.parseLong(x.get("destination") + "")).findFirst().orElse(null);
                this.copyAnnotation(annotation, copiedImage, null, null, areAllLayersCopiedOnCurrentUserLayer, areMissingLayersCopiedOnCurrentUserLayer, areAnnotationsMetadataCopied, true, destinationId);
            }
        }
        return result;
    }

    private void copyMetadata(CytomineDomain based, CytomineDomain dest) throws ClassNotFoundException {

        //copy description
        descriptionRepository.deleteAllByDomainIdent(dest.getId());
        for (Description description : descriptionRepository.findAllByDomainIdent(based.getId())) {
            JsonObject jsonObject = description.toJsonObject();
            jsonObject.remove("updated");
            jsonObject.remove("id");
            jsonObject.put("domainIdent", dest.getId());
            descriptionService.add(jsonObject);
        }

        //copy properties
        propertyRepository.deleteAllByDomainIdent(dest.getId());
        for (Property property : propertyRepository.findAllByDomainIdent(based.getId())) {
            JsonObject jsonObject = property.toJsonObject();
            jsonObject.remove("updated");
            jsonObject.remove("id");
            jsonObject.put("domainIdent", dest.getId());
            propertyService.add(jsonObject);
        }

        //copy tags
        tagDomainAssociationRepository.deleteAllByDomainIdent(dest.getId());
        for (TagDomainAssociation property : tagDomainAssociationRepository.findAllByDomainIdent(based.getId())) {
            JsonObject jsonObject = property.toJsonObject();
            jsonObject.remove("updated");
            jsonObject.remove("id");
            jsonObject.put("domainIdent", dest.getId());
            tagDomainAssociationService.add(jsonObject);
        }

        //copy attached files
        attachedFileRepository.deleteAllByDomainIdent(dest.getId());
        for (AttachedFile attachedFile : attachedFileRepository.findAllByDomainIdent(based.getId())) {
            attachedFileService.create(attachedFile.getFilename(), attachedFile.getData(), attachedFile.getKey(), dest.getId(), attachedFile.getDomainClassName());
        }
    }



    private void copyAnnotation(UserAnnotation based, ImageInstance dest, List<SecUser> usersProject, SecUser currentUser,Boolean giveMe,
                                Boolean giveMeMissingAnnot, Boolean areAnnotationsMetadataCopied, Boolean takeAllTerms, Long destinationUser) throws ClassNotFoundException {
        log.info("copyAnnotation=${}", based.getId());

        if(currentUser==null) {
            currentUser = currentUserService.getCurrentUser();
        }
        if(usersProject==null || usersProject.isEmpty()) {
            usersProject = List.of(currentUser);
        }
        if(destinationUser==null) {
            destinationUser = based.getUser().getId();
        }

        boolean userInDestProject = securityACLService.getProjectList(secUserRepository.findById(destinationUser).get(), null).contains(dest.getProject());

        log.debug("copyAnnotation=${}", based.getId());

        if(!giveMe && !giveMeMissingAnnot) {
            if(!userInDestProject) {
                log.debug("user is not in dest project, skip");
                return;
            }
        }

        //copy annotation
        JsonObject jsonObject = based.toJsonObject();
        jsonObject.remove("updated");
        jsonObject.remove("term");
        jsonObject.put("image", dest.getId());

        AbstractSlice abSliceOrigin = based.getSlice().getBaseSlice();
        AbstractSlice abSliceDest = abstractSliceRepository.findByImageAndChannelAndZStackAndTime(dest.getBaseImage(),  abSliceOrigin.getChannel(), abSliceOrigin.getTime(), abSliceOrigin.getZStack()).get();

        jsonObject.put("slice", sliceInstanceRepository.findByBaseSliceAndImage(abSliceDest, dest).get().getId());
        jsonObject.put("project", dest.getProject().getId());
        jsonObject.put("user", destinationUser);

        if(giveMe || (!userInDestProject && giveMeMissingAnnot)) {
            jsonObject.put("user", currentUser.getId());
        }

        CommandResponse result = userAnnotationService.add(jsonObject);
        UserAnnotation annotation = userAnnotationRepository.getById(result.getObject().getId());


        for (AnnotationTerm basedAT : annotationTermRepository.findAllByUserAnnotation(based)) {
            if(basedAT.getTerm().getOntology()==dest.getProject().getOntology()) {
                if(takeAllTerms || usersProject.contains(basedAT.getUser().getId())){
                    JsonObject json = basedAT.toJsonObject();
                    json.remove("updated");
                    json.remove("id");
                    json.put("userannotation", annotation.getId());
                    annotationTermService.add(json);
                }
            }
        }

        if(areAnnotationsMetadataCopied) {
            this.copyMetadata(based, annotation);
        }
    }

}
