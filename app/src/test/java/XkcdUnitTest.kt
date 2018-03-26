
import android.test.MoreAsserts.assertNotEqual
import com.transitiontose.xkcdviewand.XkcdActivity
import junit.framework.Assert
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.junit.*
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.MockitoAnnotations

//@RunWith(RobolectricGradleTestRunner::class)
//@Config(constants = BuildConfig::class, sdk = intArrayOf(16))
//@RunWith(MockitoJUnitRunner::class)
class XkcdUnitTest {
    companion object {
        var o: Object? = null
        @BeforeClass
        @JvmStatic
        fun doThisFirstOnlyOnce() {
            // do initialization here, run once for all SillyTest tests
            o = Object()
        }

        @AfterClass
        @JvmStatic
        fun doThisLastOnlyOnce() {
            // do termination here, run once for all SillyTest tests
        }
    }

    @Mock
    var mActivity : XkcdActivity = XkcdActivity()

    @Before
    fun doThisFirst() {
        MockitoAnnotations.initMocks(this)
    }


    @After
    fun doThisLast() {
        // do termination here, run on every test method
    }
    @Test
    fun thisIsReallySilly() {
        Assert.assertEquals("bit got flipped by cosmic rays", 1, 1)
        val fakeList = mock(List::class.java)
        `when`<Any>(fakeList[0]).thenReturn(1337)
        assertFalse(o == null)
        assertEquals(1339, fakeList[0])
        assertNotEqual(mActivity.randomInteger(0, 2000), -1)
    }
}