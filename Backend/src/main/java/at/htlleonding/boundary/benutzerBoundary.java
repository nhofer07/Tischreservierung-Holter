package at.htlleonding.boundary;

import at.htlleonding.DTOs.BenutzerDTO;
import at.htlleonding.repo.ArbeitsplatzRepo;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;

@Path("/api/benutzer")
@ApplicationScoped
public class benutzerBoundary {

    @Inject
    ArbeitsplatzRepo arbeitsplatzRepo;

    @Inject
    JsonWebToken token;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<BenutzerDTO> getBenutzer() {
        return arbeitsplatzRepo.getBenutzer();
    }

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public BenutzerDTO login(at.htlleonding.DTOs.LoginRequestDTO request) {
        return arbeitsplatzRepo.login(request);
    }

    @GET
    @Path("/me")
    @Authenticated
    @Produces(MediaType.APPLICATION_JSON)
    public BenutzerDTO angemeldeterBenutzer() {
        String email = claim("email");
        if (email == null) email = claim("preferred_username");
        if (email == null) email = claim("upn");
        return arbeitsplatzRepo.loginEntra(email);
    }

    private String claim(String name) {
        Object wert = token.getClaim(name);
        return wert == null || wert.toString().isBlank() ? null : wert.toString();
    }
}
