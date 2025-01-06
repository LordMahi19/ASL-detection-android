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
        // Load TFLite model
        val model = loadModelFile(context, "model.tflite")
        interpreter = Interpreter(model, Interpreter.Options().apply {
            setNumThreads(4) // Adjust number of threads as necessary
        })

        // Load labels
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
        // Ensure input data is reshaped and normalized
        val inputBuffer = ByteBuffer.allocateDirect(inputData.size * 4).apply {
            order(ByteOrder.nativeOrder())
            for (value in inputData) {
                putFloat(value) // Assume inputData is already scaled appropriately
            }
        }

        // Define the output buffer to match the model's output shape
        val outputBuffer = ByteBuffer.allocateDirect(29 * 4).apply {
            order(ByteOrder.nativeOrder())
        }

        try {
            // Run inference with the model
            interpreter.run(inputBuffer, outputBuffer)
        } catch (e: IllegalArgumentException) {
            Log.e("TFLiteHelper", "Error during inference: ${e.message}")
            throw e // Optional: Re-throw to handle the error in the caller
        }

        // Extract predictions
        outputBuffer.rewind()
        val outputArray = FloatArray(29)
        outputBuffer.asFloatBuffer().get(outputArray)

        // Log input and output for debugging
        Log.d("TFLiteHelper", "Input: ${inputData.joinToString()}")
        Log.d("TFLiteHelper", "Output: ${outputArray.joinToString()}")

        // Find the index of the maximum probability
        return outputArray.indices.maxByOrNull { outputArray[it] } ?: -1
    }

    fun getLabel(index: Int): String {
        return if (index in labels.indices) labels[index] else "Unknown"
    }
}
