package org.github.jeannyil;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.github.jeannyil.beans.Legume;

/**
 * A JAX-RS interface.  An implementation of this interface must be provided.
 */
@Path("/legumes")
public interface LegumesResource {
  /**
   * Returns a list of hard-coded legumes
   */
  @GET
  @Produces("application/json")
  List<Legume> returnsAListOfHardcodedLegumes();
}
