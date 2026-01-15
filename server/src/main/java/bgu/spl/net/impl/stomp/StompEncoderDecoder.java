package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.MessageEncoderDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * MessageEncoderDecoder for STOMP protocol.
 * STOMP frames are null-terminated strings (\0).
 */
public class StompEncoderDecoder implements MessageEncoderDecoder<String> {

    private byte[] bytes = new byte[1 << 10]; // Start with 1KB
    private int len = 0;

    @Override
    public String decodeNextByte(byte nextByte) {
        // STOMP frames are terminated with null byte (\0)
        if (nextByte == '\0') {
            return popString();
        }

        pushByte(nextByte);
        return null; // Frame not complete yet
    }

    @Override
    public byte[] encode(String message) {
        // Append null terminator to the message
        return (message + "\0").getBytes(StandardCharsets.UTF_8);
    }

    private void pushByte(byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2); // Double the array size
        }
        bytes[len++] = nextByte;
    }

    private String popString() {
        // Decode the string from UTF-8 bytes
        String result = new String(bytes, 0, len, StandardCharsets.UTF_8);
        len = 0; // Reset for next frame
        return result;
    }
}
