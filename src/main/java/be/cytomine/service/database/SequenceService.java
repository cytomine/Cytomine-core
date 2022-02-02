package be.cytomine.service.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Service
@Transactional
public class SequenceService {

//    @Autowired
//    EntityManager entityManager;

    @Autowired
    private EntityManager entityManager;



//    private Session getSession() {
//        return entityManager.unwrap(Session.class);
//    }
//
//
//    private SessionFactory getSessionFactory() {
//        Session session = entityManager.unwrap(Session.class);
//        return session.getSessionFactory().;
//    }

    public final static String SEQ_NAME = "hibernate_sequence";

    /**
     * Create database sequence
     */
//    public void initSequences() {
//        try (Connection connection = dataSource.getConnection()) {
//            Statement statement = connection.createStatement();
//            String createSequenceQuery = "CREATE SEQUENCE " + SEQ_NAME + " START 1;";
//            statement.execute(createSequenceQuery);
//        } catch (SQLException throwables) {
//            throwables.printStackTrace();
//        }
//    }
    /**
     * Get a new id number
     */
    public Long generateID()  {
        Statement statement = null;
        try {
            Query query = entityManager.createNativeQuery("select nextval('" + SEQ_NAME + "');");
            BigInteger val = (BigInteger) query.getSingleResult();
            return val.longValue();
        } catch (Exception e) {
            throw new RuntimeException("Cannot generate ID with sequence: " + e, e);
        }

    }

}
