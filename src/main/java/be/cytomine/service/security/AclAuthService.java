package be.cytomine.service.security;

import be.cytomine.domain.CytomineDomain;
import be.cytomine.domain.security.SecUser;
import be.cytomine.repository.security.AclRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AclAuthService {

    private final AclRepository aclRepository;

    public List<Integer> get(Long domainId, SecUser user) {
        return aclRepository.listMaskForUsers(domainId, user.humanUsername());
    }
}
