package si.fri.rso.samples.artikli.models.converters;

import si.fri.rso.samples.artikli.lib.Artikli;
import si.fri.rso.samples.artikli.models.entities.ArtikliEntity;

public class ArtikliConverter {

    public static Artikli toDto(ArtikliEntity entity) {

        Artikli dto = new Artikli();
        dto.setArtikelId(entity.getId());
        dto.setName(entity.getName());
        dto.setStore(entity.getStore());
        dto.setPrice(entity.getPrice());

        return dto;

    }

    public static ArtikliEntity toEntity(Artikli dto) {

        ArtikliEntity entity = new ArtikliEntity();
        entity.setName(dto.getName());
        entity.setStore(dto.getStore());
        entity.setPrice(dto.getPrice());

        return entity;

    }

}
