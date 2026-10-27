package at.htlleonding.boundary;

import at.htlleonding.DTOs.RaumAuswahlDTO;
import at.htlleonding.DTOs.RaumDTO;
import at.htlleonding.repo.RaumRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.List;

@Path("/api/raeume")
@ApplicationScoped
public class raumBoundary {

    @Inject
    RaumRepo raumRepo;

    @GET
    @Path("/standort/{standortId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RaumAuswahlDTO> getRoomsByLocation(@PathParam("standortId") String standortId) {
        return raumRepo.getRoomsByLocation(standortId);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public RaumDTO getRoom(
        @PathParam("id") String id,
        @QueryParam("von") String von,
        @QueryParam("bis") String bis,
        @QueryParam("benutzerId") String benutzerId
    ) {
        return raumRepo.getRoom(id, von, bis, benutzerId);
    }
}
