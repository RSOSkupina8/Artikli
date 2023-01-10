package si.fri.rso.samples.artikli.api.v1.resources;

import com.kumuluz.ee.cors.annotations.CrossOrigin;
import com.kumuluz.ee.logs.cdi.Log;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import si.fri.rso.samples.artikli.api.v1.dtos.UploadArtikliResponse;
import si.fri.rso.samples.artikli.lib.Artikli;
import si.fri.rso.samples.artikli.services.beans.ArtikliBean;
import si.fri.rso.samples.artikli.services.clients.ArtikliProcessingApi;
import si.fri.rso.samples.artikli.services.dtos.ArtikliProcessRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;


@Log
@ApplicationScoped
@Path("/artikli")
@Produces(MediaType.APPLICATION_JSON)
@Consumes({MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN})
@CrossOrigin(supportedMethods = "GET, POST, PUT, DELETE, HEAD, OPTIONS")
public class ArtikliResource {

    private Logger log = Logger.getLogger(ArtikliResource.class.getName());

    @Inject
    private ArtikliBean artikliBean;

    @Inject
    @RestClient
    private ArtikliProcessingApi artikliProcessingApi;

    @Context
    protected UriInfo uriInfo;

    @Operation(description = "Get all artikli metadata.", summary = "Get all metadata")
    @APIResponses({
            @APIResponse(responseCode = "200",
                    description = "List of image metadata",
                    content = @Content(schema = @Schema(implementation = Artikli.class, type = SchemaType.ARRAY)),
                    headers = {@Header(name = "X-Total-Count", description = "Number of objects in list")}
            )})
    @GET
    public Response getArtikli() {

        List<Artikli> artikli = artikliBean.getArtikliFilter(uriInfo);
        return Response.status(Response.Status.OK).entity(artikli).build();
    }

    @Operation(description = "Get all artikli with name.", summary = "Get all metadata")
    @APIResponses({
            @APIResponse(responseCode = "200",
                    description = "List of artikli",
                    content = @Content(schema = @Schema(implementation = Artikli.class, type = SchemaType.ARRAY)),
                    headers = {@Header(name = "X-Total-Count", description = "Number of objects in list")}
            )})
    @GET
    @Path("/name/{name}")
    public Response getArtikli(@Parameter(description = "Name of artikel.", required = true)
                                   @PathParam("name") String name) {

        List<Artikli> artikli = artikliBean.getArtikliWithName(name);
        return Response.status(Response.Status.OK).entity(artikli).build();
    }


    @Operation(description = "Get metadata for artikel.", summary = "Get metadata for artikel")
    @APIResponses({
            @APIResponse(responseCode = "200",
                    description = "Image metadata",
                    content = @Content(
                            schema = @Schema(implementation = Artikli.class))
            )})
    @GET
    @Path("/{artikliId}")
    public Response getArtikli(@Parameter(description = "Metadata ID.", required = true)
                                     @PathParam("artikliId") Integer artikliId) {

        Artikli artikli = artikliBean.getArtikli(artikliId);

        if (artikli == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.OK).entity(artikli).build();
    }

    @Operation(description = "Add artikel metadata.", summary = "Add metadata")
    @APIResponses({
            @APIResponse(responseCode = "201",
                    description = "Metadata successfully added."
            ),
            @APIResponse(responseCode = "405", description = "Validation error .")
    })
    @POST
    public Response createArtikli(@RequestBody(
            description = "DTO object with artikel metadata.",
            required = true, content = @Content(
            schema = @Schema(implementation = Artikli.class))) Artikli artikli) {

        if ((artikli.getName() == null || artikli.getStore() == null)) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        else {
            artikli = artikliBean.createArtikli(artikli);
        }

        return Response.status(Response.Status.CREATED).entity(artikli).build();

    }


    @Operation(description = "Update data for an item.", summary = "Update artikli")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Metadata successfully updated."
            )
    })
    @PUT
    @Path("{artikliId}")
    public Response putArtikli(@Parameter(description = "Artikli ID.", required = true)
                               @PathParam("artikliId") Integer artikliId,
                               @RequestBody(
                                       description = "DTO object with artikli metadata.",
                                       required = true, content = @Content(
                                       schema = @Schema(implementation = Artikli.class)))
                               Artikli artikli){

        artikli = artikliBean.getArtikli(artikliId);

        artikli = artikliBean.putArtikli(artikliId, artikli);

        if (artikli == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.NOT_MODIFIED).build();

    }

    @PUT
    @Path("refresh/{artikliId}")
    public Response putRefreshArtikli(@Parameter(description = "Artikli Name.", required = true)
                               @PathParam("artikliId") Integer artikliId,
                               @RequestBody(
                                       description = "DTO object with artikli metadata.",
                                       required = true, content = @Content(
                                       schema = @Schema(implementation = Artikli.class)))
                               Artikli artikli){

        artikli = artikliBean.getArtikli(artikliId);

        CompletionStage<String> stringCompletionStage =
                artikliProcessingApi.processArtikliAsynch(new ArtikliProcessRequest(artikliId, 0));

        stringCompletionStage.whenComplete((s, throwable) -> System.out.println(s));
        stringCompletionStage.exceptionally(throwable -> {
            log.severe(throwable.getMessage());
            return throwable.getMessage();
        });

//        artikli = artikliBean.putArtikli(artikliId, artikli);
        return Response.status(Response.Status.NOT_MODIFIED).build();

    }

    @Operation(description = "Delete metadata for artikel.", summary = "Delete metadata")
    @APIResponses({
            @APIResponse(
                    responseCode = "200",
                    description = "Metadata successfully deleted."
            ),
            @APIResponse(
                    responseCode = "404",
                    description = "Not found."
            )
    })
    @DELETE
    @Path("{artikliId}")
    public Response deleteArtikli(@Parameter(description = "Metadata ID.", required = true)
                                      @PathParam("artikliId") Integer artikliId){

        boolean deleted = artikliBean.deleteArtikli(artikliId);

        if (deleted) {
            return Response.status(Response.Status.NO_CONTENT).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }
}
