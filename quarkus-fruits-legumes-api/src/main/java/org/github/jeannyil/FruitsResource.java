package org.github.jeannyil;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import org.github.jeannyil.beans.Fruit;

/**
 * A JAX-RS interface.  An implementation of this interface must be provided.
 */
@Path("/fruits")
public interface FruitsResource {
  /**
   * Returns a list of hard-coded and added fruits
   */
  @GET
  @Produces("application/json")
  List<Fruit> getFruits();

  /**
   * Adds a fruit in the hard-coded list
   */
  @POST
  @Produces("application/json")
  @Consumes("application/json")
  List<Fruit> addFruit(Fruit data);
}
