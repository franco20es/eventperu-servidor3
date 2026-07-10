package com.example.payment_service.Repository;

import com.example.payment_service.Model.ComprobanteModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ComprobanteRepository extends JpaRepository<ComprobanteModel, String> {
    List<ComprobanteModel> findByUsuarioIdOrderByFechaEmisionDesc(String usuarioId);
    Optional<ComprobanteModel> findByNumeroComprobante(String numero);
    Optional<ComprobanteModel> findByTransactionReference(String reference);
    Optional<ComprobanteModel> findTopByTipoComprobanteOrderByFechaEmisionDesc(String tipoComprobante);
}