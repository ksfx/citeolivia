package com.citeolivia.library;

import java.lang.reflect.Field;
import java.util.List;

/** Builds {@link Reference} instances for unit tests (no database). */
public final class TestReferences {

    private TestReferences() {}

    public static Reference of(ReferenceDraft draft, String key) {
        Reference r = Reference.create(1L, draft.normalize(), key);
        setId(r, 1L);
        return r;
    }

    public static ReferenceDraft draft(ReferenceType type, String title, String year, Contributor... people) {
        ReferenceDraft d = new ReferenceDraft();
        d.setType(type);
        d.setTitle(title);
        d.setYear(year);
        d.setContributors(new java.util.ArrayList<>(List.of(people)));
        return d;
    }

    private static void setId(Reference r, Long id) {
        try {
            Field f = Reference.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(r, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
