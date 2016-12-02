package ioswarm;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import com.hazelcast.config.Config;
import com.typesafe.config.ConfigFactory;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;
import ioswarm.oers.OERSService;
import ioswarm.oers.util.Converter;
import ioswarm.oers.util.Util;

public class OERS extends OERSService {
	
	public static Router router;
	private HttpServer server;
	
	protected void deployVerticles() throws Exception {
		Set<String> verticles = new HashSet<>();
		// get verticles from MANIFESTs
		Enumeration<URL> e = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
		while (e.hasMoreElements()) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new InputStreamReader(e.nextElement().openStream()));
				String line = null;
				while((line = in.readLine()) != null)
					if (line.startsWith("Deploy-Verticle:")) 
						for (String v : Util.splitLine(line.substring(line.indexOf(":")+1).trim(), ",")) {
							debug("add "+v+" to deployment-list - source: MANIFEST");
							verticles.add(v);
						}
			} finally {
				try { in.close(); } catch(Exception ex) {  }
			}
		}
		// get verticles from config
		for (com.typesafe.config.Config cfg : getConfig().getConfigList("oers.deployment"))
			if (cfg.hasPath("verticle")) {
				debug("add "+cfg.getString("verticle")+" to deployment-list - source: CONFIG");
				verticles.add(cfg.getString("verticle"));
			}
		
		// start deployment
		for (String v : verticles)
			deploy(v, hdl -> {
				if (hdl.succeeded())
					info("Verticle "+v+" deployed with id "+hdl.result());
				else
					error("ERROR while deploy verticle "+v+".", hdl.cause());
			});
	}
	
	@Override
	public void start() throws Exception {
		router = Router.router(vertx);
		router.route().handler(BodyHandler.create());
		
		server = vertx.createHttpServer().requestHandler(router::accept).listen(getConfig().getInt("oers.http.port"));
		
		deployVerticles();
	}
	
	@Override
	public void stop() throws Exception {		
		try {
			super.stop();
			
			if (server != null) {
				server.close();
				server = null;
			}
		} finally {
			System.exit(0);
		}
	}

	public static void main(String[] args) throws Exception {
		final Logger log = LoggerFactory.getLogger("OERS");
		final com.typesafe.config.Config cfg = ConfigFactory.load();
		
		// TODO change IP to HOST add JVM Arguments and Config
		String localIp = (System.getenv("LOCAL_IP") == null) ? "localhost" : System.getenv("LOCAL_IP"); 
		int localPort = (System.getenv("LOCAL_PORT") == null) ? 0 : Converter.convert(Integer.class, System.getenv("LOCAL_PORT"));
		
		String publicIp = (System.getenv("PUBLIC_IP") == null) ? null : System.getenv("PUBLIC_IP");
		int publicPort = (System.getenv("PUBLIC_PORT") == null) ? -1 : Converter.convert(Integer.class, System.getenv("PUBLIC_PORT"));
		
		int clusterPort = (System.getenv("CLUSTER_PORT") == null) ? 5701 : Converter.convert(Integer.class, System.getenv("CLUSTER_PORT"));
		
		log.info("Start OERS-Cluster with:");
		
		final Config config = new Config();
		config.setProperty("hazelcast.local.localAddress", localIp);
		config.getNetworkConfig().setPort(clusterPort);
		
		if (publicIp != null) {
			config.getNetworkConfig().setPublicAddress(publicIp);
			config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
			config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
			// TODO add Member
			// docker run -it --name oers1 -P -p 5701:5701 -p 8080:8080 -p 4001:4000 -e PUBLIC_IP=192.168.178.79 -e PUBLIC_PORT=4001 -e LOCAL_PORT=4000 -e CLUSTER_PORT=5701 ioswarm/oers
			
		}
		
		ClusterManager mgr = new HazelcastClusterManager(config);
		
		final VertxOptions opts = new VertxOptions()
				.setClustered(true)
				.setClusterManager(mgr)
				.setClusterHost(localIp)
				.setClusterPort(localPort)
				.setWorkerPoolSize(100);
				
		if (publicIp != null) opts.setClusterPublicHost(publicIp);
		if (publicPort>=0) opts.setClusterPublicPort(publicPort);
		
		
		log.info("\tcluster-host: "+opts.getClusterHost()+":"+opts.getClusterPort());
		log.info("\tcluster-public-host: "+opts.getClusterPublicHost()+":"+opts.getClusterPublicPort());
		
		Vertx.clusteredVertx(opts, res -> {
			if (res.succeeded()) {
				final Vertx vertx = res.result();
				vertx.deployVerticle("ioswarm.OERS", hdl -> {
					if (hdl.succeeded())
						log.info("OERS startd at "+hdl.result());
					else
						log.error("ERROR while starting OERS-Service...", hdl.cause());
				});				
			} else {
				log.error("ERROR while start OERS-cluster...", res.cause());
			}
		});
	}
	
}
