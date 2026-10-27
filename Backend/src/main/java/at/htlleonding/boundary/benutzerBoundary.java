package at.htlleonding.boundary;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.repo.BenutzerRepo;
import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/api/benutzer")
@ApplicationScoped
public class benutzerBoundary {

    @Inject
    BenutzerRepo benutzerRepo;

    @Inject
    JsonWebToken token;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<BenutzerDTO> getUsers() {
        return benutzerRepo.getUsers();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BenutzerDTO loginWithTestAccount(at.htlleonding.DTOs.LoginRequestDTO request) {
        return benutzerRepo.loginWithTestAccount(request);
    }

    @GET
    @Path("/me")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public BenutzerDTO getCurrentEntraUser() {
        String email = getTokenClaim("email");
        if (email == null) {
            email = getTokenClaim("preferred_username");
        }
        if (email == null) {
            email = getTokenClaim("upn");
        }
        return benutzerRepo.loginWithEntra(email);
    }

    private String getTokenClaim(String name) {
        Object wert = token.getClaim(name);
        return wert == null || wert.toString().isBlank() ? null : wert.toString();
    }
}
