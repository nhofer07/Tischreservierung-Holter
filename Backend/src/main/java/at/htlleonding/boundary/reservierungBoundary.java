package at.htlleonding.boundary;

import at.htlleonding.DTOs.ReservierungRequestDTO;
import at.htlleonding.DTOs.ReservierungDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
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
    ArbeitsplatzRepo arbeitsplatzRepo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response reservieren(ReservierungRequestDTO reservierungRequestDTO) {
        ReservierungDTO reservierung = arbeitsplatzRepo.reservieren(reservierungRequestDTO);
        return Response.status(Response.Status.CREATED).entity(reservierung).build();
    }

    @GET
    @Path("/benutzer/{benutzerId}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ReservierungDTO> getReservierungen(@PathParam("benutzerId") String benutzerId) {
        return arbeitsplatzRepo.getReservierungen(benutzerId);
    }

    @DELETE
    @Path("/{id}")
    public Response stornieren(
            @PathParam("id") String id,
            @QueryParam("benutzerId") String benutzerId
    ) {
        arbeitsplatzRepo.stornieren(id, benutzerId);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReservierungDTO bearbeiten(@PathParam("id") String id, ReservierungRequestDTO request) {
        return arbeitsplatzRepo.reservierungAendern(id, request);
    }
}
