package com.jcwhatever.bukkit.phantom;

import com.jcwhatever.nucleus.managed.language.ILanguageContext;
import com.jcwhatever.nucleus.managed.language.Localized;

public class Lang {

    private Lang() {}

    @Localized
    public static String get(String text, Object... params) {
        ILanguageContext context = PhantomPackets.getPlugin().getLanguageContext();

        return context.get(text, params);
    }
}

