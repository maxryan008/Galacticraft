package dev.galacticraft.mod.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DynamicFluidRenderer {

    public static final class TrackedMesh {
        private final MeshData mesh;
        private final ByteBufferBuilder builder;

        public TrackedMesh(MeshData mesh, ByteBufferBuilder builder) {
            this.mesh = mesh;
            this.builder = builder;
        }

        public MeshData mesh() {
            return mesh;
        }

        public void close() {
            builder.close();
        }
    }

    public static final Map<BlockPos, TrackedMesh> COMPILED_TRANSLUCENT_MESH_BUFFERS = new ConcurrentHashMap<>();

    public static void storeOrRemove(BlockPos origin, MeshData mesh) {
        BlockPos key = origin.immutable();

        TrackedMesh old = COMPILED_TRANSLUCENT_MESH_BUFFERS.remove(key);
        if (old != null) old.close();

        if (mesh == null) return;

        ByteBuffer original = mesh.vertexBuffer();
        int size = original.remaining();

        ByteBufferBuilder builder = new ByteBufferBuilder(size);
        long dest = builder.reserve(size);
        MemoryUtil.memCopy(MemoryUtil.memAddress(original), dest, size);

        MeshData.DrawState drawState = new MeshData.DrawState(
                mesh.drawState().format(),
                mesh.drawState().vertexCount(),
                mesh.drawState().indexCount(),
                mesh.drawState().mode(),
                mesh.drawState().indexType()
        );

        MeshData newMesh = new MeshData(builder.build(), drawState);
        COMPILED_TRANSLUCENT_MESH_BUFFERS.put(key, new TrackedMesh(newMesh, builder));
    }

    public static TrackedMesh get(BlockPos origin) {
        return COMPILED_TRANSLUCENT_MESH_BUFFERS.get(origin);
    }

    public static void clear(BlockPos origin) {
        BlockPos key = origin.immutable();
        TrackedMesh mesh = COMPILED_TRANSLUCENT_MESH_BUFFERS.remove(key);
        if (mesh != null) mesh.close();
    }

    public static TrackedMesh cloneMesh(MeshData original) {
        ByteBuffer src = original.vertexBuffer();
        int size = src.remaining();

        ByteBufferBuilder builder = new ByteBufferBuilder(size);
        long dest = builder.reserve(size);
        MemoryUtil.memCopy(MemoryUtil.memAddress(src), dest, size);

        MeshData.DrawState drawState = new MeshData.DrawState(
                original.drawState().format(),
                original.drawState().vertexCount(),
                original.drawState().indexCount(),
                original.drawState().mode(),
                original.drawState().indexType()
        );

        return new TrackedMesh(new MeshData(builder.build(), drawState), builder);
    }

    public static TrackedMesh mergeMeshes(TrackedMesh a, MeshData b) {
        if (a == null) return new TrackedMesh(b, null);

        MeshData aMesh = a.mesh();
        MeshData.DrawState aDraw = aMesh.drawState();
        MeshData.DrawState bDraw = b.drawState();

        VertexFormat format = aDraw.format();
        if (!format.equals(bDraw.format())) {
            throw new IllegalStateException("Vertex formats do not match! A=" + format + " B=" + bDraw.format());
        }
        if (aDraw.mode() != bDraw.mode()) {
            throw new IllegalStateException("Vertex modes do not match! A=" + aDraw.mode() + " B=" + bDraw.mode());
        }

        int vertexSize = format.getVertexSize();
        int aVertexCount = aDraw.vertexCount();
        int bVertexCount = bDraw.vertexCount();
        int totalVertexCount = aVertexCount + bVertexCount;

        int aBytes = aVertexCount * vertexSize;
        int bBytes = bVertexCount * vertexSize;

        ByteBuffer aBuf = aMesh.vertexBuffer();
        ByteBuffer bBuf = b.vertexBuffer();

        if (aBuf.remaining() < aBytes || bBuf.remaining() < bBytes) {
            throw new IllegalStateException("Insufficient vertex buffer data.");
        }

        ByteBufferBuilder builder = new ByteBufferBuilder(aBytes + bBytes);
        long dest = builder.reserve(aBytes + bBytes);

        MemoryUtil.memCopy(MemoryUtil.memAddress(aBuf), dest, aBytes);
        MemoryUtil.memCopy(MemoryUtil.memAddress(bBuf), dest + aBytes, bBytes);

        MeshData.DrawState mergedDraw = new MeshData.DrawState(
                format,
                totalVertexCount,
                aDraw.indexCount() + (bVertexCount / 4 * 6),
                aDraw.mode(),
                aDraw.indexType()
        );

        return new TrackedMesh(new MeshData(builder.build(), mergedDraw), builder);
    }
}