package at.htlleonding.boundary;

import at.htlleonding.DTOs.FirmenEinstellungDTO;
import at.htlleonding.repo.EinstellungRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/einstellungen")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class einstellungBoundary {

    @Inject
    EinstellungRepo einstellungRepo;

    @ConfigProperty(name = "deskvision.test-login.enabled", defaultValue = "false")
    boolean testLoginEnabled;

    @GET
    @Path("/laufzeit")
    public Map<String, Boolean> getRuntimeSettings() {
        return Map.of("testLoginAktiv", testLoginEnabled);
    }

    @GET
    @Path("/reservierung")
    public at.htlleonding.DTOs.ReservierungsEinstellungDTO getBookingSettings() {
        return einstellungRepo.getBookingSettings();
    }

    @GET
    public FirmenEinstellungDTO getCompanySettings() {
        return einstellungRepo.getCompanySettings();
    }
}
