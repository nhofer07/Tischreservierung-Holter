package at.htlleonding.boundary;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/benutzer")
@ApplicationScoped
public class benutzerBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @GET
    @Path("/demo")
    @Produces(MediaType.APPLICATION_JSON)
    public BenutzerDTO getDemoBenutzer() {
        return arbeitsplatzRepo.getDemoBenutzer();
    }
}
