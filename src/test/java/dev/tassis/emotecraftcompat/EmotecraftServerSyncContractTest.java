package dev.tassis.emotecraftcompat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotecraftServerSyncContractTest {
    private static final String BUILDER = "io/github/kosmx/emotes/common/network/EmotePacket$Builder";

    @Test
    void nativePresenceResponseSendsEveryServerEmoteWithOneMiBLimit() throws IOException {
        MethodNode method = method(
                classNode("io/github/kosmx/emotes/server/network/IServerNetworkInstance"),
                "presenceResponse",
                "()V"
        );

        assertTrue(readsField(
                method,
                "io/github/kosmx/emotes/server/serializer/UniversalEmoteSerializer",
                "serverEmotes"
        ));
        assertTrue(invokes(method, BUILDER, "configureToSaveEmote"));
        assertTrue(loadsConstant(method, 1_048_576));
        assertTrue(invokes(method, BUILDER, "setSizeLimit"));
        assertTrue(invokes(
                method,
                "io/github/kosmx/emotes/server/network/IServerNetworkInstance",
                "sendMessage"
        ));
        assertTrue(callsInOrderWithinLoop(
                method,
                "configureToSaveEmote",
                "setSizeLimit",
                "sendMessage"
        ));
    }

    @Test
    void forgeServerBuildsAClientboundCustomPayloadForEmoteData() throws IOException {
        MethodNode method = method(
                classNode("io/github/kosmx/emotes/forge/network/ServerNetwork"),
                "newS2CEmotesPacket",
                "(Lio/github/kosmx/emotes/common/network/objects/NetData;Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/network/protocol/Packet;"
        );

        assertTrue(readsField(method, "io/github/kosmx/emotes/forge/network/ServerNetwork", "channelID"));
        assertTrue(invokes(method, BUILDER, "setVersion"));
        assertTrue(creates(method, "net/minecraft/network/protocol/game/ClientboundCustomPayloadPacket"));
    }

    private static ClassNode classNode(String internalName) throws IOException {
        String resource = internalName + ".class";
        try (InputStream stream = EmotecraftServerSyncContractTest.class.getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(stream, "Missing classpath resource " + resource);
            ClassNode node = new ClassNode();
            new ClassReader(stream).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }

    private static MethodNode method(ClassNode owner, String name, String descriptor) {
        return owner.methods.stream()
                .filter(method -> method.name.equals(name) && method.desc.equals(descriptor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing method " + owner.name + "." + name + descriptor));
    }

    private static boolean readsField(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.GETSTATIC
                    && field.owner.equals(owner)
                    && field.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean invokes(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode invocation
                    && invocation.owner.equals(owner)
                    && invocation.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean creates(MethodNode method, String type) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof TypeInsnNode typeInstruction
                    && typeInstruction.getOpcode() == Opcodes.NEW
                    && typeInstruction.desc.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean loadsConstant(MethodNode method, int value) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof LdcInsnNode constant && Integer.valueOf(value).equals(constant.cst)) {
                return true;
            }
        }
        return false;
    }

    private static boolean callsInOrderWithinLoop(MethodNode method, String first, String second, String third) {
        int firstIndex = invocationIndex(method, first, 0);
        int secondIndex = invocationIndex(method, second, firstIndex + 1);
        int thirdIndex = invocationIndex(method, third, secondIndex + 1);
        if (firstIndex < 0 || secondIndex < 0 || thirdIndex < 0) {
            return false;
        }

        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof JumpInsnNode jump
                    && method.instructions.indexOf(instruction) > thirdIndex
                    && method.instructions.indexOf(jump.label) < firstIndex) {
                return true;
            }
        }
        return false;
    }

    private static int invocationIndex(MethodNode method, String name, int startIndex) {
        for (int index = Math.max(0, startIndex); index < method.instructions.size(); index++) {
            AbstractInsnNode instruction = method.instructions.get(index);
            if (instruction instanceof MethodInsnNode invocation && invocation.name.equals(name)) {
                return index;
            }
        }
        return -1;
    }
}
