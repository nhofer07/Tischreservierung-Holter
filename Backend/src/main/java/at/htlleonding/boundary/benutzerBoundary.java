package at.htlleonding.boundary;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/benutzer")
@ApplicationScoped
public class benutzerBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<BenutzerDTO> getBenutzer() {
        return arbeitsplatzRepo.getBenutzer();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BenutzerDTO login(at.htlleonding.DTOs.LoginRequestDTO request) {
        return arbeitsplatzRepo.login(request);
    }
}
