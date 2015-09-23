package com.jcwhatever.phantom;

import com.jcwhatever.nucleus.managed.language.ILanguageContext;
import com.jcwhatever.nucleus.managed.language.Localized;
import com.jcwhatever.nucleus.utils.text.components.IChatMessage;

public class Lang {

    private Lang() {}

    @Localized
    public static IChatMessage get(CharSequence text, Object... params) {
        ILanguageContext context = PhantomPackets.getPlugin().getLanguageContext();
        return context.get(text, params);
    }
}

