package at.htlleonding.boundary;

import at.htlleonding.DTOs.ReservierungDTO;
import at.htlleonding.DTOs.ReservierungRequestDTO;
import at.htlleonding.repo.ReservierungRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/api/reservierungen")
@ApplicationScoped
public class reservierungBoundary {

    @Inject
    ReservierungRepo reservierungRepo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createReservation(ReservierungRequestDTO reservierungRequestDTO) {
        ReservierungDTO reservierung = reservierungRepo.createReservation(reservierungRequestDTO);
        return Response.status(Response.Status.CREATED).entity(reservierung).build();
    }

    @GET
    @Path("/benutzer/{benutzerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ReservierungDTO> getReservations(@PathParam("benutzerId") String benutzerId) {
        return reservierungRepo.getReservations(benutzerId);
    }

    @DELETE
    @Path("/{id}")
    public Response cancelReservation(
        @PathParam("id") String id,
        @QueryParam("benutzerId") String benutzerId
    ) {
        reservierungRepo.cancelReservation(id, benutzerId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReservierungDTO updateReservation(
        @PathParam("id") String id,
        ReservierungRequestDTO request
    ) {
        return reservierungRepo.updateReservation(id, request);
    }
}
