package co.julian.bitacora.data

/** Contenido fijo del plan: horarios base, ruta de 16 semanas y lecciones. */
object Plan {

    private fun b(i: String, f: String, t: String, n: String, d: String = "") = Bloque(i, f, t, n, d)

    val TAREAS = listOf(
        Tarea("logica", "study", "Lógica, algoritmos o SQL", "Tema de la semana en la ruta"),
        Tarea("proyecto", "study", "Proyecto o backend", "Git, Spring Boot o portafolio"),
        Tarea("ingles", "eng", "Inglés", "Anki, escucha o hablar en voz alta"),
        Tarea("ejercicio", "gym", "Ejercicio", "Rutina en casa o caminar 30 min"),
        Tarea("dormir", "rest", "A dormir a tiempo", "Celular lejos de la cama 30 min antes"),
    )

    val TIPOS = listOf(
        "uni" to "Universidad",
        "study" to "Estudio",
        "eng" to "Inglés",
        "gym" to "Ejercicio",
        "work" to "Trabajo",
        "rest" to "Descanso u otro",
    )

    fun nombreTipo(t: String) = TIPOS.firstOrNull { it.first == t }?.second ?: t

    private val domingo = DiaBase(
        listOf("ejercicio", "proyecto", "ingles", "dormir"),
        listOf(
            b("9:00", "9:40", "gym", "Caminar o rutina", "Algo suave para mover el cuerpo"),
            b("10:00", "12:00", "study", "Proyecto", "Un avance concreto y un commit"),
            b("16:00", "16:45", "eng", "Inglés", "Serie o video técnico con subtítulos en inglés"),
            b("19:00", "19:30", "rest", "Planear la semana", "Haz el repaso semanal y elige 3 metas"),
        ),
    )

    private val sabado = DiaBase(
        listOf("ejercicio", "ingles", "logica", "dormir"),
        listOf(
            b("7:00", "7:40", "gym", "Ejercicio", "Rutina en casa o trotar"),
            b("", "", "work", "Trabajo (tranquilo)", "En los ratos libres: lectura y repaso"),
            b("", "", "eng", "Inglés en las pausas", "Vocabulario y escucha"),
            b("", "", "study", "Teoría en las pausas", "Pro Git, Big O o SQL desde el celular"),
        ),
    )

