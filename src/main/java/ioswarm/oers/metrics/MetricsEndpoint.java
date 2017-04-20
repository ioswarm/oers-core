package ioswarm.oers.metrics;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ioswarm.oers.OERSHttpService;

public class MetricsEndpoint extends OERSHttpService {

	@Override
	public void start() {
		deploy(OSMetricsService.class);
		
		getRouter().get("/oers/metrics/os").handler(this::handleGetOsMetrics);
	}
	
	public void handleGetOsMetrics(RoutingContext ctx) {
		final HttpServerResponse resp = ctx.response().putHeader("content-type", "application/json; charset=UTF-8");
		vertx.eventBus().send("oers.metrics.os", new JsonObject(), (AsyncResult<Message<JsonObject>> rpl) -> {
			if (rpl.succeeded()) resp.end(rpl.result().body().encodePrettily());
			else {
				error("ERROR while fetch os-metrics.", rpl.cause());
				resp.setStatusCode(500).end("");
			}
		});
	}
	
}
