package be.cytomine.config;

import be.cytomine.utils.LTreeType;
import com.vladmihalcea.hibernate.type.array.IntArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonNodeBinaryType;
import com.vladmihalcea.hibernate.type.json.JsonNodeStringType;
import com.vladmihalcea.hibernate.type.json.JsonStringType;
import org.hibernate.dialect.PostgreSQL10Dialect;
import org.hibernate.spatial.dialect.postgis.PostgisDialect;

import java.sql.Types;

public class CustomPostgreSQLDialect extends PostgisDialect {


    public CustomPostgreSQLDialect () {
        super();
//        registerHibernateType(Types.OTHER, IntArrayType.class.getName());
        registerHibernateType(Types.ARRAY, IntArrayType.class.getName());

        registerHibernateType(Types.OTHER, LTreeType.class.getName());
//        registerHibernateType(Types.OTHER, JsonBinaryType.class.getName());
//        registerHibernateType(Types.OTHER, JsonNodeBinaryType.class.getName());
//        registerHibernateType(Types.OTHER, JsonNodeStringType.class.getName());
    }
}
