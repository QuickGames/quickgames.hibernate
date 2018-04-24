package quickgames.hibernate;

import quickgames.hibernate.annotation.RecordField;
import quickgames.hibernate.annotation.RecordPrimaryKey;
import quickgames.hibernate.annotation.RecordTable;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.Vector;

public abstract class DataBaseObject {

    private Connection m_conn;

    private int m_primaryKeyValue;
    private String m_primaryKeyName;
    private Field m_fieldPrimaryKey;

    //region CONSTRUCTOR

    protected DataBaseObject() throws DBException {
        // find primary key field
        Class clazz = getClass();
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            RecordPrimaryKey recordPrimaryKey = field.getAnnotation(RecordPrimaryKey.class);
            if (recordPrimaryKey != null) {

                RecordField recordField = field.getAnnotation(RecordField.class);
                if (recordField == null)
                    throw new DBException("Not set RecordField for field: " + field);

                m_primaryKeyName = recordField.value();
                m_fieldPrimaryKey = field;

                break;
            }
        }

        throw new DBException("Not set RecordPrimaryKey for class: " + clazz);
    }

    //endregion

    public boolean select() {
        boolean result = true;

        Class clazz = getClass();


        return result;
    }

    //region SERVICE_METHODS

    void setConnection(Connection conn) {
        m_conn = conn;
    }

    void setPrimaryKey(int primaryKey) throws DBException {

        try {
            m_fieldPrimaryKey.setInt(this, primaryKey);
            m_primaryKeyValue = primaryKey;
        } catch (IllegalAccessException e) {
            throw new DBException(e);
        }

    }

    //endregion

    //region READ

    public void read() throws DBException {
        String SQL_read = m_getSQL_read();
        try {
            PreparedStatement ps = m_conn.prepareStatement(SQL_read);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Class clazz = getClass();
                Field[] fields = clazz.getFields();
                for (Field field : fields) {
                    RecordField recordField = field.getAnnotation(RecordField.class);
                    if (recordField != null) {
                        Class fieldType = (Class) field.getGenericType();
                        try {
                            if (fieldType == int.class)
                                field.set(this, rs.getInt(recordField.value()));
                            else if (fieldType == String.class)
                                field.set(this, rs.getString(recordField.value()));
                        } catch (IllegalAccessException e) {
                            throw new DBException(e);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }
    }

    private String m_getSQL_read() throws DBException {
        Vector<String> result = new Vector<>();

        Class clazz = getClass();

        RecordTable table = (RecordTable) clazz.getAnnotation(RecordTable.class);
        if (table == null)
            throw new DBException("Not set RecordTable for class: " + clazz);

        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            RecordField recordField = field.getAnnotation(RecordField.class);
            if (recordField != null) {
                result.add(recordField.value());
            }
        }

        String[] resultArr = new String[result.size()];
        result.toArray(resultArr);

        return "select " + stringJoin(resultArr)
                + " from " + table.value()
                + " where " + m_primaryKeyName + " = " + m_primaryKeyValue;
    }

    //endregion

    //region WRITE

    private String m_getSQL_write() throws DBException {

        Class clazz = getClass();

        RecordTable table = (RecordTable) clazz.getAnnotation(RecordTable.class);
        if (table == null)
            throw new DBException("Not set RecordTable for class: " + clazz);

        boolean isNew = (m_primaryKeyValue == 0);

        Vector<Object> objectParams = new Vector<>();
        Vector<String> objectFields = new Vector<>();
        Vector<String> questionMarks = new Vector<>();

        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            if (!(isNew && m_fieldPrimaryKey == field)) {
                RecordField recordField = field.getAnnotation(RecordField.class);
                if (recordField != null) {
                    try {
                        objectParams.add(field.get(this));
                        objectFields.add(recordField.value());
                        questionMarks.add("?");
                    } catch (IllegalAccessException e) {
                        throw new DBException(e);
                    }
                }
            }
        }

        Object[] objectParamsArr = new Object[objectParams.size()];
        String[] objectFieldsArr = new String[objectFields.size()];
        String[] questionMarksArr = new String[questionMarks.size()];

        objectParams.toArray(objectParamsArr);
        objectFields.toArray(objectFieldsArr);
        questionMarks.toArray(questionMarksArr);

        return  "replace into " + table.value() + " (" + stringJoin(", ", objectFieldsArr) + ")"
                + " values(" + stringJoin(", ", questionMarksArr) + ")";
    }

    public boolean write() throws DBException {

        String sql_write = m_getSQL_write();

        try {
            PreparedStatement ps = m_conn.prepareStatement(sql_write, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                m_primaryKeyValue = rs.getInt(1);
                try {
                    m_fieldPrimaryKey.setInt(this, m_primaryKeyValue);
                } catch (IllegalAccessException e) {
                    throw new DBException(e);
                }
            }
        } catch (SQLException e) {
            throw new DBException(e);
        }

        return true;
    }

    //endregion

    //region STRING_JOIN

    private String stringJoin(String[] strings) {
        return stringJoin(", ", strings);
    }

    private String stringJoin(String delimiter, String[] strings) {
        StringBuilder sb = new StringBuilder();

        int length = strings.length;
        if (length > 0) {
            sb.append(strings[0]);

            for (int i = 1; i < length; i++) {
                sb.append(delimiter);
                sb.append(strings[i]);
            }
        }

        return sb.toString();
    }

    //endregion

}
