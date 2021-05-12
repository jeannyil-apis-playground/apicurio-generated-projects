package org.github.jeannyil;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.github.jeannyil.beans.ValidationResult;

/**
 * A JAX-RS interface.  An implementation of this interface must be provided.
 */
@Path("/validateMembershipXML")
public interface ValidateMembershipXMLResource {
  /**
   * Validates a `Membership` instance
   */
  @POST
  @Produces("application/json")
  @Consumes("text/xml")
  ValidationResult validateMembershipXML(@HeaderParam("app_id") String appId,
      @HeaderParam("app_key") String appKey, String data);
}
