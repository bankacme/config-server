# config-server

Servidor de configuración del sistema bancario (paso 0.2, completado en P2 paso 2.7).
Ficha: `bank-docs/services/config-server.md`.

Entrega a cada servicio su archivo de `bank-config` (`<servicio>.yml`) combinado con `application.yml`
y, si el servicio corre con un perfil, con `application-<perfil>.yml` y `<servicio>-<perfil>.yml`.

## Origen de la configuración

| Dónde corre | `CONFIG_GIT_URI` | Qué lee |
|---|---|---|
| IDE, sin variable | `https://github.com/bankacme/bank-config` (valor por defecto) | Lo que esté subido en `main` |
| `docker-compose` de `bank-platform` | sin variable, el mismo valor por defecto | Lo mismo: lo subido en `main` |

En los dos casos es Git: un cambio en `bank-config` sin commit no se ve. Para probarlo hay que hacer
commit (y push, si se usa GitHub) y reiniciar el Config Server y el servicio.

## Endpoints útiles

- `GET http://localhost:8888/customer-service/default`: lo que recibe `customer-service` desde el IDE.
- `GET http://localhost:8888/account-service/docker`: lo que recibe dentro del `docker-compose`.
- `GET http://localhost:8888/actuator/health`: salud (la usa el `docker-compose`).

## Pruebas

| Clase | Qué comprueba |
|---|---|
| `ConfigServerApplicationTests` | Arranca en modo `native` sobre `../bank-config` (sin red) y revisa que un servicio recibe su puerto y lo común, y que el perfil `docker` cambia Mongo y Eureka a los nombres de la red |
| `BankConfigRepositoryTest` | Cada servicio tiene su archivo con el puerto de la ficha, no hay puertos repetidos, las URI de Mongo usan `${bank.mongo.host:localhost}` y no hay secretos (`password`, `token`, `private-key`) |

Las dos leen la carpeta hermana `../bank-config`, así que necesitan el mismo orden de carpetas que el
resto del proyecto.

## Comandos

- Pruebas, Checkstyle y Jacoco: `./mvnw verify` (reporte en `target/site/jacoco/index.html`).
- Arrancar desde el IDE: `./mvnw spring-boot:run` (puerto 8888). Es el primero en arrancar.
- Imagen: `docker build -t bankacme/config-server .` (normalmente lo hace `bank-platform`).
