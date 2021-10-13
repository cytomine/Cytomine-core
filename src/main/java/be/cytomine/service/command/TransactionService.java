package be.cytomine.service.command;

import be.cytomine.domain.command.Transaction;
import be.cytomine.domain.security.SecUser;
import be.cytomine.exceptions.ServerException;
import be.cytomine.repository.security.SecUserRepository;
import be.cytomine.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {

    private final EntityManager entityManager;

    public Transaction start() {
        synchronized (this.getClass()) {
            //A transaction is a simple domain with a id (= transaction id)
            Transaction transaction = new Transaction();
            entityManager.persist(transaction);
            entityManager.flush();
            return transaction;
        }
    }


}
