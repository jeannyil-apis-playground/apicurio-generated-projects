package org.github.jeannyil;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.github.jeannyil.beans.Membership;
import org.github.jeannyil.beans.ValidationResult;

/**
 * A JAX-RS interface.  An implementation of this interface must be provided.
 */
@Path("/validateMembershipJSON")
public interface ValidateMembershipJSONResource {
  /**
   * Validates a `Membership` JSON instance
   */
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  ValidationResult validateMembershipJSON(Membership data);
}
