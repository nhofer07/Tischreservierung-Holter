package at.htlleonding.boundary;

import at.htlleonding.DTOs.RaumDTO;
import at.htlleonding.DTOs.RaumAuswahlDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
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
    ArbeitsplatzRepo arbeitsplatzRepo;

    @GET
    @Path("/standort/{standortId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<RaumAuswahlDTO> getRaeumeByStandort(@PathParam("standortId") String standortId) {
        return arbeitsplatzRepo.getRaeumeByStandort(standortId);
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public RaumDTO getRaum(
            @PathParam("id") String id,
            @QueryParam("von") String von,
            @QueryParam("bis") String bis
    ) {
        return arbeitsplatzRepo.getRaum(id, von, bis);
    }
}
