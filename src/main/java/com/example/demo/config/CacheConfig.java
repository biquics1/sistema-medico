package com.example.demo.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

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
