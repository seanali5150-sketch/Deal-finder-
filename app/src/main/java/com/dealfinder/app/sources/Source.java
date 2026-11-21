package com.dealfinder.app.sources;

import com.dealfinder.app.Deal;
import com.dealfinder.app.Prefs;

import java.util.List;

public interface Source {
    String name();
    /** False when the user hasn't added the key this source needs. */
    boolean enabled(Prefs p);
    List<Deal> fetch(Prefs p) throws Exception;
}
