package gavinjs.chapter1.helper;

import gavinjs.chapter1.util.CollctionUtil;
import gavinjs.chapter1.util.PropsUtil;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.apache.commons.dbutils.handlers.MapListHandler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public final class DatabaseHelper {

    private static final QueryRunner QUERY_RUNNER;

    private static final ThreadLocal<Connection> CONNECTION_HOLDER;

    private static final BasicDataSource DATA_SOURCE;

    static {

        CONNECTION_HOLDER = new ThreadLocal<Connection>();

        QUERY_RUNNER = new QueryRunner();

        Properties conf = PropsUtil.loadProps("config.properties");
        String driver = conf.getProperty("jdbc.driver");
        String url = conf.getProperty("jdbc.url");
        String username = conf.getProperty("jdbc.username");
        String password = conf.getProperty("jdbc.password");

        DATA_SOURCE = new BasicDataSource();
        DATA_SOURCE.setDriverClassName(driver);
        DATA_SOURCE.setUrl(url);
        DATA_SOURCE.setUsername(username);
        DATA_SOURCE.setPassword(password);
    }

    public static Connection getConnection() {
        Connection conn = CONNECTION_HOLDER.get();
        if (conn == null) {
            try {
                conn = DATA_SOURCE.getConnection();
            } catch (SQLException e) {
//                LOGGER.error("get connection failure", e);
                throw new RuntimeException(e);
            } finally {
                CONNECTION_HOLDER.set(conn);
            }
        }
        return conn;
    }

    //查询实体列表
    //书上少一个conn参数
    //其实是为了让conn对程序员透明，避免线程不安全问题
    public static <T> List<T> queryEntityList(Class<T> entityClass, String sql, Object... params) {
        List<T> entityList;
        try {
            Connection conn = getConnection();
            entityList = QUERY_RUNNER.query(conn, sql, new BeanListHandler<T>(entityClass), params);
        } catch (SQLException e) {
//            LOGGER.error("query entity list failure", e);
            throw new RuntimeException(e);
        }
        return entityList;
    }

    //查询实体
    public static <T> T queryEntity(Class<T> entityClass, String sql, Object...params) {
        T entity;
        try {
            Connection conn = getConnection();
            entity = QUERY_RUNNER.query(conn, sql, new BeanHandler<T>(entityClass),params);
        } catch (SQLException e) {
//            LOGGER.error("query entity failure", e);
            throw new RuntimeException(e);
        }

        return entity;
    }

    //执行查询语句
    public static List<Map<String, Object>> executeQuery(String sql, Object...params){
        List<Map<String, Object>> result;
        try {
            Connection conn = getConnection();
            result = QUERY_RUNNER.query(conn, sql, new MapListHandler(), params);
        } catch (Exception e) {
//            LOGGER.error("execute query failure", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    //执行更新语句 包括update insert delete
    public static int executeUpdate(String sql, Object...params) {
        int rows = 0;
        try {
            Connection conn = getConnection();
            rows = QUERY_RUNNER.update(conn, sql, params);
        } catch (SQLException e) {
//            LOGGER.error("excute update failure", e);
            throw new RuntimeException(e);
        }

        return rows;
    }

    //插入数据
    public static <T> boolean insertEntity(Class<T> entityClass, Map<String, Object> fieldMap) {
        if (CollctionUtil.isEmpty(fieldMap)) {
//            LOGGER.error("cannot insert entity: fieldMap is empty");
            return false;
        }
        String sql = "INSERT INTO " + getTableName(entityClass);
        StringBuilder columns = new StringBuilder("(");
        StringBuilder values = new StringBuilder("(");
        for (String filedName : fieldMap.keySet()) {
            columns.append(filedName).append(",");
            values.append("?,");
        }
        columns.replace(columns.lastIndexOf(","), columns.length(), ")");
        values.replace(values.lastIndexOf(","), values.length(), ")");
        sql += columns + " VALUES " + values;

        Object[] params = fieldMap.values().toArray();

        return executeUpdate(sql, params) == 1;
    }

    //更新实体
    public static <T> boolean updateEntity(Class<T> entityClass, long id, Map<String, Object> fieldMap) {
        if (CollctionUtil.isEmpty(fieldMap)) {
//            LOGGER.error("can not update entity: fieldMap is empty");
            return false;
        }

        String sql = "UPDATE " + getTableName(entityClass) + " SET ";
        StringBuilder columns = new StringBuilder();
        for (String fieldName : fieldMap.keySet()) {
            columns.append(fieldName).append("=?,");
        }
        sql += columns.substring(0, columns.lastIndexOf(",")) + " WHERE id = ?";

        List<Object> paramList = new ArrayList<Object>();
        paramList.addAll(fieldMap.values());
        paramList.add(id);
        Object[] params = paramList.toArray();

        return executeUpdate(sql, params) == 1;
    }

    //删除实体
    public static <T> boolean deleteEntity(Class<T> entityClass, long id) {
        String sql = "DELETE FROM " + getTableName(entityClass) + " WHERE id = ?";
        return executeUpdate(sql, id) == 1;
    }

    private static String getTableName(Class<?> entityClass) {
        return entityClass.getSimpleName();
    }


}
