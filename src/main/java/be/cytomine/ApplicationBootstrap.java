package be.cytomine;

import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import be.cytomine.domain.security.User;
import be.cytomine.repository.image.server.StorageRepository;
import be.cytomine.repository.security.AclRepository;
import be.cytomine.repository.security.SecUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@Slf4j
@RequiredArgsConstructor
class ApplicationBootstrap implements ApplicationListener<ApplicationReadyEvent> {

    private final SecUserRepository secUserRepository;

    private final AclRepository aclRepository;

    private final StorageRepository storageRepository;

    @Override
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("ApplicationListener#onApplicationEvent()");
        for (SecUser user : secUserRepository.findAll()) {
            log.info("User " + user.humanUsername());
        }

        for (Storage storage : storageRepository.findAll()) {
            log.info("Storage " + storage.getName() + " for user " + (storage.getUser()!=null ? storage.getUser().getUsername() : null));
        }

        log.info(aclRepository.listMaskForUsers(90L, "admin").toString());
    }

}