// ============================================================
// common.js
// Logica compartida por TODAS las paginas del panel interno:
// sesion, permisos por rol, apiFetch, datos del usuario en el header
// y proteccion de acceso a nivel de pagina.
// Se incluye con <script src="common.js"></script> ANTES del <script>
// propio de cada pagina.
// ============================================================
    // ============================================================
    // GUARDIA DE SESIÓN
    // ============================================================
    const jwt = sessionStorage.getItem('jwt');
    if (!jwt) {
        window.location.href = 'login.html';
    }

    // CORREGIDO: se usa .trim() por si el rol llega con espacios en blanco
    // extra desde el backend/DB (ej. "Cajero " en vez de "Cajero"), lo cual
    // rompía silenciosamente la comparación exacta en tienePermiso() y
    // ocultaba botones/páginas enteras sin ningún error visible.
    const rolActual = (sessionStorage.getItem('rol') || '').trim();

    function cerrarSesion() {
        sessionStorage.clear();
        _catalogoCache = {}; // NUEVO: limpiar caché de catálogos al salir
        window.location.href = 'login.html';
    }

    function iniciales(nombreCompleto) {
        if (!nombreCompleto) return '--';
        const partes = nombreCompleto.trim().split(/\s+/);
        return ((partes[0]?.[0] || '') + (partes[1]?.[0] || '')).toUpperCase();
    }

    (function mostrarUsuario() {
        const nombreCompleto = sessionStorage.getItem('nombreCompleto') || sessionStorage.getItem('nombreUsuario') || 'Usuario';
        const sucursalNombre = sessionStorage.getItem('sucursalNombre') || '';
        document.getElementById('nombreUsuario').innerText = nombreCompleto;
        // NUEVO: si el usuario está atado a una sede (ej. Administrador de
        // Sede, Médico, Enfermero, etc.) se muestra junto al rol, para que
        // quede claro a qué sucursal está acotado ("Administrador · Sede Central").
        document.getElementById('rolUsuario').innerText = sucursalNombre ? `${rolActual} · ${sucursalNombre}` : rolActual;
        document.getElementById('avatarIniciales').innerText = iniciales(nombreCompleto);
        const dashboardNombreEl = document.getElementById('dashboardNombre');
        if (dashboardNombreEl) dashboardNombreEl.innerText = nombreCompleto; // solo existe en index.html
    })();


    // ============================================================
    // CONTROL DE MENÚ POR ROL (RN implícita de cada CU: cada actor
    // solo debe ver las opciones de sus propios casos de uso)
    // ============================================================
    function tienePermiso(rolesPermitidos) {
        if (rolesPermitidos === '*') return true;
        // CORREGIDO: se normaliza (.trim()) cada rol de la lista antes de
        // comparar, para que un espacio de más en el atributo data-roles
        // del HTML (ej. "Cajero, Administrador") no rompa la comparación
        // exacta que usa Array.includes().
        return rolesPermitidos.split(',').map(r => r.trim()).includes(rolActual);
    }

    function aplicarPermisosDeMenu() {
        document.querySelectorAll('.nav-item').forEach(item => {
            const permitido = tienePermiso(item.dataset.roles);
            item.style.display = permitido ? 'flex' : 'none';
        });
    }


    async function apiFetch(url, opciones = {}) {
        const resp = await fetch(url, {
            ...opciones,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + sessionStorage.getItem('jwt'),
                ...(opciones.headers || {})
            }
        });
        if (resp.status === 401) { cerrarSesion(); return; }
        const data = await resp.json().catch(() => ({}));
        if (!resp.ok) throw new Error(data.mensaje || data.message || 'Ocurrió un error inesperado.');
        return data;
    }

    // ============================================================
    // NUEVO: CACHÉ DE CATÁLOGOS (RN implícita en CU-00/01/03/04/07:
    // "Catálogos deben de estar previamente cargado en cache").
    //
    // Vive en memoria del navegador mientras la pestaña está abierta.
    // La primera pantalla que pide un catálogo (especialidades,
    // sucursales, roles, sucursal_especialidad, etc.) dispara el fetch
    // real contra la API; el resto de pantallas de la misma sesión
    // reutilizan esa respuesta sin volver a golpear el backend.
    //
    // Uso: reemplazar, SOLO para catálogos de solo lectura (dropdowns),
    // el llamado apiFetch(url) por apiFetchCached(url) en cada página.
    // Ej: const especialidades = await apiFetchCached('/api/especialidades');
    //
    // El backend YA cachea estos mismos endpoints con Caffeine (ver
    // CacheConfig.java), así que esto es una segunda capa: evita incluso
    // el viaje de red cuando el usuario navega entre pantallas.
    // ============================================================
    let _catalogoCache = {};

    async function apiFetchCached(url, opciones = {}) {
        if (_catalogoCache[url]) return _catalogoCache[url];
        const data = await apiFetch(url, opciones);
        _catalogoCache[url] = data;
        return data;
    }

    // Limpia el caché de catálogos del navegador. Llamar SOLO desde la
    // propia pantalla de administración de catálogos (ej. catalogos.html,
    // sedes-esp.html, usuarios.html) justo después de crear/editar/eliminar
    // un registro, para que la sesión actual vea el dato actualizado en
    // vez de la copia cacheada.
    // Sin argumento: limpia todo. Con argumento: limpia solo esa URL.
    function invalidarCatalogoCache(url) {
        if (url) {
            delete _catalogoCache[url];
        } else {
            _catalogoCache = {};
        }
    }

    // ============================================================
    // Marca como activo el item del menu que corresponde a la pagina
    // actual (usa el atributo data-tab del <body>).
    // ============================================================
    function marcarPaginaActiva() {
        const tabActual = document.body.dataset.tab;
        document.querySelectorAll('.nav-item').forEach(item => {
            const activo = item.dataset.tab === tabActual;
            item.classList.toggle('bg-brand-yellow', activo);
            item.classList.toggle('text-brand-black', activo);
            item.classList.toggle('text-gray-300', !activo);
            item.classList.toggle('hover:bg-brand-gray', !activo);
            item.classList.toggle('hover:text-brand-yellow', !activo);
        });
    }

    // ============================================================
    // Control de acceso a nivel de pagina: si el rol actual no puede
    // ver este modulo, reemplaza el contenido principal por el aviso
    // de "Sin acceso" y detiene la inicializacion propia de la pagina.
    // Se llama al inicio del <script> de cada pagina:
    //   if (!protegerPagina()) { /* no seguir inicializando */ }
    // ============================================================
    function protegerPagina() {
        const rolesPermitidos = document.body.dataset.roles;
        if (tienePermiso(rolesPermitidos)) return true;

        document.getElementById('main-content').innerHTML = `
        <section id="section-sin-acceso" class="space-y-6">
            <div class="bg-white p-10 rounded-xl border border-gray-200 shadow-sm text-center max-w-xl mx-auto">
                <i class="fa-solid fa-lock text-4xl text-gray-300 mb-3"></i>
                <p class="text-gray-500 font-medium">Tu rol no tiene acceso a esta sección.</p>
            </div>
        </section>
        `;
        document.getElementById('page-title').innerText = 'Acceso no disponible';
        return false;
    }

    aplicarPermisosDeMenu();
    marcarPaginaActiva();
