package com.adaptionsoft.games.uglytrivia;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PRUEBA DE CARACTERIZACION (Golden Master / Approval Test).
 *
 * Objetivo: NO verifica que el comportamiento sea "correcto" segun una
 * especificacion (no existe ninguna), sino que documenta y CONGELA el
 * comportamiento observable actual del sistema (todo lo que imprime por
 * consola) para poder refactorizar con confianza.
 *
 * Como se construyo:
 *  1. Se definio un guion FIJO y determinista de tiradas/respuestas
 *     (SCRIPT) que ejercita, con 3 jugadores, las ramas principales del
 *     codigo original: movimiento normal, vuelta al tablero (place > 11),
 *     las 4 categorias, entrada/permanencia/salida de la caja de
 *     penalizacion, y varias rondas de acumulacion de monedas.
 *  2. Se ejecuto ese guion contra el codigo TAL CUAL estaba (sin tocar
 *     nada) y se capturo la salida exacta -> ese es el archivo
 *     "golden-master.approved.txt" (el "maestro aprobado").
 *  3. A partir de aqui, cualquier refactor que cambie una sola letra de
 *     esa salida hace fallar este test, obligando a decidir de forma
 *     consciente: "es un efecto secundario no deseado (revertir)" o
 *     "es un arreglo intencional (actualizar el maestro aprobado y
 *     documentarlo)".
 *
 * IMPORTANTE: el archivo golden-master.approved.txt de este repositorio
 * ya corresponde a la version REFACTORIZADA, en la que se corrigio a
 * proposito el typo "corrent" -> "correct" (ver informe/GUIA de
 * refactoring, paso "Arreglo de bug descubierto por duplicacion"). Si se
 * ejecuta este mismo test contra el Game.java ORIGINAL (sin refactorizar),
 * fallara en exactamente 15 lineas, todas con el texto
 * "Answer was corrent!!!!" vs "Answer was correct!!!!": esa es
 * precisamente la evidencia que se muestra en el video.
 */
public class GameCharacterizationTest {

    // roll, thenCorrect (true = respuesta correcta, false = respuesta incorrecta)
    private static final int[][] SCRIPT = {
        {5, 1}, {3, 1}, {4, 1},
        {2, 0}, {6, 1}, {1, 1},
        {2, 1}, {3, 1}, {5, 1},
        {1, 1}, {2, 1}, {4, 1},
        {3, 1}, {6, 1}, {2, 1},
        {5, 1}, {1, 1}, {4, 1},
        {3, 1}, {6, 1}, {2, 1},
    };

    @Test
    void theFullScriptedGameMatchesTheApprovedGoldenMaster() throws Exception {
        String actual = runScriptedGameAndCaptureOutput();
        String approved = readResource("/golden-master.approved.txt");

        assertEquals(approved, actual,
            "La salida por consola cambio respecto al golden master aprobado. "
          + "Si el cambio es intencional, revisa y actualiza "
          + "src/test/resources/golden-master.approved.txt de forma consciente.");
    }

    private String runScriptedGameAndCaptureOutput() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream captured = new PrintStream(buffer, true, "UTF-8");
        PrintStream realOut = System.out;
        System.setOut(captured);
        try {
            Game game = new Game();
            game.add("Chet");
            game.add("Pat");
            game.add("Sue");

            for (int[] step : SCRIPT) {
                int roll = step[0];
                boolean correct = step[1] == 1;
                game.roll(roll);
                if (correct) {
                    game.wasCorrectlyAnswered();
                } else {
                    game.wrongAnswer();
                }
            }
        } finally {
            System.setOut(realOut);
        }
        return buffer.toString("UTF-8");
    }

    private String readResource(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("No se encontro el recurso " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
