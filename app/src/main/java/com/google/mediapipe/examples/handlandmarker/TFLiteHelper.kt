import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TFLiteHelper(context: Context) {
    private val interpreter: Interpreter
    private val labels: List<String>

    init {
        val model = loadModelFile(context, "model.tflite")
        interpreter = Interpreter(model, Interpreter.Options().apply {
            setNumThreads(4)
        })
        labels = context.assets.open("labels.txt").bufferedReader().useLines { it.toList() }
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun predict(inputData: FloatArray): Int {
        val inputBuffer = ByteBuffer.allocateDirect(inputData.size * 4).apply {
            order(ByteOrder.nativeOrder())
            for (value in inputData) {
                putFloat(value)
            }
        }

        val outputBuffer = ByteBuffer.allocateDirect(29 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val outputArray = FloatArray(29)
        outputBuffer.asFloatBuffer().get(outputArray)

        return outputArray.indices.maxByOrNull { outputArray[it] } ?: -1
    }

    fun getLabel(index: Int): String = if (index in labels.indices) labels[index] else "Unknown"
}