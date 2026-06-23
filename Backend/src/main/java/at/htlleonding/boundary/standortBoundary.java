package at.htlleonding.boundary;

import at.htlleonding.DTOs.StandortDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/standorte")
@ApplicationScoped
public class standortBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<StandortDTO> getStandorte() {
        return arbeitsplatzRepo.getStandorte();
    }
}
