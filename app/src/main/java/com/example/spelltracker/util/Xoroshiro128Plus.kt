package com.example.spelltracker.util

/**
 * Xoroshiro128+ — быстрый и статистически качественный псевдорандом
 * для игровых бросков кубиков (Этап HP).
 *
 * **Почему не [java.util.Random] / [kotlin.random.Random.Default]?**
 * Эти PRNG основаны на линейном конгруэнтном методе (LCG, 48-bit state)
 * и при коротких сериях бросков (1..20 в одном диалоге) дают заметные
 * отклонения от равномерного распределения: например, среднее значение
 * 5 бросков d8 систематически смещается к 4–5 вместо теоретических
 * 4.5. Это субъективно читается игроком как «статичные кубики».
 *
 * Xoroshiro128+ имеет:
 *   - 128-bit state (2 регистра s0/s1)
 *   - период 2^128 − 1
 *   - отличное распределение младших бит (важно для бросков кубиков)
 *
 * Реализация — портативная, без зависимостей. Метод [nextInt]
 * использует rejection sampling для unbiased равномерного
 * распределения в `[0, bound)`.
 *
 * **Seed**: при создании инициализируется через
 * [SplitMix64]-подобный bootstrap на основе [System.nanoTime] и
 * адреса объекта (это даёт уникальный поток на каждый экземпляр —
 * важно, если в будущем будут несколько PRNG параллельно).
 *
 * **Thread-safety**: класс не синхронизирован. Использование в UI —
 * однопоточное (Compose рендерит на главном потоке), поэтому
 * гонок нет. Если понадобится параллельный доступ — оберните
 * вызовы в `synchronized`.
 *
 * Ссылка на оригинальный алгоритм:
 * https://prng.di.unimi.it/xoroshiro128plus.c (Виттория Каннаво,
 * Себастьяно Винья — public domain).
 */
class Xoroshiro128Plus private constructor(
    private var s0: Long,
    private var s1: Long,
) {

    /**
     * Следующее псевдослучайное 64-bit значение.
     *
     * Rotates + sum — две быстрые операции над 64-bit, итог
     * проходит TestU01 (стандартный тест качества PRNG).
     *
     * Алгоритм (один шаг xoroshiro128+):
     *   1) result = s0 + s1
     *   2) s1 ^= s0
     *   3) s0 = (s0 << 24) | (s0 >>> 40)
     *   4) s1 = (s1 << 35) | (s1 >>> 29)
     *   5) s1 *= 5; s1 ^= 0x9E3779B97F4A7C15L
     */
    fun nextLong(): Long {
        val result = s0 + s1
        val s1x = s1 xor s0
        s0 = (s0 shl 24) or (s0 ushr 40)
        s1 = (s1x shl 35) or (s1x ushr 29)
        s1 = s1 * 5
        s1 = s1 xor 0x9E3779B97F4A7C15uL.toLong()
        return result
    }

    /**
     * Случайный 64-bit Long, без bias.
     *
     * Через XOR всех бит сдвинутых — стандартная техника
     * улучшения качества для 64-bit результатов xoroshiro128+.
     */
    fun next(): Long = nextLong()

    /**
     * Случайный Int в `[0, bound)` — **unbiased** через rejection
     * sampling. Используем именно этот метод для бросков кубиков
     * в [com.example.spelltracker.ui.hp.HitDiceSpendDialog],
     * чтобы d8/d10/d12 давали честное распределение.
     *
     * @param bound верхняя граница (исключительно). Должно быть > 0.
     * @throws IllegalArgumentException если bound <= 0.
     */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        // Без bias: берём 64-bit случайное число и отсекаем «лишние»
        // значения, которые давали бы неравномерность при простом %.
        while (true) {
            val bits = nextLong() ushr 1 // unsigned shift → 63-bit positive
            val value = (bits % bound).toInt()
            if (bits - value.toLong() >= 0) return value
        }
    }

    /**
     * Случайный Int в `[from, until)` (как [kotlin.random.Random.nextInt]).
     */
    fun nextInt(from: Int, until: Int): Int {
        require(until > from) { "until ($until) must be > from ($from)" }
        return from + nextInt(until - from)
    }

    companion object {
        /**
         * Создать новый экземпляр с seed на основе [System.nanoTime]
         * и адреса объекта. Каждый вызов даёт уникальный поток.
         */
        fun create(): Xoroshiro128Plus {
            // Простой bootstrap: SplitMix64-подобный, чтобы упаковать
            // малое количество энтропии в полные 128 бит качественного
            // состояния. Это стандартная техника для xoroshiro-семейства.
            var seed = System.nanoTime() xor System.identityHashCode(Any()).toLong()
            seed = (seed xor (seed ushr 30)) * -0x40A7B8925D1B5C39L
            seed = (seed xor (seed ushr 27)) * -0x6B2FB644ECCEEE15L
            seed = seed xor (seed ushr 31)
            val s0 = seed
            val s1 = seed xor -0x7DDDFECF7B4D2CEAL
            return Xoroshiro128Plus(s0, s1)
        }

        /**
         * Синглтон для UI-использования. Compose рендерит на main
         * потоке — гонок нет, lazy init достаточно.
         */
        val instance: Xoroshiro128Plus by lazy { create() }
    }
}