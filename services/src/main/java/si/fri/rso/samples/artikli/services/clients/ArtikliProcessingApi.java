package si.fri.rso.samples.artikli.services.clients;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import si.fri.rso.samples.artikli.services.dtos.ArtikliProcessRequest;

import javax.enterprise.context.Dependent;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import java.util.List;
import java.util.concurrent.CompletionStage;

@Path("processing")
@RegisterRestClient(configKey="artikli-processing-api")
@Dependent
public interface ArtikliProcessingApi {

    @PUT
    CompletionStage<String> processArtikliAsynch(ArtikliProcessRequest artikliProccessRequest);

}

