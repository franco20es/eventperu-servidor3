package com.example.payment_service.dto.Response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuponKpisResponse {
    private long   totalCupones;
    private long   cuporesActivos;
    private long   cuponesExpirados;
    private long   cuponesAgotados;
    private long   totalUsos;
}