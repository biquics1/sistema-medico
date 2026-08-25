package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

// ============================================================
// Caché de catálogos del sistema.
//
// Cubre la RN implícita mencionada como precondición en varios CU
// ("Catálogos deben de estar previamente cargado en cache"):
// CU-00, CU-01, CU-03, CU-04, CU-07.
//
// Los catálogos (especialidad, sucursal, rol, estado_cita,
// examen_laboratorio, sucursal_especialidad, medicamento, laboratorio) cambian con muy poca
// frecuencia (los administra el Administrador General desde CU-14/CU-15)
// pero se consultan constantemente para llenar selects/dropdowns en
// casi toda la aplicación. Por eso se cachean en memoria del servidor
// con Caffeine y se invalidan automáticamente cuando el propio catálogo
// se crea, edita o elimina (@CacheEvict en los controllers).
//
// NO se cachea nada que dependa del usuario/sede/estado dinámico de una
// cita, pago, orden de laboratorio, etc. — solo catálogos "maestros".
// ============================================================
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_ESPECIALIDADES = "especialidades";
    public static final String CACHE_SUCURSALES = "sucursales";
    public static final String CACHE_ROLES = "roles";
    public static final String CACHE_ESTADOS_CITA = "estadosCita";
    public static final String CACHE_EXAMENES_LABORATORIO = "examenesLaboratorio";
    public static final String CACHE_BRANCH_SPECIALTY = "branchSpecialty";
    public static final String CACHE_MEDICAMENTOS = "medicamentos";
    public static final String CACHE_LABORATORIOS = "laboratorios";

    @Bean
    public CaffeineCacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
                CACHE_ESPECIALIDADES,
                CACHE_SUCURSALES,
                CACHE_ROLES,
                CACHE_ESTADOS_CITA,
                CACHE_EXAMENES_LABORATORIO,
                CACHE_BRANCH_SPECIALTY,
                CACHE_MEDICAMENTOS,
                CACHE_LABORATORIOS
        );
        // 30 minutos es suficiente para catálogos que casi no cambian;
        // igual se invalida al instante con @CacheEvict en cada escritura.
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(200));
        return manager;
    }
}
