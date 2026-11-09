package co.julian.bitacora.data

import java.time.LocalDate

object Prompts {

    data class TipoExamen(val id: String, val nombre: String, val descripcion: String, val disponible: Boolean)

    fun tipos(hoy: LocalDate): List<TipoExamen> {
        val n = Logica.semanaPlan(hoy)
        val domingo = Logica.dow(hoy) == 0
        val etapaHoy = domingo && Logica.semanaN(hoy) in Plan.FIN_ETAPA
        val etapa = Logica.etapaDe(n)
        return listOf(
            TipoExamen("diario", "Diario", "5 preguntas · tema de la semana + inglés", true),
            TipoExamen(
                "practico", "Ejercicio práctico",
                (if (n <= 2) "Diagrama o pseudocódigo" else "Código sencillo") + " · Claude lo califica", true,
            ),
            TipoExamen("semanal", "Repaso semanal", if (domingo) "10 preguntas · toda la semana" else "Se abre el domingo", domingo),
            TipoExamen(
                "etapa", "Examen de etapa",
                if (etapaHoy) "20 preguntas · ${etapa.first}" else "Al cerrar cada etapa (semanas 3, 6, 12 y 16)", etapaHoy,
            ),
        )
    }

    fun nombre(id: String) = when (id) {
        "diario" -> "Diario"
        "practico" -> "Ejercicio práctico"
        "semanal" -> "Repaso semanal"
        "etapa" -> "Examen de etapa"
        else -> id
    }

    private const val PERFIL =
        "Julian es estudiante de séptimo semestre de Ingeniería de Sistemas en Bogotá, Colombia. Ya maneja Java, JavaFX, Node.js y Docker a nivel universitario y está retomando sus bases de lógica para prepararse a entrevistas de practicante o desarrollador junior."

    fun examen(tipo: String, e: Estado, hoy: LocalDate): String {
        val n = Logica.semanaPlan(hoy)
        val temas = Logica.temasHasta(n)
        val actual = temas.last()
        val previos = temas.dropLast(1)
        val fallos = Logica.fallosRecientes(e, hoy, if (tipo == "diario") 7 else 21)
        val leccion = Logica.leccion(hoy)
        val nota = Logica.entrada(e, hoy).nota
        val cantidad: Int
        var foco: String
        when (tipo) {
            "semanal" -> {
                cantidad = 10
                foco = "8 preguntas que cubran a fondo el tema de la semana ($actual) y 2 de inglés técnico."
            }
            "etapa" -> {
                val etapa = Logica.etapaDe(n)
                cantidad = 20
                foco = "16 preguntas que cubran toda la etapa '${etapa.first}' (temas: ${etapa.second.joinToString("; ")}) y 4 de inglés técnico, con dificultad de entrevista para practicante."
            }
            else -> {
                cantidad = 5
                val anterior = if (previos.isNotEmpty()) "un tema anterior (${previos.joinToString("; ")})" else "lógica básica de programación"
                foco = "3 preguntas sobre el tema de la semana ($actual), 1 sobre $anterior y 1 de inglés técnico para desarrolladores (vocabulario o comprensión de una frase corta en inglés)."
                if (leccion != null) {
                    foco += " El tema exacto de hoy es '${leccion.titulo}': ${leccion.pasos.joinToString(" ")} Enfoca las 3 preguntas del tema en esto."
                }
            }
        }
        if (n <= 3) {
            foco += " Nivel: está retomando bases, así que usa código muy sencillo (PSeInt o Java básico de máximo 8 líneas) y pide predecir qué imprime o encontrar el error."
        }
        val repaso = if (fallos.isNotEmpty()) {
            "\nIncluye 1 o 2 preguntas que repasen estos conceptos en los que se equivocó hace poco (con otra redacción): " +
                fallos.joinToString(" | ") { it.q }
        } else ""
        val notas = if (nota.isNotBlank()) "\nNotas recientes del estudiante sobre lo que le costó: ${nota.take(400)}" else ""
        return """Eres un profesor que prepara un examen corto. $PERFIL
Escribe exactamente $cantidad preguntas de opción múltiple en español: $foco$repaso$notas
Reglas: 4 opciones por pregunta, una sola correcta, distractores creíbles, preguntas prácticas (qué imprime este código, qué estructura usar, qué hace un comando) más que definiciones de memoria. Si la pregunta usa código, ponlo en "codigo" (máximo 12 líneas: Java, SQL, pseudocódigo o comandos de Git); si no, deja "codigo" vacío. La explicación debe tener 1 o 2 frases y enseñar el porqué. Varía la posición de la respuesta correcta.
Responde solo con JSON con esta forma: {"preguntas":[{"tema":"...","pregunta":"...","codigo":"","opciones":["...","...","...","..."],"correcta":0,"explicacion":"..."}]}"""
    }

    fun formatoPractico(hoy: LocalDate) = if (Logica.semanaPlan(hoy) <= 2) "diagrama" else "codigo"

    fun practico(hoy: LocalDate): String {
        val tema = Logica.temaSemana(hoy)
        val leccion = Logica.leccion(hoy)
        val formato = formatoPractico(hoy)
        val hoyTxt = leccion?.let { " Tema de hoy: ${it.titulo} (${it.pasos.joinToString(" ")})." } ?: ""
        val forma = if (formato == "diagrama") {
            "Debe poder resolverse con un diagrama de flujo o pseudocódigo de PSeInt de 5 a 15 pasos."
        } else {
            "Debe poder resolverse en Java con 10 a 25 líneas, sin librerías externas."
        }
        return """Eres profesor de programación. $PERFIL
Propón UN ejercicio corto. Tema de la semana: $tema.$hoyTxt $forma
Nada de trucos: un problema claro y cotidiano, con contexto colombiano si encaja (pesos, notas de 0 a 5, etc.). No repitas los ejercicios típicos de la lección; propón uno distinto del mismo tema.
Responde solo con JSON: {"enunciado":"...","ejemplo":"entrada y salida esperada","pista":"una pista corta"}"""
    }

    fun evaluar(p: Practico, respuesta: String, conFoto: Boolean): String {
        val escrito = respuesta.trim().ifBlank { "(no escribió texto; revisa la imagen del diagrama)" }
        val foto = if (conFoto) {
            "\nTambién adjuntó una foto de su diagrama de flujo: revisa los símbolos (inicio/fin, entrada/salida, proceso, decisión), el orden y que las flechas lleven a un fin."
        } else ""
        val ejemplo = if (p.ejemplo.isNotBlank()) "\nEjemplo: ${p.ejemplo}" else ""
        return """Eres un profesor exigente pero amable. Califica la solución de Julian, que está retomando bases de programación.
Ejercicio: ${p.enunciado}$ejemplo
Su solución escrita:
$escrito$foto
Revisa si resuelve el problema, si contempla casos límite y si es clara. No penalices detalles menores de sintaxis en pseudocódigo.
Responde solo con JSON: {"nota": un entero de 0 a 10, "bien":"1 o 2 frases", "mejorar":"1 a 3 frases concretas", "solucion":"una solución corta en el mismo formato que usó"}"""
    }

    const val PRUEBA = "Responde solo con la palabra: listo"
}
