package be.cytomine.repository.meta;

import be.cytomine.domain.meta.Tag;
import be.cytomine.domain.ontology.Ontology;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag>  {

    Optional<Tag> findByNameIgnoreCase(String name);
}
