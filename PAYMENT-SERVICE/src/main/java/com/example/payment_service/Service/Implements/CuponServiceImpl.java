package com.example.payment_service.Service.Implements;

import com.example.payment_service.Model.CuponModel;
import com.example.payment_service.Model.EstadoCupon;
import com.example.payment_service.Model.TipoDescuento;
import com.example.payment_service.Repository.CuponRepository;
import com.example.payment_service.Service.CuponService;
import com.example.payment_service.dto.Request.CuponRequest;
import com.example.payment_service.dto.Request.ValidarCuponRequest;
import com.example.payment_service.dto.Response.CuponKpisResponse;
import com.example.payment_service.dto.Response.CuponResponse;
import com.example.payment_service.dto.Response.ValidarCuponResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CuponServiceImpl implements CuponService {

    private final CuponRepository cuponRepo;

    // ── Crear ─────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public CuponResponse crear(CuponRequest req, String emailAdmin) {
        log.info("Creando cupón: {}", req.getCodigo());

        if (cuponRepo.existsByCodigo(req.getCodigo())) {
            throw new RuntimeException("Ya existe un cupón con el código: " + req.getCodigo());
        }

        validarFechas(req);
        validarDescuento(req);

        CuponModel cupon = CuponModel.builder()
                .codigo(req.getCodigo().toUpperCase().trim())
                .descripcion(req.getDescripcion())
                .tipoDescuento(req.getTipoDescuento())
                .valorDescuento(req.getValorDescuento())
                .montoMinimo(req.getMontoMinimo())
                .descuentoMaximo(req.getDescuentoMaximo())
                .eventoId(req.getEventoId())
                .limiteUsos(req.getLimiteUsos())
                .usosActuales(0)
                .fechaInicio(req.getFechaInicio())
                .fechaExpiracion(req.getFechaExpiracion())
                .estado(EstadoCupon.ACTIVO)
                .creadoPor(emailAdmin)
                .build();

        return toResponse(cuponRepo.save(cupon));
    }

    // ── Listar ────────────────────────────────────────────────────────────────
    @Override
    public Page<CuponResponse> listar(String busqueda, String estado, Pageable pageable) {
        EstadoCupon estadoEnum = null;
        if (estado != null && !estado.isBlank()) {
            try { estadoEnum = EstadoCupon.valueOf(estado); }
            catch (Exception ignored) {}
        }

        // Actualiza estados expirados/agotados antes de listar
        actualizarEstadosExpirados();

        return cuponRepo.buscar(
                (busqueda != null && busqueda.isBlank()) ? null : busqueda,
                estadoEnum,
                pageable
        ).map(this::toResponse);
    }

    // ── Obtener ───────────────────────────────────────────────────────────────
    @Override
    public CuponResponse obtener(String id) {
        return toResponse(findById(id));
    }

    // ── Actualizar ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public CuponResponse actualizar(String id, CuponRequest req) {
        CuponModel cupon = findById(id);

        if (!cupon.getCodigo().equals(req.getCodigo().toUpperCase()) &&
                cuponRepo.existsByCodigo(req.getCodigo().toUpperCase())) {
            throw new RuntimeException("Ya existe un cupón con el código: " + req.getCodigo());
        }

        validarFechas(req);
        validarDescuento(req);

        cupon.setCodigo(req.getCodigo().toUpperCase().trim());
        cupon.setDescripcion(req.getDescripcion());
        cupon.setTipoDescuento(req.getTipoDescuento());
        cupon.setValorDescuento(req.getValorDescuento());
        cupon.setMontoMinimo(req.getMontoMinimo());
        cupon.setDescuentoMaximo(req.getDescuentoMaximo());
        cupon.setEventoId(req.getEventoId());
        cupon.setLimiteUsos(req.getLimiteUsos());
        cupon.setFechaInicio(req.getFechaInicio());
        cupon.setFechaExpiracion(req.getFechaExpiracion());

        return toResponse(cuponRepo.save(cupon));
    }

    // ── Activar / Desactivar ──────────────────────────────────────────────────
    @Override
    @Transactional
    public CuponResponse activar(String id) {
        CuponModel cupon = findById(id);
        if (cupon.getEstado() == EstadoCupon.EXPIRADO) {
            throw new RuntimeException("No se puede activar un cupón expirado");
        }
        if (cupon.getUsosActuales() >= cupon.getLimiteUsos()) {
            throw new RuntimeException("No se puede activar un cupón agotado");
        }
        cupon.setEstado(EstadoCupon.ACTIVO);
        log.info("Cupón activado: {}", cupon.getCodigo());
        return toResponse(cuponRepo.save(cupon));
    }

    @Override
    @Transactional
    public CuponResponse desactivar(String id) {
        CuponModel cupon = findById(id);
        cupon.setEstado(EstadoCupon.INACTIVO);
        log.info("Cupón desactivado: {}", cupon.getCodigo());
        return toResponse(cuponRepo.save(cupon));
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void eliminar(String id) {
        CuponModel cupon = findById(id);
        if (cupon.getUsosActuales() > 0) {
            throw new RuntimeException("No se puede eliminar un cupón que ya fue usado");
        }
        cuponRepo.delete(cupon);
        log.info("Cupón eliminado: {}", cupon.getCodigo());
    }

    // ── Validar (solo consulta, no incrementa) ────────────────────────────────
    @Override
    public ValidarCuponResponse validar(ValidarCuponRequest req) {
        CuponModel cupon = cuponRepo.findByCodigo(req.getCodigo().toUpperCase().trim())
                .orElse(null);

        if (cupon == null) {
            return invalido(req.getCodigo(), "Cupón no encontrado");
        }

        return calcularValidacion(cupon, req, false);
    }

    // ── Aplicar (valida e incrementa uso) ────────────────────────────────────
    @Override
    @Transactional
    public ValidarCuponResponse aplicar(ValidarCuponRequest req) {
        CuponModel cupon = cuponRepo.findByCodigo(req.getCodigo().toUpperCase().trim())
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado: " + req.getCodigo()));

        ValidarCuponResponse resultado = calcularValidacion(cupon, req, true);

        if (resultado.isValido()) {
            cupon.setUsosActuales(cupon.getUsosActuales() + 1);
            if (cupon.getUsosActuales() >= cupon.getLimiteUsos()) {
                cupon.setEstado(EstadoCupon.AGOTADO);
            }
            cuponRepo.save(cupon);
            log.info("Cupón aplicado: {} | uso {}/{}", cupon.getCodigo(),
                    cupon.getUsosActuales(), cupon.getLimiteUsos());
        }

        return resultado;
    }

    // ── KPIs ──────────────────────────────────────────────────────────────────
    @Override
    public CuponKpisResponse getKpis() {
        actualizarEstadosExpirados();
        List<CuponModel> all = cuponRepo.findAll();

        return CuponKpisResponse.builder()
                .totalCupones(all.size())
                .cuporesActivos(all.stream().filter(c -> c.getEstado() == EstadoCupon.ACTIVO).count())
                .cuponesExpirados(all.stream().filter(c -> c.getEstado() == EstadoCupon.EXPIRADO).count())
                .cuponesAgotados(all.stream().filter(c -> c.getEstado() == EstadoCupon.AGOTADO).count())
                .totalUsos(all.stream().mapToLong(CuponModel::getUsosActuales).sum())
                .build();
    }

    // ── Helpers privados ──────────────────────────────────────────────────────
    private ValidarCuponResponse calcularValidacion(CuponModel cupon,
                                                    ValidarCuponRequest req,
                                                    boolean esAplicacion) {
        LocalDateTime ahora = LocalDateTime.now();

        if (cupon.getEstado() == EstadoCupon.INACTIVO) {
            return invalido(cupon.getCodigo(), "El cupón está desactivado");
        }
        if (ahora.isAfter(cupon.getFechaExpiracion())) {
            if (cupon.getEstado() != EstadoCupon.EXPIRADO) {
                cupon.setEstado(EstadoCupon.EXPIRADO);
                cuponRepo.save(cupon);
            }
            return invalido(cupon.getCodigo(), "El cupón ha expirado");
        }
        if (ahora.isBefore(cupon.getFechaInicio())) {
            return invalido(cupon.getCodigo(), "El cupón aún no está vigente");
        }
        if (cupon.getUsosActuales() >= cupon.getLimiteUsos()) {
            return invalido(cupon.getCodigo(), "El cupón ha alcanzado su límite de usos");
        }
        if (cupon.getMontoMinimo() != null &&
                req.getMontoCompra().compareTo(cupon.getMontoMinimo()) < 0) {
            return invalido(cupon.getCodigo(),
                    "El monto mínimo para usar este cupón es S/ " + cupon.getMontoMinimo());
        }
        if (cupon.getEventoId() != null && req.getEventoId() != null &&
                !cupon.getEventoId().equals(req.getEventoId())) {
            return invalido(cupon.getCodigo(), "El cupón no aplica para este evento");
        }

        BigDecimal descuento = cupon.calcularDescuento(req.getMontoCompra())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal montoFinal = req.getMontoCompra().subtract(descuento)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);

        return ValidarCuponResponse.builder()
                .valido(true)
                .codigo(cupon.getCodigo())
                .mensaje("Cupón aplicado correctamente")
                .tipoDescuento(cupon.getTipoDescuento().name())
                .valorDescuento(cupon.getValorDescuento())
                .montoCompra(req.getMontoCompra())
                .montoDescuento(descuento)
                .montoFinal(montoFinal)
                .build();
    }

    private ValidarCuponResponse invalido(String codigo, String mensaje) {
        return ValidarCuponResponse.builder()
                .valido(false)
                .codigo(codigo)
                .mensaje(mensaje)
                .build();
    }

