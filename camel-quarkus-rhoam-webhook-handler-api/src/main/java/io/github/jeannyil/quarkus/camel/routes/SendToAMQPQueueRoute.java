package io.github.jeannyil.quarkus.camel.routes;

import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

/* Route that sends RHOAM Admin/Developer Portal webhook event to an AMQP queue.

/!\ The @ApplicationScoped annotation is required for @Inject and @ConfigProperty to work in a RouteBuilder. 
	Note that the @ApplicationScoped beans are managed by the CDI container and their life cycle is thus a bit 
	more complex than the one of the plain RouteBuilder. 
	In other words, using @ApplicationScoped in RouteBuilder comes with some boot time penalty and you should 
	therefore only annotate your RouteBuilder with @ApplicationScoped when you really need it. */
public class SendToAMQPQueueRoute extends RouteBuilder {

    private static String logName = SendToAMQPQueueRoute.class.getName();

    @Override
    public void configure() throws Exception {
        
        from("direct:sendToAMQPQueue")
            .routeId("send-to-amqp-queue-route")
            .log(LoggingLevel.INFO, logName, ">>> ${routeId} - RHOAM Admin/Developer Portal received event: in.headers[${headers}] - in.body[${body}]")
            .removeHeaders("*", "breadcrumbId")
            .setHeader("RHOAM_EVENT_TYPE").xpath("//event/type", String.class)
            .setHeader("RHOAM_EVENT_ACTION").xpath("//event/action", String.class)
            .log(LoggingLevel.INFO, logName, ">>> ${routeId} - Sending to RHOAM.WEBHOOK.EVENTS.QUEUE AMQP address...")
            .to(ExchangePattern.InOnly, "amqp:queue:RHOAM.WEBHOOK.EVENTS.QUEUE")
			.setBody()
				.method("responseMessageHelper", "generateOKResponseMessage()")
				.id("set-OK-reponseMessage")
            .end()
            .marshal().json(JsonLibrary.Jackson, true).id("marshal-OK-responseMessage-to-json")
            .log(LoggingLevel.INFO, logName, ">>> ${routeId} - sendToAMQPQueue response: headers:[${headers}] - body:[${body}]")
        ;

    }

}
