package com.here.naksha.activitylog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.here.xyz.XyzSerializable;
import com.here.xyz.models.geojson.implementation.Feature;
import com.here.mapcreator.ext.naksha.PsqlDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDBWriter {
    public static void fromActicityLogDBToFeature(PsqlDataSource dataSourceLocalHost,PsqlDataSource dataSourceActivityLog, String tableName, Integer limit) {
        String schema = dataSourceActivityLog.getSchema();
        List<String> featureList = new ArrayList<>();
        List<String> geoList = new ArrayList<>();
        List<Integer> iList = new ArrayList<>();
        int intMaxIFeaturesTable = 0;
        int intMaxIActivityTable = 0;
        try (Connection conn = dataSourceActivityLog.getConnection()) {
            String SQLMaxIActivityTable = sqlQueryMaxIActivity(schema,tableName);
            try (final PreparedStatement stmt = conn.prepareStatement(SQLMaxIActivityTable)) {
                final ResultSet result = stmt.executeQuery();
                if (result!=null && result.next()) {
                    intMaxIActivityTable = result.getInt(1);
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            //Assigning this value for testing purpose
            intMaxIActivityTable=100;
            try (Connection connLocalHost = dataSourceLocalHost.getConnection()) {
                String queryPreRequisite = sqlQueryPreRequisites(schema);
                sqlExecute(connLocalHost,queryPreRequisite);
                String SQLMaxIFeaturesTable = sqlQuerySelectMaxIFeatures();
                try (final PreparedStatement stmt = connLocalHost.prepareStatement(SQLMaxIFeaturesTable)) {
                    final ResultSet result = stmt.executeQuery();
                    if (result!=null && result.next()) {
                        intMaxIFeaturesTable = result.getInt(1);
                    }
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                Integer batchNumber = intMaxIFeaturesTable + limit;
                while (batchNumber < (intMaxIActivityTable + limit)) {
                    String SQLSelectActiLog = sqlQuerySelectFromActivityLog(schema,tableName,batchNumber,limit);
                    try (final PreparedStatement stmt = conn.prepareStatement(SQLSelectActiLog)) {
                        final ResultSet result = stmt.executeQuery();
                        if(result!=null) {
                            while (result.next()) {
                                try {
                                    Feature activityLogFeature = XyzSerializable.deserialize(result.getString(1), Feature.class);
                                    geoList.add(result.getString(2));
                                    iList.add(result.getInt(3));
                                    ActivityLogHandler.fromActivityLogFormat(activityLogFeature);
                                    featureList.add(activityLogFeature.serialize());
                                } catch (JsonProcessingException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                    }
                    if(featureList.size()!=0) {
                        String sqlBulkInsertQuery = sqlQueryInsertConvertedFeatures(featureList, schema, geoList, iList);
                        sqlExecute(connLocalHost,sqlBulkInsertQuery);
                    }
                    connLocalHost.commit();
                    batchNumber += limit;
                    geoList.clear();
                    iList.clear();
                    featureList.clear();
                }
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
    public static String sqlQuerySelectFromActivityLog(String schema,String tableName,int batchNumber, int limit){
        String SQLSelectActiLog = "SELECT jsondata,geo,i FROM " + schema + ".\"" + tableName + "\" Where i>" + (batchNumber - limit) + " And i<="+batchNumber+" limit " + limit + ";";
        return SQLSelectActiLog;
    }

    public static String sqlQuerySelectMaxIFeatures(){
        String SQLMaxIFeaturesTable = "select i from activity.\"Features_Original_Format\" order by i desc limit 1;";
        return SQLMaxIFeaturesTable;
    }

    public static String sqlQueryMaxIActivity(String schema,String tableName){
        String SQLMaxIActivityTable = "select i from " + schema + ".\"" + tableName + "\"" + "order by i desc limit 1;";
        return SQLMaxIActivityTable;
    }

    public static String sqlQueryInsertConvertedFeatures(List<String> featureList,String schema,List<String> geoList,List<Integer> iList){
        String firstPart = "INSERT INTO "+ schema +".\"Features_Original_Format\"(jsondata,geo,i) VALUES ";
        for(int iterator=1; iterator<featureList.size()+1;iterator++){
            firstPart+= "(" + "\'"+featureList.get(iterator-1)+"\', " + "'"+geoList.get(iterator-1) + "', "+iList.get(iterator-1)+ ")";
            if(iterator!=featureList.size()){
                firstPart+=",";
            }
        }
        return firstPart;
    }

    public static String sqlQueryPreRequisites(String schema){
        String sqlQuery = "CREATE SCHEMA IF NOT EXISTS "+ schema + ";" +
                "CREATE TABLE IF NOT EXISTS " + schema +".\"Features_Original_Format\"" +
                "(\n" +
                "    jsondata      jsonb," +
                "    geo           varchar," +
                "    i             int8 PRIMARY KEY" +
                ");" +
                "CREATE SEQUENCE IF NOT EXISTS " + schema +"."+ "Features_Original_Format_i_seq" +" AS int8 OWNED BY " + schema +".\"Features_Original_Format\".i;";
        return sqlQuery;
    }
    public static void sqlExecute(Connection conn, String sqlQuery){
        try (final PreparedStatement stmt = conn.prepareStatement(sqlQuery)) {
            stmt.execute();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
