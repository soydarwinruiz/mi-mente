## Mi Mente 1.0.1: arreglo de la conexión con Gemini

Google retiró `gemini-2.5-flash` para cuentas nuevas, y era el modelo por defecto de la app. Por eso la IA no respondía.

- El modelo por defecto pasa a ser **`gemini-flash-latest`**, el alias que Google mantiene siempre apuntando al Flash más reciente.
- **Guardar y probar** en Ajustes ahora hace una petición real a Gemini, no solo lista modelos. Si el modelo elegido no está disponible para tu clave, prueba los de tu cuenta y se queda con el primero que responde.
- Si un modelo deja de estar disponible más adelante, la app cambia sola a otro y te avisa.
- Mensajes de error más claros, que distinguen entre clave no válida, modelo no disponible y cuota agotada, e incluyen el texto exacto de Google.
- Al pulsar «Detener» ya no aparece un mensaje de error.

## Instalar
Descarga `MiMente-1.0.1.apk` (abajo, en *Assets*). Si tenías instalada la 1.0.0 que se envió por el chat, desinstálala primero: está firmada con otra clave. La 1.0.0 del Release de GitHub sí se actualiza directamente.

Después abre **Ajustes → Google AI Studio → Guardar y probar**.