    /** Semestre con universidad (hasta el 20 de noviembre). Clave: 0 = domingo. */
    val F1: Map<Int, DiaBase> = mapOf(
        1 to DiaBase(
            listOf("logica", "ingles", "dormir"),
            listOf(
                b("7:05", "7:50", "rest", "Levantarse y alistarse", "Luz y agua apenas te levantes"),
                b("7:50", "8:50", "eng", "Camino a la U · inglés", "Solo audio: podcast en inglés por el intercomunicador"),
                b("8:50", "12:09", "uni", "Análisis y Verificación de Algoritmos", "Bloque G · 302"),
                b("12:10", "12:35", "rest", "Traslado al trabajo"),
                b("12:40", "13:40", "study", "Lógica cerca del trabajo", "Almuerza y haz la lección del día"),
                b("14:00", "21:00", "work", "Trabajo"),
                b("21:00", "22:00", "rest", "Camino a casa", "Escucha en inglés si vas cómodo"),
                b("23:00", "23:30", "rest", "Celular lejos", "Mañana te levantas a las 5:15: si puedes, duérmete a las 11:00"),
            ),
        ),
        2 to DiaBase(
            listOf("logica", "ingles", "dormir"),
            listOf(
                b("5:15", "6:00", "rest", "Levantarse y alistarse", "Luz y agua apenas te levantes"),
                b("6:00", "7:00", "eng", "Camino a la U · inglés", "Solo audio: podcast en inglés por el intercomunicador"),
                b("7:00", "10:19", "uni", "Telecomunicaciones", "Bloque K · 006"),
                b("10:30", "12:30", "study", "Bloque fuerte de lógica", "En la biblioteca de la U: tu mejor hueco del semestre"),
                b("12:30", "13:30", "rest", "Almuerzo"),
                b("13:35", "14:00", "rest", "Traslado al trabajo"),
                b("14:00", "21:00", "work", "Trabajo"),
                b("21:00", "22:00", "eng", "Camino a casa · inglés", "Solo audio: repite el episodio de la ida"),
                b("23:00", "23:30", "rest", "Celular lejos", "Mañana te levantas a las 5:15: si puedes, duérmete a las 11:00"),
            ),
        ),
        3 to DiaBase(
            listOf("ingles", "dormir"),
            listOf(
                b("5:15", "6:00", "rest", "Levantarse y alistarse"),
                b("6:00", "7:00", "eng", "Camino a la U · inglés", "Solo escucha, sin presión"),
                b("7:00", "8:29", "uni", "Práctica Aplicada a Sistemas", "Bloque G · 302"),
                b("8:50", "10:19", "uni", "Minería de Datos", "Bloque K · 003"),
                b("10:40", "13:59", "uni", "Ingeniería de Software 2", "City Campus · 409"),
                b("14:00", "14:25", "rest", "Traslado al trabajo", "Llegas a las 2:25"),
                b("14:25", "21:00", "work", "Trabajo", "Día pesado: no te exijas más"),
                b("21:00", "22:00", "rest", "Camino a casa", "Descansa"),
                b("23:00", "23:30", "rest", "Celular lejos", "Mañana tienes la mañana libre"),
            ),
        ),
        4 to DiaBase(
            listOf("ejercicio", "logica", "proyecto", "ingles", "dormir"),
            listOf(
                b("6:45", "7:00", "rest", "Levantarse", "Luz y agua"),
                b("7:00", "7:40", "gym", "Ejercicio", "Rutina en casa: 3 rondas"),
                b("8:30", "10:30", "study", "Lógica y estructuras", "Tu mañana libre: el bloque más largo"),
                b("10:45", "11:45", "study", "Proyecto o Git", "Avanza en algo que puedas mostrar"),
                b("11:45", "12:15", "eng", "Inglés", "Hablar en voz alta y grabarte"),
                b("12:15", "13:00", "rest", "Almuerzo"),
                b("13:00", "14:00", "rest", "Camino al trabajo", "Escucha en inglés"),
                b("14:00", "21:00", "work", "Trabajo"),
                b("21:00", "22:00", "rest", "Camino a casa"),
                b("23:00", "23:30", "rest", "Celular lejos", "Mañana te levantas a las 5:15: si puedes, duérmete a las 11:00"),
            ),
        ),
        5 to DiaBase(
            listOf("logica", "ingles", "dormir"),
            listOf(
                b("5:15", "6:00", "rest", "Levantarse y alistarse"),
                b("6:00", "7:00", "eng", "Camino a la U · inglés", "Solo audio: podcast en inglés"),
                b("7:00", "8:29", "uni", "Minería de Datos", "City Campus · 408"),
                b("8:40", "10:30", "study", "Estudio en el campus", "Aprovecha el hueco entre clases"),
                b("10:40", "12:09", "uni", "Práctica Aplicada a Sistemas", "City Campus · 512"),
                b("12:10", "12:35", "rest", "Traslado al trabajo"),
                b("12:40", "13:40", "eng", "Almuerzo e inglés", "Anki o un video corto"),
                b("14:00", "21:00", "work", "Trabajo"),
                b("21:00", "22:00", "rest", "Camino a casa"),
                b("23:00", "23:30", "rest", "Celular lejos"),
            ),
        ),
        6 to sabado,
        0 to domingo,
    )

    private val vacacionesFuerte = DiaBase(
        listOf("ejercicio", "logica", "proyecto", "ingles", "dormir"),
        listOf(
            b("6:45", "7:00", "rest", "Levantarse", "Luz y agua"),
            b("7:00", "7:40", "gym", "Ejercicio", "Rutina en casa: 3 rondas"),
            b("8:00", "9:50", "study", "Lógica, algoritmos o SQL", "Tema de la semana"),
            b("10:00", "11:00", "study", "Proyecto o backend", "Construir y hacer commit"),
            b("11:00", "11:45", "eng", "Inglés", "Anki y hablar en voz alta"),
            b("12:00", "13:00", "rest", "Almuerzo"),
            b("13:00", "14:00", "rest", "Camino al trabajo", "Escucha en inglés"),
            b("14:00", "21:00", "work", "Trabajo"),
            b("21:00", "22:00", "rest", "Camino a casa"),
        ),
    )

