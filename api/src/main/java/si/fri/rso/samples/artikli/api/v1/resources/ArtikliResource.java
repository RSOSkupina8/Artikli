package si.fri.rso.samples.artikli.api.v1.resources;

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
import org.json.JSONObject;
import si.fri.rso.samples.artikli.api.v1.dtos.UploadArtikliResponse;
import si.fri.rso.samples.artikli.lib.Artikli;
import si.fri.rso.samples.artikli.services.beans.ArtikliBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;


@Log
@ApplicationScoped
@Path("/artikli")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ArtikliResource {

    private Logger log = Logger.getLogger(ArtikliResource.class.getName());

    @Inject
    private ArtikliBean artikliBean;


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

    @Operation(description = "Get all artikli with name and store.", summary = "Get all metadata")
    @APIResponses({
            @APIResponse(responseCode = "200",
                    description = "List of artikli",
                    content = @Content(schema = @Schema(implementation = Artikli.class, type = SchemaType.ARRAY)),
                    headers = {@Header(name = "X-Total-Count", description = "Number of objects in list")}
            )})
    @GET
    @Path("/name/{name}/{store}")
    public Response getArtikliByNameAndStore(@Parameter(description = "Name of artikel.", required = true)
                                             @PathParam("name") String name,
                                             @Parameter(description = "Store of artikel.", required = true)
                                             @PathParam("store") String store) {

        Artikli artikli = artikliBean.getArtikliWithNameStore(name, store);
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

    @PUT
    @Path("refresh/{artikliName}")
    public Response putRefreshArtikli(@Parameter(description = "Artikli Name.", required = true)
                                      @PathParam("artikliName") String artikliName){

        List<Artikli> artikliList = artikliBean.getArtikliWithName(artikliName);
        float priceM = 0;
        try {
            priceM = makeMercatorApiCall(artikliName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        float priceS = 0;
        try {
            priceS = makeSparApiCall(artikliName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        CompletableFuture<List<Float>> apiResult = CompletableFuture.supplyAsync(() -> {
            List<Float> prices = null;
            try {
                prices = makeAsnycApiCallScrapper(artikliName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return prices;
        });

        List<Float> result = apiResult.join();
        System.out.println(result);
        Artikli artikliM = artikliBean.getArtikliWithNameStore(artikliName, "Mercator");
        Artikli artikliSN = artikliBean.getArtikliWithNameStore(artikliName, "SN");
        Artikli artikliS = artikliBean.getArtikliWithNameStore(artikliName, "Spar");
        Artikli artikliT = artikliBean.getArtikliWithNameStore(artikliName, "Tus");
        Float priceT = result.get(0);
        Float priceSN = result.get(1);
        artikliM.setPrice(priceM);
        artikliSN.setPrice(priceSN);
        artikliS.setPrice(priceS);
        artikliT.setPrice(priceT);
        artikliM = artikliBean.putArtikli(artikliM.getArtikelId(), artikliM);
        artikliSN = artikliBean.putArtikli(artikliSN.getArtikelId(), artikliSN);
        artikliS = artikliBean.putArtikli(artikliS.getArtikelId(), artikliS);
        artikliT = artikliBean.putArtikli(artikliT.getArtikelId(), artikliT);
        artikliList.clear();
        artikliList.add(artikliM);
        artikliList.add(artikliSN);
        artikliList.add(artikliS);
        artikliList.add(artikliT);
        return Response.status(Response.Status.OK).entity(artikliList).build();

    }

    private static float makeMercatorApiCall(String name) throws Exception{
        float price = 0;
        try{
            URL url = new URL("https://trgovina.mercator.si/market/products/browseProducts/getProducts?limit=1&offset=0&filterData[search]="+
                    URLEncoder.encode(name, "UTF-8"));

            System.out.println(url.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            String contentStr = content.toString().substring(1, content.length()-1);
            JSONObject jsonObject = new JSONObject(contentStr);
            JSONObject dataObject = jsonObject.getJSONObject("data");
            price = dataObject.getFloat("current_price");
            System.out.println("Price: "+price);
        } catch (IOException e){
            e.printStackTrace();
        }
        return price;
    }

    private static float makeSparApiCall(String name) throws Exception{
        float price = 0;
        try{
            URL url = new URL("https://search-spar.spar-ics.com/fact-finder/rest/v2/search/products_lmos_si?q="+
                    URLEncoder.encode(name, "UTF-8")+"&query="+
                    URLEncoder.encode(name, "UTF-8")+
                    "&hitsPerPage=1");

            System.out.println(url.toString());
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder content = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
            in.close();

            JSONObject jsonObject = new JSONObject(content.toString());
            JSONObject dataObject = jsonObject.getJSONArray("hits").getJSONObject(0)
                                              .getJSONObject("masterValues");

            price = dataObject.getFloat("price");
            System.out.println("Price: "+price);
        } catch (IOException e){
            e.printStackTrace();
        }
        return price;
    }

    private static List<Float> makeAsnycApiCallScrapper(String name) throws IOException {
        // Make a URL to the web page
        URL url = new URL("http://20.73.149.162:8080/v1/scrapper/"+URLEncoder.encode(name, "UTF-8"));
        System.out.println(url.toString());

        // Get the input stream through URL Connection
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        String[] elements = content.toString().replaceAll("\\[|\\]", "").split(",");

        // Parse the elements into floats
        List<Float> prices = Arrays.stream(elements)
                .map(Float::parseFloat)
                .collect(Collectors.toList());

        // Print the list of floats
        System.out.println(prices);

        // Return the response
        return prices;
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

        artikli = artikliBean.putArtikli(artikliId, artikli);

        if (artikli == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

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
