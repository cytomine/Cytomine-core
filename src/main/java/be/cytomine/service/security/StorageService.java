package be.cytomine.service.security;

import be.cytomine.domain.image.server.Storage;
import be.cytomine.domain.security.SecUser;
import be.cytomine.utils.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StorageService {

    private final SecurityACLService securityACLService;

    public List<Storage> list(SecUser user, String searchString) {
        return securityACLService.getStorageList(user, false, searchString);
    }

}
