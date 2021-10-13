package be.cytomine.config;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;

public class CustomIdentifierGenerator extends org.hibernate.id.SequenceGenerator implements IdentifierGenerator {

    @Override
    public String getSequenceName() {
        return "hibernate_sequence";
    }

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object)
            throws HibernateException {
        // TODO Auto-generated method stub
        Serializable id = session.getEntityPersister(null, object)
                .getClassMetadata().getIdentifier(object, session);
        return id != null ? id : super.generate(session, object);
    }
}
