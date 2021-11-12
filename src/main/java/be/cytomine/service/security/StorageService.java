package be.cytomine.service.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.command.AddCommand;
import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import be.cytomine.service.ModelService;
import be.cytomine.utils.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StorageService extends ModelService {

    private final SecurityACLService securityACLService;

    private final ApplicationContext applicationContext;

    public List<Storage> list(SecUser user, String searchString) {
        return securityACLService.getStorageList(user, false, searchString);
    }


    public void initUserStorage(final SecUser user) {
        log.info ("create storage for $user.username");
        final SecUser finalUser = user;
        SecurityUtils.doWithAuth(applicationContext, user.getUsername(), () -> createStorage(finalUser));
//                {
//                Command c = new AddCommand(user);
//                executeCommand(c,null, JsonObject.of("name", user.getUsername() + " storage", "user", user.getId()));
//        });
    }

    public CommandResponse createStorage(SecUser user) {
        return executeCommand(new AddCommand(user),null, JsonObject.of("name", user.getUsername() + " storage", "user", user.getId()));
    }


    @Override
    public CommandResponse add(JsonObject jsonObject) {
        return null;
    }

    @Override
    public Class currentDomain() {
        return null;
    }

    @Override
    public CytomineDomain createFromJSON(JsonObject json) {
        return null;
    }

    @Override
    public CommandResponse update(CytomineDomain domain, JsonObject jsonNewData, Transaction transaction) {
        return null;
    }

    @Override
    public CommandResponse delete(CytomineDomain domain, Transaction transaction, Task task, boolean printMessage) {
        return null;
    }

    @Override
    public void checkDoNotAlreadyExist(CytomineDomain domain) {

    }
}
