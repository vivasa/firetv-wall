package com.clock.firetv.companion

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.espresso.matcher.ViewMatchers.Visibility
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsFragmentTest {

    @Test
    fun disconnectedStateShowsHintText() {
        launchFragmentInContainer<SettingsFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.notConnectedHint))
            .check(matches(isDisplayed()))
    }

    @Test
    fun disconnectedStateHidesSettingsControls() {
        launchFragmentInContainer<SettingsFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.settingsControls))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun settingsContainerIsDisplayed() {
        launchFragmentInContainer<SettingsFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.settingsContainer))
            .check(matches(isDisplayed()))
    }
}
