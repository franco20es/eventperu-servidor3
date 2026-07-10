package com.example.payment_service.Service;

import com.example.payment_service.dto.Request.CuponRequest;
import com.example.payment_service.dto.Request.ValidarCuponRequest;
import com.example.payment_service.dto.Response.CuponKpisResponse;
import com.example.payment_service.dto.Response.CuponResponse;
import com.example.payment_service.dto.Response.ValidarCuponResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CuponService {

    /** Crea un nuevo cupón. Lanza excepción si el código ya existe. */
    CuponResponse crear(CuponRequest request, String emailAdmin);

    /** Lista paginada con filtros opcionales de búsqueda y estado. */
    Page<CuponResponse> listar(String busqueda, String estado, Pageable pageable);

    /** Obtiene un cupón por su ID. */
    CuponResponse obtener(String id);

    /** Actualiza los campos editables de un cupón. */
    CuponResponse actualizar(String id, CuponRequest request);

    /** Activa un cupón (lo pone en estado ACTIVO). */
    CuponResponse activar(String id);

    /** Desactiva un cupón (lo pone en estado INACTIVO). */
    CuponResponse desactivar(String id);

    /** Elimina un cupón que nunca fue usado. */
    void eliminar(String id);

    /**
     * Valida si un cupón es aplicable y calcula el descuento.
     * No incrementa el contador de usos — eso lo hace aplicar().
     */
    ValidarCuponResponse validar(ValidarCuponRequest request);

    /**
     * Aplica el cupón: valida, calcula descuento e incrementa usosActuales.
     * Usado al momento de confirmar el pago.
     */
    ValidarCuponResponse aplicar(ValidarCuponRequest request);

    /** KPIs del módulo de cupones. */
    CuponKpisResponse getKpis();
}
