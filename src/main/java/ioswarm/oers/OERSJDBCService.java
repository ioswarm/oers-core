package ioswarm.oers;

import com.typesafe.config.ConfigFactory;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public abstract class OERSJDBCService extends OERSService {

	protected JsonObject jdbcconf;
	
	public abstract String datasource();
	
	public JsonObject jdbcConfig() {
		if (jdbcconf == null) {
			com.typesafe.config.Config cfg = ConfigFactory.load().getConfig("oers.jdbc."+datasource());
			jdbcconf = new JsonObject()
					.put("url", cfg.getString("url"))
					.put("driver_class", cfg.getString("driver"))
					.put("user", cfg.getString("user"))
					.put("password", cfg.getString("password"))
					.put("max_pool_size", cfg.hasPath("max_pool_size") ? cfg.getInt("max_pool_size") : 10);
		}
		return jdbcconf;
	}
	
	public JDBCClient jdbcClient() {
		return JDBCClient.createShared(vertx, jdbcConfig(), datasource());
	}
	
}