    private val vacacionesSuave = DiaBase(
        listOf("logica", "proyecto", "ingles", "dormir"),
        listOf(
            b("7:00", "7:30", "rest", "Caminar opcional"),
            b("8:00", "9:50", "study", "Lógica, algoritmos o SQL", "Tema de la semana"),
            b("10:00", "11:00", "study", "Proyecto o backend"),
            b("11:00", "11:45", "eng", "Inglés", "Escucha y vocabulario"),
            b("12:00", "13:00", "rest", "Almuerzo"),
            b("13:00", "14:00", "rest", "Camino al trabajo", "Escucha en inglés"),
            b("14:00", "21:00", "work", "Trabajo"),
            b("21:00", "22:00", "rest", "Camino a casa"),
        ),
    )

    /** Vacaciones (desde el 21 de noviembre). */
    val F2: Map<Int, DiaBase> = mapOf(
        1 to vacacionesFuerte, 3 to vacacionesFuerte, 5 to vacacionesFuerte,
        2 to vacacionesSuave, 4 to vacacionesSuave,
        6 to sabado, 0 to domingo,
    )

    /** Días con un horario especial ya conocido. */
    val DIAS_BASE: Map<String, List<Bloque>> = mapOf(
        "2026-11-07" to listOf(
            b("6:10", "7:00", "rest", "Levantarse y alistarse", "Luz y agua apenas te levantes"),
            b("7:00", "8:00", "eng", "Camino al trabajo · inglés", "Solo audio: podcast en inglés"),
            b("8:00", "15:00", "work", "Trabajo (turno de mañana)"),
            b("15:00", "16:00", "eng", "Camino a casa · inglés", "Solo audio: podcast en inglés"),
            b("16:30", "17:10", "gym", "Ejercicio", "Rutina en casa o caminata"),
            b("17:30", "19:00", "study", "Prepara tus herramientas", "PSeInt, HackerRank, Anki y la prueba EF SET"),
            b("23:00", "23:30", "rest", "Celular lejos", "Dormido antes de las 11:30"),
        ),
    )

    val PLAN: List<Pair<String, List<String>>> = listOf(
        "Bases de lógica" to listOf(
            "Diagramas de flujo y pseudocódigo en PSeInt: secuencia y condicionales",
            "Ciclos, arreglos y funciones en PSeInt, luego pasarlos a Java",
            "Problemas de lógica en HackerRank y Git a fondo (ramas, merge, rebase)",
        ),
        "Estructuras de datos y SQL" to listOf(
            "Listas, pilas, colas y mapas en Java",
            "Recursión, ordenamiento, búsqueda y complejidad (Big O)",
            "SQL a fondo (JOIN, GROUP BY, subconsultas) y repaso de POO y SOLID",
        ),
        "Backend de tu Bitácora" to listOf(
            "Spring Boot: crea la API de la Bitácora con el registro diario (versión mínima)",
            "JPA con PostgreSQL: registros, rutina y exámenes, con validaciones y manejo de errores",
            "Pruebas con JUnit y Mockito para la API de la Bitácora",
            "Usuarios y autenticación con JWT",
            "Docker y un pipeline con GitHub Actions para la Bitácora",
            "Desplegar la API en la nube y conectar los exámenes con IA",
        ),
        "Portafolio y aplicaciones" to listOf(
            "Mini proyecto bancario: transferencias con @Transactional, validación de saldo, concurrencia y pruebas",
            "Pulir la Bitácora: Swagger, README en inglés, diagrama de arquitectura y demo",
            "CV de una página, LinkedIn y entrevistas técnicas de práctica",
            "Aplicar a vacantes y simulacros de entrevista en inglés",
        ),
    )

