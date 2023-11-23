package be.cytomine.service.database;

/*
* Copyright (c) 2009-2022. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import java.math.BigInteger;
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
