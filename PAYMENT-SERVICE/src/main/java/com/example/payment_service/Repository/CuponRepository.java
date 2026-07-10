package com.example.payment_service.Repository;


import com.example.payment_service.Model.CuponModel;
import com.example.payment_service.Model.EstadoCupon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CuponRepository extends JpaRepository<CuponModel, String> {

    Optional<CuponModel> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    Page<CuponModel> findByEstado(EstadoCupon estado, Pageable pageable);

//    @Query("""
//        SELECT c FROM CuponModel c
//        WHERE (:busqueda IS NULL OR
//               LOWER(c.codigo) LIKE LOWER(CONCAT('%', :busqueda, '%')) OR
//               LOWER(c.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))
//          AND (:estado IS NULL OR c.estado = :estado)
//        """)
@Query("""
    SELECT c FROM CuponModel c
    WHERE (:busqueda IS NULL OR 
           LOWER(c.codigo) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')) OR
           LOWER(c.descripcion) LIKE LOWER(CONCAT('%', CAST(:busqueda AS string), '%')))
      AND (:estado IS NULL OR c.estado = :estado)
    """)
    Page<CuponModel> buscar(
            @Param("busqueda") String busqueda,
            @Param("estado")   EstadoCupon estado,
            Pageable pageable);

    @Modifying
    @Query("UPDATE CuponModel c SET c.estado = 'EXPIRADO' " +
            "WHERE c.estado = 'ACTIVO' AND c.fechaExpiracion < CURRENT_TIMESTAMP")
    int expirarCuponesVencidos();
}
