package ioswarm.oers;

import java.util.HashMap;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Verticle;
import ioswarm.oers.annotations.NoBind;
import ioswarm.oers.annotations.Worker;
import ioswarm.vertx.service.Service;

public class OERSService extends Service {

	private Config config;
	private Map<String, String> deployedVerticles = new HashMap<>();
	
	public Config getConfig() {
		if (config == null) 
			config = ConfigFactory.load();
		return config;
	}
	
	protected Config verticleConfig(String verticleName) {
		for (Config cfg : getConfig().getConfigList("oers.deployment")) 
			if (cfg.hasPath("verticle") && verticleName.equals(cfg.getString("verticle")))
				return cfg;
		return null;
	}
	
	public void deploy(String verticleName, Handler<AsyncResult<String>> handler) {
		DeploymentOptions opts = new DeploymentOptions();
		Config vcfg = verticleConfig(verticleName);
		if (vcfg != null) {
			if (!(vcfg.hasPath("nobind") && vcfg.getBoolean("nobind"))) {
				if (vcfg.hasPath("worker") && vcfg.getBoolean("worker")) 
					opts.setWorker(true);
				if (vcfg.hasPath("instances"))
					opts.setInstances(vcfg.getInt("instances"));
			} else {
				warn("Verticle "+verticleName+" is configuered with nobind.");
				return;
			}
		} else {
			try {
				Class<?> cls = Class.forName(verticleName);
				if (!cls.isAnnotationPresent(NoBind.class)) {
					if (cls.isAnnotationPresent(Worker.class)) {
						info("mark Verticle "+verticleName+" as Worker.");
						opts.setWorker(true);
					}
					if (cls.isAnnotationPresent(ioswarm.oers.annotations.Config.class)) {
						ioswarm.oers.annotations.Config cfg = cls.getAnnotation(ioswarm.oers.annotations.Config.class);
						info("start Verticle "+verticleName+" with "+cfg.instances()+" instances.");
						opts.setInstances(cfg.instances());
						// TODO populate @Property
					}
				} else  {
					warn("Verticle "+verticleName+" is marked with NoBind.");
					return;
				}
			} catch(Exception e) {
				error(verticleName+" is not a known Java-Class...", e);
			}
		}
		
		vertx.executeBlocking((Future<String> f) -> {
			vertx.deployVerticle(verticleName, opts, hdl -> {
				if (hdl.succeeded()) {
					deployedVerticles.put(verticleName, hdl.result());
					f.complete(hdl.result());
				} else {
					f.fail(hdl.cause());
				}
			});
		}, handler);
	}
	
	public void deploy(String verticleName) {
		deploy(verticleName, null);
	}
	
	public void deploy(Class<? extends Verticle> verticle, Handler<AsyncResult<String>> handler) {
		deploy(verticle.getName(), handler);
	}
	
	public void deploy(Class<? extends Verticle> verticle) {
		deploy(verticle, null);
	}
	
	protected void undeployVerticles() throws Exception {
		for (final String v : deployedVerticles.keySet()) 
			vertx.undeploy(deployedVerticles.get(v), res -> {
				if (res.succeeded())
					info("Verticle "+v+" is successfully undeployed.");
				else 
					error("Error while undeploy verticle "+v+" ["+deployedVerticles.get(v)+"].", res.cause());
			});
		deployedVerticles.clear();
	}
	
	@Override
	public void stop() throws Exception {
		undeployVerticles();
	}
	
}
