package dev.tassis.emotecraftcompat.compat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotecraftServerSyncCompatTest {
    @Test
    void smallDefinitionUsesOnePlayPacketWithoutMutatingTheSource() {
        byte[] bytes = bytes(128);
        ByteBuffer source = ByteBuffer.wrap(bytes);
        source.position(7);

        List<EmotecraftServerSyncCompat.PacketChunk> chunks = EmotecraftServerSyncCompat.createChunks(source);

        assertEquals(7, source.position());
        assertEquals(1, chunks.size());
        assertFalse(chunks.getFirst().stream());
        assertArrayEquals(slice(bytes, 7), chunks.getFirst().payload());
    }

    @Test
    void packetLimitStartsAStreamWithoutOverwritingDefinitionBytes() {
        assertStreamRoundTrip(EmotecraftServerSyncCompat.MAX_PACKET_SIZE, 2);
    }

    @Test
    void travelerEchoesDefinitionSizeRoundTripsAcrossThreePackets() {
        assertStreamRoundTrip(79_716, 3);
    }

    @Test
    void largestPlayPacketStaysOnThePlayChannel() {
        byte[] bytes = bytes(EmotecraftServerSyncCompat.MAX_PACKET_SIZE - 1);

        List<EmotecraftServerSyncCompat.PacketChunk> chunks =
                EmotecraftServerSyncCompat.createChunks(ByteBuffer.wrap(bytes));

        assertEquals(1, chunks.size());
        assertFalse(chunks.getFirst().stream());
        assertArrayEquals(bytes, chunks.getFirst().payload());
    }

    private static void assertStreamRoundTrip(int size, int expectedChunks) {
        byte[] bytes = bytes(size);
        List<EmotecraftServerSyncCompat.PacketChunk> chunks =
                EmotecraftServerSyncCompat.createChunks(ByteBuffer.wrap(bytes));

        assertEquals(expectedChunks, chunks.size());
        assertTrue(chunks.stream().allMatch(EmotecraftServerSyncCompat.PacketChunk::stream));
        assertTrue(chunks.stream().allMatch(chunk ->
                chunk.payload().length <= EmotecraftServerSyncCompat.MAX_PACKET_SIZE
        ));

        ByteBuffer first = ByteBuffer.wrap(chunks.getFirst().payload());
        assertEquals(size, first.getInt());
        ByteArrayOutputStream reconstructed = new ByteArrayOutputStream(size);
        reconstructed.writeBytes(readRemaining(first));
        for (int index = 1; index < chunks.size(); index++) {
            reconstructed.writeBytes(chunks.get(index).payload());
        }
        assertArrayEquals(bytes, reconstructed.toByteArray());
    }

    private static byte[] bytes(int size) {
        byte[] bytes = new byte[size];
        for (int index = 0; index < size; index++) {
            bytes[index] = (byte) (index * 31 + 17);
        }
        return bytes;
    }

    private static byte[] slice(byte[] bytes, int start) {
        byte[] slice = new byte[bytes.length - start];
        System.arraycopy(bytes, start, slice, 0, slice.length);
        return slice;
    }

    private static byte[] readRemaining(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }
}
