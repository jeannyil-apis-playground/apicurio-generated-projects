package io.github.jeannyil;

import io.github.jeannyil.beans.ResponseMessage;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * A JAX-RS interface.  An implementation of this interface must be provided.
 */
@Path("/webhook")
public interface WebhookResource {
  /**
   * Sends RHOAM Admin/Developer Portal webhook event to an AMQP queue
   */
  @Path("/amqpbridge")
  @POST
  @Produces("application/json")
  @Consumes("text/xml")
  ResponseMessage sendToAMQPQueue(String data);
}
