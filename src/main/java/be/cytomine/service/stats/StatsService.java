package be.cytomine.service.stats;

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

import be.cytomine.domain.ontology.AnnotationTerm;
import be.cytomine.domain.ontology.Term;
import be.cytomine.domain.ontology.UserAnnotation;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.domain.social.AnnotationAction;
import be.cytomine.domain.social.PersistentImageConsultation;
import be.cytomine.domain.social.PersistentProjectConnection;
import be.cytomine.repository.ontology.RelationRepository;
import be.cytomine.repository.ontology.TermRepository;
import be.cytomine.repository.ontology.UserAnnotationRepository;
import be.cytomine.service.dto.StorageStats;
import be.cytomine.service.middleware.ImageServerService;
import be.cytomine.service.project.ProjectService;
import be.cytomine.service.security.SecUserService;
import be.cytomine.service.security.SecurityACLService;
import be.cytomine.utils.JsonObject;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static be.cytomine.utils.SQLUtils.castToLong;
import static org.springframework.security.acls.domain.BasePermission.READ;

@Service
@Transactional
public class StatsService {

    @Autowired
    EntityManager entityManager;

    @Autowired
    SecUserService secUserService;

    @Autowired
    ProjectService projectService;

    @Autowired
    UserAnnotationRepository userAnnotationRepository;

    @Autowired
    TermRepository termRepository;

    @Autowired
    RelationRepository relationRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ImageServerService imageServerService;

    @Autowired
    SecurityACLService securityACLService;
    
    public Long total(Class domain){
        return entityManager.createQuery("SELECT COUNT(*) FROM " + domain.getName(), Long.class).getSingleResult();
    }

    public Long numberOfCurrentUsers() {
        return (long)secUserService.getAllOnlineUsers().size();
    }

    public Long numberOfActiveProjects(){
        return (long)projectService.getActiveProjects().size();
    }

    public Optional<JsonObject> mostActiveProjects(){
        return projectService.getActiveProjectsWithNumberOfUsers().stream().max(Comparator.comparing(x -> x.getJSONAttrLong("users")))
                .stream().findFirst();
    }

    public List<JsonObject> statAnnotationTermedByProject(Term term) {
        securityACLService.check(term.container(),READ);

        List<Project> projects = projectService.listByOntology(term.getOntology());

        JsonObject counts = new JsonObject();
        JsonObject percentage = new JsonObject();

        for (Project project : projects) {
            counts.put(project.getName(), 0);
            percentage.put(project.getName(), 0);

            List<Long> layers = secUserService.listLayers(project, null).stream().map(x -> x.getJSONAttrLong("id"))
                    .collect(Collectors.toList());

            if (!layers.isEmpty()) {
                List<UserAnnotation> annotations = userAnnotationRepository.findAllByProjectAndUserIdIn(project, layers);
                for (UserAnnotation annotation : annotations) {
                    if (annotation.getTerms().contains(term)) {
                        Long currentValue = counts.getJSONAttrLong(project.getName(), 0L);
                        counts.put(project.getName(), currentValue + 1);
                    }
                }
            }
        }

        return counts.entrySet().stream().map(x -> JsonObject.of("key", x.getKey(), "value", x.getValue()))
                .collect(Collectors.toList());

    }

    public List<JsonObject> statAnnotationEvolution(Project project, Term term, int daysRange, Date startDate, Date endDate, boolean reverseOrder, boolean accumulate){
        securityACLService.check(project,READ);

        String request = "SELECT created " +
                "FROM UserAnnotation " +
                "WHERE project.id = " + project.getId() + " " +
                (term!=null ? "AND id IN (SELECT userAnnotation.id FROM AnnotationTerm WHERE term = " + term.getId() + ") " : "") +
                (startDate!=null ? " AND created > '"+startDate+"'" : "") +
                (endDate!=null ? " AND created < '"+endDate+"'" : "") +
                " ORDER BY created ASC";


        List<Date> annotationsDates = entityManager.createQuery(request, Date.class).getResultList();

        List<JsonObject> data = aggregateByPeriods(annotationsDates, daysRange, (startDate==null ? project.getCreated() : startDate), (endDate==null ? new Date() : endDate), accumulate);
        if(reverseOrder) {
            Collections.reverse(data);
        }

        return data;
    }

