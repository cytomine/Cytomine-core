package be.cytomine.postgresql;

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

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

public class LTreeType implements UserType {

    public int[] sqlTypes() {
        return  new int[] {Types.OTHER};
    }

    @SuppressWarnings("rawtypes")
    public Class returnedClass() {
        return String.class;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        return x!= null && x.equals(y);
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public Object nullSafeGet(ResultSet resultSet, String[] strings, SessionImplementor sessionImplementor, Object o) throws HibernateException, SQLException {
        return resultSet.getString(strings[0]);
    }

    public void nullSafeSet(PreparedStatement preparedStatement, Object o, int i, SessionImplementor sessionImplementor) throws HibernateException, SQLException {
        preparedStatement.setObject(i, o, Types.OTHER);
    }


    public Object deepCopy(Object value) throws HibernateException {
        if(value == null){
            return null;
        }
        return new String((String)value);
    }

    public boolean isMutable() {
        return false;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable)value;
    }

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return deepCopy(original);
    }

}