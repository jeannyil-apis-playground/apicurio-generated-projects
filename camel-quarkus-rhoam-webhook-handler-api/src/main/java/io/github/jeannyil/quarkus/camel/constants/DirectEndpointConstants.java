package io.github.jeannyil.quarkus.camel.constants;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Common direct Apache Camel endpoints used in this project
 */
@RegisterForReflection // Lets Quarkus register this class for reflection during the native build
public class DirectEndpointConstants {

	public static final String DIRECT_GENERATE_ERROR_MESSAGE = "direct:generateErrorResponse";
	public static final String DIRECT_SEND_TO_AMQP_QUEUE = "direct:sendToAMQPQueue";
	public static final String DIRECT_PING_WEBHOOK = "direct:pingWebhook";
	
}
