package com.kAIS.KAIMyEntity.renderer

import com.kAIS.KAIMyEntity.KAIMyEntityClient
import com.kAIS.KAIMyEntity.NativeFunc
import com.kAIS.KAIMyEntity.renderer.MMDTextureManager.texture
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import gli_.gl
import glm_.vec2.Vec2i
import gln.BufferTarget
import gln.TextureTarget
import gln.identifiers.GlBuffer
import gln.identifiers.GlTexture
import gln.texture.TexMagFilter
import gln.texture.TexMinFilter
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gl.ShaderProgram
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.texture.TextureManager
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.world.LightType
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.opengl.GL46C
import java.nio.ByteBuffer
import java.nio.ByteOrder

class MMDModelOpenGL(
    val model: Long,
    val modelDir: String,
    val vertexCount: Int,
    val posBuffer: ByteBuffer,
    val colorBuffer: ByteBuffer,
    val normalBuffer: ByteBuffer,
    var uv0Buffer: ByteBuffer,
    var uv1Buffer: ByteBuffer,
    var uv2Buffer: ByteBuffer,
    var vertexArrayObject: Int,
    var indexBufferObject: Int,
    var vertexBufferObject: GlBuffer,
    var colorBufferObject: Int,
    var normalBufferObject: GlBuffer,
    var uv0BufferObject: GlBuffer,
    var uv1BufferObject: Int,
    var uv2BufferObject: Int,
    var indexElementSize: Int,
    var indexType: Int,
    val mats: List<MMDTextureManager.Texture>,
    val lightMapMaterial: MMDTextureManager.Texture,
) : IMMDModel {
    var shaderProgram: Int = 0

    var positionLocation: Int = 0
    var normalLocation: Int = 0
    var uv0Location: Int = 0
    var uv1Location: Int = 0
    var uv2Location: Int = 0
    var colorLocation: Int = 0

    var I_positionLocation: Int = 0
    var I_normalLocation: Int = 0
    var I_uv0Location: Int = 0
    var I_uv2Location: Int = 0
    var I_colorLocation: Int = 0

    val light0Direction = Vector3f()
    val light1Direction = Vector3f()
    val q = Quaternionf()

    override fun Render(
        entityIn: Entity,
        entityYaw: Float,
        entityPitch: Float,
        entityTrans: Vector3f,
        mat: MatrixStack,
        packedLight: Int
    ) {
        Update()
        RenderModel(entityIn, entityYaw, entityPitch, entityTrans, mat)
    }

    override fun ChangeAnim(anim: Long, layer: Long) = NativeFunc.ChangeModelAnim(model, anim, layer)

    override fun ResetPhysics() = NativeFunc.ResetModelPhysics(model)

    override fun GetModelLong() = model

    override fun GetModelDir() = modelDir

    fun Update() {
        NativeFunc.UpdateModel(model)
    }

    fun RenderModel(
        entityIn: Entity,
        entityYaw: Float,
        entityPitch: Float,
        entityTrans: Vector3f,
        deliverStack: MatrixStack
    ) {
        val MCinstance = MinecraftClient.getInstance()
        light0Direction.set(1.0f, 0.75f, 0.0f)
        light1Direction.set(-1.0f, 0.75f, 0.0f)
        light0Direction.normalize()
        light1Direction.normalize()
        q.set(0f, 0f, 0f, 1f)
        q.rotateY(entityYaw * (Math.PI.toFloat() / 180f))
        light0Direction.rotate(q)
        light1Direction.rotate(q)

        deliverStack.multiply(Quaternionf().rotateY(-entityYaw * (Math.PI.toFloat() / 180f)))
        deliverStack.multiply(Quaternionf().rotateX(entityPitch * (Math.PI.toFloat() / 180f)))
        deliverStack.translate(entityTrans.x, entityTrans.y, entityTrans.z)
        deliverStack.scale(0.09f, 0.09f, 0.09f)

        shaderProgram = RenderSystem.getShader()!!.glRef
        setUniforms(RenderSystem.getShader()!!, deliverStack)
        RenderSystem.getShader()!!.bind()

        updateLocation(shaderProgram)

        BufferRenderer.reset()
        GL46C.glBindVertexArray(vertexArrayObject)
        RenderSystem.enableBlend()
        RenderSystem.enableDepthTest()
        RenderSystem.blendEquation(GL46C.GL_FUNC_ADD)
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)

        // Position
        val posAndNorSize = vertexCount * 12 // float * 3
        val posData = NativeFunc.GetPoss(model)
        NativeFunc.CopyDataToByteBuffer(posBuffer, posData, posAndNorSize.toLong())
        vertexBufferObject.bind(BufferTarget.ARRAY) { data(posBuffer) }
        val posLoc = when {
            positionLocation != -1 -> positionLocation
            I_positionLocation != -1 -> I_positionLocation
            else -> null
        }
        if (posLoc != null) {
            GL46C.glEnableVertexAttribArray(posLoc)
            GL46C.glVertexAttribPointer(posLoc, 3, GL46C.GL_FLOAT, false, 0, 0)
        }

        // Normal
        val normalData = NativeFunc.GetNormals(model)
        NativeFunc.CopyDataToByteBuffer(normalBuffer, normalData, posAndNorSize.toLong())
        normalBufferObject.bind(BufferTarget.ARRAY) { data(normalBuffer) }
        val normalLoc = when {
            normalLocation != -1 -> normalLocation
            I_normalLocation != -1 -> I_normalLocation
            else -> null
        }
        if (normalLoc != null) {
            GL46C.glEnableVertexAttribArray(normalLoc)
            GL46C.glVertexAttribPointer(normalLoc, 3, GL46C.GL_FLOAT, false, 0, 0)
        }

        // UV0
        val uv0Size = vertexCount * 8 //float * 2
        val uv0Data = NativeFunc.GetUVs(model)
        NativeFunc.CopyDataToByteBuffer(uv0Buffer, uv0Data, uv0Size.toLong())
        uv0BufferObject.bind(BufferTarget.ARRAY) { data(uv0Buffer) }
        val uv0Loc = when {
            uv0Location != -1 -> uv0Location
            I_uv0Location != -1 -> I_uv0Location
            else -> null
        }
        if (uv0Loc != null) {
            GL46C.glEnableVertexAttribArray(uv0Loc)
            GL46C.glVertexAttribPointer(uv0Loc, 2, GL46C.GL_FLOAT, false, 0, 0)
        }

        //UV2
        MCinstance.world!!.calculateAmbientDarkness()
        val blockBrightness = 16 * entityIn.world.getLightLevel(
            LightType.BLOCK,
            entityIn.blockPos.up((entityIn.eyeY - entityIn.blockY).toInt())
        )
        val skyBrightness = Math.round(
            (15.0f - MCinstance.world!!.ambientDarkness) * (entityIn.world.getLightLevel(
                LightType.SKY, entityIn.blockPos.up(
                    (entityIn.eyeY - entityIn.blockY).toInt()
                )
            ) / 15.0f) * 16
        )
        uv2Buffer.clear()
        for (i in 0 until vertexCount) {
            uv2Buffer.putInt(blockBrightness)
            uv2Buffer.putInt(skyBrightness)
        }
        uv2Buffer.flip()
        if (uv2Location != -1) {
            GL46C.glEnableVertexAttribArray(uv2Location)
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject)
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv2Buffer, GL46C.GL_STATIC_DRAW)
            GL46C.glVertexAttribIPointer(uv2Location, 2, GL46C.GL_INT, 0, 0)
        }

        //UV1
        uv1Buffer.position(0)
        if (uv1Location != -1) {
            GL46C.glEnableVertexAttribArray(uv1Location)
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv1BufferObject)
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv1Buffer, GL46C.GL_STATIC_DRAW)
            GL46C.glVertexAttribIPointer(uv1Location, 2, GL46C.GL_INT, 0, 0)
        }

        //color
        if (colorLocation != -1) {
            GL46C.glEnableVertexAttribArray(colorLocation)
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, colorBufferObject)
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, colorBuffer, GL46C.GL_STATIC_DRAW)
            GL46C.glVertexAttribPointer(colorLocation, 4, GL46C.GL_FLOAT, false, 0, 0)
        }

        GL46C.glBindBuffer(GL46C.GL_ELEMENT_ARRAY_BUFFER, indexBufferObject)

        //Iris
        if (I_uv2Location != -1) {
            GL46C.glEnableVertexAttribArray(I_uv2Location)
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, uv2BufferObject)
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, uv2Buffer, GL46C.GL_STATIC_DRAW)
            GL46C.glVertexAttribIPointer(I_uv2Location, 2, GL46C.GL_INT, 0, 0)
        }
        if (I_colorLocation != -1) {
            GL46C.glEnableVertexAttribArray(I_colorLocation)
            GL46C.glBindBuffer(GL46C.GL_ARRAY_BUFFER, colorBufferObject)
            GL46C.glBufferData(GL46C.GL_ARRAY_BUFFER, colorBuffer, GL46C.GL_STATIC_DRAW)
            GL46C.glVertexAttribPointer(I_colorLocation, 4, GL46C.GL_FLOAT, false, 0, 0)
        }

        //Draw
        RenderSystem.activeTexture(GL46C.GL_TEXTURE0)
        val subMeshCount = NativeFunc.GetSubMeshCount(model)
        for (i in 0 until subMeshCount) {
            val materialID = NativeFunc.GetSubMeshMaterialID(model, i)
            val alpha = NativeFunc.GetMaterialAlpha(model, materialID.toLong())
            if (alpha == 0.0f) continue

            if (NativeFunc.GetMaterialBothFace(model, materialID.toLong())) {
                RenderSystem.disableCull()
            } else {
                RenderSystem.enableCull()
            }
            if (mats[materialID].tex == 0) MCinstance.entityRenderDispatcher.textureManager.bindTexture(TextureManager.MISSING_IDENTIFIER)
            else GL46C.glBindTexture(GL46C.GL_TEXTURE_2D, mats[materialID].tex)
            val startPos = NativeFunc.GetSubMeshBeginIndex(model, i).toLong() * indexElementSize
            val count = NativeFunc.GetSubMeshVertexCount(model, i)

            RenderSystem.assertOnRenderThread()
            GL46C.glDrawElements(GL46C.GL_TRIANGLES, count, indexType, startPos)
        }

        RenderSystem.getShader()!!.unbind()
        BufferRenderer.reset()
    }

    fun updateLocation(shaderProgram: Int) {
        positionLocation = GlStateManager._glGetAttribLocation(shaderProgram, "Position")
        normalLocation = GlStateManager._glGetAttribLocation(shaderProgram, "Normal")
        uv0Location = GlStateManager._glGetAttribLocation(shaderProgram, "UV0")
        uv1Location = GlStateManager._glGetAttribLocation(shaderProgram, "UV1")
        uv2Location = GlStateManager._glGetAttribLocation(shaderProgram, "UV2")
        colorLocation = GlStateManager._glGetAttribLocation(shaderProgram, "Color")

        I_positionLocation = GlStateManager._glGetAttribLocation(shaderProgram, "iris_Position")
        I_normalLocation = GlStateManager._glGetAttribLocation(shaderProgram, "iris_Normal")
        I_uv0Location = GlStateManager._glGetAttribLocation(shaderProgram, "iris_UV0")
        I_uv2Location = GlStateManager._glGetAttribLocation(shaderProgram, "iris_UV2")
        I_colorLocation = GlStateManager._glGetAttribLocation(shaderProgram, "iris_Color")
    }

    fun setUniforms(shader: ShaderProgram, deliverStack: MatrixStack) {
        shader.modelViewMat?.set(deliverStack.peek().positionMatrix)
        shader.projectionMat?.set(RenderSystem.getProjectionMatrix())
        shader.viewRotationMat?.set(RenderSystem.getInverseViewRotationMatrix())
        shader.colorModulator?.set(RenderSystem.getShaderColor())
        shader.light0Direction?.set(light0Direction)
        shader.light1Direction?.set(light1Direction)
        shader.fogStart?.set(RenderSystem.getShaderFogStart())
        shader.fogEnd?.set(RenderSystem.getShaderFogEnd())
        shader.fogColor?.set(RenderSystem.getShaderFogColor())
        shader.fogShape?.set(RenderSystem.getShaderFogShape().id)
        shader.textureMat?.set(RenderSystem.getTextureMatrix())
        shader.gameTime?.set(RenderSystem.getShaderGameTime())
        val screenSize = shader.screenSize
        if (screenSize != null) {
            val window = MinecraftClient.getInstance().window
            screenSize[window.width.toFloat()] = window.height.toFloat()
        }
        shader.lineWidth?.set(RenderSystem.getShaderLineWidth())
        shader.addSampler("Sampler1", lightMapMaterial.tex)
        shader.addSampler("Sampler2", lightMapMaterial.tex)
    }

    companion object {
        @JvmStatic
        fun Create(modelFilename: String, modelDir: String, isPMD: Boolean, layerCount: Long): MMDModelOpenGL? {
            val model = with(NativeFunc) {
                if (isPMD) {
                    LoadModelPMD(modelFilename, modelDir, layerCount)
                } else {
                    LoadModelPMX(modelFilename, modelDir, layerCount)
                }
            }
            if (model == 0L) {
                KAIMyEntityClient.logger.info(String.format("Cannot open model: '%s'.", modelFilename))
                return null
            }
            BufferRenderer.reset()
            //Model exists,now we prepare data for OpenGL
            val vertexArrayObject = GL46C.glGenVertexArrays()
            val ibo = GlBuffer.gen()
            val positionBufferObject = GlBuffer.gen()
            val colorBufferObject = GL46C.glGenBuffers()
            val normalBufferObject = GlBuffer.gen()
            val uv0BufferObject = GlBuffer.gen()
            val uv1BufferObject = GL46C.glGenBuffers()
            val uv2BufferObject = GL46C.glGenBuffers()

            val vertexCount = NativeFunc.GetVertexCount(model).toInt()
            val posBuffer = ByteBuffer.allocateDirect(vertexCount * 12) //float * 3
            val colorBuffer = ByteBuffer.allocateDirect(vertexCount * 16) //float * 4
            val norBuffer = ByteBuffer.allocateDirect(vertexCount * 12) //float * 3
            val uv0Buffer = ByteBuffer.allocateDirect(vertexCount * 8) //float * 2
            val uv1Buffer = ByteBuffer.allocateDirect(vertexCount * 8) //int * 2
            val uv2Buffer = ByteBuffer.allocateDirect(vertexCount * 8) //int * 2
            colorBuffer.order(ByteOrder.LITTLE_ENDIAN)
            uv1Buffer.order(ByteOrder.LITTLE_ENDIAN)
            uv2Buffer.order(ByteOrder.LITTLE_ENDIAN)

            GL46C.glBindVertexArray(vertexArrayObject)
            //Init indexBufferObject
            val indexElementSize = NativeFunc.GetIndexElementSize(model).toInt()
            val indexCount = NativeFunc.GetIndexCount(model).toInt()
            val indexSize = indexCount * indexElementSize
            val indexData = NativeFunc.GetIndices(model)
            val indexBuffer = ByteBuffer.allocateDirect(indexSize)
            for (i in 0 until indexSize) indexBuffer.put(NativeFunc.ReadByte(indexData, i.toLong()))
            indexBuffer.position(0)
            ibo.bind(BufferTarget.ARRAY) { data(indexBuffer) }

            val indexType = when (indexElementSize) {
                1 -> GL46C.GL_UNSIGNED_BYTE
                2 -> GL46C.GL_UNSIGNED_SHORT
                4 -> GL46C.GL_UNSIGNED_INT
                else -> 0
            }

            //Material
            val materials = buildList {
                repeat(NativeFunc.GetMaterialCount(model).toInt()) {
                    val file = NativeFunc.GetMaterialTex(model, it.toLong())
                    val tex = if (file.isNullOrBlank()) {
                        MMDTextureManager.Texture()
                    } else {
                        texture(file).orElseGet { MMDTextureManager.Texture() }
                    }
                    add(tex)
                }
            }

            // lightMap
            val lightMapMaterial = texture("$modelDir/lightMap.png").orElseGet {
                val tex = GlTexture.gen()
                tex.bound(TextureTarget._2D) {
                    val texBuffer = ByteBuffer.allocateDirect(16 * 16 * 4)
                    texBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    for (i in 0 until 16 * 16) {
                        texBuffer.put(255.toByte())
                        texBuffer.put(255.toByte())
                        texBuffer.put(255.toByte())
                        texBuffer.put(255.toByte())
                    }
                    texBuffer.flip()
                    image2D(
                        gl.InternalFormat.RGBA_UNORM, Vec2i(16, 16),
                        gl.ExternalFormat.RGBA, gl.TypeFormat.U8, texBuffer
                    )
                    maxLevel = 0
                    minFilter = TexMinFilter.LINEAR
                    magFilter = TexMagFilter.LINEAR
                }
                MMDTextureManager.Texture(tex.name, true)
            }

            for (i in 0 until vertexCount) {
                colorBuffer.putFloat(1.0f)
                colorBuffer.putFloat(1.0f)
                colorBuffer.putFloat(1.0f)
                colorBuffer.putFloat(1.0f)
            }
            colorBuffer.flip()

            for (i in 0 until vertexCount) {
                uv1Buffer.putInt(15)
                uv1Buffer.putInt(15)
            }
            uv1Buffer.flip()

            return MMDModelOpenGL(
                model, modelDir, vertexCount, posBuffer,
                colorBuffer, norBuffer, uv0Buffer, uv1Buffer, uv2Buffer,
                vertexArrayObject, ibo.name, positionBufferObject, colorBufferObject,
                normalBufferObject, uv0BufferObject, uv1BufferObject,
                uv2BufferObject, indexElementSize, indexType,
                materials, lightMapMaterial
            )
        }

        @JvmStatic
        fun Delete(model: MMDModelOpenGL) = NativeFunc.DeleteModel(model.model)
    }
}
