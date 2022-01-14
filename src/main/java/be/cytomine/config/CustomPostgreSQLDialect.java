package be.cytomine.config;

import be.cytomine.utils.LTreeType;
import com.vladmihalcea.hibernate.type.array.IntArrayType;
import com.vladmihalcea.hibernate.type.array.LongArrayType;
import org.hibernate.spatial.dialect.postgis.PostgisDialect;

import java.sql.Types;

public class CustomPostgreSQLDialect extends PostgisDialect {


    public CustomPostgreSQLDialect () {
        super();
//        registerHibernateType(Types.OTHER, IntArrayType.class.getName());
//        registerHibernateType(Types.ARRAY, IntArrayType.class.getName());
        registerHibernateType(Types.ARRAY, LongArrayType.class.getName());
        registerHibernateType(Types.OTHER, LTreeType.class.getName());
//        registerHibernateType(Types.OTHER, JsonBinaryType.class.getName());
//        registerHibernateType(Types.OTHER, JsonNodeBinaryType.class.getName());
//        registerHibernateType(Types.OTHER, JsonNodeStringType.class.getName());
    }
}
