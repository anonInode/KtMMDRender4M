package com.kAIS.KAIMyEntity.renderer

import com.kAIS.KAIMyEntity.KAIMyEntityClient
import com.kAIS.KAIMyEntity.NativeFunc
import org.lwjgl.opengl.GL46C
import java.nio.ByteBuffer

object MMDTextureManager {
    private val textures = mutableMapOf<String, Texture>()

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

                val tex = GL46C.glGenTextures()
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, tex)
                val texSize = x * y * (if (hasAlpha) 4 else 3)
                val texBuffer = ByteBuffer.allocateDirect(texSize)
                CopyDataToByteBuffer(texBuffer, texData, texSize.toLong())
                if (hasAlpha) {
                    GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 4)
                    GL46C.glTexImage2D(
                        GL46C.GL_TEXTURE_2D,
                        0,
                        GL46C.GL_RGBA,
                        x,
                        y,
                        0,
                        GL46C.GL_RGBA,
                        GL46C.GL_UNSIGNED_BYTE,
                        texBuffer
                    )
                } else {
                    GL46C.glPixelStorei(GL46C.GL_UNPACK_ALIGNMENT, 1)
                    GL46C.glTexImage2D(
                        GL46C.GL_TEXTURE_2D,
                        0,
                        GL46C.GL_RGB,
                        x,
                        y,
                        0,
                        GL46C.GL_RGB,
                        GL46C.GL_UNSIGNED_BYTE,
                        texBuffer
                    )
                }
                DeleteTexture(nfTex)

                GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAX_LEVEL, 0)
                GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MIN_FILTER, GL46C.GL_LINEAR)
                GL46C.glTexParameteri(GL46C.GL_TEXTURE_2D, GL46C.GL_TEXTURE_MAG_FILTER, GL46C.GL_LINEAR)
                GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, 0)

                Texture(tex, hasAlpha)
            }
        }
    }

    data class Texture(
        val tex: Int,
        val hasAlpha: Boolean = false,
    )
}
