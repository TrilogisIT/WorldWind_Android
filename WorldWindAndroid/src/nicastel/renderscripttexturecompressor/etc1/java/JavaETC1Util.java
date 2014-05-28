package nicastel.renderscripttexturecompressor.etc1.java;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


/**
 * Utility methods for using ETC1 compressed textures.
 *
 */
public class JavaETC1Util {

    /**
     * A utility class encapsulating a compressed ETC1 texture.
     */
    public static class ETC1Texture {
        public ETC1Texture(int width, int height, ByteBuffer data) {
            mWidth = width;
            mHeight = height;
            mData = data;
        }

        /**
         * Get the width of the texture in pixels.
         * @return the width of the texture in pixels.
         */
        public int getWidth() { return mWidth; }

        /**
         * Get the height of the texture in pixels.
         * @return the width of the texture in pixels.
         */
        public int getHeight() { return mHeight; }

        /**
         * Get the compressed data of the texture.
         * @return the texture data.
         */
        public ByteBuffer getData() { return mData; }

        private int mWidth;
        private int mHeight;
        private ByteBuffer mData;
    }

    /**
     * Create a new ETC1Texture from an input stream containing a PKM formatted compressed texture.
     * @param input an input stream containing a PKM formatted compressed texture.
     * @return an ETC1Texture read from the input stream.
     * @throws IOException
     */
    public static ETC1Texture createTexture(InputStream input) throws IOException {
        int width = 0;
        int height = 0;
        byte[] ioBuffer = new byte[4096];
        {
            if (input.read(ioBuffer, 0, JavaETC1.ETC_PKM_HEADER_SIZE) != JavaETC1.ETC_PKM_HEADER_SIZE) {
                throw new IOException("Unable to read PKM file header.");
            }
            ByteBuffer headerBuffer = ByteBuffer.allocateDirect(JavaETC1.ETC_PKM_HEADER_SIZE)
                .order(ByteOrder.nativeOrder());
            headerBuffer.put(ioBuffer, 0, JavaETC1.ETC_PKM_HEADER_SIZE).position(0);
            if (!JavaETC1.isValid(headerBuffer)) {
                throw new IOException("Not a PKM file.");
            }
            width = JavaETC1.getWidth(headerBuffer);
            height = JavaETC1.getHeight(headerBuffer);
        }
        int encodedSize = JavaETC1.getEncodedDataSize(width, height);
        ByteBuffer dataBuffer = ByteBuffer.allocateDirect(encodedSize).order(ByteOrder.nativeOrder());
        for (int i = 0; i < encodedSize; ) {
            int chunkSize = Math.min(ioBuffer.length, encodedSize - i);
            if (input.read(ioBuffer, 0, chunkSize) != chunkSize) {
                throw new IOException("Unable to read PKM file data.");
            }
            dataBuffer.put(ioBuffer, 0, chunkSize);
            i += chunkSize;
        }
        dataBuffer.position(0);
        return new ETC1Texture(width, height, dataBuffer);
    }

    /**
     * Helper function that compresses an image into an ETC1Texture.
     * @param input a native order direct buffer containing the image data
     * @param width the width of the image in pixels
     * @param height the height of the image in pixels
     * @param pixelSize the size of a pixel in bytes (2 or 3)
     * @param stride the width of a line of the image in bytes
     * @return the ETC1 texture.
     */
    public static ETC1Texture compressTexture(Buffer input, int width, int height, int pixelSize, int stride){
        int encodedImageSize = JavaETC1.getEncodedDataSize(width, height);
        System.out.println("encodedImageSize : "+encodedImageSize);
        ByteBuffer compressedImage = ByteBuffer.allocateDirect(encodedImageSize).
            order(ByteOrder.nativeOrder());
        
        // TODO : there is a bug in the android sdk :
        // ETC1.encodeImage((ByteBuffer) input, width, height, 3, stride, compressedImage); should be
        JavaETC1.encodeImage((ByteBuffer) input, width, height, pixelSize, stride, compressedImage);
        
        compressedImage.position(0);
        return new ETC1Texture(width, height, compressedImage);
    }

    /**
     * Helper function that writes an ETC1Texture to an output stream formatted as a PKM file.
     * @param texture the input texture.
     * @param output the stream to write the formatted texture data to.
     * @throws IOException
     */
    public static void writeTexture(ETC1Texture texture, OutputStream output) throws IOException {
        ByteBuffer dataBuffer = texture.getData();
        dataBuffer.rewind();
        System.out.println(dataBuffer.remaining());
        int originalPosition = dataBuffer.position();
        try {
            int width = texture.getWidth();
            int height = texture.getHeight();
            ByteBuffer header = ByteBuffer.allocateDirect(JavaETC1.ETC_PKM_HEADER_SIZE).order(ByteOrder.nativeOrder());
            JavaETC1.formatHeader(header, width, height);
            header.position(0);
            byte[] ioBuffer = new byte[4096];
            header.get(ioBuffer, 0, JavaETC1.ETC_PKM_HEADER_SIZE);
            output.write(ioBuffer, 0, JavaETC1.ETC_PKM_HEADER_SIZE);
            while (dataBuffer.remaining()>0) {
                int chunkSize = Math.min(ioBuffer.length, dataBuffer.remaining());
                dataBuffer.get(ioBuffer, 0, chunkSize);
                output.write(ioBuffer, 0, chunkSize);
            }
        } finally {
            dataBuffer.position(originalPosition);
        }
    }
}
