package com.npo.lookup;

import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LookupValidationTest {

    @Test
    void rejectsMissingBlankAndLongNames() {
        Skill skill = new Skill(null, "Beginner");
        assertThrows(IllegalArgumentException.class, skill::validate);

        skill.setName("   ");
        assertThrows(IllegalArgumentException.class, skill::validate);

        skill.setName("a".repeat(121));
        assertThrows(IllegalArgumentException.class, skill::validate);
    }

    @Test
    void rejectsNonTextFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new Skill().populate(
                        Map.of("name", 123, "level", "Beginner")));

        assertThrows(IllegalArgumentException.class,
                () -> new Skill().populate(
                        Map.of("name", "Teaching", "level", 123)));

        assertThrows(IllegalArgumentException.class,
                () -> new ItemCategory().populate(
                        Map.of("name", "Food", "unit", 123)));

        assertThrows(IllegalArgumentException.class,
                () -> new NeedCategory().populate(
                        Map.of("name", "Medical", "description", 123)));
    }

    @Test
    void rejectsMissingExtraFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new Skill("Teaching", null).validate());

        assertThrows(IllegalArgumentException.class,
                () -> new ItemCategory("Food", "   ").validate());

        assertThrows(IllegalArgumentException.class,
                () -> new NeedCategory("Medical", null).validate());
    }

    @Test
    void rejectsUnknownSkillLevel() {
        assertThrows(IllegalArgumentException.class,
                () -> new Skill("Teaching", "Expert").validate());
    }

    @Test
    void rejectsOversizedExtraFields() {
        assertThrows(IllegalArgumentException.class,
                () -> new ItemCategory("Food", "a".repeat(501)).validate());

        assertThrows(IllegalArgumentException.class,
                () -> new NeedCategory("Medical", "a".repeat(501)).validate());
    }

    @Test
    void acceptsValidValuesAndTrimsSpaces() {
        Skill skill = new Skill(" Teaching ", " Advanced ");
        ItemCategory item = new ItemCategory(" Food ", " kg ");
        NeedCategory need = new NeedCategory(" Medical ", " Medicine ");

        assertDoesNotThrow(skill::validate);
        assertDoesNotThrow(item::validate);
        assertDoesNotThrow(need::validate);

        assertEquals("Teaching", skill.getName());
        assertEquals("Advanced", skill.getLevel());
        assertEquals("Food", item.getName());
        assertEquals("kg", item.getUnit());
        assertEquals("Medical", need.getName());
        assertEquals("Medicine", need.getDescription());
    }
}