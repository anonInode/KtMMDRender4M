package com.kAIS.KAIMyEntity

import net.minecraft.client.MinecraftClient
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.util.*

private val RuntimePath: String = File(System.getProperty("java.home")).parent
private val gameDirectory: String = MinecraftClient.getInstance().runDirectory.absolutePath
private val isAndroid = File("/system/build.prop").exists()
private val isLinux = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("linux")
private val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("windows")

private const val WIN_DLL =
    "https://github.com/Gengorou-C/KAIMyEntitySaba/releases/download/20221215/KAIMyEntitySaba.dll"
private const val ANDROID_SO =
    "https://github.com.cnpmjs.org/asuka-mio/KAIMyEntitySaba/releases/download/crossplatform/KAIMyEntitySaba.so"
private const val ANDROID_LIBCXX =
    "https://github.com.cnpmjs.org/asuka-mio/KAIMyEntitySaba/releases/download/crossplatform/libc++_shared.so"

private fun download(url: URL, file: File) {
    if (file.exists()) {
        runCatching {
            System.load(file.absolutePath)
            return
        }.onFailure {
            KAIMyEntityClient.logger.info("\"" + file.absolutePath + "\" broken! Trying recover it!")
        }
    }
    try {
        file.delete()
        file.createNewFile()
        FileUtils.copyURLToFile(url, file, 30000, 30000)
        System.load(file.absolutePath)
    } catch (e: IOException) {
        file.delete()
        KAIMyEntityClient.logger.info("Download \"" + url.path + "\" failed!")
        KAIMyEntityClient.logger.info("Cannot download runtime!")
        KAIMyEntityClient.logger.info("Check you internet connection and restart game!")
        e.printStackTrace()
        throw e
    }
}

private fun load(file: File) {
    try {
        System.load(file.absolutePath)
    } catch (e: Error) {
        KAIMyEntityClient.logger.info("Runtime \"" + file.absolutePath + "\" not found!")
        throw e
    }
}

object NativeFunc {
    init {
        when {
            isWindows -> runCatching {
                KAIMyEntityClient.logger.info("Win32 Env Detected!")
                load(File(gameDirectory, "KAIMyEntitySaba.dll"))
            }.onFailure {
                download(URL(WIN_DLL), File(gameDirectory, "KAIMyEntitySaba.dll"))
            }

            isLinux && !isAndroid -> {
                KAIMyEntityClient.logger.info("Linux Env Detected!")
                load(File(gameDirectory, "KAIMyEntitySaba.so"))
            }

            isLinux && isAndroid -> runCatching {
                KAIMyEntityClient.logger.info("Android Env Detected!")
                load(File(RuntimePath, "libc++_shared.so"))
                load(File(RuntimePath, "KAIMyEntitySaba.so"))
            }.onFailure {
                download(URL(ANDROID_LIBCXX), File(RuntimePath, "libc++_shared.so"))
                download(URL(ANDROID_SO), File(RuntimePath, "KAIMyEntitySaba.so"))
            }

            else -> error("Unsupported OS!!!")
        }
    }

    external fun GetVersion(): String?

    external fun ReadByte(data: Long, pos: Long): Byte

    external fun CopyDataToByteBuffer(buffer: ByteBuffer?, data: Long, pos: Long)

    external fun LoadModelPMX(filename: String?, dir: String?, layerCount: Long): Long

    external fun LoadModelPMD(filename: String?, dir: String?, layerCount: Long): Long

    external fun DeleteModel(model: Long)

    external fun UpdateModel(model: Long)

    external fun GetVertexCount(model: Long): Long

    external fun GetPoss(model: Long): Long

    external fun GetNormals(model: Long): Long

    external fun GetUVs(model: Long): Long

    external fun GetIndexElementSize(model: Long): Long

    external fun GetIndexCount(model: Long): Long

    external fun GetIndices(model: Long): Long

    external fun GetMaterialCount(model: Long): Long

    external fun GetMaterialTex(model: Long, pos: Long): String?

    external fun GetMaterialSpTex(model: Long, pos: Long): String?

    external fun GetMaterialToonTex(model: Long, pos: Long): String?

    external fun GetMaterialAmbient(model: Long, pos: Long): Long

    external fun GetMaterialDiffuse(model: Long, pos: Long): Long

    external fun GetMaterialSpecular(model: Long, pos: Long): Long

    external fun GetMaterialSpecularPower(model: Long, pos: Long): Float

    external fun GetMaterialAlpha(model: Long, pos: Long): Float

    external fun GetMaterialTextureMulFactor(model: Long, pos: Long): Long

    external fun GetMaterialTextureAddFactor(model: Long, pos: Long): Long

    external fun GetMaterialSpTextureMode(model: Long, pos: Long): Int

    external fun GetMaterialSpTextureMulFactor(model: Long, pos: Long): Long

    external fun GetMaterialSpTextureAddFactor(model: Long, pos: Long): Long

    external fun GetMaterialToonTextureMulFactor(model: Long, pos: Long): Long

    external fun GetMaterialToonTextureAddFactor(model: Long, pos: Long): Long

    external fun GetMaterialBothFace(model: Long, pos: Long): Boolean

    external fun GetSubMeshCount(model: Long): Long

    external fun GetSubMeshMaterialID(model: Long, pos: Long): Int

    external fun GetSubMeshBeginIndex(model: Long, pos: Long): Int

    external fun GetSubMeshVertexCount(model: Long, pos: Long): Int

    external fun ChangeModelAnim(model: Long, anim: Long, layer: Long)

    external fun ResetModelPhysics(model: Long)

    external fun CreateMat(): Long

    external fun DeleteMat(mat: Long)

    external fun GetRightHandMat(model: Long, mat: Long)

    external fun GetLeftHandMat(model: Long, mat: Long)

    external fun LoadTexture(filename: String?): Long

    external fun DeleteTexture(tex: Long)

    external fun GetTextureX(tex: Long): Int

    external fun GetTextureY(tex: Long): Int

    external fun GetTextureData(tex: Long): Long

    external fun TextureHasAlpha(tex: Long): Boolean

    external fun LoadAnimation(model: Long, filename: String?): Long

    external fun DeleteAnimation(anim: Long)
}
