# ms-andesstay-reservations

Microservicio de reservas de AndesStay (puerto **8081**). Es el **productor** de mensajes RabbitMQ del sistema.

## Perfiles

| Perfil | Base de datos | Uso |
|---|---|---|
| `local` (por defecto) | H2 en archivo (`./data`) | Desarrollo en el PC |
| `cloud` | Base de datos en la nube (AWS RDS) | Ambiente cloud |
| `eduardo` | — | Tenant de Azure AD de Eduardo |

### Perfil `cloud` (AWS RDS)

La conexión se configura con variables de entorno, sin credenciales en el repositorio:

| Variable | Ejemplo |
|---|---|
| `DB_URL` | `jdbc:postgresql://<endpoint-rds>:5432/andesstay` |
| `DB_USERNAME` | usuario de la RDS |
| `DB_PASSWORD` | clave de la RDS |
| `DB_DDL_AUTO` | `update` (por defecto) |

PowerShell:

```powershell
$env:DB_URL="jdbc:postgresql://<endpoint-rds>:5432/andesstay"
$env:DB_USERNAME="usuario"
$env:DB_PASSWORD="clave"
$env:SPRING_PROFILES_ACTIVE="cloud"
.\mvnw.cmd spring-boot:run
```

Drivers incluidos: PostgreSQL y Oracle (`ojdbc17`). La entidad `Reservation` y `ReservationRepository` (Spring Data JPA) son los mismos en todos los perfiles.

## Mensajería (RabbitMQ)

- Nombres de exchanges, colas, routing keys y DLQ en `application.yaml` (`andesstay.messaging`) → `MessagingProperties`.
- Topología (beans `Queue`, `Exchange`, `Binding`, DLX y DLQ): `messaging/config/RabbitMQConfig`.
- Publicación después del commit: `ReservationCommandEventHandler` → `ReservationCommandPublisher`.
- Confirmaciones del broker: `RabbitPublisherCallbacksConfig`.

Detalle de la topología: repositorio `infra-andesstay-messaging`.

## Ejecutar y probar

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd test
```
