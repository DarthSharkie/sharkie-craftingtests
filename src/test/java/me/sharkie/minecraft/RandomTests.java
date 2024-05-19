package me.sharkie.minecraft;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

public class RandomTests {
    @Test
    void test1() {
        Identifier id = new Identifier("sharkie", "ingot");
        String idString = id.toString();
        Identifier id2 = new Identifier(idString);
        assert id.equals(id2);
    }
}
