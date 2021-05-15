package io.github.jeannyil.quarkus.camel.routes;

import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import io.github.jeannyil.quarkus.camel.constants.DirectEndpointConstants;

/* Route that handles the webhook ping

/!\ The @ApplicationScoped annotation is required for @Inject and @ConfigProperty to work in a RouteBuilder. 
	Note that the @ApplicationScoped beans are managed by the CDI container and their life cycle is thus a bit 
	more complex than the one of the plain RouteBuilder. 
	In other words, using @ApplicationScoped in RouteBuilder comes with some boot time penalty and you should 
	therefore only annotate your RouteBuilder with @ApplicationScoped when you really need it. */
public class WebhookPingRoute extends RouteBuilder {

	private static String logName = WebhookPingRoute.class.getName();

    @Override
    public void configure() throws Exception {

		/**
		 * Catch unexpected exceptions
		 */
		// onException(Exception.class)
        //     .handled(true)
        //     .maximumRedeliveries(0)
        //     .log(LoggingLevel.ERROR, logName, ">>> ${routeId} - Caught exception: ${exception.stacktrace}").id("log-pingWebhook-unexpected")
        //     .to(DirectEndpointConstants.DIRECT_GENERATE_ERROR_MESSAGE).id("generate-pingWebhook-500-errorresponse")
        //     .log(LoggingLevel.INFO, logName, ">>> ${routeId} - OUT: headers:[${headers}] - body:[${body}]").id("log-pingWebhook-unexpected-response")
        // ;
        
        from(DirectEndpointConstants.DIRECT_PING_WEBHOOK)
			.routeId("ping-webhook-route")
			.setBody()
				.method("responseMessageHelper", "generateOKResponseMessage()")
				.id("set-pingOK-reponseMessage")
            .end()
            .marshal().json(JsonLibrary.Jackson, true).id("marshal-pingOK-responseMessage-to-json")
		;

    }

}