//    @Transactional
//    protected void actualizarEstadosExpirados() {
//        cuponRepo.findAll().stream()
//                .filter(c -> c.getEstado() == EstadoCupon.ACTIVO &&
//                        LocalDateTime.now().isAfter(c.getFechaExpiracion()))
//                .forEach(c -> {
//                    c.setEstado(EstadoCupon.EXPIRADO);
//                    cuponRepo.save(c);
//                });
//    }
@Transactional
protected void actualizarEstadosExpirados() {
    cuponRepo.findAll().stream()
            .filter(c -> c.getEstado() == EstadoCupon.ACTIVO &&
                    LocalDateTime.now().isAfter(c.getFechaExpiracion()))
            .forEach(c -> {
                c.setEstado(EstadoCupon.EXPIRADO);
                cuponRepo.save(c);
            });
}

    private CuponModel findById(String id) {
        return cuponRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Cupón no encontrado: " + id));
    }

    private void validarFechas(CuponRequest req) {
        if (req.getFechaExpiracion().isBefore(req.getFechaInicio())) {
            throw new RuntimeException("La fecha de expiración debe ser posterior a la fecha de inicio");
        }
    }

    private void validarDescuento(CuponRequest req) {
        if (req.getTipoDescuento() == TipoDescuento.PORCENTAJE &&
                req.getValorDescuento().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new RuntimeException("El descuento porcentual no puede superar el 100%");
        }
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private CuponResponse toResponse(CuponModel c) {
        int disponibles = c.getLimiteUsos() - c.getUsosActuales();
        double pct = c.getLimiteUsos() > 0
                ? (double) c.getUsosActuales() / c.getLimiteUsos() * 100
                : 0;

        return CuponResponse.builder()
                .id(c.getId())
                .codigo(c.getCodigo())
                .descripcion(c.getDescripcion())
                .tipoDescuento(c.getTipoDescuento().name())
                .valorDescuento(c.getValorDescuento())
                .montoMinimo(c.getMontoMinimo())
                .descuentoMaximo(c.getDescuentoMaximo())
                .eventoId(c.getEventoId())
                .limiteUsos(c.getLimiteUsos())
                .usosActuales(c.getUsosActuales())
                .usosDisponibles(Math.max(disponibles, 0))
                .pctUso(Math.round(pct * 10.0) / 10.0)
                .fechaInicio(c.getFechaInicio())
                .fechaExpiracion(c.getFechaExpiracion())
                .estado(c.getEstado().name())
                .creadoPor(c.getCreadoPor())
                .createdAt(c.getCreatedAt())
                .build();
    }
}