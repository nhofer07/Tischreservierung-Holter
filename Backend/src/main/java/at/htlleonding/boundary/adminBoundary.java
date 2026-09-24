package at.htlleonding.boundary;

import at.htlleonding.DTOs.ArbeitsplatzDTO;
import at.htlleonding.DTOs.ArbeitsplatzRequestDTO;
import at.htlleonding.DTOs.RaumAuswahlDTO;
import at.htlleonding.DTOs.RaumRequestDTO;
import at.htlleonding.DTOs.AbteilungDTO;
import at.htlleonding.DTOs.NameRequestDTO;
import at.htlleonding.DTOs.ReservierungDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Path("/api/admin")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class adminBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @GET
    @Path("/einstellungen")
    public at.htlleonding.DTOs.FirmenEinstellungDTO einstellungen(@QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.pruefeSuperadminZugriff(adminId);
        return arbeitsplatzRepo.getFirmenEinstellungen();
    }

    @PUT
    @Path("/einstellungen")
    public at.htlleonding.DTOs.FirmenEinstellungDTO einstellungenSpeichern(
            @QueryParam("adminId") String adminId,
            at.htlleonding.DTOs.FirmenEinstellungDTO request) {
        return arbeitsplatzRepo.firmenEinstellungenSpeichern(adminId, request);
    }

    @GET
    @Path("/einstellungen/historie")
    public List<at.htlleonding.DTOs.FirmenDesignHistorieDTO> einstellungenHistorie(@QueryParam("adminId") String adminId) {
        return arbeitsplatzRepo.getFirmenDesignHistorie(adminId);
    }

    @PUT
    @Path("/einstellungen/historie/{id}")
    public at.htlleonding.DTOs.FirmenEinstellungDTO einstellungenHistorieVerwenden(
            @PathParam("id") String id, @QueryParam("adminId") String adminId) {
        return arbeitsplatzRepo.firmenDesignAusHistorieVerwenden(adminId, id);
    }

    @DELETE
    @Path("/einstellungen/historie/{id}")
    public Response einstellungenHistorieLoeschen(
            @PathParam("id") String id, @QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.firmenDesignHistorieLoeschen(adminId, id);
        return Response.noContent().build();
    }

    @GET
    @Path("/benutzer")
    public List<at.htlleonding.DTOs.BenutzerDTO> benutzer(@QueryParam("adminId") String adminId) {
        return arbeitsplatzRepo.getBenutzerAlsAdmin(adminId);
    }

    @POST
    @Path("/benutzer")
    public Response benutzerAnlegen(@QueryParam("adminId") String adminId, at.htlleonding.DTOs.BenutzerRequestDTO request) {
        return Response.status(Response.Status.CREATED).entity(arbeitsplatzRepo.benutzerAnlegen(adminId, request)).build();
    }

    @PUT
    @Path("/benutzer/{id}")
    public at.htlleonding.DTOs.BenutzerDTO benutzerAendern(@PathParam("id") String id, @QueryParam("adminId") String adminId,
                                                           at.htlleonding.DTOs.BenutzerRequestDTO request) {
        return arbeitsplatzRepo.benutzerAendern(adminId, id, request);
    }

    @PUT
    @Path("/benutzer/{id}/passwort")
    public Response passwort(@PathParam("id") String id, @QueryParam("adminId") String adminId,
                             at.htlleonding.DTOs.PasswortRequestDTO request) {
        arbeitsplatzRepo.passwortZuruecksetzen(adminId, id, request); return Response.noContent().build();
    }

    @DELETE
    @Path("/benutzer/{id}")
    public Response benutzerLoeschen(@PathParam("id") String id, @QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.benutzerLoeschen(adminId, id);
        return Response.noContent().build();
    }

    @POST
    @Path("/standorte")
    public Response standortAnlegen(@QueryParam("adminId") String adminId, at.htlleonding.DTOs.StandortRequestDTO request) {
        return Response.status(Response.Status.CREATED).entity(arbeitsplatzRepo.standortAnlegen(adminId, request)).build();
    }

    @PUT
    @Path("/standorte/{id}")
    public at.htlleonding.DTOs.StandortDTO standortAendern(@PathParam("id") String id, @QueryParam("adminId") String adminId,
                                                          at.htlleonding.DTOs.StandortRequestDTO request) {
        return arbeitsplatzRepo.standortAendern(adminId, id, request);
    }

    @DELETE
    @Path("/standorte/{id}")
    public Response standortLoeschen(@PathParam("id") String id, @QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.standortLoeschen(adminId, id); return Response.noContent().build();
    }

    @GET
    @Path("/abteilungen")
    public List<AbteilungDTO> abteilungen(@QueryParam("adminId") String adminId) {
        return arbeitsplatzRepo.getAbteilungen(adminId);
    }

    @POST
    @Path("/abteilungen")
    public Response abteilungAnlegen(@QueryParam("adminId") String adminId, NameRequestDTO request) {
        return Response.status(Response.Status.CREATED).entity(arbeitsplatzRepo.abteilungAnlegen(adminId, request)).build();
    }

    @DELETE
    @Path("/abteilungen/{id}")
    public Response abteilungLoeschen(@PathParam("id") String id, @QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.abteilungLoeschen(adminId, id);
        return Response.noContent().build();
    }

    @GET
    @Path("/equipment")
    public List<String> equipment(@QueryParam("adminId") String adminId) {
        return arbeitsplatzRepo.getEquipment(adminId);
    }

    @POST
    @Path("/equipment")
    public Response equipmentAnlegen(@QueryParam("adminId") String adminId, NameRequestDTO request) {
        return Response.status(Response.Status.CREATED).entity(arbeitsplatzRepo.equipmentAnlegen(adminId, request)).build();
    }

    @DELETE
    @Path("/equipment/{name}")
    public Response equipmentLoeschen(@PathParam("name") String name, @QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.equipmentLoeschen(adminId, URLDecoder.decode(name, StandardCharsets.UTF_8));
        return Response.noContent().build();
    }

    @GET
    @Path("/reservierungen")
    public List<ReservierungDTO> reservierungen(@QueryParam("adminId") String adminId) {
        return arbeitsplatzRepo.getAlleReservierungen(adminId);
    }

    @DELETE
    @Path("/reservierungen/{id}")
    public Response reservierungStornieren(@PathParam("id") String id, @QueryParam("adminId") String adminId,
                                           NameRequestDTO request) {
        arbeitsplatzRepo.reservierungAlsAdminStornieren(adminId, id, request);
        return Response.noContent().build();
    }

    @GET
    @Path("/audit")
    public List<org.bson.Document> audit(@QueryParam("adminId") String adminId) {
        return arbeitsplatzRepo.getAuditLog(adminId);
    }

    @POST
    @Path("/raeume")
    public Response raumAnlegen(@QueryParam("adminId") String adminId, RaumRequestDTO request) {
        RaumAuswahlDTO raum = arbeitsplatzRepo.raumAnlegen(adminId, request);
        return Response.status(Response.Status.CREATED).entity(raum).build();
    }

    @PUT
    @Path("/raeume/{id}")
    public RaumAuswahlDTO raumAendern(@PathParam("id") String id, @QueryParam("adminId") String adminId,
                                     RaumRequestDTO request) {
        return arbeitsplatzRepo.raumAendern(adminId, id, request);
    }

    @DELETE
    @Path("/raeume/{id}")
    public Response raumLoeschen(@PathParam("id") String id, @QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.raumLoeschen(adminId, id);
        return Response.noContent().build();
    }

    @POST
    @Path("/arbeitsplaetze")
    public Response arbeitsplatzAnlegen(@QueryParam("adminId") String adminId, ArbeitsplatzRequestDTO request) {
        ArbeitsplatzDTO arbeitsplatz = arbeitsplatzRepo.arbeitsplatzAnlegen(adminId, request);
        return Response.status(Response.Status.CREATED).entity(arbeitsplatz).build();
    }

    @PUT
    @Path("/arbeitsplaetze/{id}")
    public ArbeitsplatzDTO arbeitsplatzAendern(@PathParam("id") String id,
                                               @QueryParam("adminId") String adminId,
                                               ArbeitsplatzRequestDTO request) {
        return arbeitsplatzRepo.arbeitsplatzAendern(adminId, id, request);
    }

    @DELETE
    @Path("/arbeitsplaetze/{id}")
    public Response arbeitsplatzLoeschen(@PathParam("id") String id, @QueryParam("adminId") String adminId) {
        arbeitsplatzRepo.arbeitsplatzLoeschen(adminId, id);
        return Response.noContent().build();
    }
}
