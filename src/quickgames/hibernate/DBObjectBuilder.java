package quickgames.hibernate;

import java.sql.Connection;

public class DBObjectBuilder {

    private Connection m_conn;

    public DBObjectBuilder(Connection conn) {
        m_conn = conn;
    }
//
//    public <DBClass extends DataBaseObject> DBClass select(Class<DBClass> clazz, String... fields){
//
//
//
//    }

    private <DBClass extends DataBaseObject> DBClass m_createObject(Class<DBClass> clazz, int primaryKey) throws DBException {
        DBClass dbObject = null;

        try {
            dbObject = clazz.newInstance();
            dbObject.setConnection(m_conn);
            dbObject.setPrimaryKey(primaryKey);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new DBException(e);
        }

        return dbObject;
    }

    public <DBClass extends DataBaseObject> DBClass create(Class<DBClass> clazz) throws DBException {
        return m_createObject(clazz, 0);
    }

    public <DBClass extends DataBaseObject> DBClass read(Class<DBClass> clazz, int primaryKey) throws DBException {
        DBClass dbObject = m_createObject(clazz, primaryKey);
        dbObject.read();

        return dbObject;
    }

    public boolean write(DataBaseObject dbObject) throws DBException {
        return dbObject.write();
    }

}
