package com.example.payment_service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cupones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
public class CuponModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 50)
    private String codigo;

    @Column(nullable = false, length = 100)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDescuento tipoDescuento; // PORCENTAJE | MONTO_FIJO

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valorDescuento;   // 20 = 20% o S/20

    @Column(precision = 10, scale = 2)
    private BigDecimal montoMinimo;      // monto mínimo de compra para aplicar

    @Column(precision = 10, scale = 2)
    private BigDecimal descuentoMaximo;  // tope máximo si es porcentaje

    private String eventoId;             // null = aplica a todos los eventos

    @Column(nullable = false)
    private Integer limiteUsos;          // total de usos permitidos

    @Column(nullable = false)
    private Integer usosActuales;        // usos realizados

    @Column(nullable = false)
    private LocalDateTime fechaInicio;

    @Column(nullable = false)
    private LocalDateTime fechaExpiracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCupon estado;          // ACTIVO | INACTIVO | EXPIRADO | AGOTADO

    private String creadoPor;            // email del admin que lo creó

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ── Helpers ───────────────────────────────────────────────────────────────
    public boolean isValido() {
        return estado == EstadoCupon.ACTIVO
                && LocalDateTime.now().isBefore(fechaExpiracion)
                && LocalDateTime.now().isAfter(fechaInicio)
                && usosActuales < limiteUsos;
    }

    public BigDecimal calcularDescuento(BigDecimal montoCompra) {
        if (tipoDescuento == TipoDescuento.PORCENTAJE) {
            BigDecimal descuento = montoCompra.multiply(valorDescuento)
                    .divide(BigDecimal.valueOf(100));
            if (descuentoMaximo != null && descuento.compareTo(descuentoMaximo) > 0) {
                return descuentoMaximo;
            }
            return descuento;
        }
        return valorDescuento.min(montoCompra);
    }
}