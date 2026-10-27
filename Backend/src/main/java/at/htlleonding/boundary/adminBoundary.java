package at.htlleonding.boundary;

import at.htlleonding.DTOs.AbteilungDTO;
import at.htlleonding.DTOs.ArbeitsplatzDTO;
import at.htlleonding.DTOs.ArbeitsplatzRequestDTO;
import at.htlleonding.DTOs.NameRequestDTO;
import at.htlleonding.DTOs.RaumAuswahlDTO;
import at.htlleonding.DTOs.RaumRequestDTO;
import at.htlleonding.DTOs.ReservierungDTO;
import at.htlleonding.repo.AbteilungRepo;
import at.htlleonding.repo.ArbeitsplatzRepo;
import at.htlleonding.repo.AuditRepo;
import at.htlleonding.repo.BenutzerRepo;
import at.htlleonding.repo.BenutzerZugriff;
import at.htlleonding.repo.EinstellungRepo;
import at.htlleonding.repo.EquipmentRepo;
import at.htlleonding.repo.RaumRepo;
import at.htlleonding.repo.ReservierungRepo;
import at.htlleonding.repo.StandortRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

@Path("/api/admin")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class adminBoundary {

    @Inject
    AbteilungRepo abteilungRepo;

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @Inject
    AuditRepo auditRepo;

    @Inject
    BenutzerRepo benutzerRepo;

    @Inject
    BenutzerZugriff benutzerZugriff;

    @Inject
    EinstellungRepo einstellungRepo;

    @Inject
    EquipmentRepo equipmentRepo;

    @Inject
    RaumRepo raumRepo;

    @Inject
    ReservierungRepo reservierungRepo;

    @Inject
    StandortRepo standortRepo;

    @PUT
    @Path("/reservierungseinstellungen")
    public at.htlleonding.DTOs.ReservierungsEinstellungDTO saveBookingSettings(
        @QueryParam("adminId") String adminId,
        at.htlleonding.DTOs.ReservierungsEinstellungDTO request
    ) {
        return einstellungRepo.saveBookingSettings(adminId, request);
    }

    @GET
    @Path("/einstellungen")
    public at.htlleonding.DTOs.FirmenEinstellungDTO getCompanySettings(
        @QueryParam("adminId") String adminId
    ) {
        benutzerZugriff.requireSuperadmin(adminId);
        return einstellungRepo.getCompanySettings();
    }

    @PUT
    @Path("/einstellungen")
    public at.htlleonding.DTOs.FirmenEinstellungDTO saveCompanySettings(
        @QueryParam("adminId") String adminId,
        at.htlleonding.DTOs.FirmenEinstellungDTO request
    ) {
        return einstellungRepo.saveCompanySettings(adminId, request);
    }

    @GET
    @Path("/einstellungen/historie")
    public List<at.htlleonding.DTOs.FirmenDesignHistorieDTO> getBrandingHistory(
        @QueryParam("adminId") String adminId
    ) {
        return einstellungRepo.getBrandingHistory(adminId);
    }

    @PUT
    @Path("/einstellungen/historie/{id}")
    public at.htlleonding.DTOs.FirmenEinstellungDTO applyBrandingHistoryEntry(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId
    ) {
        return einstellungRepo.applyBrandingHistoryEntry(adminId, id);
    }

    @DELETE
    @Path("/einstellungen/historie/{id}")
    public Response deleteBrandingHistoryEntry(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId
    ) {
        einstellungRepo.deleteBrandingHistoryEntry(adminId, id);
        return Response.noContent().build();
    }

    @GET
    @Path("/benutzer")
    public List<at.htlleonding.DTOs.BenutzerDTO> getManagedUsers(
        @QueryParam("adminId") String adminId
    ) {
        return benutzerRepo.getManagedUsers(adminId);
    }

    @POST
    @Path("/benutzer")
    public Response createUser(
        @QueryParam("adminId") String adminId,
        at.htlleonding.DTOs.BenutzerRequestDTO request
    ) {
        return Response.status(Response.Status.CREATED)
            .entity(benutzerRepo.createUser(adminId, request))
            .build();
    }

    @PUT
    @Path("/benutzer/{id}")
    public at.htlleonding.DTOs.BenutzerDTO updateUser(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId,
        at.htlleonding.DTOs.BenutzerRequestDTO request
    ) {
        return benutzerRepo.updateUser(adminId, id, request);
    }

    @DELETE
    @Path("/benutzer/{id}")
    public Response deleteUser(@PathParam("id") String id, @QueryParam("adminId") String adminId) {
        benutzerRepo.deleteUser(adminId, id);
        return Response.noContent().build();
    }

    @GET
    @Path("/benutzer/import/vorlage")
    @Produces("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public Response downloadUserImportTemplate(@QueryParam("adminId") String adminId) {
        return Response.ok(benutzerRepo.createUserImportTemplate(adminId))
            .header("Content-Disposition", "attachment; filename=mitarbeiter-import-vorlage.xlsx")
            .build();
    }

    @POST
    @Path("/benutzer/import/pruefen")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public at.htlleonding.DTOs.MitarbeiterImportErgebnisDTO validateUserImport(
        @QueryParam("adminId") String adminId,
        @RestForm("datei") FileUpload datei
    ) {
        validateImportFile(datei);
        try (InputStream eingabe = Files.newInputStream(datei.uploadedFile())) {
            return benutzerRepo.validateUserImport(adminId, eingabe);
        } catch (IOException exception) {
            throw new BadRequestException("Die Excel-Datei konnte nicht gelesen werden.");
        }
    }

    @POST
    @Path("/benutzer/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public at.htlleonding.DTOs.MitarbeiterImportErgebnisDTO importUsers(
        @QueryParam("adminId") String adminId,
        @RestForm("datei") FileUpload datei
    ) {
        validateImportFile(datei);
        try (InputStream eingabe = Files.newInputStream(datei.uploadedFile())) {
            return benutzerRepo.importUsers(adminId, eingabe);
        } catch (IOException exception) {
            throw new BadRequestException("Die Excel-Datei konnte nicht gelesen werden.");
        }
    }

    private void validateImportFile(FileUpload datei) {
        if (
            datei == null ||
            datei.uploadedFile() == null ||
            datei.fileName() == null ||
            !datei.fileName().toLowerCase().endsWith(".xlsx")
        ) {
            throw new BadRequestException("Bitte eine Excel-Datei im Format .xlsx auswählen.");
        }
        try {
            if (Files.size(datei.uploadedFile()) > 5L * 1024 * 1024) {
                throw new BadRequestException("Die Excel-Datei darf maximal 5 MB groß sein.");
            }
        } catch (IOException exception) {
            throw new BadRequestException("Die Excel-Datei konnte nicht geprüft werden.");
        }
    }

    @POST
    @Path("/standorte")
    public Response createLocation(
        @QueryParam("adminId") String adminId,
        at.htlleonding.DTOs.StandortRequestDTO request
    ) {
        return Response.status(Response.Status.CREATED)
            .entity(standortRepo.createLocation(adminId, request))
            .build();
    }

    @PUT
    @Path("/standorte/{id}")
    public at.htlleonding.DTOs.StandortDTO updateLocation(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId,
        at.htlleonding.DTOs.StandortRequestDTO request
    ) {
        return standortRepo.updateLocation(adminId, id, request);
    }

    @DELETE
    @Path("/standorte/{id}")
    public Response deleteLocation(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId
    ) {
        standortRepo.deleteLocation(adminId, id);
        return Response.noContent().build();
    }

    @GET
    @Path("/abteilungen")
    public List<AbteilungDTO> getDepartments(@QueryParam("adminId") String adminId) {
        return abteilungRepo.getDepartments(adminId);
    }

    @POST
    @Path("/abteilungen")
    public Response createDepartment(
        @QueryParam("adminId") String adminId,
        NameRequestDTO request
    ) {
        return Response.status(Response.Status.CREATED)
            .entity(abteilungRepo.createDepartment(adminId, request))
            .build();
    }

    @DELETE
    @Path("/abteilungen/{id}")
    public Response deleteDepartment(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId
    ) {
        abteilungRepo.deleteDepartment(adminId, id);
        return Response.noContent().build();
    }

    @GET
    @Path("/equipment")
    public List<String> getEquipmentCatalog(@QueryParam("adminId") String adminId) {
        return equipmentRepo.getEquipmentCatalog(adminId);
    }

    @POST
    @Path("/equipment")
    public Response createEquipment(@QueryParam("adminId") String adminId, NameRequestDTO request) {
        return Response.status(Response.Status.CREATED)
            .entity(equipmentRepo.createEquipment(adminId, request))
            .build();
    }

    @DELETE
    @Path("/equipment/{name}")
    public Response deleteEquipment(
        @PathParam("name") String name,
        @QueryParam("adminId") String adminId
    ) {
        equipmentRepo.deleteEquipment(adminId, URLDecoder.decode(name, StandardCharsets.UTF_8));
        return Response.noContent().build();
    }

    @GET
    @Path("/reservierungen")
    public List<ReservierungDTO> getAllReservations(@QueryParam("adminId") String adminId) {
        return reservierungRepo.getAllReservations(adminId);
    }

    @DELETE
    @Path("/reservierungen/{id}")
    public Response cancelReservationAsAdmin(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId,
        NameRequestDTO request
    ) {
        reservierungRepo.cancelReservationAsAdmin(adminId, id, request);
        return Response.noContent().build();
    }

    @GET
    @Path("/audit")
    public List<org.bson.Document> getAuditLog(@QueryParam("adminId") String adminId) {
        return auditRepo.getAuditLog(adminId);
    }

    @POST
    @Path("/raeume")
    public Response createRoom(@QueryParam("adminId") String adminId, RaumRequestDTO request) {
        RaumAuswahlDTO raum = raumRepo.createRoom(adminId, request);
        return Response.status(Response.Status.CREATED).entity(raum).build();
    }

    @PUT
    @Path("/raeume/{id}")
    public RaumAuswahlDTO updateRoom(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId,
        RaumRequestDTO request
    ) {
        return raumRepo.updateRoom(adminId, id, request);
    }

    @DELETE
    @Path("/raeume/{id}")
    public Response deleteRoom(@PathParam("id") String id, @QueryParam("adminId") String adminId) {
        raumRepo.deleteRoom(adminId, id);
        return Response.noContent().build();
    }

    @POST
    @Path("/arbeitsplaetze")
    public Response createWorkstation(
        @QueryParam("adminId") String adminId,
        ArbeitsplatzRequestDTO request
    ) {
        ArbeitsplatzDTO arbeitsplatz = arbeitsplatzRepo.createWorkstation(adminId, request);
        return Response.status(Response.Status.CREATED).entity(arbeitsplatz).build();
    }

    @PUT
    @Path("/arbeitsplaetze/{id}")
    public ArbeitsplatzDTO updateWorkstation(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId,
        ArbeitsplatzRequestDTO request
    ) {
        return arbeitsplatzRepo.updateWorkstation(adminId, id, request);
    }

    @DELETE
    @Path("/arbeitsplaetze/{id}")
    public Response deleteWorkstation(
        @PathParam("id") String id,
        @QueryParam("adminId") String adminId
    ) {
        arbeitsplatzRepo.deleteWorkstation(adminId, id);
        return Response.noContent().build();
    }
}
