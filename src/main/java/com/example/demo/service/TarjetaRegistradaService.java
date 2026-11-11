package com.example.demo.service;

import com.example.demo.dto.PageResponseDTO;
import com.example.demo.dto.TarjetaMarcaDTO;
import com.example.demo.modelo.TarjetaMarca;
import com.example.demo.modelo.TarjetaRegistrada;
import com.example.demo.repository.TarjetaRegistradaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

// Historial de tarjetas usadas en pagos en línea (CU-04). Reconoce si una tarjeta
// ya se usó antes sin guardar el número completo: solo HMAC-SHA256 + últimos 4 dígitos.
@Service
@RequiredArgsConstructor
public class TarjetaRegistradaService {

    private final TarjetaRegistradaRepository repository;

    @Value("${app.tarjeta.hash-secret:CambiarEstaClaveEnApplicationProperties-hash-tarjetas-2026}")
    private String hashSecret;

    // Si la tarjeta ya se usó suma un uso; si no, la registra. Misma transacción que el pago aprobado.
    @Transactional
    public TarjetaRegistrada registrarUso(String numeroSinEspacios, String nombreTitular, TarjetaMarca marca) {
        String hash = hash(numeroSinEspacios);
        LocalDateTime ahora = LocalDateTime.now();

        if (repository.sumarUso(hash, ahora) == 0) {
            TarjetaRegistrada nueva = new TarjetaRegistrada();
            nueva.setHashNumero(hash);
            nueva.setMarca(marca);
            nueva.setUltimosCuatro(numeroSinEspacios.substring(numeroSinEspacios.length() - 4));
            nueva.setNombreTitular(nombreTitular);
            nueva.setVecesUsada(1);
            nueva.setPrimerUso(ahora);
            nueva.setUltimoUso(ahora);
            return repository.saveAndFlush(nueva);
        }
        return repository.findByHashNumero(hash).orElseThrow();
    }

    private String hash(String numero) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(numero.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la tarjeta.", e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TarjetaMarcaDTO.RegistradaDTO> listar(String filtro, Pageable pageable) {
        Page<TarjetaRegistrada> page = (filtro == null || filtro.isBlank())
                ? repository.findAll(pageable)
                : repository.findAll((root, q, cb) -> {
            String like = "%" + filtro.toLowerCase().trim() + "%";
            return cb.or(cb.like(cb.lower(root.get("nombreTitular")), like),
                    cb.like(root.get("ultimosCuatro"), like));
        }, pageable);
        List<TarjetaMarcaDTO.RegistradaDTO> contenido = page.getContent().stream().map(t -> {
            TarjetaMarcaDTO.RegistradaDTO d = new TarjetaMarcaDTO.RegistradaDTO();
            d.setId(t.getId());
            d.setMarca(t.getMarca() != null ? t.getMarca().getNombre() : "-");
            d.setTarjeta("**** **** **** " + t.getUltimosCuatro());
            d.setNombreTitular(t.getNombreTitular());
            d.setVecesUsada(t.getVecesUsada());
            d.setPrimerUso(t.getPrimerUso());
            d.setUltimoUso(t.getUltimoUso());
            return d;
        }).toList();
        return new PageResponseDTO<>(contenido, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}