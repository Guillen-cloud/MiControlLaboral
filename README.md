# Mi Control Laboral

Proyecto Android nativo en Java para el control offline de jornadas, pagos e informes.

## Estado

Aplicación funcional inicial en Java con Room, MVVM, registro de jornadas, pagos,
informes, respaldo local, autenticación Firebase y respaldo remoto versionado mediante WorkManager.

La sincronización bidireccional con resolución de conflictos todavía está pendiente.

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
