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

class OverlayView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {

    private var results: HandLandmarkerResult? = null
    private var linePaint = Paint()
    private var pointPaint = Paint()
    private var textPaint = Paint()

    private var scaleFactor: Float = 1f
    private var imageWidth: Int = 1
    private var imageHeight: Int = 1
    private var label: String = ""
    private var predictionTextView: TextView? = null

    // Lazy initialization for TFLiteHelper
    private val helper by lazy { TFLiteHelper(context!!) }

    init {
        initPaints()
    }

    fun clear() {
        results = null
        label = ""
        invalidate()
    }

    private fun initPaints() {
        linePaint.color = ContextCompat.getColor(context!!, R.color.mp_color_primary)
        linePaint.strokeWidth = LANDMARK_STROKE_WIDTH
        linePaint.style = Paint.Style.STROKE

        pointPaint.color = Color.YELLOW
        pointPaint.strokeWidth = LANDMARK_STROKE_WIDTH
        pointPaint.style = Paint.Style.FILL

//        textPaint.color = Color.RED
//        textPaint.textSize = 60f
//        textPaint.style = Paint.Style.FILL

        textPaint.apply {
            color = Color.WHITE
            textSize = 80f
            style = Paint.Style.FILL
            textAlign = Paint.Align.CENTER

            strokeWidth = 8f
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
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

                for (normalizedLandmark in landmark) {
                    xList.add(normalizedLandmark.x())
                    yList.add(normalizedLandmark.y())
                }

                val minX = xList.minOrNull() ?: 0f
                val minY = yList.minOrNull() ?: 0f

                for (normalizedLandmark in landmark) {
                    val x = (normalizedLandmark.x() - minX)
                    val y = (normalizedLandmark.y() - minY)
                    dataAux.add(x)
                    dataAux.add(y)

                    canvas.drawPoint(
                        normalizedLandmark.x() * imageWidth * scaleFactor,
                        normalizedLandmark.y() * imageHeight * scaleFactor,
                        pointPaint
                    )
                }

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

            // Perform prediction if dataAux is valid
            if (dataAux.size == 42) {
                try {
                    val predictedIndex = helper.predict(dataAux.toFloatArray())
                    label = helper.getLabel(predictedIndex)
                    Log.d("OverlayView", "Predicted Label: $label")
                    postInvalidate()
                } catch (e: Exception) {
                    Log.e("OverlayView", "Prediction failed: ${e.message}")
                }
            }
        }

        if (label.isNotEmpty()) {
            predictionTextView?.post {
                predictionTextView?.text = label
            }

//            textPaint.style = Paint.Style.FILL
//            textPaint.color = Color.BLACK
//            canvas.drawText(
//                label,
//                width / 2f,
//                100f,
//                textPaint
//            )
//
//            textPaint.style = Paint.Style.FILL
//            textPaint.color = Color.WHITE
//            canvas.drawText(
//                label,
//                width / 2f,
//                100f,
//                textPaint
//            )
        }
    }

    fun setResults(
        handLandmarkerResults: HandLandmarkerResult,
        imageHeight: Int,
        imageWidth: Int,
        runningMode: RunningMode = RunningMode.IMAGE
    ) {
        results = handLandmarkerResults

        this.imageHeight = imageHeight
        this.imageWidth = imageWidth

        scaleFactor = when (runningMode) {
            RunningMode.IMAGE, RunningMode.VIDEO -> min(width * 1f / imageWidth, height * 1f / imageHeight)
            RunningMode.LIVE_STREAM -> max(width * 1f / imageWidth, height * 1f / imageHeight)
        }
        invalidate()
    }

    fun setPredictionTextView(textView: TextView) {
        predictionTextView = textView
    }

    companion object {
        private const val LANDMARK_STROKE_WIDTH = 8F
    }
}
