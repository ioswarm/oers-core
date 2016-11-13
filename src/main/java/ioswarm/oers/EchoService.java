package ioswarm.oers;

import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageConsumer;
import ioswarm.vertx.service.Service;

public class EchoService extends Service {

	private MessageConsumer<Object> echoConsumer;
	
	@Override
	public void start() {
		echoConsumer = vertx.eventBus().consumer("echo", this::handleEchoEvent);
	}
	
	@Override
	public void stop() {
		if (echoConsumer != null)
			echoConsumer.unregister();
	}
	
	public void handleEchoEvent(Message<Object> msg) {
		msg.reply("ECHO: "+msg.body());
	}
	
}
