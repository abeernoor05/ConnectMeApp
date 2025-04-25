import android.app.Activity
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.abeernoor.i221122.ActualChatActivity
import com.abeernoor.i221122.DmsActivity
import com.abeernoor.i221122.R
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatScreenTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ActualChatActivity::class.java)


    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun tearDown() {
        Intents.release()
    }

    @Test
    fun testBackButtonNavigatesToPreviousScreen() {
        onView(withId(R.id.backButton)).perform(click())

        Intents.intended(hasComponent(DmsActivity::class.java.name))
    }

    @Test
    fun testSystemBackButtonNavigatesToPreviousScreen() {
        pressBack()

        onView(withId(R.id.backIcon)).check(matches(isDisplayed()))
    }

    @Test
    fun testVanishModeButtonToggles() {
        onView(withId(R.id.vanishMode)).perform(click())

        onView(withId(R.id.messagesContainer)).check(matches(withEffectiveVisibility(Visibility.GONE)))
    }
}
