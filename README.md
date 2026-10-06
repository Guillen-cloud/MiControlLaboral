# Mi Control Laboral

Aplicación Android nativa para registrar jornadas laborales, controlar cobros y consultar saldos de Santillana y del trabajo en el campo.

## Estado del proyecto

**Versión:** desarrollo inicial
**Lenguaje:** Java
**Persistencia principal:** Room sobre SQLite
**Sincronización:** Firebase + WorkManager, en desarrollo
**Plan Firebase:** Spark
**Zona horaria:** `America/La_Paz`
**Android mínimo:** API 26

### Funcionalidades disponibles

- Empleos Santillana y Campo.
- Tarifas configurables y valores históricos en centavos.
- Jornadas completas, medias y personalizadas.
- Prevención de duplicados por empleo y fecha.
- Edición e historial de jornadas.
- Pagos parciales y agrupados.
- Asignaciones explícitas a jornadas.
- Totales generado, cobrado y pendiente.
- Informes semanales, mensuales y personalizados.
- Subtotales separados por empleo.
- Exportación y restauración local en JSON.
- Registro e inicio de sesión con Firebase Authentication.
- Respaldo remoto versionado mediante WorkManager.

## Arquitectura

```text
presentation -> ViewModel -> Repository -> Room
									  \-> WorkManager -> Firestore
```

Room es la fuente principal de datos. La aplicación debe seguir funcionando sin conexión para registrar y consultar información local.

## Tecnologías

- Android nativo.
- Java y XML.
- MVVM.
- Repository Pattern.
- Room / SQLite.
- LiveData.
- WorkManager.
- Firebase Authentication.
- Cloud Firestore.
- JUnit.

## Estructura principal

```text
app/src/main/java/com/micontrollaboral/
├── database/       Entidades, DAOs, migraciones y base Room
├── domain/         Modelos de informes y reglas de dominio
├── presentation/   Activities, ViewModels y flujo de interfaz
├── repository/     Acceso y operaciones de datos
├── sync/           Worker y programación de sincronización
└── utils/          Utilidades monetarias
```

## Configuración local

Requisitos:

- JDK 21.
- Android SDK Platform 36.
- Android Build Tools compatibles.
- Git Bash o una terminal equivalente.

Configura `local.properties` con la ruta del SDK. Este archivo no se versiona.

Coloca el archivo descargado desde Firebase en:

```text
app/google-services.json
```

## Compilar y probar

En Git Bash:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew test --no-daemon --console=plain
./gradlew assembleDebug --no-daemon
```

El APK de depuración se genera en:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Firebase

El proyecto Firebase **ya fue creado** y la aplicación Android ya está registrada.
Cloud Firestore también **ya fue creado** con la base `(default)`.

No necesitas crear colecciones manualmente. La aplicación las generará al sincronizar datos autenticados.

Configuración necesaria en Firebase Console:

1. Authentication con proveedor correo electrónico/contraseña.
2. Cloud Firestore en modo producción.
3. Reglas publicadas desde `firestore.rules`.
4. Plan Spark sin activar facturación.

Las reglas deben restringir los datos al usuario autenticado bajo `users/{uid}/...`.

## Sincronización actual

Cada usuario utiliza una base local Room independiente:

```text
mi_control_laboral_<uid>.db
```

Las modificaciones locales generan estados `PENDING`. WorkManager intenta subirlas cuando hay conectividad y las marca como `SYNCED` o `FAILED`.

Los respaldos remotos versionados se almacenan bajo:

```text
users/{uid}/backups/{timestamp}/
```

Room no se elimina ni se sobrescribe si Firebase falla. La descarga bidireccional y la resolución visual de conflictos siguen pendientes.

## Roadmap

- [x] Núcleo local Room y MVVM.
- [x] Jornadas, pagos e informes.
- [x] Respaldo local JSON.
- [x] Authentication Firebase.
- [x] Estados locales de sincronización.
- [ ] Descargar cambios desde Firestore.
- [ ] Resolver conflictos sin sobrescritura.
- [ ] Pruebas en dispositivo físico o emulador.
- [ ] Validación final de Gradle y generación del APK.

## Seguridad y privacidad

- No se incluyen credenciales administrativas.
- `local.properties` está excluido del repositorio.
- Los datos remotos se organizan por `uid`.
- Room conserva los datos locales ante errores de red.
- No se requieren permisos de ubicación.
