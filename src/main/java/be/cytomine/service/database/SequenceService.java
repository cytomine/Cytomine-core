package be.cytomine.service.database;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import javax.transaction.Transactional;

@Service
@Transactional
public class SequenceService {

//    @Autowired
//    EntityManager entityManager;

    @Autowired
    private DataSource dataSource;



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
//    /**
//     * Get a new id number
//     */
//    Long generateID() throws SQLException {
//        Statement statement = dataSource.getConnection().createStatement();
//        ResultSet res = statement.executeQuery("select nextval('" + SEQ_NAME + "');");
//        res.next();
//        Long nextVal = res.getLong("nextval");
//        return nextVal;
//    }

}
