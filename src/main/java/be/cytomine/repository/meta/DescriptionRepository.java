package be.cytomine.repository.meta;

import be.cytomine.domain.meta.Description;
import be.cytomine.domain.meta.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DescriptionRepository extends JpaRepository<Description, Long>, JpaSpecificationExecutor<Description>  {

    Optional<Description> findByDomainIdentAndDomainClassName(Long id, String className);
}
