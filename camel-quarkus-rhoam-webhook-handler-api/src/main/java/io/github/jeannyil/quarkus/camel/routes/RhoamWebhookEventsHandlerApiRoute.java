package io.github.jeannyil.quarkus.camel.routes;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.model.rest.RestParamType;

import io.github.jeannyil.quarkus.camel.models.ResponseMessage;


/* Exposes the RHOAM Webhook Events Handler API

/!\ The @ApplicationScoped annotation is required for @Inject and @ConfigProperty to work in a RouteBuilder. 
	Note that the @ApplicationScoped beans are managed by the CDI container and their life cycle is thus a bit 
	more complex than the one of the plain RouteBuilder. 
	In other words, using @ApplicationScoped in RouteBuilder comes with some boot time penalty and you should 
	therefore only annotate your RouteBuilder with @ApplicationScoped when you really need it. */
@ApplicationScoped
public class RhoamWebhookEventsHandlerApiRoute extends RouteBuilder {

	private static String logName = RhoamWebhookEventsHandlerApiRoute.class.getName();

	@Inject
	private CamelContext camelctx;
	
	@Override
	public void configure() throws Exception {

		// Enable Stream caching
        camelctx.setStreamCaching(true);
        // Enable use of breadcrumbId
        camelctx.setUseBreadcrumb(true);
		
		/**
		 * Catch unexpected exceptions
		 */
		onException(Exception.class)
			.handled(true)
			.maximumRedeliveries(0)
			.log(LoggingLevel.ERROR, logName, ">>> ${routeId} - Caught exception: ${exception.stacktrace}").id("log-api-unexpected")
			.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(constant(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())))
			.setHeader(Exchange.HTTP_RESPONSE_TEXT, constant(constant(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())))
			.setBody()
				.method("responseMessageHelper", 
						"generateKOResponseMessage(${headers.CamelHttpResponseCode}, ${headers.CamelHttpResponseText}, ${exception})")
				.id("set-unexpected-reponseMessage")
			.end()
			.marshal().json(JsonLibrary.Jackson, true).id("marshal-unexpected-responseMessage-to-json")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - OUT: headers:[${headers}] - body:[${body}]").id("log-api-unexpected-response")
		;
		
		/**
		 * REST configuration with Camel Quarkus Platform HTTP component
		 */
		restConfiguration()
			.component("platform-http")
			.enableCORS(true)
			.bindingMode(RestBindingMode.off) // RESTful responses will be explicitly marshaled for logging purposes
			.dataFormatProperty("prettyPrint", "true")
			.scheme("http")
			.host("0.0.0.0")
			.port("8080")
			.contextPath("/")
			.clientRequestValidation(true)
		;

		/**
		 * REST endpoint for the Service OpenAPI document 
		  */
		rest().id("openapi-document-restapi")
			.produces(MediaType.APPLICATION_JSON)
		  
			// Gets the OpenAPI document for this service
			.get("/openapi.json")
				.id("get-openapi-spec-route")
				.description("Gets the OpenAPI document for this service in JSON format")
				.route()
					.log(LoggingLevel.INFO, logName, ">>> ${routeId} - IN: headers:[${headers}] - body:[${body}]").id("log-openapi-doc-request")
					.setHeader(Exchange.CONTENT_TYPE, constant("application/vnd.oai.openapi+json")).id("set-content-type")
					.setBody()
						.constant("resource:classpath:openapi/openapi.json")
						.id("setBody-for-openapi-document")
					.log(LoggingLevel.INFO, logName, ">>> ${routeId} - OUT: headers:[${headers}] - body:[${body}]").id("log-openapi-doc-response")
				.end()
		;
		
		/**
		 * REST endpoint for the RHOAM Webhook Events Handler API
		 */
		rest().id("rhoam-webhook-events-handler-api")
				
			// Handles RHOAM webhook ping
			.get("/webhook/amqpbridge")
				.id("webhook-amqpbridge-ping-route")
				.description("Handles RHOAM webhook ping")
				.produces(MediaType.APPLICATION_JSON)
				.responseMessage()
					.code(Response.Status.OK.getStatusCode())
					.message(Response.Status.OK.getReasonPhrase())
					.responseModel(ResponseMessage.class)
				.endResponseMessage()
				// Call the WebhookPingRoute
				.to("direct:pingWebhook")
			
			// Handles the RHOAM Admin/Developer Portal webhook event and sends it to an AMQP queue
			.post("/webhook/amqpbridge")
				.id("webhook-amqpbridge-handler-route")
				.consumes(MediaType.WILDCARD)
				.produces(MediaType.APPLICATION_JSON)
				.description("Sends RHOAM Admin/Developer Portal webhook event to an AMQP queue")
				.param()
					.name("body")
					.type(RestParamType.body)
					.description("RHOAM Admin/Developer Portal XML event")
					.dataType("string")
					.required(true)
				.endParam()
				.responseMessage()
					.code(Response.Status.OK.getStatusCode())
					.message(Response.Status.OK.getReasonPhrase())
					.responseModel(ResponseMessage.class)
				.endResponseMessage()
				.responseMessage()
					.code(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
					.message(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())
					.responseModel(ResponseMessage.class)
				.endResponseMessage()
				// call the SendToAMQPQueueRoute
				.to("direct:sendToAMQPQueue")

		;
			
	}

}
