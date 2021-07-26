package com.dezhik.conf;

import javax.validation.constraints.NotNull;

/**
 * @author ilya.dezhin
 */
public interface UpdatesLoader {

    @NotNull
    ConfValues getUpdates(final long lastUpdateTime);
}
