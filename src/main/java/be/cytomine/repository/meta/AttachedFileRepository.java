package be.cytomine.repository.meta;

import be.cytomine.domain.meta.AttachedFile;
import be.cytomine.domain.meta.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttachedFileRepository extends JpaRepository<AttachedFile, Long>, JpaSpecificationExecutor<AttachedFile>  {

    List<AttachedFile> findAllByDomainClassNameAndDomainIdent(String domainClassName, Long domainIdent);

}
