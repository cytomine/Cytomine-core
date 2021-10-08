package be.cytomine.service;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.project.Project;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final EntityManager entityManager;

    private final SecUserRepository secUserRepository;


    public String getCurrentUsername() {
        return SecurityUtils.getCurrentUserLogin().get();
    }

    public Optional<SecUser> readCurrentUser() {
        return secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get());
    }

    public SecUser getCurrentUser() {
        return secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get()).orElseThrow(() -> new ServerException("Cannot read current user"));
    }

    boolean isUserAlgo() {
        return getCurrentUser().isAlgo();
    }

//    public CytomineDomain getDomain(Long id,String className) {
//        Class.forName(className, false, Thread.currentThread().contextClassLoader).read(id)
//    }




}