    val TEMAS: List<String> = PLAN.flatMap { it.second }

    /** Semanas en las que se cierra una etapa (examen de etapa el domingo). */
    val FIN_ETAPA = listOf(3, 6, 12, 16)

    private fun l(t: String, p: List<String>, i: String, m: String) = Leccion(t, p, i, m)

    val LECCIONES: Map<String, Leccion> = mapOf(
        "2026-11-07" to l("Prepara tus herramientas", listOf(
            "Descarga PSeInt para Linux desde su página oficial y abre el editor de diagramas de flujo.",
            "Crea tu cuenta en HackerRank.",
            "Instala Anki en el celular y crea un mazo llamado 'Inglés técnico'.",
            "Haz la prueba gratis EF SET (unos 50 minutos) y anota tu nivel en la nota de hoy.",
        ), "La prueba EF SET es tu punto de partida.", "No estudies nada todavía: hoy solo dejas todo listo."),
        "2026-11-08" to l("Tu repositorio de práctica", listOf(
            "Crea en GitHub un repositorio llamado ruta-marzo con carpetas semana-01, semana-02 y semana-03.",
            "Clónalo en tu computador y haz un primer commit con un README que diga tu meta para marzo.",
            "Lee qué es un algoritmo: una lista finita de pasos ordenados que resuelve un problema.",
            "En papel, escribe el algoritmo para preparar un café en máximo 8 pasos.",
        ), "Agrega a Anki: algorithm, step, input, output, repository.", "Fíjate en que cada paso sea concreto. 'Hacer el café' no es un paso."),
        "2026-11-09" to l("Entrada, proceso y salida", listOf(
            "Aprende los 4 símbolos básicos del diagrama de flujo: inicio/fin (óvalo), proceso (rectángulo), entrada/salida (paralelogramo) y decisión (rombo).",
            "En PSeInt: pide el nombre del usuario y muestra 'Hola, <nombre>'.",
            "En PSeInt: pide dos números y muestra su suma.",
            "Guarda los dos archivos .psc en semana-01 y haz commit.",
        ), "Anki: variable, value, read, write, print.", "Antes de abrir PSeInt, escribe los pasos en papel. Ese hábito es la base de todo."),
        "2026-11-10" to l("Variables y operaciones", listOf(
            "Repasa tipos de datos: entero, real, caracter y lógico.",
            "Ejercicio: área de un rectángulo (base por altura).",
            "Ejercicio: convertir grados Celsius a Fahrenheit (F = C × 9/5 + 32).",
            "Ejercicio: promedio de 3 notas.",
            "Haz prueba de escritorio a uno: una tabla con el valor de cada variable en cada paso.",
        ), "Anki: integer, float, string, boolean, operator.", "Revisa la precedencia: (a+b)/2 no es lo mismo que a+b/2."),
        "2026-11-11" to l("Día ligero", listOf(
            "20 minutos de Anki.",
            "Lee qué es una estructura condicional (Si-Entonces-SiNo). Mañana la practicas.",
        ), "Escucha un video corto en inglés sobre 'if statements'.", "Hoy no te exijas más: lo importante es no romper la racha."),
        "2026-11-12" to l("Condicionales", listOf(
            "Operadores relacionales (>, <, =, <>) y lógicos (Y, O, NO).",
            "Ejercicio: decir si un número es par o impar (usa MOD).",
            "Ejercicio: el mayor de tres números.",
            "Ejercicio: con una nota de 0 a 5, decir si aprobó (3,0 o más) o reprobó.",
            "Ejercicio: saber si un año es bisiesto.",
            "Pasa 'par o impar' a Java y súbelo.",
        ), "Anki: if, else, condition, true, false, greater than.", "Prueba tus condicionales con casos límite: la nota 3,0 exacta, el número 0 y años como 1900 y 2000."),
        "2026-11-13" to l("Selección múltiple", listOf(
            "Aprende la estructura Segun (switch en Java).",
            "Ejercicio: pedir un número del 1 al 7 y mostrar el día de la semana.",
            "Ejercicio: calculadora con menú (1 sumar, 2 restar, 3 multiplicar, 4 dividir) que avise si intentas dividir entre 0.",
        ), "Anki: switch, case, default, menu, divide by zero.", "Piensa siempre en la entrada inválida: ¿qué pasa si el usuario escribe 9?"),
        "2026-11-14" to l("Teoría en las pausas", listOf(
            "Lee el capítulo 1 de Pro Git en español (qué es el control de versiones).",
            "Repasa en el celular las tablas de verdad de Y, O y NO.",
        ), "Escucha 15 minutos de un podcast o video técnico en inglés.", "Aprovecha los ratos muertos, pero sin culpa si el trabajo no te deja."),
        "2026-11-15" to l("Cierre de la semana 1", listOf(
            "Pasa 5 ejercicios de la semana a Java.",
            "Actualiza el README con lo que aprendiste.",
            "Haz el repaso semanal en la pestaña Examen.",
            "Mira tus fallos de la semana y elige 1 tema para reforzar.",
        ), "Practica en voz alta: 'This week I learned conditionals and flowcharts'.", "Revisa qué tipo de ejercicio te costó más y escríbelo en la nota."),
        "2026-11-16" to l("Ciclo Mientras", listOf(
            "Aprende qué es un ciclo y la diferencia entre contador y acumulador.",
            "Ejercicio: mostrar los números del 1 al 10.",
            "Ejercicio: sumar números hasta que el usuario escriba 0.",
            "Haz la prueba de escritorio del segundo ejercicio con 3 números.",
        ), "Anki: loop, while, counter, accumulator, until.", "El error más común es el ciclo infinito: confirma que la condición pueda volverse falsa."),
        "2026-11-17" to l("Ciclo Para", listOf(
            "Aprende la estructura Para (for en Java).",
            "Ejercicio: tabla de multiplicar de un número.",
            "Ejercicio: factorial de un número.",
            "Ejercicio: suma de los pares del 1 al 100.",
            "Pasa el factorial a Java.",
        ), "Anki: for, range, iterate, factorial, even, odd.", "Antes de programar, decide desde dónde y hasta dónde va el ciclo."),
        "2026-11-18" to l("Día ligero", listOf(
            "20 minutos de Anki.",
            "Lee la diferencia entre Mientras y Repetir (while y do-while).",
        ), "Video corto en inglés sobre 'loops'.", "Descansa: mañana tienes la mañana libre."),
        "2026-11-19" to l("Repetir y problemas clásicos", listOf(
            "Ejercicio con Repetir: menú que se muestra hasta que el usuario elija 'Salir'.",
            "Ejercicio: decir si un número es primo.",
            "Ejercicio: adivinar un número aleatorio con pistas de 'mayor' o 'menor'.",
            "Pasa 'número primo' a Java.",
        ), "Anki: random, guess, prime number, break.", "En el primo, piensa cuándo puedes dejar de buscar divisores."),
        "2026-11-20" to l("Arreglos", listOf(
            "Aprende qué es un arreglo (vector) y cómo recorrerlo con Para.",
            "Ejercicio: guardar 5 notas y mostrar el promedio, la mayor y la menor.",
            "Ejercicio: contar cuántos números de un arreglo son pares.",
            "Último día de clases: celébralo descansando bien esta noche.",
        ), "Anki: array, index, length, element.", "Los índices en Java empiezan en 0. En PSeInt depende de la configuración."),
        "2026-11-21" to l("Teoría: subprocesos", listOf(
            "En las pausas del trabajo, lee qué es una función o subproceso y por qué sirve para no repetir código.",
            "Piensa qué ejercicios de esta semana podrías convertir en funciones.",
        ), "Escucha 15 minutos de inglés técnico.", "Empiezan tus vacaciones: desde el lunes cambia la agenda."),
        "2026-11-22" to l("Funciones", listOf(
            "Ejercicio: subproceso esPrimo(n) que devuelva verdadero o falso.",
            "Ejercicio: subproceso factorial(n).",
            "Pásalos a Java como métodos static y llámalos desde main.",
            "Haz el repaso semanal y sube todo a GitHub.",
        ), "Practica en voz alta: 'I wrote a function that checks if a number is prime'.", "Una función hace una sola cosa. Si hace dos, pártela."),
        "2026-11-23" to l("HackerRank y ramas", listOf(
            "HackerRank: 'Solve Me First', 'Simple Array Sum' y 'Compare the Triplets'.",
            "Git: crea una rama ejercicios-semana3, trabaja en ella y haz merge a main.",
            "Antes de enviar cada solución, pruébala con un caso que se te ocurra.",
        ), "Lee el enunciado en inglés de HackerRank sin traductor y anota las palabras nuevas en Anki.", "Lee el enunciado dos veces y escribe la entrada y la salida esperada antes de programar."),
        "2026-11-24" to l("Arreglos en HackerRank", listOf(
            "HackerRank: 'A Very Big Sum', 'Diagonal Difference' y 'Plus Minus'.",
            "Git: provoca un conflicto a propósito (cambia la misma línea en dos ramas) y resuélvelo.",
        ), "Anki: merge, conflict, branch, overflow, matrix.", "'A Very Big Sum' te enseña por qué existe long en Java."),
        "2026-11-25" to l("Más práctica", listOf(
            "HackerRank: 'Staircase', 'Mini-Max Sum' y 'Birthday Cake Candles'.",
            "Learn Git Branching: completa los niveles de la sección de Introducción.",
        ), "Graba un audio de 1 minuto explicando en inglés cómo resolviste 'Staircase'.", "Si te bloqueas más de 20 minutos, mira una pista, entiéndela y vuelve a escribir la solución sin mirar."),
        "2026-11-26" to l("Matrices y texto", listOf(
            "HackerRank: 'Time Conversion'.",
            "Ejercicio: llenar una matriz de 3x3 y mostrar la suma de cada fila.",
            "Learn Git Branching: sección 'Ramp up'.",
        ), "Anki: string, substring, row, column, format.", "Practica dibujar la matriz en papel con índices antes de recorrerla."),
        "2026-11-27" to l("Cadenas y repositorios remotos", listOf(
            "Ejercicio: contar vocales de una frase.",
            "Ejercicio: decir si una palabra es palíndromo.",
            "Ejercicio: invertir una cadena.",
            "Git: practica push, pull y fetch, y abre un pull request en tu propio repositorio.",
        ), "Escribe en inglés la descripción de tu pull request.", "Compara tu solución del palíndromo con otra que no use una cadena invertida."),
        "2026-11-28" to l("Repaso en las pausas", listOf(
            "Relee tus notas de las 3 semanas.",
            "Haz una lista de los 3 temas que más te costaron.",
        ), "Escucha 15 minutos de inglés técnico.", "Ser honesto con lo que no dominas te ahorra tiempo después."),
        "2026-11-29" to l("Examen de etapa", listOf(
            "Haz el examen de etapa en la pestaña Examen.",
            "Repasa cada pregunta que fallaste.",
            "Deja el README de ruta-marzo con la lista de ejercicios resueltos.",
            "Mira el tema de la semana 4: listas, pilas, colas y mapas.",
        ), "Practica: 'In the last three weeks I practiced logic, loops, arrays and Git'.", "Si sacaste menos de 60 %, repite 2 días de práctica antes de avanzar."),
    )

    /** Alarmas con las que arranca la app. */
    val ALARMAS_INICIALES = listOf(
        Alarma(1, 5, 15, listOf(2, 3, 5), "Clase a las 7:00 · sal a las 6:00", "f1"),
        Alarma(2, 7, 5, listOf(1), "Algoritmos a las 8:50 · sal a las 7:50", "f1"),
        Alarma(3, 6, 45, listOf(4), "Ejercicio a las 7:00 · mañana libre", "f1"),
        Alarma(4, 6, 45, listOf(1, 2, 3, 4, 5, 6), "Buenos días · ejercicio a las 7:00", "f2"),
    )
}
