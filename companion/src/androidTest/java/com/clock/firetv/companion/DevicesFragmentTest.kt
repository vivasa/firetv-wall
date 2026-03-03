package com.clock.firetv.companion

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DevicesFragmentTest {

    @Test
    fun manualEntryButtonIsDisplayed() {
        launchFragmentInContainer<DevicesFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.btnManualEntry))
            .check(matches(isDisplayed()))
    }

    @Test
    fun deviceListIsDisplayed() {
        launchFragmentInContainer<DevicesFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.deviceList))
            .check(matches(isDisplayed()))
    }

    @Test
    fun manualEntryButtonHasCorrectText() {
        launchFragmentInContainer<DevicesFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.btnManualEntry))
            .check(matches(withText("Enter IP manually")))
    }
}
