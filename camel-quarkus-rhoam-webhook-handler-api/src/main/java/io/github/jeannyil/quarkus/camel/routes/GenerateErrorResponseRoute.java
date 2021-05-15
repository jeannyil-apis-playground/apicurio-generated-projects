package io.github.jeannyil.quarkus.camel.routes;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;

import io.github.jeannyil.quarkus.camel.constants.DirectEndpointConstants;

/* Route that returns the error response message in JSON format

/!\ The @ApplicationScoped annotation is required for @Inject and @ConfigProperty to work in a RouteBuilder. 
	Note that the @ApplicationScoped beans are managed by the CDI container and their life cycle is thus a bit 
	more complex than the one of the plain RouteBuilder. 
	In other words, using @ApplicationScoped in RouteBuilder comes with some boot time penalty and you should 
	therefore only annotate your RouteBuilder with @ApplicationScoped when you really need it. */
public class GenerateErrorResponseRoute extends RouteBuilder {
	
	private static String logName = GenerateErrorResponseRoute.class.getName();

	@Override
	public void configure() throws Exception {
		
		/**
		 * Route that returns the error response message in JSON format
		 * The following properties are expected to be set on the incoming Camel Exchange Message if customization is needed:
		 * <br>- CamelHttpResponseCode ({@link org.apache.camel.Exchange#HTTP_RESPONSE_CODE})
		 * <br>- CamelHttpResponseText ({@link org.apache.camel.Exchange#HTTP_RESPONSE_TEXT})
		 */
		from(DirectEndpointConstants.DIRECT_GENERATE_ERROR_MESSAGE)
			.routeId("generate-error-response-route")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - IN: headers:[${headers}] - body:[${body}]").id("log-errormessage-request")
			.filter(simple("${in.header.CamelHttpResponseCode} == null")) // Defaults to 500 HTTP Code
				.setHeader(Exchange.HTTP_RESPONSE_CODE, constant(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())).id("set-500-http-code")
				.setHeader(Exchange.HTTP_RESPONSE_TEXT, constant(Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase())).id("set-500-http-reason")
			.end() // end filter
			.setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_JSON)).id("set-json-content-type")
			.setBody()
				.method("responseMessageHelper", 
						"generateKOResponseMessage(${headers.CamelHttpResponseCode}, ${headers.CamelHttpResponseText}, ${exception})")
				.id("set-errorresponse-object")
			.end()
			.marshal().json(JsonLibrary.Jackson, true).id("marshal-errorresponse-to-json")
			.convertBodyTo(String.class).id("convert-errorresponse-to-string")
			.log(LoggingLevel.INFO, logName, ">>> ${routeId} - OUT: headers:[${headers}] - body:[${body}]").id("log-errorresponse")
		;
		
	}

}