package com.lsaegis.eoms.engineer.demo;

import com.lsaegis.eoms.web.ImageUploadActivity;

public final class MainActivity extends ImageUploadActivity {
    @Override protected String entryUrl() {
        return BuildConfig.ENTRY_URL;
    }
}
