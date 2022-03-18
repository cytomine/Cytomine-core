package be.cytomine.service;

import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ForbiddenException;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.utils.SecurityUtils;
import be.cytomine.utils.WeakConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Service
public class CurrentUserService {

    @Autowired
    private SecUserRepository secUserRepository;


    public String getCurrentUsername() {
        return SecurityUtils.getCurrentUserLogin().get();
    }

    public Optional<SecUser> readCurrentUser() {
        Optional<SecUser> opt = secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get());
        opt.ifPresent(this::checkAccountStatus);
        return opt;
    }

    public SecUser getCurrentUser() {
        SecUser secUser =  secUserRepository.findByUsernameLikeIgnoreCase(SecurityUtils.getCurrentUserLogin().get()).orElseThrow(() -> new ServerException("Cannot read current user"));
        checkAccountStatus(secUser);
        return secUser;
    }

    private void checkAccountStatus(SecUser secUser) {
        if (secUser.getAccountExpired()) {
            throw new ForbiddenException("Account expired");
        } else if (secUser.getAccountLocked()) {
            throw new ForbiddenException("Account locked");
        } else if (!secUser.getEnabled()) {
            throw new ForbiddenException("Account disabled");
        }

    }

}
