package be.cytomine.service.security;

import be.cytomine.domain.security.SecRole;
import be.cytomine.repository.security.SecRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional(readOnly = true)
public class SecRoleService {

    @Autowired
    SecurityACLService securityACLService;

    @Autowired
    SecRoleRepository secRoleRepository;

    public Optional<SecRole> find(Long id) {
        securityACLService.checkGuest();
        return secRoleRepository.findById(id);
    }

    public Optional<SecRole> findByAuthority(String authority) {
        securityACLService.checkGuest();
        return secRoleRepository.findByAuthority(authority);
    }

    public List<SecRole> list() {
        securityACLService.checkGuest();
        return secRoleRepository.findAll();
    }
}
