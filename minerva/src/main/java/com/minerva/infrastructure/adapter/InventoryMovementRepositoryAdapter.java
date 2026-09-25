package com.minerva.infrastructure.adapter;

import com.minerva.domain.entities.inventory.*;
import com.minerva.domain.entities.product.ProductId;
import com.minerva.domain.exceptions.DomainException;
import com.minerva.domain.exceptions.EntityRestoreException;
import com.minerva.domain.repositories.InventoryMovementRepository;
import com.minerva.domain.valueObject.ProductQuantity;
import com.minerva.domain.valueObject.id.*;
import com.minerva.infrastructure.persistence.entity.*;
import com.minerva.infrastructure.persistence.repository.JpaInventoryMovementRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class InventoryMovementRepositoryAdapter implements InventoryMovementRepository {
    @PersistenceContext private EntityManager entityManager;
    private final JpaInventoryMovementRepository movements;

    public InventoryMovementRepositoryAdapter(JpaInventoryMovementRepository movements) { this.movements = movements; }

    @Override @Transactional
    public void append(InventoryMovement movement) { movements.save(toEntity(movement)); }

    @Override @Transactional
    public void appendAll(List<InventoryMovement> values) {
        movements.saveAll(values.stream().map(this::toEntity).toList());
    }

    @Override @Transactional(readOnly = true)
    public List<InventoryMovement> findAll(ProductId productId, InventoryMovementType type,
                                           InventoryMovementSource source, LocalDateTime from, LocalDateTime to,
                                           int page, int size) {
        UUID id = productId == null ? null : productId.getIdValue();
        Specification<InventoryMovementEntity> specification = (root, query, builder) -> builder.conjunction();
        if (id != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("product").get("productId"), id));
        }
        if (type != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("movementType"), type));
        }
        if (source != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("movementSource"), source));
        }
        if (from != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("registrationDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("registrationDate"), to));
        }
        return movements.findAll(specification,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "registrationDate")))
                .getContent().stream().map(this::toDomain).toList();
    }

    private InventoryMovementEntity toEntity(InventoryMovement movement) {
        return new InventoryMovementEntity(movement.getId().getIdValue(),
                entityManager.getReference(ProductEntity.class, movement.getProductId().getIdValue()),
                movement.getQuantity(), movement.getStockBefore().getValue(), movement.getStockAfter().getValue(),
                movement.getType(), movement.getSource(), movement.getSourceId(),
                entityManager.getReference(AppUserEntity.class, movement.getActorId().getIdValue()),
                movement.getRegistrationDate());
    }

    private InventoryMovement toDomain(InventoryMovementEntity entity) {
        try {
            return new InventoryMovement(new InventoryMovementIdImpl(entity.getInventoryMovementId()),
                    new ProductIdImpl(entity.getProduct().getProductId()), entity.getQuantity(),
                    new ProductQuantity(entity.getStockBefore()), new ProductQuantity(entity.getStockAfter()),
                    entity.getMovementType(), entity.getMovementSource(), entity.getSourceId(),
                    new UserName(entity.getActor().getUserName()), entity.getRegistrationDate());
        } catch (DomainException e) {
            throw new EntityRestoreException("Error al restaurar el movimiento de inventario.", e);
        }
    }
}
