# Demo API — Gestión de Usuarios y Generador de Colores

Aplicación web construida con **Spring Boot 3 / Java 24** que combina una **API REST** con **vistas renderizadas en el servidor mediante Thymeleaf**.

La aplicación cumple dos funciones principales:

- **Generación de colores:** produce un color hexadecimal aleatorio (`#rrggbb`) que se muestra en la página principal y se asocia a cada usuario creado.
- **Gestión de usuarios:** crea usuarios (persistidos en MariaDB), los lista de forma paginada en una vista HTML y permite eliminarlos a través de la API REST.

El sistema se desplegó sobre un **servidor Linux dentro de la red privada VPN de la universidad**. Se publicó de forma segura usando **Nginx como proxy inverso** con un **certificado SSL autofirmado** generado con OpenSSL.

> **Acceso:** `https://10.20.250.65` (requiere conexión activa a la VPN universitaria).

Este documento describe **cómo quedó construido y desplegado** el proyecto: la arquitectura implementada, la configuración aplicada en el servidor y las decisiones que se tomaron durante el despliegue.

---

## Tabla de Contenidos

1. [Arquitectura del Sistema](#arquitectura-del-sistema)
2. [Stack Tecnológico](#stack-tecnológico)
3. [Base de Datos](#base-de-datos)
4. [Despliegue en el Servidor (Linux + systemd)](#despliegue-en-el-servidor-linux--systemd)
5. [Nginx y SSL Autofirmado](#nginx-y-ssl-autofirmado)
6. [Acceso a la Aplicación](#acceso-a-la-aplicación)
7. [Endpoints y Vistas](#endpoints-y-vistas)
8. [Consideraciones de Desarrollo (Windows vs. Linux)](#consideraciones-de-desarrollo-windows-vs-linux)

---

## Arquitectura del Sistema

El tráfico del cliente no llega directamente a Spring Boot. Se montó Nginx como punto de entrada único: termina el TLS (HTTPS) y reenvía las peticiones internamente por HTTP al puerto `8082`.

```text
                         VPN Universitaria (red privada)
   ┌──────────────┐        │
   │   Cliente    │  HTTPS │        ┌────────────────────┐       ┌───────────────────────┐       ┌──────────────────┐
   │  (navegador) │ ───────┼──────► │        Nginx        │ ────► │     Spring Boot        │ ────► │     MariaDB       │
   │              │  :443  │        │  (Proxy Inverso)    │ HTTP  │  (app embebida Tomcat) │ JDBC  │   BD: api         │
   └──────────────┘        │        │  :80 → redirige :443│ :8082 │   :8082                │ :3306 │   :3306           │
                           │        └────────────────────┘       └───────────────────────┘       └──────────────────┘
                      10.20.250.65
```

Flujo del tráfico:

`Cliente (HTTPS/443)` → `Nginx (Proxy Inverso, termina SSL)` → `Spring Boot (HTTP/8082)` → `MariaDB (BD: api)`

Decisiones aplicadas:

- Nginx escucha en el puerto **443** (HTTPS) y redirige automáticamente el **80** (HTTP) a HTTPS.
- Spring Boot escucha únicamente en **localhost:8082** y confía en las cabeceras `X-Forwarded-*` que envía Nginx. Para ello se activó `server.forward-headers-strategy=framework`.
- La persistencia se realiza en **MariaDB**, base de datos `api`, puerto `3306`.

---

## Stack Tecnológico

| Componente           | Tecnología                                  |
| -------------------- | ------------------------------------------- |
| Lenguaje             | Java 24                                     |
| Framework            | Spring Boot 3 (Spring MVC, Spring Data JPA) |
| Motor de plantillas  | Thymeleaf                                   |
| Base de datos        | MariaDB (driver `mariadb-java-client`)      |
| Build / Dependencias | Maven (con wrapper `mvnw`)                  |
| Validación           | Jakarta Bean Validation                     |
| Servidor web / Proxy | Nginx (reverse proxy + TLS)                 |
| Certificados         | OpenSSL (SSL autofirmado)                   |
| Despliegue           | Linux + systemd                             |

---

## Base de Datos

Se usó **MariaDB** con una base de datos llamada `api`, escuchando en el puerto `3306`. La aplicación se conecta mediante un usuario con permisos sobre esa base de datos.

La base de datos y el usuario de la aplicación quedaron configurados así:

```sql
CREATE DATABASE IF NOT EXISTS api CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'api_user'@'localhost' IDENTIFIED BY '********';
GRANT ALL PRIVILEGES ON api.* TO 'api_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## Despliegue en el Servidor (Linux + systemd)

### Configuración de `application.properties`

El archivo `src/main/resources/application.properties` se parametrizó con **variables de entorno**, de modo que las credenciales no quedan expuestas en el repositorio. Esta es la configuración que quedó aplicada:

```properties
server.port=${PORT:8082}
server.forward-headers-strategy=framework

server.error.include-stacktrace=never
server.error.include-message=never

spring.datasource.url=jdbc:mariadb://localhost:3306/${DB_NAME}?useSSL=false&serverTimezone=UTC
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.datasource.driver-class-name=org.mariadb.jdbc.Driver

spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=false

logging.level.root=WARN
logging.level.org.springframework.web=INFO
```

La URL de conexión resuelve a `jdbc:mariadb://localhost:3306/api`, ya que el nombre `api` se inyecta mediante la variable `DB_NAME`.

Se dejó `spring.jpa.hibernate.ddl-auto=validate`: Hibernate **no crea ni modifica el esquema**, solo valida que las tablas existan y coincidan con las entidades. El esquema se gestionó de forma controlada en la base de datos.

### Empaquetado

El JAR ejecutable se generó con Maven:

```bash
./mvnw clean package -DskipTests
```

El artefacto resultante (`target/demo-0.0.1-SNAPSHOT.jar`) se copió al servidor en una ruta estable, `/opt/spring-app/app.jar`.

### Variables de entorno del servicio

Las credenciales se almacenaron en un archivo de entorno protegido (`/etc/spring-app.env`, permisos `600`), que no se versiona en Git. Contiene las claves `PORT`, `DB_NAME`, `DB_USERNAME` y `DB_PASSWORD`.

### Servicio systemd (`spring-app.service`)

Para que la aplicación se ejecute de forma continua en segundo plano y se reinicie automáticamente ante fallos, se creó la unidad `/etc/systemd/system/spring-app.service` con el siguiente contenido:

```ini
[Unit]
Description=Spring Boot - Demo API (Usuarios y Colores)
After=network.target mariadb.service
Wants=mariadb.service

[Service]
Type=simple
User=www-data
Group=www-data
WorkingDirectory=/opt/spring-app
EnvironmentFile=/etc/spring-app.env
ExecStart=/usr/bin/java -jar /opt/spring-app/app.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

El servicio se habilitó para arrancar junto con el sistema y se puso en marcha. Para administrarlo (consultar estado, logs o reiniciar tras un nuevo despliegue) se usan los comandos habituales de `systemctl` y `journalctl`:

```bash
sudo systemctl status spring-app.service     # estado del servicio
sudo journalctl -u spring-app.service -f      # logs en tiempo real
sudo systemctl restart spring-app.service     # reinicio tras actualizar el JAR
```

---

## Nginx y SSL Autofirmado

### Certificado autofirmado (OpenSSL)

El certificado y la clave privada se generaron con OpenSSL, con validez de 365 días y el `Common Name (CN)` igual a la IP de acceso `10.20.250.65`:

```bash
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout /etc/ssl/private/nginx-selfsigned.key \
  -out /etc/ssl/certs/nginx-selfsigned.crt \
  -subj "/C=CO/ST=Narino/L=Pasto/O=Universidad/OU=TI/CN=10.20.250.65"
```

Además se generaron parámetros Diffie-Hellman para reforzar la negociación TLS:

```bash
sudo openssl dhparam -out /etc/nginx/dhparam.pem 2048
```

Los archivos quedaron ubicados en:

- Certificado: `/etc/ssl/certs/nginx-selfsigned.crt`
- Clave privada: `/etc/ssl/private/nginx-selfsigned.key`

### Configuración de Nginx

El sitio se definió en `/etc/nginx/sites-available/mi-app` y se habilitó mediante un enlace simbólico en `sites-enabled`. La configuración aplicada redirige todo el HTTP (80) a HTTPS (443) y reenvía las peticiones a Spring Boot pasando las cabeceras del cliente real:

```nginx
# Redirección automática de HTTP (80) a HTTPS (443)
server {
    listen 80;
    listen [::]:80;
    server_name 10.20.250.65;

    return 301 https://$host$request_uri;
}

# Servidor HTTPS (443) con proxy inverso hacia Spring Boot
server {
    listen 443 ssl;
    listen [::]:443 ssl;
    server_name 10.20.250.65;

    # Certificado SSL autofirmado
    ssl_certificate     /etc/ssl/certs/nginx-selfsigned.crt;
    ssl_certificate_key /etc/ssl/private/nginx-selfsigned.key;
    ssl_dhparam         /etc/nginx/dhparam.pem;

    ssl_protocols       TLSv1.2 TLSv1.3;
    ssl_ciphers         HIGH:!aNULL:!MD5;

    location / {
        proxy_pass http://127.0.0.1:8082;

        # Cabeceras reenviadas para que Spring Boot conozca el cliente real y el esquema
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host  $host;
        proxy_set_header X-Forwarded-Port  $server_port;
    }
}
```

Tras validar la configuración (`nginx -t`), se recargó Nginx para dejarla activa.

---

## Acceso a la Aplicación

Para entrar a la aplicación ya desplegada:

1. **Conectarse a la VPN de la universidad.** La IP `10.20.250.65` pertenece a la red privada, por lo que no es accesible sin la VPN activa.
2. Abrir el navegador en `https://10.20.250.65`.
3. Como el certificado es **autofirmado**, el navegador muestra una advertencia de seguridad ("La conexión no es privada" / "Riesgo potencial de seguridad"). Es un comportamiento esperado; hay que continuar aceptando el riesgo.
4. La página principal muestra el color generado. La gestión de usuarios está en `https://10.20.250.65/users`.

---

## Endpoints y Vistas

### Vistas HTML (Thymeleaf)

Renderizadas en el servidor. Las plantillas están en `src/main/resources/templates/`.

| Método | Ruta               | Plantilla     | Descripción                                                                                     |
| ------ | ------------------ | ------------- | ----------------------------------------------------------------------------------------------- |
| `GET`  | `/`                | `color.html`  | Página principal. Genera y muestra un color hexadecimal aleatorio.                              |
| `GET`  | `/users?pagina=0`  | `users.html`  | Lista paginada de usuarios (5 por página). El parámetro `pagina` es opcional (por defecto `0`). |

`base.html` es la plantilla base/layout que reutilizan `color.html` y `users.html`; no se accede directamente desde una URL.

### API REST (JSON)

Controladores ubicados en el paquete `com.example.demo.Controller.api`.

| Método   | Endpoint         | Descripción                                                           | Cuerpo de la petición              | Respuesta                                                        |
| -------- | ---------------- | --------------------------------------------------------------------- | ---------------------------------- | --------------------------------------------------------------- |
| `GET`    | `/color`         | Devuelve un color hexadecimal aleatorio.                              | —                                  | `200 OK` → `{ "color": "#a3f2c1" }`                             |
| `POST`   | `/users/saludar` | Crea un usuario con el nombre dado, le asigna un color y lo persiste. | `{ "nombre": "Juan" }` (requerido) | `201 CREATED` → `{ "saludo": "¡Hola Juan!", "color": "#a3f2c1" }` |
| `DELETE` | `/users/{id}`    | Elimina un usuario por su `id` (UUID).                                | —                                  | `204 NO CONTENT` (sin cuerpo)                                   |

#### Estructura de errores

Las respuestas de error siguen el formato definido en `ErrResponse`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "detail": "Descripción del error"
}
```

---

## Consideraciones de Desarrollo (Windows vs. Linux)

> **Sensibilidad a mayúsculas/minúsculas (*case-sensitivity*) en las plantillas Thymeleaf.**

El sistema de archivos de **Windows es *case-insensitive***, mientras que el de **Linux es *case-sensitive***. Durante el paso de desarrollo (Windows) a producción (Linux) esto puede provocar un error frecuente:

- En Windows, un controlador que devuelva `return "Base";` encuentra igualmente el archivo `base.html`.
- En Linux, esa misma referencia falla con un error del tipo `TemplateInputException` / `Error resolving template`, porque `Base` ≠ `base`.

Por eso se mantuvo esta convención en el proyecto:

1. El nombre que devuelve el controlador coincide **exactamente** (incluidas mayúsculas/minúsculas) con el nombre del archivo de plantilla.
2. Los archivos reales están todos en minúsculas:
   - `templates/color.html`
   - `templates/users.html`
   - `templates/base.html` → se llama `base.html`, no `Base.html`. Las referencias (`th:replace`, `th:insert`, layouts) usan `base`, no `Base`.
3. El mismo criterio se aplicó a los fragmentos, a las rutas de recursos estáticos (`static/`) y a los nombres de archivo en general.
