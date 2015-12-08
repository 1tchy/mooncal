import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static play.test.Helpers.*;

@Ignore
public class IntegrationTest {

	/**
	 * add your integration test here
	 * in this example we just check if the welcome page is being shown
	 */
	@Test
	public void test() {
		running(testServer(3333, fakeApplication()), HTMLUNIT, browser -> {
			browser.goTo("http://localhost:3333");
			assertThat(browser.pageSource(), containsString("mondkalender"));
		});
	}

}
