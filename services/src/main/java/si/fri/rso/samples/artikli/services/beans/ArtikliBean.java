package si.fri.rso.samples.artikli.services.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.kumuluz.ee.rest.beans.QueryParameters;
import com.kumuluz.ee.rest.utils.JPAUtils;

import org.eclipse.microprofile.metrics.annotation.Timed;
import si.fri.rso.samples.artikli.lib.Artikli;
import si.fri.rso.samples.artikli.models.converters.ArtikliConverter;
import si.fri.rso.samples.artikli.models.entities.ArtikliEntity;


@RequestScoped
public class ArtikliBean {

    private Logger log = Logger.getLogger(ArtikliBean.class.getName());

    @Inject
    private EntityManager em;

    public List<Artikli> getArtikli() {

        TypedQuery<ArtikliEntity> query = em.createNamedQuery(
                "ArtikliEntity.getAll", ArtikliEntity.class);

        List<ArtikliEntity> resultList = query.getResultList();

        return resultList.stream().map(ArtikliConverter::toDto).collect(Collectors.toList());

    }

    @Timed
    public List<Artikli> getArtikliFilter(UriInfo uriInfo) {

        QueryParameters queryParameters = QueryParameters.query(uriInfo.getRequestUri().getQuery()).defaultOffset(0)
                .build();

        return JPAUtils.queryEntities(em, ArtikliEntity.class, queryParameters).stream()
                .map(ArtikliConverter::toDto).collect(Collectors.toList());
    }

    public Artikli getArtikli(Integer id) {

        ArtikliEntity artikliEntity = em.find(ArtikliEntity.class, id);

        if (artikliEntity == null) {
            throw new NotFoundException();
        }

        Artikli artikli = ArtikliConverter.toDto(artikliEntity);

        return artikli;
    }

    public Artikli createArtikli(Artikli artikli) {

        ArtikliEntity artikliEntity = ArtikliConverter.toEntity(artikli);

        try {
            beginTx();
            em.persist(artikliEntity);
            commitTx();
        }
        catch (Exception e) {
            rollbackTx();
        }

        if (artikliEntity.getId() == null) {
            throw new RuntimeException("Entity was not persisted");
        }

        return ArtikliConverter.toDto(artikliEntity);
    }

    public Artikli putArtikli(Integer id, Artikli artikli) {

        ArtikliEntity c = em.find(ArtikliEntity.class, id);

        if (c == null) {
            return null;
        }

        ArtikliEntity updatedArtikliEntity = ArtikliConverter.toEntity(artikli);

        try {
            beginTx();
            updatedArtikliEntity.setId(c.getId());
            updatedArtikliEntity = em.merge(updatedArtikliEntity);
            commitTx();
        }
        catch (Exception e) {
            rollbackTx();
        }

        return ArtikliConverter.toDto(updatedArtikliEntity);
    }

    public boolean deleteArtikli(Integer id) {

        ArtikliEntity Artikli = em.find(ArtikliEntity.class, id);

        if (Artikli != null) {
            try {
                beginTx();
                em.remove(Artikli);
                commitTx();
            }
            catch (Exception e) {
                rollbackTx();
            }
        }
        else {
            return false;
        }

        return true;
    }

    private void beginTx() {
        if (!em.getTransaction().isActive()) {
            em.getTransaction().begin();
        }
    }

    private void commitTx() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().commit();
        }
    }

    private void rollbackTx() {
        if (em.getTransaction().isActive()) {
            em.getTransaction().rollback();
        }
    }
}