    public List<JsonObject>  statAlgoAnnotationEvolution(Project project, Term term, int daysRange, Date startDate, Date endDate, boolean reverseOrder, boolean accumulate){
        securityACLService.check(project,READ);

        String request = "SELECT created " +
                "FROM AlgoAnnotation " +
                "WHERE project.id = " + project.getId() + " " +
                (term!=null ? "AND id IN (SELECT annotationIdent FROM AlgoAnnotationTerm WHERE term = " + term.getId() + ") " : "") +
                (startDate!=null ? "AND created > '"+startDate+"'" : "") +
                (endDate!=null ? "AND created < '"+endDate+"'" : "") +
                "ORDER BY created ASC";

        List<Date> annotationsDates = entityManager.createQuery(request, Date.class).getResultList();

        List<JsonObject> data = aggregateByPeriods(annotationsDates, daysRange, (startDate==null ? project.getCreated() : startDate), (endDate==null ? new Date() : endDate), accumulate);
        if(reverseOrder) {
            Collections.reverse(data);
        }

        return data;
    }

    public List<JsonObject>  statReviewedAnnotationEvolution(Project project, Term term, int daysRange, Date startDate, Date endDate, boolean reverseOrder, boolean accumulate){
        securityACLService.check(project,READ);

        String request = "SELECT created " +
                "FROM reviewed_annotation " +
                "WHERE project_id = " + project.getId() + " " +
                (term!=null ? "AND id IN (SELECT reviewed_annotation_terms_id FROM reviewed_annotation_term WHERE term_id = " + term.getId() + ") " : "") +
                (startDate!=null ? "AND created > '"+startDate+"'" : "") +
                (endDate!=null ? "AND created < '"+endDate+"'" : "") +
                "ORDER BY created ASC";

        List<java.util.Date> annotationsDates = entityManager.createNativeQuery(request).getResultList();

        List<JsonObject> data = aggregateByPeriods(annotationsDates, daysRange, (startDate==null ? project.getCreated() : startDate), (endDate==null ? new Date() : endDate), accumulate);
        if(reverseOrder) {
            Collections.reverse(data);
        }
        return data;
    }

    public List<JsonObject> statUserSlide(Project project, Date startDate, Date endDate) {
        securityACLService.check(project,READ);
        Session session = ((Session)entityManager.getDelegate());

        Criteria criteria = session.createCriteria(UserAnnotation.class);
        criteria.add(Restrictions.eq("project", project));
        if(startDate!=null) {
            criteria.add(Restrictions.gt("created", startDate));
        }
        if(endDate!=null) {
            criteria.add(Restrictions.lt("created", endDate));
        }
        criteria.setProjection( Projections.projectionList()
                .add( Projections.groupProperty("user.id") )
                .add( Projections.countDistinct("image.id") ));

        List<Object[]> numberOfAnnotatedImagesByUser = criteria.list();

        // Build empty result table
        Map<Long, JsonObject> result = new HashMap<Long, JsonObject>();
        for (JsonObject user : secUserService.listLayers(project, null)) {
            JsonObject item = new JsonObject();
            item.put("id",user.get("id"));
            item.put("key",user.get("firstname") + " " + user.get("lastname"));
            item.put("value",0);
            result.put(item.getId(), item);
        }

        for (Object[] entry : numberOfAnnotatedImagesByUser) {
            JsonObject user = result.get(entry[0]);
            if (user!=null) {
                user.put("value", entry[1]);
            }
        }
        return new ArrayList<>(result.values());
    }

