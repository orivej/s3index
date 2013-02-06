package model

import java.util.zip.Deflater
import java.util.zip.Inflater
import scala.util.Random

class Compressor() {
  
  def compress(bytesToCompress: Array[Byte]): Array[Byte] = {
    val deflater: Deflater = new Deflater();
    deflater.setInput(bytesToCompress);
    deflater.finish();
    val bytesCompressed: Array[Byte] = Array.ofDim(bytesToCompress.size)
    val numberOfBytesAfterCompression = deflater.deflate(bytesCompressed)
    val result: Array[Byte] = Array.ofDim(numberOfBytesAfterCompression)
    System.arraycopy(bytesCompressed, 0, result, 0, numberOfBytesAfterCompression)
    result
  }

  def compress(stringToCompress: String): Array[Byte] = {
    var returnValues: Array[Byte] = Array.empty;
    compress(stringToCompress.getBytes("UTF-8"));
  }

  def decompress(bytesToDecompress: Array[Byte]): Array[Byte] = {
    val inflater: Inflater = new Inflater();
    val numberOfBytesToDecompress = bytesToDecompress.length;
    inflater.setInput(bytesToDecompress, 0, numberOfBytesToDecompress);
    val compressionFactorMaxLikely = 3;
    val bufferSizeInBytes = numberOfBytesToDecompress * compressionFactorMaxLikely;
    val bytesDecompressed: Array[Byte] = Array.ofDim(bufferSizeInBytes);
    val numberOfBytesAfterDecompression = inflater.inflate(bytesDecompressed);
    val result: Array[Byte] = Array.ofDim(numberOfBytesAfterDecompression)
    System.arraycopy(bytesDecompressed, 0, result, 0, numberOfBytesAfterDecompression);
    inflater.end();

    result;
  }

  def decompressToString(bytesToDecompress: Array[Byte]): String = {
    val bytesDecompressed: Array[Byte] = decompress(bytesToDecompress)
    new String(bytesDecompressed, 0, bytesDecompressed.length, "UTF-8");
  }

}