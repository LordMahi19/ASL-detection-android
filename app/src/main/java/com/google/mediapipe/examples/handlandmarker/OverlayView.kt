package com.google.mediapipe.examples.handlandmarker

import TFLiteHelper
import android.content.Context
import android.gesture.Prediction
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import kotlin.math.max
import kotlin.math.min

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    private var results: HandLandmarkerResult? = null
    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var label: String = ""
    private var predictionTextView: TextView? = null
    private val helper by lazy { TFLiteHelper(context!!) }

    private val linePaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 8F
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint().apply {
        color = Color.YELLOW
        strokeWidth = 8F
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 80f
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
    }

    fun clear() {
        results = null
        label = ""
        invalidate()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val dataAux = mutableListOf<Float>()
        val xList = mutableListOf<Float>()
        val yList = mutableListOf<Float>()

        results?.let { handLandmarkerResult ->
            for (landmark in handLandmarkerResult.landmarks()) {
                xList.clear()
                yList.clear()

                // Collect x and y coordinates
                for (normalizedLandmark in landmark) {
                    xList.add(normalizedLandmark.x())
                    yList.add(normalizedLandmark.y())
                }

                // Normalize coordinates relative to the minimum x and y
                val minX = xList.minOrNull() ?: 0f
                val minY = yList.minOrNull() ?: 0f

                // Process landmarks for prediction
                for (normalizedLandmark in landmark) {
                    val x = normalizedLandmark.x() - minX
                    val y = normalizedLandmark.y() - minY
                    dataAux.add(x)
                    dataAux.add(y)

                    // Draw landmark points
                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

                // Draw connections between landmarks
                HandLandmarker.HAND_CONNECTIONS.forEach {
                    canvas.drawLine(
                        landmark[it!!.start()].x() * imageWidth * scaleFactor,
                        landmark[it.start()].y() * imageHeight * scaleFactor,
                        landmark[it.end()].x() * imageWidth * scaleFactor,
                        landmark[it.end()].y() * imageHeight * scaleFactor,
                        linePaint
                    )
                }
            }

            // Perform sign language prediction if we have the correct number of points
            if (dataAux.size == 42) {
                try {
                    val predictedIndex = helper.predict(dataAux.toFloatArray())
                    label = helper.getLabel(predictedIndex)
                    predictionTextView?.post {
                        predictionTextView?.text = label
                    }
                } catch (e: Exception) {
                    Log.e("OverlayView", "Prediction failed: ${e.message}")
                }
            }
        }
    }

    fun setResults(
        handLandmarkerResults: HandLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int
    ) {
        results = handLandmarkerResults
        this.imageHeight = imageHeight
        this.imageWidth = imageWidth
        scaleFactor = max(width * 1f / imageWidth, height * 1f / imageHeight)
        invalidate()
    }

    fun setPredictionTextView(textView: TextView) {
        predictionTextView = textView
    }
}
