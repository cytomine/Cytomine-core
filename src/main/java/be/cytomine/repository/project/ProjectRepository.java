package be.cytomine.repository.project;

import be.cytomine.domain.ontology.Ontology;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.User;
import be.cytomine.dto.NamedCytomineDomain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long>  {

    @Query(value = "select distinct project "+
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, Project as project "+
            "where aclObjectId.objectId = project.id " +
            "and aclEntry.aclObjectIdentity = aclObjectId.id "+
            "and project.ontology = :ontology " +
            "and aclEntry.sid = aclSid.id and aclSid.sid like :username")
    List<Project> findAllProjectForUserByOntology(String username, Ontology ontology);


    @Query(value = "select distinct project "+
            "from AclObjectIdentity as aclObjectId, AclEntry as aclEntry, AclSid as aclSid, Project as project "+
            "where aclObjectId.objectId = project.id " +
            "and aclEntry.aclObjectIdentity = aclObjectId.id "+
            "and aclEntry.sid = aclSid.id and aclSid.sid like :username")
    List<Project> findAllProjectForUser(String username);




    @Query(value = "select id, name from creator_project where user_id = :userId", nativeQuery = true)
    List<NamedCytomineDomain> listByCreator(Long userId);

    default List<NamedCytomineDomain> listByCreator(User user) {
        return listByCreator(user.getId());
    }

    @Query(value = "select id, name from admin_project where user_id = :userId", nativeQuery = true)
    List<NamedCytomineDomain> listByAdmin(Long userId);

    default List<NamedCytomineDomain> listByAdmin(User user) {
        return listByAdmin(user.getId());
    }

    @Query(value = "select id, name from user_project where user_id = :userId", nativeQuery = true)
    List<NamedCytomineDomain> listByUser(Long userId);

    default List<NamedCytomineDomain> listByUser(User user) {
        return listByUser(user.getId());
    }


    List<Project> findAllByOntology(Ontology ontology);
}
