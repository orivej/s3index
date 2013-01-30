package model

import java.util.zip.Deflater
import java.util.zip.Inflater
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import scala.util.Random
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class Compressor(encryptionKey: String) {
  
  private val asciiSymbols = 'a' to 'z'
  private val numbers = '0' to '9'

  private val key = {
    val factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
    val spec = new PBEKeySpec(encryptionKey.toCharArray(), generateRandomString(4).getBytes(), 1024, 256)
    new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "DES")
  }
  
  private val ivBytes = new IvParameterSpec(generateRandomString(8).getBytes("UTF-8"))

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

  def encrypt(bytesToEncrypt: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, key, ivBytes);
    val encypted = Array.ofDim[Byte](cipher.getOutputSize(bytesToEncrypt.length));
    var numberOfBytesAfterCompression = cipher.update(bytesToEncrypt, 0, bytesToEncrypt.length, encypted, 0);
    numberOfBytesAfterCompression += cipher.doFinal(encypted, numberOfBytesAfterCompression);
    val result: Array[Byte] = Array.ofDim(numberOfBytesAfterCompression)
    System.arraycopy(encypted, 0, result, 0, numberOfBytesAfterCompression)
    result
  }

  def decrypt(bytesToDecrypt: Array[Byte]): Array[Byte] = {
    val cipher = Cipher.getInstance("DES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, key, ivBytes);
    val decrypted = Array.ofDim[Byte](cipher.getOutputSize(bytesToDecrypt.length))
    var numberOfBytesAfterDecryption = cipher.update(bytesToDecrypt, 0, bytesToDecrypt.length, decrypted, 0);
    numberOfBytesAfterDecryption += cipher.doFinal(decrypted, numberOfBytesAfterDecryption);
    val result: Array[Byte] = Array.ofDim(numberOfBytesAfterDecryption)
    System.arraycopy(decrypted, 0, result, 0, numberOfBytesAfterDecryption)
    result
  }

  def compress(stringToCompress: String): Array[Byte] = {
    var returnValues: Array[Byte] = Array.empty;
    compress(stringToCompress.getBytes("UTF-8"));
  }

  def compressAndEncrypt(string: String): Array[Byte] = {
    var returnValues: Array[Byte] = Array.empty;
    encrypt(compress(string.getBytes("UTF-8")));
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

  def decryptAndDecompress(bytes: Array[Byte]): String = {
    val bytesDecompressed: Array[Byte] = decompress(decrypt(bytes))
    new String(bytesDecompressed, 0, bytesDecompressed.length, "UTF-8");
  }
  
  private def generateRandomString(len: Int): String = {
    val res = (0 until len).foldLeft("") {
      (s, i) =>
        val r = math.abs(Random.nextInt() % 8)
        r % 2 match {
          case 0 => s + asciiSymbols(r)
          case 1 => s + numbers(r)
        }
    }
    res
  }

}