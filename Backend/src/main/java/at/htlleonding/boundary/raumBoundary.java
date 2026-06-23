package at.htlleonding.boundary;

import at.htlleonding.DTOs.RaumDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/raeume")
@ApplicationScoped
public class raumBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public RaumDTO getRaum(@PathParam("id") String id) {
        return arbeitsplatzRepo.getRaum(id);
    }
}
