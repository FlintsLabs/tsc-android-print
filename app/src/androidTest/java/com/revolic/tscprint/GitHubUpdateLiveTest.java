package com.revolic.tscprint;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.revolic.tscprint.core.Cancellation;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Opt-in network probe: downloads a public APK but never installs it or changes releases. */
@RunWith(AndroidJUnit4.class)
public class GitHubUpdateLiveTest {
    @Test public void downloadsLatestPublicReleaseOverAndroidHttpsAndVerifiesChecksum() throws Exception {
        assumeTrue("Requires -e liveGithub true", "true".equals(InstrumentationRegistry.getArguments().getString("liveGithub")));
        File target = new File(InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(), "live-release.apk");
        try {
            GitHubReleaseClient client = new GitHubReleaseClient();
            Cancellation cancel = new Cancellation();
            UpdateRelease release = client.latest(cancel);
            UpdateRelease verified = client.download(release, target, cancel, bytes -> {});
            assertEquals(release.size, target.length());
            assertNotNull(verified.sha256);
            if (release.sha256 != null) assertEquals(release.sha256, verified.sha256);
            android.util.Log.i("TscUpdaterLive", release.tag + " size=" + target.length() + " sha256=" + verified.sha256);
        } finally { target.delete(); new File(target + ".part").delete(); }
    }
}
