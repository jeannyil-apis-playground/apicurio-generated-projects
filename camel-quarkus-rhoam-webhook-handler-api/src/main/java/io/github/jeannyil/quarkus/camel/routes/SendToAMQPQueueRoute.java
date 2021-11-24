package io.github.jeannyil.quarkus.camel.routes;

import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.LoggingLevel;
import org.apache.camel.TypeConversionException;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import io.github.jeannyil.quarkus.camel.constants.DirectEndpointConstants;

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

        /**
		 * Catch unexpected exceptions
		 */
		onException(Exception.class)
            .handled(true)
            .maximumRedeliveries(0)
            .log(LoggingLevel.ERROR, logName, ">>> ${routeId} - Caught exception: ${exception.stacktrace}").id("log-sendToAMQPQueue-unexpected")
            .to(DirectEndpointConstants.DIRECT_GENERATE_ERROR_MESSAGE).id("generate-sendToAMQPQueue-500-errorresponse")
            .log(LoggingLevel.INFO, logName, ">>> ${routeId} - OUT: headers:[${headers}] - body:[${body}]").id("log-sendToAMQPQueue-unexpected-response")
        ;

        /**
		 * Catch unexpected exceptions
		 */
		onException(TypeConversionException.class)
            .handled(true)
            .maximumRedeliveries(0)
            .log(LoggingLevel.ERROR, logName, ">>> ${routeId} - Caught TypeConversionException: ${exception.stacktrace}").id("log-sendToAMQPQueue-400")
            .setHeader(Exchange.HTTP_RESPONSE_CODE, constant(Response.Status.BAD_REQUEST.getStatusCode())) // 400 Http Code
            .setProperty(Exchange.HTTP_RESPONSE_TEXT, constant(Response.Status.BAD_REQUEST.getReasonPhrase())) // 400 Http Code Text
            .to(DirectEndpointConstants.DIRECT_GENERATE_ERROR_MESSAGE).id("generate-sendToAMQPQueue-400-errorresponse")
            .log(LoggingLevel.INFO, logName, ">>> ${routeId} - OUT: headers:[${headers}] - body:[${body}]").id("log-sendToAMQPQueue-400-response")
        ;
        
        from(DirectEndpointConstants.DIRECT_SEND_TO_AMQP_QUEUE)
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
