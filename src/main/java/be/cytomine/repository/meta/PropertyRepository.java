package be.cytomine.repository.meta;

import be.cytomine.domain.meta.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PropertyRepository extends JpaRepository<Property, Long>, JpaSpecificationExecutor<Property>  {

    Optional<Property> findByKey(String key);

    Optional<Property> findByDomainIdentAndKey(Long domainIdent, String key);

    List<Property> findAllByDomainIdentAndKeyIn(Long domainIdent, Collection<String> keys);

    void deleteAllByDomainIdentAndKeyIn(Long domainIdent, Collection<String> keys);
}
