package com.fang.home;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by qiaodan on 2017/12/14.
 */
public class DBModel {

    private JSONObject DBSet;
    private String driverclassname; //com.microsoft.sqlserver.jdbc.SQLServerDriver
    private String username; // home_sendorder_test_admin
    private String url;
    private String password; // a67R34c6
    private String type;  //com.alibaba.druid.pool.DruidDataSource

    public DBModel(JSONObject dbset) {
        this.DBSet = dbset;
        this.type = "com.alibaba.druid.pool.DruidDataSource";
        this.password = dbset.get("DBpw").toString();
        this.username = dbset.get("DBusername").toString();

        if (DBSet.get("DBtype").toString().toLowerCase().equals("mysql")){
            this.driverclassname="com.mysql.jdbc.Driver";
            // jdbc:mysql://124.251.50.124:3305/Home_OutboundCall_Test?useUnicode=true&characterEncoding=utf8&useSSL=false
            String connectionStr = "jdbc:"+dbset.get("DBtype")+"://"+dbset.get("DBaddr")+":"+dbset.get("DBport")+"/"+dbset.get("DBname")+"?useUnicode=true&characterEncoding=utf8&useSSL=false";
            this.url = connectionStr.trim();
        }else {
            // jdbc:sqlserver://124.251.46.179:1433;DatabaseName=home_sendorder_test
            this.driverclassname="com.microsoft.sqlserver.jdbc.SQLServerDriver";
            String connectionStr = "jdbc:"+dbset.get("DBtype")+"://"+dbset.get("DBaddr")+":"+dbset.get("DBport")+";DatabaseName="+dbset.get("DBname");
            this.url = connectionStr.trim();
        }
        return;
    }

    public JSONObject getDBSet() {
        return DBSet;
    }

    public void setDBSet(JSONObject DBSet) {
        this.DBSet = DBSet;
    }

    public String getDriverclassname() {
        return driverclassname;
    }

    public void setDriverclassname(String driverclassname) {
        this.driverclassname = driverclassname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
