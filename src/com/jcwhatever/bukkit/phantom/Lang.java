package com.jcwhatever.bukkit.phantom;

import com.jcwhatever.nucleus.language.LanguageManager;
import com.jcwhatever.nucleus.language.Localized;

public class Lang {

    private Lang() {}

    @Localized
    public static String get(String text, Object... params) {
        LanguageManager lang = PhantomPackets.getPlugin().getLanguageManager();

        return lang.get(text, params);
    }
}

