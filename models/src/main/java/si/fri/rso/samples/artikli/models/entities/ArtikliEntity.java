package si.fri.rso.samples.artikli.models.entities;

import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "artikli")
@NamedQueries(value =
        {
                @NamedQuery(name = "ArtikliEntity.getAll",
                        query = "SELECT im FROM ArtikliEntity im")
        })
public class ArtikliEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name")
    private String name;

    @Column(name = "store")
    private String store;

    @Column(name = "price")
    private Float price;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStore() {
        return store;
    }

    public void setStore(String store) {
        this.store = store;
    }

    public Float getPrice() {
        return price;
    }

    public void setPrice(Float price) {
        this.price = price;
    }

}