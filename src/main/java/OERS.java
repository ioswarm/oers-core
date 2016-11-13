

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import com.hazelcast.config.Config;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import ioswarm.oers.annotations.NoBind;
import ioswarm.oers.annotations.Worker;
import ioswarm.oers.util.Util;
import ioswarm.vertx.service.Service;

public class OERS extends Service {
	
	private Map<String, String> deployedVerticles = new HashMap<>();
	
	protected void deployVerticles() throws Exception {
		Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
		while (e.hasMoreElements()) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(e.nextElement().openStream()));
				String line = null;
				while((line = in.readLine()) != null)
					if (line.startsWith("Deploy-Verticle:")) 
						for (String v : Util.splitLine(line.substring(line.indexOf(":")+1).trim(), ",")) {
							try {
								Class<?> cls = Class.forName(v);
								DeploymentOptions opts = new DeploymentOptions();
								if (cls.isAnnotationPresent(NoBind.class)) {
									warn("Verticle "+v+" is marked with NoBind, abort deployment.");
									continue;
								}
								if (cls.isAnnotationPresent(Worker.class)) {
									info("mark Verticle "+v+" as Worker.");
									opts.setWorker(true);
								}
								if (cls.isAnnotationPresent(ioswarm.oers.annotations.Config.class)) {
									ioswarm.oers.annotations.Config cfg = cls.getAnnotation(ioswarm.oers.annotations.Config.class);
									info("start Verticle "+v+" with "+cfg.instances()+" instances.");
									opts.setInstances(cfg.instances());
									// TODO populate @Property
								}
								vertx.deployVerticle(v, opts, res -> {
									if (res.succeeded()) {
										info("Verticle "+v+" deployed with id "+res.result());
										deployedVerticles.put(v, res.result());
									} else
										error("Verticle "+v+" not deployed.", res.cause());
								});								
									
							} catch(Exception ex) {
								error("Can't deploy "+v+".", ex);
							}
						}
					
			} finally {
				try { in.close(); } catch(Exception ex) {  }
			}
		}
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
	
	public void start() throws Exception {			
		
		deployVerticles();
	}
	
	public void stop() throws Exception {
		undeployVerticles();
		
	}

	public static void main(String[] args) throws Exception {
		Config config = new Config();
		ClusterManager mgr = new HazelcastClusterManager(config);
		VertxOptions opts = new VertxOptions().setClusterManager(mgr);
		Vertx.clusteredVertx(opts, res -> {
			if (res.succeeded()) {
				final Vertx vertx = res.result();
				vertx.deployVerticle("OERS");
			} else {
				// TODO ERROR
			}
		});
	}
	
}
