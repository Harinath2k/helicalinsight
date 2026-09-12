
package com.helicalinsight.adhoc.services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.helicalinsight.datasource.GsonUtility;
import com.helicalinsight.datasource.nosql.NoSQLLoader;
import com.helicalinsight.efw.exceptions.EfwServiceException;
import com.mongodb.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;


/**
 * @author Somen
 * Created on 11/15/2017.
 */

@Component("com.helicalinsight.nosql.mongo")
@Scope("prototype")
public class MongoDrillLoader extends NoSQLLoader {
    @Override
    public boolean loadToMiddleWare(JsonObject formDataJson) {
        JsonObject mongo = new JsonObject();
        String username = formDataJson.get("userName").getAsString();
        String password = formDataJson.get("password").getAsString();
        String jdbcUrl = GsonUtility.optString(formDataJson, "jdbcUrl");
        String storageName = formDataJson.get("name").getAsString();
        String theId = formDataJson.get("theId").getAsString();
        mongo.addProperty("type", "mongo");

        String connectionString = createConnectionString(jdbcUrl, username, password);
        mongo.addProperty("connection", connectionString);
        mongo.addProperty("enabled", true);

        String drillStorageUrl = DrillCsvDataSourceCreator.getUrlOfDrill();

        String resourceUrl = drillStorageUrl + "/storage/" + storageName + "_" + theId + ".json";

        JsonObject storageJson = new JsonObject();
        storageJson.addProperty("name", storageName + "_" + theId);
        storageJson.add("config", mongo);

        String result = DrillCsvDataSourceCreator.drillRestApiCall(resourceUrl, "POST", storageJson.toString());
        if (result == null) {
            throw new EfwServiceException("There was some problem creating drill mongo connection");
        } else {
            try {
                JsonObject resultJSON = new Gson().fromJson(result,JsonObject.class);

            } catch (JsonSyntaxException e) {
                throw new EfwServiceException("There was a problem " + result);
            }
        }
        return true;
    }

    @Override
    public boolean testConnection(JsonObject formData) {
        String uri = GsonUtility.optString(formData,"jdbcUrl");
        String database = GsonUtility.optString(formData,"database");
        String username = GsonUtility.optString(formData,"userName");
        String password = GsonUtility.optString(formData,"password");
        if (StringUtils.isEmpty(database)) {
            database = GsonUtility.optString(formData,"databaseName");
        }
        String connectionString = createConnectionString(uri, username, password);
        MongoClientURI mongoUri = new MongoClientURI(connectionString);
        String databaseName = StringUtils.isNotBlank(database) ? database : mongoUri.getDatabase();
        if (StringUtils.isBlank(databaseName)) {
            databaseName = "admin";
        }

        try (MongoClient mongo = new MongoClient(mongoUri)) {
            return mongo.getDB(databaseName).command("ping").ok();
        }
    }

    static String createConnectionString(String uri, String username, String password) {
        if (StringUtils.isBlank(uri)) {
            throw new EfwServiceException("MongoDB connection URL is required");
        }

        MongoClientURI mongoUri = new MongoClientURI(uri);
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password) || mongoUri.getCredentials() != null) {
            return uri;
        }

        URI parsedUri = URI.create(uri);
        String authority = parsedUri.getRawAuthority();
        if (StringUtils.isBlank(authority)) {
            throw new EfwServiceException("MongoDB connection URL must include a host");
        }

        StringBuilder connectionString = new StringBuilder(parsedUri.getScheme())
                .append("://")
                .append(encode(username))
                .append(":")
                .append(encode(password))
                .append("@")
                .append(authority);
        if (parsedUri.getRawPath() != null) {
            connectionString.append(parsedUri.getRawPath());
        }
        if (parsedUri.getRawQuery() != null) {
            connectionString.append("?").append(parsedUri.getRawQuery());
        }
        return connectionString.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}

