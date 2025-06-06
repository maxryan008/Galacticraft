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

    // Store full builder to preserve native memory
    public static final class MeshDataCopy {
        private final ByteBufferBuilder builder;
        private final ByteBufferBuilder.Result result;
        private final MeshData.DrawState drawState;

        public MeshDataCopy(ByteBufferBuilder builder, ByteBufferBuilder.Result result, MeshData.DrawState drawState) {
            this.builder = builder;
            this.result = result;
            this.drawState = drawState;
        }

        public MeshData cloneMeshData() {
            ByteBuffer original = result.byteBuffer();
            int size = original.remaining();

            ByteBufferBuilder copyBuilder = new ByteBufferBuilder(size);
            long dest = copyBuilder.reserve(size);
            MemoryUtil.memCopy(MemoryUtil.memAddress(original), dest, size);

            ByteBufferBuilder.Result copyResult = copyBuilder.build();
            return new MeshData(copyResult, drawState);
        }

        public MeshData.DrawState drawState() {
            return drawState;
        }

        public ByteBufferBuilder.Result result() {
            return result;
        }
    }

    private static final Map<BlockPos, MeshDataCopy> COMPILED_TRANSLUCENT_MESH_BUFFERS = new ConcurrentHashMap<>();

    public static void storeOrRemove(BlockPos origin, MeshData mesh) {
        if (mesh == null) {
            COMPILED_TRANSLUCENT_MESH_BUFFERS.remove(origin);
            return;
        }
        var original = mesh.vertexBuffer();
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

        ByteBufferBuilder.Result result = builder.build();

        COMPILED_TRANSLUCENT_MESH_BUFFERS.put(origin.immutable(), new MeshDataCopy(builder, result, drawState));
    }

    public static MeshDataCopy get(BlockPos origin) {
        return COMPILED_TRANSLUCENT_MESH_BUFFERS.get(origin);
    }

    public static void clear(BlockPos origin) {
        COMPILED_TRANSLUCENT_MESH_BUFFERS.remove(origin);
    }

    public static MeshData mergeMeshes(DynamicFluidRenderer.MeshDataCopy a, MeshData b) {
        if (a == null) return b;

        MeshData aMesh = a.cloneMeshData();
        MeshData.DrawState aDraw = a.drawState();
        MeshData.DrawState bDraw = b.drawState();

        // --- Sanity checks ---
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

        if (aBuf.remaining() < aBytes) throw new IllegalStateException("aBuf has fewer bytes than expected!");
        if (bBuf.remaining() < bBytes) throw new IllegalStateException("bBuf has fewer bytes than expected!");

        // --- Allocate destination buffer ---
        ByteBufferBuilder builder = new ByteBufferBuilder(aBytes + bBytes);
        long dest = builder.reserve(aBytes + bBytes);

        // --- Copy just the vertex bytes ---
        MemoryUtil.memCopy(MemoryUtil.memAddress(aBuf), dest, aBytes);
        MemoryUtil.memCopy(MemoryUtil.memAddress(bBuf), dest + aBytes, bBytes);

        // --- Create merged MeshData ---
        MeshData.DrawState mergedDraw = new MeshData.DrawState(
                format,
                totalVertexCount,
                aDraw.indexCount() + (bVertexCount / 4 * 6),
                aDraw.mode(),
                aDraw.indexType()
        );

        return new MeshData(builder.build(), mergedDraw);
    }
}