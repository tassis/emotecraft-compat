package dev.tassis.emotecraftcompat.compat;

import io.github.kosmx.emotes.arch.network.EmotePacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class EmotecraftServerSyncCompat {
    static final int MAX_PACKET_SIZE = 32_751;

    private EmotecraftServerSyncCompat() {
    }

    public static void sendServerEmote(IPayloadContext context, ByteBuffer source) {
        for (PacketChunk chunk : createChunks(source)) {
            ByteBuffer payload = ByteBuffer.wrap(chunk.payload());
            if (chunk.stream()) {
                context.reply(EmotePacketPayload.streamPacket(payload));
            } else {
                context.reply(EmotePacketPayload.playPacket(payload));
            }
        }
    }

    static List<PacketChunk> createChunks(ByteBuffer source) {
        ByteBuffer remaining = source.slice();
        int byteCount = remaining.remaining();
        if (byteCount < MAX_PACKET_SIZE) {
            return List.of(new PacketChunk(false, copyBytes(remaining, byteCount)));
        }

        List<PacketChunk> chunks = new ArrayList<>();
        int firstPayloadSize = Math.min(MAX_PACKET_SIZE - Integer.BYTES, remaining.remaining());
        byte[] firstPayload = copyBytes(remaining, firstPayloadSize);
        ByteBuffer firstPacket = ByteBuffer.allocate(Integer.BYTES + firstPayload.length);
        firstPacket.putInt(byteCount).put(firstPayload);
        chunks.add(new PacketChunk(true, firstPacket.array()));

        while (remaining.hasRemaining()) {
            chunks.add(new PacketChunk(
                    true,
                    copyBytes(remaining, Math.min(MAX_PACKET_SIZE, remaining.remaining()))
            ));
        }
        return List.copyOf(chunks);
    }

    private static byte[] copyBytes(ByteBuffer source, int length) {
        byte[] bytes = new byte[length];
        source.get(bytes);
        return bytes;
    }

    record PacketChunk(boolean stream, byte[] payload) {
    }
}
