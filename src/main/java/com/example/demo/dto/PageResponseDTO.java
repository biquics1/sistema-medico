package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// DTO genérico de paginación, usado por todos los listados con paginación del
// sistema (5 argumentos: contenido, número de página, tamaño, total de elementos
// y total de páginas).
@Data
@AllArgsConstructor
public class PageResponseDTO<T> {
    private List<T> contenido;
    private int pagina;
    private int tamano;
    private long totalElementos;
    private int totalPaginas;
}