    public List<JsonObject> statTermSlide(Project project, Date startDate, Date endDate) {
        securityACLService.check(project,READ);
        Map<Long,JsonObject> result = new HashMap<>();
        //Get project term
        List<Term> terms = termRepository.findAllByOntology(project.getOntology());

        //build empty result table
        for (Term term : terms) {
            JsonObject item = JsonObject.of(
                    "id", term.getId(),
                    "key", term.getName(),
                    "value", 0,
                    "color", term.getColor()
            );
            result.put(item.getJSONAttrLong("id"), item);
        }


        //add an item for the annotations not associated to any term
        result.put(0L, JsonObject.of("value", 0));

        //Get the number of annotation for each term
        String request = "" +
                "SELECT at.term_id, count(DISTINCT ua.image_id) " +
                "FROM user_annotation ua " +
                "LEFT JOIN annotation_term at " +
                "ON at.user_annotation_id = ua.id " +
                "WHERE ua.project_id = "+project.getId() +" " +
                (startDate!=null ? "AND at.created > '"+startDate+"'" : "") +
                (endDate!=null ? "AND at.created < '"+endDate+"'" : "") +
                "GROUP BY at.term_id ";

        List<Tuple> rows = entityManager.createNativeQuery(request, Tuple.class).getResultList();

        for (Tuple row : rows) {
            JsonObject value = result.get(row.get(0)==null ? 0L : ((BigInteger)row.get(0)).longValue());
            if (value != null) {
                value.put("value", ((BigInteger)row.get(1)).longValue());
            }
        }
        return new ArrayList<>(result.values());
    }

    public List<JsonObject> statPerTermAndImage(Project project, Date startDate, Date endDate) {
        securityACLService.check(project,READ);
        List<JsonObject> result = new ArrayList<>();


        //Get the number of annotation for each term
        String request = "" +
                "SELECT ua.image_id, at.term_id, COUNT(ua.id) as count  " +
                "FROM user_annotation ua " +
                "LEFT JOIN annotation_term at ON at.user_annotation_id = ua.id " +
                "WHERE ua.deleted is NULL and at.deleted is NULL and ua.project_id = "  +project.getId() + " "+
                (startDate!=null ? "AND at.created > '"+startDate+"'" : "") +
                (endDate!=null ? "AND at.created < '"+endDate+"'" : "") +
                "GROUP BY ua.image_id, at.term_id " +
                "ORDER BY ua.image_id, at.term_id ";

        List<Tuple> rows = entityManager.createNativeQuery(request, Tuple.class).getResultList();

        for (Tuple row : rows) {
            JsonObject value = JsonObject.of(
                    "image", castToLong(row.get(0)),
                    "term", castToLong(row.get(1)),
                    "countAnnotations", castToLong(row.get(2))
            );
            result.add(value);
        }
        return result;
    }


