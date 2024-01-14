package com.kAIS.KAIMyEntity.renderer

import com.kAIS.KAIMyEntity.KAIMyEntityClient
import com.kAIS.KAIMyEntity.NativeFunc
import gli_.gl.ExternalFormat.RGBA
import gli_.gl.InternalFormat.RGBA_UNORM
import gli_.gl.InternalFormat.RGB_UNORM
import gli_.gl.TypeFormat.U8
import glm_.vec2.Vec2i
import gln.TextureTarget
import gln.identifiers.GlTexture
import gln.texture.TexMagFilter
import gln.texture.TexMinFilter
import org.lwjgl.opengl.GL46C
import java.nio.ByteBuffer

private val textures = mutableMapOf<String, MMDTextureManager.Texture>()

object MMDTextureManager {
    fun texture(filename: String): Texture {
        return textures.computeIfAbsent(filename) {
            with(NativeFunc) {
                val nfTex = LoadTexture(filename)
                if (nfTex == 0L) {
                    KAIMyEntityClient.logger.info(String.format("Cannot find texture: %s", filename))
                    return@computeIfAbsent Texture(0, false)
                }
                val x = GetTextureX(nfTex)
                val y = GetTextureY(nfTex)
                val texData = GetTextureData(nfTex)
                val hasAlpha = TextureHasAlpha(nfTex)

                val tex = GlTexture.gen()
                tex.bound(TextureTarget._2D) {
                    val texSize = x * y * (if (hasAlpha) 4 else 3)
                    val texBuffer = ByteBuffer.allocateDirect(texSize)
                    CopyDataToByteBuffer(texBuffer, texData, texSize.toLong())
                    val alignment = if (hasAlpha) 4 else 1
                    GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, alignment)
                    image2D(
                        if (hasAlpha) RGBA_UNORM else RGB_UNORM,
                        Vec2i(x, y), RGBA, U8, texBuffer,
                    )
                    DeleteTexture(nfTex)
                    maxLevel = 0
                    minFilter = TexMinFilter.LINEAR
                    magFilter = TexMagFilter.LINEAR
                }
                Texture(tex.name, hasAlpha)
            }
        }
    }

    data class Texture(
        val tex: Int,
        val hasAlpha: Boolean,
    )
}
