package at.htlleonding.boundary;

import at.htlleonding.DTOs.FirmenEinstellungDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/einstellungen")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class einstellungBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @GET
    public FirmenEinstellungDTO getEinstellungen() {
        return arbeitsplatzRepo.getFirmenEinstellungen();
    }
}
