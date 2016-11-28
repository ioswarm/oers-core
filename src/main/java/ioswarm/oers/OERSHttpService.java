package ioswarm.oers;

import io.vertx.core.Handler;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import ioswarm.OERS;

public class OERSHttpService extends OERSService {

	public Router getRouter() { return OERS.router; }
	
	public Route delete(String path) {
		return getRouter().delete(path);
	}
	
	public void delete(String path, Handler<RoutingContext> requestHandler) {
		delete(path).handler(requestHandler);
	}
	
	public Route get(String path) {
		return getRouter().get(path);
	}
	
	public Route get(String path, Handler<RoutingContext> requestHandler) {
		return get(path).handler(requestHandler);
	}
	
	public Route post(String path) {
		return getRouter().post(path);
	}
	
	public Route post(String path, Handler<RoutingContext> requestHandler) {
		return post(path).handler(requestHandler);
	}
	
	public Route put(String path) {
		return getRouter().put(path);
	}
	
	public Route put(String path, Handler<RoutingContext> requestHandler) {
		return put(path).handler(requestHandler);
	}
	
}
