package com.clock.firetv

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DriftAnimatorTest {

    @Test
    fun `calculateDriftPosition returns midpoint of current and target`() {
        val (x, y) = DriftAnimator.calculateDriftPosition(0f, 0f, 20f, 10f)
        assertThat(x).isEqualTo(10f)
        assertThat(y).isEqualTo(5f)
    }

    @Test
    fun `calculateDriftPosition clamps to positive MAX_DRIFT`() {
        // currentX=20, targetX=60 → midpoint=40, clamped to 30
        val (x, _) = DriftAnimator.calculateDriftPosition(20f, 0f, 60f, 0f)
        assertThat(x).isEqualTo(30f)
    }

    @Test
    fun `calculateDriftPosition clamps to negative MAX_DRIFT`() {
        // currentX=-20, targetX=-60 → midpoint=-40, clamped to -30
        val (x, _) = DriftAnimator.calculateDriftPosition(-20f, 0f, -60f, 0f)
        assertThat(x).isEqualTo(-30f)
    }

    @Test
    fun `calculateDriftPosition with custom maxDrift clamps correctly`() {
        val (x, y) = DriftAnimator.calculateDriftPosition(0f, 0f, 100f, -100f, maxDrift = 10f)
        assertThat(x).isEqualTo(10f)
        assertThat(y).isEqualTo(-10f)
    }

    @Test
    fun `calculateDriftPosition at origin with zero target stays at origin`() {
        val (x, y) = DriftAnimator.calculateDriftPosition(0f, 0f, 0f, 0f)
        assertThat(x).isEqualTo(0f)
        assertThat(y).isEqualTo(0f)
    }
}
