package be.cytomine.service;

import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.Optional;

@Slf4j
@Service
public class CurrentUserService {

    @Autowired
    private SecUserRepository secUserRepository;


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
