package at.htlleonding.boundary;

import at.htlleonding.DTOs.ArbeitsplatzDTO;
import at.htlleonding.DTOs.ReservierungRequestDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/reservierungen")
@ApplicationScoped
public class reservierungBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reservieren(ReservierungRequestDTO reservierungRequestDTO) {
        ArbeitsplatzDTO arbeitsplatz = arbeitsplatzRepo.reservieren(reservierungRequestDTO);
        return Response.accepted(arbeitsplatz).build();
    }
}