    public List<JsonObject> statTerm(Project project, Date startDate, Date endDate, boolean leafsOnly) {
        securityACLService.check(project,READ);
        //Get leaf term (parent term cannot be map with annotation)
        List<Term> terms = leafsOnly ?
                termRepository.findAllLeafTerms(project.getOntology(), relationRepository.getParent()) :
                new ArrayList<>(project.getOntology().getTerms());

        JsonObject stats = new JsonObject();
        JsonObject color = new JsonObject();
        JsonObject ids = new JsonObject();
        JsonObject idsRevert = new JsonObject();
        List<JsonObject> list = new ArrayList<>();

        for (Term term : terms) {
            stats.put(term.getName(), 0);
            color.put(term.getName(), term.getColor());
            ids.put(term.getName(), term.getId());
            idsRevert.put(term.getId().toString(), term.getName());
        }

        //add an item for the annotations not associated to any term
        stats.put("0",0);

        //Get the number of annotation for each term
        String request = "" +
                "SELECT at.term_id, count(*) " +
                "FROM user_annotation ua " +
                "LEFT JOIN annotation_term at " +
                "ON at.user_annotation_id = ua.id " +
                "WHERE ua.project_id = "+project.getId()+" " +
                (startDate!=null ? "AND at.created > '"+startDate+"'" : "") +
                (endDate!=null ? "AND at.created < '"+endDate+"'" : "") +
                "GROUP BY at.term_id ";

        List<Tuple> rows = entityManager.createNativeQuery(request, Tuple.class).getResultList();
        for (Tuple row : rows) {
            if (row.get(0) == null) {
                stats.put("0", ((BigInteger)row.get(1)).longValue());
            } else {
                String name = (String) idsRevert.get(String.valueOf(row.get(0)));
                if (name != null) {
                    stats.put(name, (row.get(1) instanceof BigInteger ? ((BigInteger)row.get(1)).longValue() : row.get(1)));
                }
            }
        }
        //return new ArrayList<>(result.values());

        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            list.add(JsonObject.of("id", ids.get(entry.getKey()), "key", (entry.getKey().equals("0") ? null : entry.getKey()), "value", entry.getValue(), "color", color.get(entry.getKey())));
        }
        return list;
    }

    public List<JsonObject> statUserAnnotations(Project project){
        securityACLService.check(project,READ);
        Map<Long, JsonObject> result = new HashMap<>();

        //Get project terms
        List<Term> terms = termRepository.findAllByOntology(project.getOntology());
        if(terms.isEmpty()) {
            return new ArrayList<>();
        }

        //compute number of annotation for each user and each term
        Session session = ((Session)entityManager.getDelegate());
        Criteria criteria = session.createCriteria(AnnotationTerm.class);
        criteria.add(Restrictions.in("term", terms));
        criteria.createAlias("userAnnotation", "a");
        criteria.add(Restrictions.eq("a.project", project));
        criteria.setProjection( Projections.projectionList()
                .add( Projections.groupProperty("user.id") )
                .add( Projections.groupProperty("term.id") )
                .add( Projections.count("term") ));

        List<Object[]> nbAnnotationsByUserAndTerms = criteria.list();


        for (SecUser user : secUserService.listUsers(project)) {
            JsonObject item = new JsonObject();
            item.put("id", user.getId());
            item.put("key", ((User)user).getFirstname() + " " + ((User)user).getLastname());
            item.put("terms", new ArrayList<>());

            for (Term term : terms) {
                JsonObject t = new JsonObject();
                t.put("id", term.getId());
                t.put("name", term.getName());
                t.put("color", term.getColor());
                t.put("value", 0L);
                ((List<JsonObject>)item.get("terms")).add(t);
            }

            result.put(user.getId(), item);
        }

        for (Object[] row : nbAnnotationsByUserAndTerms) {
            JsonObject user = result.get(row[0]);
            if (user != null) {
                for (JsonObject jsonObject : ((List<JsonObject>) user.get("terms"))) {
                    if (Objects.equals(jsonObject.getJSONAttrLong("id"),row[1])) {
                        jsonObject.put("value", row[2]);
                    }
                }

            }
        }
        return new ArrayList<>(result.values());

    }

    public List<JsonObject> statUser(Project project, Date startDate, Date endDate){
        securityACLService.check(project,READ);
        Map<Long, JsonObject> result = new HashMap<>();


        //compute number of annotation for each user

        //compute number of annotation for each user and each term
        Session session = ((Session)entityManager.getDelegate());
        Criteria criteria = session.createCriteria(UserAnnotation.class);
        criteria.add(Restrictions.eq("project", project));
        if(startDate!=null) {
            criteria.add(Restrictions.gt("created", startDate));
        }
        if(endDate!=null) {
            criteria.add(Restrictions.lt("created", endDate));
        }
        criteria.setFetchMode("user", FetchMode.JOIN); //right join possible ? it will be sufficient

        criteria.setProjection( Projections.projectionList()
                .add( Projections.countDistinct("id") )
                .add( Projections.groupProperty("user.id")));

        List<Object[]> userAnnotations = criteria.list();

        //build empty result table
        for (JsonObject user : secUserService.listLayers(project, null)) {
            JsonObject item = new JsonObject();
            item.put("id",user.get("id"));
            item.put("key",user.get("firstname") + " " + user.get("lastname"));
            item.put("username",user.get("username"));
            item.put("value",0);
            result.put(item.getId(), item);
        }

        //fill result table with number of annotation
        for (Object[] row : userAnnotations) {
            JsonObject user = result.get(row[1]);
            if (user != null) {
                user.put("value", row[0]);

            }
        }
        return new ArrayList<>(result.values());
    }

    public JsonObject statUsedStorage() {
        securityACLService.checkAdmin(secUserService.getCurrentUser());

        Long used = 0L;
        Long available = 0L;

        StorageStats stats = null;
        try {
            stats = imageServerService.storageSpace();
            used += stats.getUsed();
            available += stats.getAvailable();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Long total = used + available;
        double percentage = 0.0;
        if (total > 0) {
            percentage = (double) used / (double) total;
        }

        return JsonObject.of("total", total, "available", available, "used", used, "usedP", percentage);
    }

    public List<JsonObject> statConnectionsEvolution(Project project, int daysRange, Date startDate, Date endDate, boolean accumulate) {
        securityACLService.check(project,READ);
        Query query = new Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("project").is(project.getId()));
        if (startDate!=null && endDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").gte(startDate).lte(endDate));
        } else if (startDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").gte(startDate));
        } else if (endDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").lte(endDate));
        }
        query.with(Sort.by(Sort.Direction.ASC, "created"));

        List<PersistentProjectConnection> persistentProjectConnections = mongoTemplate.find(query, PersistentProjectConnection.class);
        List<Date> createdDates = persistentProjectConnections.stream().map(x -> x.getCreated()).collect(Collectors.toList());
        return this.aggregateByPeriods(createdDates, daysRange, (startDate==null ? project.getCreated() : startDate), (endDate==null ? new Date() : endDate), accumulate);
    }


    public List<JsonObject> statImageConsultationsEvolution(Project project, int daysRange, Date startDate, Date endDate, boolean accumulate) {
        securityACLService.check(project,READ);
        Query query = new Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("project").is(project.getId()));
        if (startDate!=null && endDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").gte(startDate).lte(endDate));
        } else if (startDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").gte(startDate));
        } else if (endDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").lte(endDate));
        }
        query.with(Sort.by(Sort.Direction.ASC, "created"));

        List<PersistentImageConsultation> persistentProjectConnections = mongoTemplate.find(query, PersistentImageConsultation.class);
        List<Date> createdDates = persistentProjectConnections.stream().map(x -> x.getCreated()).collect(Collectors.toList());
        return this.aggregateByPeriods(createdDates, daysRange, (startDate==null ? project.getCreated() : startDate), (endDate==null ? new Date() : endDate), accumulate);
    }

    public List<JsonObject> statAnnotationActionsEvolution(Project project, int daysRange, Date startDate, Date endDate, boolean accumulate, String type) {
        securityACLService.check(project,READ);
        Query query = new Query();
        query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("project").is(project.getId()));
        if (startDate!=null && endDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").gte(startDate).lte(endDate));
        } else if (startDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").gte(startDate));
        } else if (endDate!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("created").lte(endDate));
        }
        if(type!=null) {
            query.addCriteria(org.springframework.data.mongodb.core.query.Criteria.where("action").is(type));
        }
        query.with(Sort.by(Sort.Direction.ASC, "created"));

        List<AnnotationAction> persistentProjectConnections = mongoTemplate.find(query, AnnotationAction.class);
        List<Date> createdDates = persistentProjectConnections.stream().map(x -> x.getCreated()).collect(Collectors.toList());
        return this.aggregateByPeriods(createdDates, daysRange, (startDate==null ? project.getCreated() : startDate), (endDate==null ? new Date() : endDate), accumulate);
    }


    private List<JsonObject> aggregateByPeriods(List<Date> creationDates, int daysRange,  Date startDate, Date endDate, boolean accumulate) {
        List<JsonObject> data = new ArrayList<>();
        int nbItems = creationDates.size();
        int count = 0;
        int idx = 0;

        java.util.Date current = startDate;
        Long endTime = endDate.getTime();
        Calendar cal = Calendar.getInstance();

        //for each period (of duration daysRange), compute the number of items
        while(current.getTime() <= endTime) {
            JsonObject item = new JsonObject();
            item.put("date", current.getTime());

            //add a new step
            cal.setTime(current);
            cal.add(Calendar.DATE, daysRange);
            current = cal.getTime();

            if(!accumulate) {
                count = 0;
            }

            while(idx < nbItems && creationDates.get(idx).getTime() < current.getTime()) {
                idx++;
                count++;
            }

            item.put("endDate", Math.min(current.getTime(), endTime));
            item.put("size", count);
            data.add(item);
        }
        return data;
    }



}
