package ioswarm.oers;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import io.vertx.core.json.JsonObject;
import ioswarm.vertx.ext.cassandra.CassandraClient;

public class OERSCassandraService extends OERSService {

	protected JsonObject conf;
	protected CassandraClient client;
	
	public JsonObject cassandraConfig() {
		if (conf == null) {
			conf = new JsonObject().put("keyspace", "ioswarm").put("host", "localhost");
			Config config = ConfigFactory.load().getConfig("oers.cassandra");
			if (config.hasPath("hosts")) conf.put("hosts", config.getString("hosts"));
			else if (config.hasPath("host")) conf.put("host", config.getString("host"));
			if (config.hasPath("keyspace")) conf.put("keyspace", config.getString("keyspace"));
		}
		return conf;
	}
	
	public CassandraClient cassandraClient() {
		if (client == null) 
			client = CassandraClient.createShared(vertx, cassandraConfig());
		return client;
	}
	
	public JsonObject connectionError(Throwable cause) {
		return createErrorResponse(500, "Could not establish connection.", 600, cause);
	}
	
	public JsonObject queryError(Throwable cause) {
		return createErrorResponse(500, "Could not execute query.", 610, cause);
	}
	
}
