package net.walksanator.uxncraft;

import dev.architectury.injectables.annotations.ExpectPlatform;


public class ShaderExpectPlatform {
    @ExpectPlatform
    public static int getShader() {
        throw new AssertionError();
    }
}
