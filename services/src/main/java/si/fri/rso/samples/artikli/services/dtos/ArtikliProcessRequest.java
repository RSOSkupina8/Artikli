package si.fri.rso.samples.artikli.services.dtos;

public class ArtikliProcessRequest {

    public ArtikliProcessRequest() {
    }

    public ArtikliProcessRequest(Integer artikliId, float price) {
        this.artikliId = artikliId;
        this.price = price;
    }

    private Integer artikliId;
    private float price;

    public Integer geArtikliId() {
        return artikliId;
    }

    public void setArtikliId(Integer artikliId) {
        this.artikliId = artikliId;
    }

    public float getArtikliPrice() {
        return price;
    }

    public void setPrice(float price) {
        this.price = price;
    }
}
