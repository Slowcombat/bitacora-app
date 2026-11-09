# Bitácora

App de Android para seguir mi plan de estudio hasta marzo de 2027: agenda del día, horario editable, alarmas para despertar, recordatorios de cada bloque, registro de sueño y mini exámenes generados con la API de Anthropic.

## Cómo se compila

Cada vez que se sube un cambio a `main`, GitHub Actions compila el APK firmado y lo publica en la rama `builds` (`bitacora.apk`), junto con el registro de la compilación (`build.log`).

## Tecnologías

- Kotlin y Jetpack Compose (Material 3)
- AlarmManager para alarmas y recordatorios exactos
- kotlinx.serialization para guardar los datos en un archivo JSON local
- API de Messages de Anthropic para los exámenes y la calificación de ejercicios
