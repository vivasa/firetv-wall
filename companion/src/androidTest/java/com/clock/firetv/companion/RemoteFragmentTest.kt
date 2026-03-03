package com.clock.firetv.companion

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RemoteFragmentTest {

    @Test
    fun disconnectedStateShowsDisconnectedText() {
        launchFragmentInContainer<RemoteFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.connectionStatus))
            .check(matches(withText("Disconnected")))
    }

    @Test
    fun disconnectedStateShowsConnectionDot() {
        launchFragmentInContainer<RemoteFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.connectionDot))
            .check(matches(isDisplayed()))
    }

    @Test
    fun disconnectedStateDisablesPlaybackControls() {
        launchFragmentInContainer<RemoteFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.btnStop))
            .check(matches(isNotEnabled()))
        onView(withId(R.id.btnSkipNext))
            .check(matches(isNotEnabled()))
        onView(withId(R.id.btnSkipPrev))
            .check(matches(isNotEnabled()))
    }

    @Test
    fun presetChipsGroupIsDisplayed() {
        launchFragmentInContainer<RemoteFragment>(themeResId = com.google.android.material.R.style.Theme_Material3_DayNight)
        onView(withId(R.id.presetChips))
            .check(matches(isDisplayed()))
    }
}
