package be.cytomine.service.command;

import be.cytomine.domain.command.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@Service
@Transactional
public class TransactionService {

    @Autowired
    private EntityManager entityManager;

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
