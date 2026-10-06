# Mi Control Laboral

Proyecto Android nativo en Java para el control offline de jornadas, pagos e informes.

## Estado

Aplicación funcional inicial en Java con Room, MVVM, registro de jornadas, pagos,
informes, respaldo local, autenticación Firebase y respaldo remoto versionado mediante WorkManager.

Room se aísla por usuario mediante una base local `mi_control_laboral_<uid>.db`.
Las escrituras locales generan registros `PENDING`; WorkManager los intenta enviar
cuando existe conectividad y los marca `SYNCED` o `FAILED` según el resultado.

La descarga bidireccional y la resolución de conflictos todavía están pendientes.

## Abrir y validar

1. Configurar `local.properties` con la ruta local del Android SDK.
2. Colocar `google-services.json` dentro de `app/`.
3. Usar JDK 21 y ejecutar `./gradlew test` desde Git Bash.
4. Instalar Android SDK Platform 36 y Build Tools compatibles.
5. Ejecutar `./gradlew assembleDebug` para generar el APK.
6. Probar `app-debug.apk` en un emulador o dispositivo con Android 8.0 (API 26) o superior.

No se incluyen datos de demostración. Room sigue siendo la fuente principal local.

## Firebase

El proyecto Firebase debe permanecer en el plan Spark. Authentication usa correo y contraseña.
Firestore usa rutas privadas bajo `users/{uid}`. Las reglas locales están en `firestore.rules`.

El archivo `local.properties` no se versiona. `google-services.json` es la configuración cliente
de Firebase y no contiene credenciales administrativas.

## Sincronización actual

El Worker guarda una copia remota versionada bajo:

```text
users/{uid}/backups/{timestamp}/
```

Si Firebase falla, los datos de Room permanecen intactos y WorkManager reintenta
con backoff exponencial. La sincronización remota todavía es de subida; no se
descargan cambios ni se resuelven conflictos automáticamente.

## Estado de validación

Los diagnósticos del editor no muestran errores en el código actual. La ejecución
de `./gradlew test` debe confirmarse en un entorno donde la terminal devuelva el
resultado final de Gradle. La prueba en un dispositivo físico o emulador aún está pendiente.
